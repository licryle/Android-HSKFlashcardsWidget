import os
import re
import json
from typing import Optional

# Shared Mappings
MODALITY_MAPPING = {
    "ORAL": "ORAL", "WRITTEN": "WRITTEN", "ORAL_WRITTEN": "ORAL_WRITTEN",
    "oral": "ORAL", "written": "WRITTEN", "oral and written": "ORAL_WRITTEN",
    "written and oral": "ORAL_WRITTEN", "N/A": "N/A", "口语": "ORAL",
    "书面": "WRITTEN", "口语和书面": "ORAL_WRITTEN", "不适用": "N/A", "未知": "N/A", "": "N/A"
}

TYPE_MAPPING = {
    "NOUN": "NOUN", "VERB": "VERB", "ADJECTIVE": "ADJECTIVE", "ADVERB": "ADVERB",
    "CONJUNCTION": "CONJUNCTION", "PREPOSITION": "PREPOSITION", "INTERJECTION": "INTERJECTION",
    "IDIOM": "IDIOM", "noun": "NOUN", "verb": "VERB", "adjective": "ADJECTIVE",
    "adverb": "ADVERB", "conjunction": "CONJUNCTION", "preposition": "PREPOSITION",
    "interjection": "INTERJECTION", "idiom": "IDIOM", "N/A": "N/A",
    "名词": "NOUN", "动词": "VERB", "形容词": "ADJECTIVE", "副词": "ADVERB",
    "连词": "CONJUNCTION", "介词": "PREPOSITION", "感叹词": "INTERJECTION",
    "成语": "IDIOM", "俗话": "IDIOM", "不适用": "N/A", "未知": "N/A", "": "N/A"
}

def merge_json_strings(current_json: Optional[str], new_json_data: str) -> str:
    """Merges two JSON strings representing dictionaries."""
    try:
        current_data = json.loads(current_json) if current_json else {}
        if not isinstance(current_data, dict): current_data = {}
        
        new_data = json.loads(new_json_data)
        if not isinstance(new_data, dict): return current_json or new_json_data
        
        current_data.update(new_data)
        return json.dumps(current_data, ensure_ascii=False)
    except Exception:
        return new_json_data

def get_app_version(toml_path: str = "../gradle/libs.versions.toml") -> int:
    """Parses app-versionCode from libs.versions.toml."""
    try:
        if not os.path.exists(toml_path):
            return 0
        with open(toml_path, 'r', encoding='utf-8') as f:
            content = f.read()
            match = re.search(r'app-versionCode\s*=\s*"(\d+)"', content)
            if match:
                return int(match.group(1))
    except Exception:
        pass
    return 0
