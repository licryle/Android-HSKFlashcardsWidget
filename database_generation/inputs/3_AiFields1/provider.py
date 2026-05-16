import os
import sqlite3
import json
import sys
import time
import logging
from typing import Dict, Any, Iterator, Tuple

# Local import for ai_logic
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
from ai_logic import call_llm_api, generate_prompt

from lib import Provider, ProviderType, BATCH_SIZE, API_ENDPOINT, MODEL_NAME, DEFINITION_AI_LOCALE, HSK_FILES, MAX_CONSECUTIVE_FAILURES

class AiFieldsProvider(Provider):
    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def _get_cache_conn(self):
        cache_db = os.path.join(os.path.dirname(__file__), "ai_fields_cache.db")
        conn = sqlite3.connect(cache_db)
        cursor = conn.cursor()
        # Removed DEFAULT values to ensure we only store what we get, allowing NULLs
        cursor.execute('''CREATE TABLE IF NOT EXISTS `chinese_word` (
                            `simplified` TEXT NOT NULL,
                            `definition` TEXT NOT NULL,
                            `modality` TEXT,
                            `examples` TEXT,
                            `type` TEXT,
                            `synonyms` TEXT,
                            `antonym` TEXT,
                            PRIMARY KEY(`simplified`)
                        )''')
        conn.commit()
        return conn

    def update(self):
        """Fetches missing AI fields from the LLM and stores them in the local cache DB."""
        words_to_process = []
        for hsk_file in HSK_FILES:
            if os.path.exists(hsk_file):
                with open(hsk_file, 'r', encoding='utf-8') as f:
                    words_to_process.extend([line.strip() for line in f if line.strip()])

        conn = self._get_cache_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT simplified FROM chinese_word")
        cached_words = {row[0] for row in cursor.fetchall()}
        
        missing_words = [w for w in words_to_process if w not in cached_words]
        
        if not missing_words:
            self.logger.info("AiFieldsProvider: All words already cached.")
            conn.close()
            return

        self.logger.info(f"AiFieldsProvider: Found {len(missing_words)} words missing from cache. Starting LLM updates...")
        
        consecutive_failures = 0
        for i in range(0, len(missing_words), BATCH_SIZE):
            batch = missing_words[i:i + BATCH_SIZE]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'definition', 'examples', 'modality', 'type', 'synonyms', 'antonym']
            ai_results = call_llm_api(API_ENDPOINT, MODEL_NAME, prompt, required_fields)
            
            if ai_results:
                consecutive_failures = 0
                for res in ai_results:
                    word = res.pop('word')
                    # Wrap definition in the expected JSON locale map
                    res['definition'] = json.dumps({DEFINITION_AI_LOCALE: res['definition']}, ensure_ascii=False)
                    
                    # Convert empty strings to None (NULL) for better database state
                    for key in res:
                        if isinstance(res[key], str) and not res[key].strip():
                            res[key] = None

                    cols = ['simplified'] + list(res.keys())
                    placeholders = ', '.join(['?'] * len(cols))
                    vals = [word] + list(res.values())
                    cursor.execute(f"INSERT OR REPLACE INTO chinese_word ({', '.join(cols)}) VALUES ({placeholders})", vals)
                conn.commit()
                self.logger.info(f"AiFieldsProvider: Progress {i + len(batch)}/{len(missing_words)}")
            else:
                consecutive_failures += 1
                self.logger.warning(f"AiFieldsProvider: Failed to get results for batch starting with {batch[0]} (consecutive failures: {consecutive_failures})")
                if consecutive_failures >= MAX_CONSECUTIVE_FAILURES:
                    conn.close()
                    raise RuntimeError(f"AiFieldsProvider: Exiting after {MAX_CONSECUTIVE_FAILURES} consecutive failures")
            
            # Sleep to avoid rate limiting
            time.sleep(1)
                
        conn.close()

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["definition", "examples", "modality", "type", "synonyms", "antonym"],
                "index": "simplified"
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        cache_db = os.path.join(os.path.dirname(__file__), "ai_fields_cache.db")
        if not os.path.exists(cache_db):
            return

        conn = sqlite3.connect(cache_db)
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM chinese_word")
        columns = [desc[0] for desc in cursor.description]
        for row in cursor.fetchall():
            record = dict(zip(columns, row))
            # Clean up: ensure empty fields from cache are also yielded as None
            # (In case old cache data had empty strings)
            for k, v in record.items():
                if k != "simplified" and v == "":
                    record[k] = None
            yield ("chinese_word", record)
        conn.close()
