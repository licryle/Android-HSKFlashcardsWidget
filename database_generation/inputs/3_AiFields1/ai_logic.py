import json
import requests
import time
import logging
import re
from typing import List, Dict, Optional

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
    "definition": "meaning one\\nmeaning two",
    "examples": "example one\\nexample two",
    "modality": "ORAL_WRITTEN",
    "type": "VERB",
    "synonyms": "近义词1, 近义词2",
    "antonym": "反义词"
  }},
  ...
]
"""

def escape_newlines_in_json_strings(s: str) -> str:
    def replacer(match):
        return match.group(0).replace('\n', '\\n').replace('\r', '\\n')
    return re.sub(r'"(.*?)(?<!\\)"', lambda m: m.group(0).replace('\n', '\\n').replace('\r', '\\n'), s, flags=re.DOTALL)

def clean_json_string(s: str) -> str:
    s = s.strip()
    if s.startswith('\ufeff'): s = s[1:]
    start = s.find('[')
    end = s.rfind(']')
    if start != -1 and end != -1:
        s = s[start:end+1]
    return escape_newlines_in_json_strings(s)

def parse_json_permissive(s: str, required_fields: List[str]) -> Optional[List[Dict]]:
    try:
        return json.loads(s)
    except json.JSONDecodeError:
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
                    try:
                        obj = json.loads(s[start_pos:i+1])
                        if isinstance(obj, dict) and all(k in obj for k in required_fields):
                            valid_entries.append(obj)
                    except: pass
                    start_pos = None
        return valid_entries if valid_entries else None

def call_llm_api(endpoint: str, model: str, prompt: str, required_fields: List[str]) -> Optional[List[Dict]]:
    headers = {'Content-Type': 'application/json'}
    data = {
        'model': model,
        'messages': [{'role': 'user', 'content': prompt}],
        'temperature': 0.7,
        'max_tokens': 2000,
        'stream': False
    }
    try:
        response = requests.post(endpoint, headers=headers, json=data, timeout=120)
        response.raise_for_status()
        content = response.json()['choices'][0]['message']['content']
        cleaned = clean_json_string(content)
        return parse_json_permissive(cleaned, required_fields)
    except Exception as e:
        logging.error(f"LLM API Error: {e}")
        return None
