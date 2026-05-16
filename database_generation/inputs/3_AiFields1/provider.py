import os
import sqlite3
import json
import sys
from typing import Dict, Any, Iterator, Tuple

# Add current directory to sys.path to allow importing local modules
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from ai_logic import call_llm_api, generate_prompt
from base_provider import Provider, ProviderType

# Add parent directory to sys.path to access root modules like conf
parent_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)
import conf

class AiFieldsProvider(Provider):
    def _get_cache_conn(self):
        cache_db = os.path.join(os.path.dirname(__file__), "ai_fields_cache.db")
        conn = sqlite3.connect(cache_db)
        cursor = conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS `chinese_word` (
                            `simplified` TEXT NOT NULL,
                            `definition` TEXT NOT NULL,
                            `modality` TEXT DEFAULT 'N/A',
                            `examples` TEXT DEFAULT '',
                            `type` TEXT DEFAULT 'N/A',
                            `synonyms` TEXT DEFAULT '',
                            `antonym` TEXT DEFAULT '',
                            PRIMARY KEY(`simplified`)
                        )''')
        conn.commit()
        return conn

    def update(self):
        """Fetches missing AI fields from the LLM and stores them in the local cache DB."""
        words_to_process = []
        for hsk_file in conf.HSK_FILES:
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
        
        batch_size = conf.BATCH_SIZE
        for i in range(0, len(missing_words), batch_size):
            batch = missing_words[i:i + batch_size]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'definition', 'examples', 'modality', 'type', 'synonyms', 'antonym']
            ai_results = call_llm_api(conf.API_ENDPOINT, conf.MODEL_NAME, prompt, required_fields)
            
            if ai_results:
                for res in ai_results:
                    word = res.pop('word')
                    res['definition'] = json.dumps({conf.DEFINITION_AI_LOCALE: res['definition']}, ensure_ascii=False)
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
            yield ("chinese_word", record)
        conn.close()
