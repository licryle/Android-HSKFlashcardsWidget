import sqlite3
import json
import requests
from typing import List, Dict, Optional
import time
import os
import argparse
import logging
from datetime import datetime
import re  #

from conf import *

def setup_logging():
    """Setup logging configuration."""
    # Create logs directory if it doesn't exist
    os.makedirs('logs', exist_ok=True)
    
    # Create a log file with timestamp
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    log_file = f'logs/dict_gen_ai_{timestamp}.log'
    
    # Configure logging
    logging.basicConfig(
        level=logging.DEBUG,
        format='%(asctime)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file, encoding='utf-8'),
            logging.StreamHandler()  # This will still show errors in console
        ]
    )
    
    # Set console handler to show INFO and above
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)  # Changed to INFO to show all messages
    console_handler.setFormatter(logging.Formatter('%(levelname)s: %(message)s'))  # Show level for better visibility
    
    # Remove default stream handler and add our custom one
    root_logger = logging.getLogger()
    root_logger.handlers = [logging.FileHandler(log_file, encoding='utf-8'), console_handler]
    
    print(f"Log file created at: {os.path.abspath(log_file)}")  # Print the absolute path to the log file
    
    return log_file

def get_words_without_definitions(cursor, limit: int, where_clause) -> List[str]:
    """Get words that don't have HSK3 definitions yet, prioritizing HSK words."""
    # First, get HSK words without definitions
    cursor.execute('''
        SELECT simplified
        FROM chinese_word
        WHERE examples IS NULL AND hsk_level != 'NOT_HSK'
        LIMIT ?
    ''', (limit,))
    hsk_words = [row[0] for row in cursor.fetchall()]
    
    # If there are not enough HSK words, get non-HSK words to fill the batch
    if len(hsk_words) < limit:
        remaining = limit - len(hsk_words)
        cursor.execute('''
            SELECT simplified
            FROM chinese_word
            WHERE examples IS NULL AND ?
            LIMIT ?
        ''', (where_clause,remaining))
        non_hsk_words = [row[0] for row in cursor.fetchall()]
        hsk_words.extend(non_hsk_words)
        
    return hsk_words

def generate_prompt(words: List[str]) -> str:
    return f"""<|system|>
You are a precise and concise Chinese language assistant trained to teach learners using HSK3 vocabulary. Your task is to analyze Chinese words and return a JSON array with structured information. The output must follow exact formatting rules and vocabulary limits.
<|user|>
Analyze the following Chinese words and provide the following for each one:

1. "word": The original word
2. "definition": HSK3-level definitions (multiple if needed), each starting on a new line. Use HSK4 vocabulary only if no HSK3 word exists. Never define a word using the word itself.
3. "examples": One example sentence per definition, on a new line, using only HSK3 vocabulary if possible. Sentences must match definitions in order.
4. "modality": One of ["ORAL", "WRITTEN", "ORAL_WRITTEN", "N/A"]
5. "type": One of  ["NOUN", "VERB", "ADJECTIVE", "ADVERB", "CONJUNCTION", "PREPOSITION", "INTERJECTION",  "IDIOM", "N/A"]
6. "synonyms": Comma-separated simplified Chinese words (or empty string)
7. "antonym": Closest antonym in simplified Chinese (or empty string)

Only return a valid JSON array of objects. Do not include explanations or markdown.

Words to analyze:
{', '.join(words)}

Expected format:
[
  {{
    "word": "example_word",
    "definition": "meaning one\nmeaning two",
    "examples": "example one\nexample two",
    "modality": "ORAL_WRITTEN",
    "type": "VERB",
    "synonyms": "近义词1, 近义词2",
    "antonym": "反义词"
  }},
  ...
]
"""

def escape_newlines_in_json_strings(s: str) -> str:
    """Escape newlines inside double-quoted JSON string values with \\n."""
    # This regex finds newlines inside double-quoted strings and replaces them with \\n
    def replacer(match):
        # Replace literal newlines with \n inside the matched string
        return match.group(0).replace('\n', '\\n').replace('\r', '\\n').replace('\r\\n', '\\n').replace('\n', '\\n').replace('\r', '\\n').replace('\r\n', '\\n').replace('\n', '\\n').replace('\r', '\\n')
    # Only replace newlines inside double quotes
    return re.sub(r'"(.*?)(?<!\\)"', lambda m: m.group(0).replace('\n', '\\n').replace('\r', '\\n'), s, flags=re.DOTALL)

