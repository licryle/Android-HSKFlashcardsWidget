import json
import requests
import re
import logging
import time
from typing import List, Dict, Optional

logger = logging.getLogger(__name__)

def escape_newlines_in_json_strings(s: str) -> str:
    return re.sub(r'"(.*?)(?<!\\)"', lambda m: m.group(0).replace('\n', '\\n').replace('\r', '\\n'), s, flags=re.DOTALL)

def clean_json_string(s: str) -> str:
    s = s.strip()
    if s.startswith('\ufeff'): s = s[1:]
    
    # Remove markdown code blocks if present
    s = re.sub(r'```json\s*(.*?)\s*```', r'\1', s, flags=re.DOTALL)
    s = re.sub(r'```\s*(.*?)\s*```', r'\1', s, flags=re.DOTALL)

    start = s.find('[')
    end = s.rfind(']')
    if start != -1 and end != -1:
        s = s[start:end+1]
    
    return escape_newlines_in_json_strings(s)

def try_fix_malformed_json_object(s: str) -> str:
    """Attempts to fix common LLM JSON errors like extra quotes in values."""
    # Fix broken field values like "type": "NOUN",", VERB", -> "type": "NOUN, VERB",
    s = re.sub(r'":\s*"([^"]*)",\s*"([^"]*)",', r'": "\1, \2",', s)
    return s

def parse_json_permissive(s: str, required_fields: List[str]) -> List[Dict]:
    try:
        return json.loads(s)
    except json.JSONDecodeError:
        logger.warning("Standard JSON parse failed, trying permissive mode...")
        valid_entries = []
        depth = 0
        start_pos = None
        for i, char in enumerate(s):
            if char == '{':
                if depth == 0: start_pos = i
                depth += 1
            elif char == '}':
                depth -= 1
                if depth == 0 and start_pos is not None:
                    obj_str = s[start_pos:i+1]
                    try:
                        obj = json.loads(obj_str)
                        if isinstance(obj, dict) and all(k in obj for k in required_fields):
                            valid_entries.append(obj)
                    except json.JSONDecodeError:
                        try:
                            # Try one last ditch effort to fix the individual object
                            fixed_obj_str = try_fix_malformed_json_object(obj_str)
                            obj = json.loads(fixed_obj_str)
                            if isinstance(obj, dict) and all(k in obj for k in required_fields):
                                valid_entries.append(obj)
                        except:
                            pass
                    start_pos = None
        return valid_entries

def call_llm_api(endpoint: str, model: str, prompt: str, required_fields: List[str], max_retries: int = 3) -> List[Dict]:
    """Call the LLM API with retries. Returns valid objects found, or empty list if total failure."""
    headers = {'Content-Type': 'application/json'}
    data = {
        'model': model,
        'messages': [{'role': 'user', 'content': prompt}],
        'temperature': 0.1,
        'max_tokens': 2000,
        'stream': False
    }
    
    for attempt in range(max_retries):
        try:
            response = requests.post(endpoint, headers=headers, json=data, timeout=120)
            response.raise_for_status()
            
            content = response.json()['choices'][0]['message']['content']
            cleaned = clean_json_string(content)
            parsed = parse_json_permissive(cleaned, required_fields)
            
            if parsed:
                return parsed
            
            logger.warning(f"Attempt {attempt + 1}: Failed to parse any valid JSON. Content: {content[:200]}...")
        except Exception as e:
            logger.warning(f"Attempt {attempt + 1}: API call failed: {e}")
        
        if attempt < max_retries - 1:
            time.sleep(2 * (attempt + 1)) # Exponential backoff
            
    return []
