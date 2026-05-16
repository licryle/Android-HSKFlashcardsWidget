import re
import json
from typing import Dict, Any, Optional, List
from unidecode import unidecode

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

def convert_pinyin_with_tones(pinyin_string: str) -> str:
    """Converts numbered pinyin (e.g., ni3 hao3) to diacritic pinyin (nǐ hǎo)."""
    tone_marks = {
        'a': ['ā', 'á', 'ǎ', 'à'], 'e': ['ē', 'é', 'ě', 'è'], 'i': ['ī', 'í', 'ǐ', 'ì'],
        'o': ['ō', 'ó', 'ǒ', 'ò'], 'u': ['ū', 'ú', 'ǔ', 'ù'], 'ü': ['ǖ', 'ǘ', 'ǚ', 'ǜ'],
        'A': ['Ā', 'Á', 'Ǎ', 'À'], 'E': ['Ē', 'É', 'Ě', 'È'], 'I': ['Ī', 'Í', 'Ǐ', 'Ì'],
        'O': ['Ō', 'Ó', 'Ǒ', 'Ò'], 'U': ['Ū', 'Ú', 'Ǔ', 'Ù'], 'Ü': ['Ǖ', 'Ǘ', 'Ǚ', 'Ǜ']
    }
    pinyin_string = pinyin_string.replace("u:", "ü").replace("U:", "Ü")
    pinyin_pattern = re.compile(r"([a-züÜ]+)([1-5]?)", re.IGNORECASE)

    def replace_tone(match):
        syllable, tone = match.groups()
        if not tone or tone == '5': return syllable
        tone_num = int(tone) - 1
        if "iu" in syllable: return syllable.replace("u", tone_marks["u"][tone_num])
        if "ui" in syllable: return syllable.replace("i", tone_marks["i"][tone_num])
        for vowel in ["a","A","o","O","e","E","i","I","u","U","ü","Ü"]:
            if vowel in syllable: return syllable.replace(vowel, tone_marks[vowel][tone_num])
        return syllable

    return ' '.join(replace_tone(m) for m in pinyin_pattern.finditer(pinyin_string))

def merge_json_strings(current_json: Optional[str], new_json_data: str) -> str:
    """Merges two JSON strings representing dictionaries."""
    try:
        current_data = json.loads(current_json) if current_json else {}
        if not isinstance(current_data, dict): current_data = {}
        
        new_data = json.loads(new_json_data)
        if not isinstance(new_data, dict): return current_json or new_json_data
        
        current_data.update(new_data)
        return json.dumps(current_data, ensure_ascii=False)
    except:
        return new_json_data