def clean_json_string(s: str) -> str:
    """Clean the JSON string by removing any potential problematic characters and escaping newlines in string values."""
    # Remove any BOM or special characters at the start
    s = s.strip()
    if s.startswith('\ufeff'):
        s = s[1:]
    
    # Find the first [ and last ]
    start = s.find('[')
    end = s.rfind(']')
    if start != -1 and end != -1:
        s = s[start:end+1]
    # Escape newlines in JSON string values
    s = escape_newlines_in_json_strings(s)
    return s

def parse_json_permissive(s: str) -> Optional[List[Dict]]:
    """Try to parse JSON string, handling truncated responses by finding valid entries."""
    try:
        # First try normal parsing
        return json.loads(s)
    except json.JSONDecodeError as e:
        logging.debug(f"Initial JSON parsing failed, trying permissive parsing: {e}")
        
        # Find all complete JSON objects in the string
        valid_entries = []
        current_pos = 0
        depth = 0
        start_pos = None
        
        for i, char in enumerate(s):
            if char == '{':
                if depth == 0:
                    start_pos = i
                depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0 and start_pos is not None:
                    # Try to parse this object
                    try:
                        obj_str = s[start_pos:i+1]
                        obj = json.loads(obj_str)
                        if isinstance(obj, dict) and all(k in obj for k in REQUIRED_AI_FIELDS):
                            valid_entries.append(obj)
                    except json.JSONDecodeError:
                        logging.debug(f"Failed to parse object at position {start_pos}")
                    start_pos = None
        
        if valid_entries:
            logging.debug(f"Successfully parsed {len(valid_entries)} valid entries out of potentially truncated response")
            return valid_entries
        return None

def call_llm_api(prompt: str) -> Optional[List[Dict]]:
    """Call the LLM API with the prompt and return the response."""
    headers = {
        'Content-Type': 'application/json'
    }
    
    data = {
        'model': MODEL_NAME,
        'messages': [
            {
                'role': 'user',
                'content': prompt
            }
        ],
        'temperature': 0.7,
        'max_tokens': 2000,
        'stream': False
    }
    
    try:
        response = requests.post(API_ENDPOINT, headers=headers, json=data)
        response.raise_for_status()
        result = response.json()
        
        # Debug log the raw response
        logging.debug("Raw API response: %s", result)
        
        # Check if we have the expected structure
        if 'choices' not in result or not result['choices']:
            logging.error("API response missing 'choices' field")
            return None
            
        if 'message' not in result['choices'][0]:
            logging.error("API response missing 'message' field in choices")
            return None
            
        if 'content' not in result['choices'][0]['message']:
            logging.error("API response missing 'content' field in message")
            return None
            
        response_text = result['choices'][0]['message']['content']
        
        # Debug log the content before parsing
        logging.debug("Response content: %s", response_text)
        
        try:
            # Clean the response text
            cleaned_text = clean_json_string(response_text)
            logging.debug("Cleaned response: %s", cleaned_text)
            
            # Try permissive parsing
            parsed_response = parse_json_permissive(cleaned_text)
            if parsed_response is None:
                logging.error("Failed to parse any valid entries from response")
                return None
                
            if not isinstance(parsed_response, list):
                logging.error("API response is not a list")
                return None
                
            logging.info("Successfully parsed %d entries", len(parsed_response))
            return parsed_response
            
        except Exception as e:
            logging.error("Error in parsing process: %s", e)
            logging.debug("Raw response text: %s", response_text)
            logging.debug("Cleaned response text: %s", cleaned_text)
            return None
            
    except requests.exceptions.RequestException as e:
        logging.error("Network error calling API: %s", e)
        return None
    except json.JSONDecodeError as e:
        logging.error("Error parsing API response as JSON: %s", e)
        logging.debug("Raw response: %s", response.text if 'response' in locals() else "No response")
        return None
    except Exception as e:
        logging.error("Unexpected error calling API: %s", e)
        return None

