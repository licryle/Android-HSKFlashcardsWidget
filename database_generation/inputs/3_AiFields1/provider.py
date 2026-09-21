import os
import sqlite3
import json
import sys
import time
import logging
from typing import Dict, Any, Iterator, Tuple, List

from lib.utils_ai import call_llm_api
from lib import Provider, ProviderType, BATCH_SIZE, API_ENDPOINT, MODEL_NAME, DEFINITION_AI_LOCALE, HSK_FILES, load_u8_words
from lib.conf import CEDICT_FILE

def generate_prompt(words: List[str]) -> str:
    return f"""<|system|>
You are a precise simplified Chinese language assistant helping learners learn using mostly HSK3 simplified chinese vocabulary. You MUST return a valid JSON array of objects.
<|user|>
Analyze the following Chinese words. For each word, return an object with these fields:

1. "word": The original simplified chinese word FROM THE LIST BELOW. Do NOT change it to traditional characters or a different word.
2. "examples": One example sentence per definition, separated with \\n. Use HSK3 simplificed chinese vocabulary.
3. "modality": EXACTLY ONE of ["ORAL", "WRITTEN", "ORAL_WRITTEN", "N/A"].
4. "type": EXACTLY ONE of ["NOUN", "VERB", "ADJECTIVE", "ADVERB", "CONJUNCTION", "PREPOSITION", "INTERJECTION", "IDIOM", "N/A"]. If a word has multiple types, choose the most common one.
5. "synonyms": Comma-separated simplified Chinese words (or empty string).
6. "antonym": Closest antonym in simplified Chinese (or empty string).

CRITICAL: 
- Output MUST be a valid JSON array. 
- Use ONLY SIMPLIFIED CHINESE. Do NOT return traditional characters.
- No markdown formatting (no ```json).
- Fields "modality" and "type" must be a single string from the allowed list, NOT a list or multiple strings.

Words to analyze: {', '.join(words)}

Expected format:
[
  {{
    "word": "example",
    "examples": "ex1\\nex2",
    "modality": "ORAL_WRITTEN",
    "type": "NOUN",
    "synonyms": "syn1, syn2",
    "antonym": "ant1"
  }}
]
"""

class AiFieldsProvider(Provider):
    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def _get_cache_conn(self):
        cache_db = os.path.join(os.path.dirname(__file__), "ai_fields_cache.db")
        conn = sqlite3.connect(cache_db)
        return conn

    def update(self):
        """Fetches missing AI fields from the LLM and stores them in the local cache DB."""
        words_to_process = load_u8_words(CEDICT_FILE)

        conn = self._get_cache_conn()
        cursor = conn.cursor()
        
        # Check against metadata cache
        cursor.execute("SELECT simplified FROM chinese_word")
        cached_words = {row[0] for row in cursor.fetchall()}
        
        missing_words = [w for w in words_to_process if w not in cached_words]
        
        if not missing_words:
            self.logger.info("AiFieldsProvider: All words already cached.")
            conn.close()
            return

        self.logger.info(f"AiFieldsProvider: Found {len(missing_words)} words missing from cache. Starting LLM updates...")
        
        for i in range(0, len(missing_words), BATCH_SIZE):
            batch = missing_words[i:i + BATCH_SIZE]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'examples', 'modality', 'type', 'synonyms', 'antonym']
            
            ai_results = call_llm_api(API_ENDPOINT, MODEL_NAME, prompt, required_fields)
            
            if ai_results:
                for res in ai_results:
                    raw_word = res.pop('word', None)
                    if not raw_word: continue
                    
                    word = raw_word.strip()

                    # Strict Validation: The word MUST be in our current batch.
                    # This prevents traditional characters or garbage from being cached.
                    if word not in batch:
                        # Try to find the closest match in the batch (case-insensitive and trimmed)
                        match = None
                        for w in batch:
                            if w.strip() == word:
                                match = w
                                break
                        
                        if match:
                            word = match
                        else:
                            self.logger.warning(f"AiFieldsProvider: Discarding AI result for '{word}' - not in requested batch.")
                            continue
                    
                    # Strict validation: collocation and examples cannot contain non-Chinese characters (e.g., Latin letters)
                    import re
                    if (res.get('collocation') and re.search(r'[a-zA-Z]', res.get('collocation'))) or (res.get('examples') and re.search(r'[a-zA-Z]', res['examples'])):
                        self.logger.warning(f"AiFieldsProvider: Discarding AI result for '{word}' - definition or examples contain non-Chinese characters.")
                        continue
                    
                    # Convert empty strings to None (NULL) for metadata
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
                self.logger.warning(f"AiFieldsProvider: Failed to get results for batch starting with {batch[0]}. Skipping.")
            
            # Sleep to avoid rate limiting
            time.sleep(1)
                
        conn.close()

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["examples", "modality", "type", "synonyms", "antonym"],
                "index": "simplified"
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        cache_db = os.path.join(os.path.dirname(__file__), "ai_fields_cache.db")
        if not os.path.exists(cache_db):
            return

        words_to_process = load_u8_words(CEDICT_FILE)
        allowed_words = set(words_to_process)

        conn = sqlite3.connect(cache_db)
        cursor = conn.cursor()
        
        # Metadata
        cursor.execute("SELECT * FROM chinese_word")
        columns = [desc[0] for desc in cursor.description]
        for row in cursor.fetchall():
            record = dict(zip(columns, row))
            # Final validation: only yield if word is in our base dictionary
            if record['simplified'] in allowed_words:
                yield ("chinese_word", record)
            
        conn.close()
