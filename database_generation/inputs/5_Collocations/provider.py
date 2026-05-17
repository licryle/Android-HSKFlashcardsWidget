import os
import sqlite3
import sys
import time
import logging
from typing import Dict, Any, Iterator, Tuple, List

from lib.utils_ai import call_llm_api
from lib import Provider, ProviderType, BATCH_SIZE, API_ENDPOINT, MODEL_NAME, HSK_FILES, COLLOCATIONS_CACHE_DB

def generate_prompt(words: List[str]) -> str:
    return f"""<|system|>
You are a precise Chinese language assistant. You MUST return a valid JSON array of objects.
<|user|>
For each of the following Chinese words, provide a list of up to 5 most common collocations (habitual co-occurrence of words).
The collocations should be ordered by popularity/frequency descending.

CRITICAL: 
- Output MUST be a valid JSON array. 
- No trailing commas in objects.
- No markdown formatting (no ```json).
- The field "collocations" must be a SINGLE STRING, each value separated with \\n.

Words to analyze: {', '.join(words)}

Expected format:
[
  {{
    "word": "做",
    "collocations": "做决定\\n做饭\\n做功课\\n做生意\\n做运动"
  }}
]
"""

class CollocationsProvider(Provider):
    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def _get_cache_conn(self):
        os.makedirs(os.path.dirname(COLLOCATIONS_CACHE_DB), exist_ok=True)
        conn = sqlite3.connect(COLLOCATIONS_CACHE_DB)
        cursor = conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS `chinese_word` (
                            `simplified` TEXT NOT NULL,
                            `collocations` TEXT,
                            PRIMARY KEY(`simplified`)
                        )''')
        conn.commit()
        return conn

    def update(self):
        """Fetches missing collocations from the LLM and stores them in the local cache DB."""
        words_to_process = []
        for hsk_file in HSK_FILES:
            if os.path.exists(hsk_file):
                with open(hsk_file, 'r', encoding='utf-8') as f:
                    words_to_process.extend([line.strip() for line in f if line.strip()])

        conn = self._get_cache_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT simplified FROM chinese_word WHERE collocations IS NOT NULL")
        cached_words = {row[0] for row in cursor.fetchall()}
        
        missing_words = [w for w in words_to_process if w not in cached_words]
        
        if not missing_words:
            self.logger.info("CollocationsProvider: All words already cached.")
            conn.close()
            return

        self.logger.info(f"CollocationsProvider: Found {len(missing_words)} words missing from cache. Starting LLM updates...")
        
        for i in range(0, len(missing_words), BATCH_SIZE):
            batch = missing_words[i:i + BATCH_SIZE]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'collocations']
            
            ai_results = call_llm_api(API_ENDPOINT, MODEL_NAME, prompt, required_fields)
            if ai_results:
                for res in ai_results:
                    word = res.get('word')
                    collocations = res.get('collocations')
                    if word:
                        cursor.execute(f"INSERT OR REPLACE INTO chinese_word (simplified, collocations) VALUES (?, ?)", (word, collocations))
                conn.commit()
                self.logger.info(f"CollocationsProvider: Progress {i + len(batch)}/{len(missing_words)}")
            else:
                self.logger.warning(f"CollocationsProvider: Failed to get results for batch starting with {batch[0]}. Skipping.")
            
            time.sleep(1)
                
        conn.close()

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["collocations"],
                "index": "simplified"
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        if not os.path.exists(COLLOCATIONS_CACHE_DB):
            return

        conn = sqlite3.connect(COLLOCATIONS_CACHE_DB)
        cursor = conn.cursor()
        cursor.execute("SELECT simplified, collocations FROM chinese_word")
        for row in cursor.fetchall():
            yield ("chinese_word", {"simplified": row[0], "collocations": row[1]})
        conn.close()