def add_definition_to_json_map(json_string, new_key, new_value):
    """
    Adds or updates a key-value pair in a JSON map string.

    Args:
        json_string (str): The JSON string representing a dictionary.
        new_key (str): The key to add or update.
        new_value (str): The value to associate with the key.

    Returns:
        str: The updated JSON string with the new key-value pair.
    """
    try:
        definition_map = json.loads(json_string)
        if not isinstance(definition_map, dict):
            raise ValueError("JSON is not a dictionary")
    except (json.JSONDecodeError, ValueError):
        definition_map = {}

    definition_map[new_key] = new_value
    return json.dumps(definition_map, ensure_ascii=False)

def update_word_ai_data(cursor, ai_data: List[Dict]):
    """Update the database with the new ai_data."""

    # Create a dictionary mapping simplified words to ai_data
    cursor.execute('SELECT simplified, definition FROM chinese_word')
    rows = cursor.fetchall()
    local_defs = {simplified: definition for simplified, definition in rows}

    for def_data in ai_data:
        try:
            # Validate all required fields exist
            missing_fields = [field for field in REQUIRED_AI_FIELDS if field not in def_data]
            if missing_fields:
                logging.error(f"Missing required fields for word {def_data.get('word', 'UNKNOWN')}: {missing_fields}")
                continue

            # Validate and map modality
            modality = def_data['modality'].strip()
            mapped_modality = MODALITY_MAPPING.get(modality, "N/A")
            if mapped_modality == "N/A" and modality != "N/A":
                logging.warning(f"Unknown modality '{modality}' for word {def_data['word']}, defaulting to 'N/A'")
                
            # Validate and map type
            word_type = def_data['type'].strip()
            mapped_type = TYPE_MAPPING.get(word_type, "N/A")
            if mapped_type == "N/A" and word_type != "N/A":
                logging.warning(f"Unknown type '{word_type}' for word {def_data['word']}, defaulting to 'N/A'")
            
            # Ensure all fields are strings
            for field in AI_COLUMNS.keys():
                if field in def_data:
                    def_data[field] = str(def_data[field]).strip()
                else:
                    def_data[field] = ""  # Default to empty string if missing

            # Add hsk3 definition to existing set, or replace
            definition = add_definition_to_json_map(local_defs[def_data['word']], DEFINITION_AI_LOCALE, def_data['definition'])
            if DEFINITION_AI_LOCALE not in definition:
                logging.error(f"Failed to add definition in HSK3 for word {def_data.get('word', 'UNKNOWN')}")
                continue
            
            # Build the SET clause dynamically
            set_clause = ', '.join(f'{col} = ?' for col in AI_COLUMNS.keys())
            placeholders = [def_data[col] for col in AI_COLUMNS.keys()]
            # Replace modality and type with mapped values
            placeholders[list(AI_COLUMNS.keys()).index('definition')] = definition
            placeholders[list(AI_COLUMNS.keys()).index('modality')] = mapped_modality
            placeholders[list(AI_COLUMNS.keys()).index('type')] = mapped_type
            placeholders.append(def_data['word'])  # Add the word for the WHERE clause
            
            cursor.execute(f'''
                UPDATE chinese_word 
                SET {set_clause}
                WHERE simplified = ?
            ''', placeholders)
            
        except Exception as e:
            logging.error(f"Error processing word {def_data.get('word', 'UNKNOWN')}: {str(e)}")
            continue

def clear_ai_columns(cursor, conn):
    """Clear all AI-generated columns in the database."""
    try:
        for column in AI_COLUMNS.keys():
            if column == "definition":
                # ToDo Handle cleanup
                continue
            cursor.execute(f'UPDATE chinese_word SET {column} = NULL')
        conn.commit()
        logging.info("Cleared all AI-generated columns")
    except sqlite3.OperationalError as e:
        logging.error(f"Error clearing columns: {e}")
        conn.rollback()

def load_create_ai_cache_db(ai_cache_file):
    # Load from ai_cache
    conn = sqlite3.connect(ai_cache_file)
    cursor = conn.cursor()

    cursor.execute('''CREATE TABLE IF NOT EXISTS `chinese_word` (
                        `simplified` TEXT NOT NULL,
                        `definition` TEXT NOT NULL,
                        `modality` TEXT DEFAULT 'N/A' CHECK(`modality` IN ('ORAL', 'WRITTEN', 'ORAL_WRITTEN', 'N/A')),
                        `examples` TEXT DEFAULT '',
                        `type` TEXT DEFAULT 'N/A' CHECK(`type` IN ('NOUN', 'VERB', 'ADJECTIVE', 'ADVERB', 'CONJUNCTION', 'PREPOSITION', 'INTERJECTION', 'IDIOM', 'N/A')),
                        `synonyms` TEXT DEFAULT '',
                        `antonym` TEXT DEFAULT '',
                        PRIMARY KEY(`simplified`)
                    )''')

    print(f"Created/connected the ai cache db {ai_cache_file}")

    return conn, cursor


