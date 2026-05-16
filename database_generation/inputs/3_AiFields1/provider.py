import os
import sqlite3
import json
import sys
from typing import Dict, Any, Iterator, Tuple

# Local import for ai_logic
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)
from ai_logic import call_llm_api, generate_prompt

from lib import Provider, ProviderType, BATCH_SIZE, API_ENDPOINT, MODEL_NAME, DEFINITION_AI_LOCALE, HSK_FILES

class AiFieldsProvider(Provider):
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
            print("AiFieldsProvider: All words already cached.")
            conn.close()
            return

        print(f"AiFieldsProvider: Found {len(missing_words)} words missing from cache. Starting LLM updates...")
        
        for i in range(0, len(missing_words), BATCH_SIZE):
            batch = missing_words[i:i + BATCH_SIZE]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'definition', 'examples', 'modality', 'type', 'synonyms', 'antonym']
            ai_results = call_llm_api(API_ENDPOINT, MODEL_NAME, prompt, required_fields)
            
            if ai_results:
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
                print(f"AiFieldsProvider: Progress {i + len(batch)}/{len(missing_words)}")
            else:
                print(f"AiFieldsProvider: Failed to get results for batch starting with {batch[0]}")
                
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