def populate_ai_columns(db_file, ai_cache_file: str, clear_first, where_clause):
    # Setup logging
    log_file = setup_logging()
    logging.info("Starting dictionary generation")
    logging.info("Log file: %s", log_file)
    
    # Connect to the database
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    
    try:
        # Clear AI columns if requested
        if clear_first:
            clear_ai_columns(cursor, conn)
        
        # Get total count of words
        cursor.execute('SELECT COUNT(*) FROM chinese_word WHERE ' + where_clause)
        total_words = cursor.fetchone()[0]
        logging.info("Found %d to process", total_words)

        # populate from ai_cache
        conn_cache, cursor_cache = load_create_ai_cache_db(ai_cache_file)

        cached_data = []
        cursor_cache.execute('SELECT * FROM `chinese_word`')
        columns = [desc[0] for desc in cursor_cache.description]
        for row in cursor_cache.fetchall():
            word_dict = dict(zip(columns, row))
            word_dict['word'] = word_dict['simplified']  # To match AI output for update_word_ai_data function
            del word_dict['simplified']

            definitions = json.loads(word_dict['definition'])
            word_dict['definition'] = definitions[DEFINITION_AI_LOCALE]
            cached_data.append(word_dict)
        logging.info("Retrieved %d cached records", len(cached_data))

        update_word_ai_data(cursor, cached_data)
        conn.commit()
        logging.info("Write %d cached records to the main database", len(cached_data))
        
        # Get count of words without definitions
        cursor.execute('SELECT COUNT(*) FROM chinese_word WHERE examples IS NULL AND ' + where_clause)
        remaining_words = cursor.fetchone()[0]
        
        logging.info("Total words: %d", total_words)
        logging.info("Words without definitions: %d", remaining_words)
        
        consecutive_failures = 0
        
        while remaining_words > 0:
            # Get batch of words without definitions
            words = get_words_without_definitions(cursor, BATCH_SIZE, where_clause)
            if not words:
                break
                
            logging.info("Processing %d words: %s", len(words), ', '.join(words))
            
            # Generate prompt and call API
            prompt = generate_prompt(words)
            ai_data = call_llm_api(prompt)
            
            if ai_data:
                # Reset failure counter on success
                consecutive_failures = 0
                
                # Update database with new ai_data
                update_word_ai_data(cursor, ai_data)
                conn.commit()  # Commit after each successful batch

                # update the cache too!
                update_word_ai_data(cursor_cache, ai_data)
                conn_cache.commit()
                
                # Update remaining count
                cursor.execute('SELECT COUNT(*) FROM chinese_word WHERE examples IS NULL')
                remaining_words = cursor.fetchone()[0]
                logging.info("Remaining words: %d", remaining_words)
            else:
                consecutive_failures += 1
                logging.warning("Failed to get definitions from API (consecutive failures: %d)", consecutive_failures)
                
                if consecutive_failures >= MAX_CONSECUTIVE_FAILURES:
                    logging.error("Exiting after %d consecutive failures", MAX_CONSECUTIVE_FAILURES)
                    break
            
            # Sleep to avoid rate limiting
            time.sleep(1)
        
        logging.info("Processing complete!")
    except Exception as e:
        logging.error("An error occurred: %s", e)
        conn.rollback()  # Rollback on error
    finally:
        conn.close()  # Ensure connection is always closed

if __name__ == "__main__":
    # Parse command line arguments
    parser = argparse.ArgumentParser(description='Generate AI-enhanced Chinese dictionary entries')
    parser.add_argument('--clear', action='store_true', help='Clear all AI-generated columns before starting')
    args = parser.parse_args()

    populate_ai_columns(DB_FILE, AI_CACHE_DB, args.clear, AI_WORDS_WHERE_CLAUSE)