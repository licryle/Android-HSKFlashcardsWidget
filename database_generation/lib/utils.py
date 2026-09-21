import os
import re
import json
from typing import Dict, Any, Optional, List, Iterator
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

def parse_cedict_line(line: str) -> Optional[Dict[str, str]]:
    """Parses a single CEDICT line into a dictionary with simplified, traditional, pinyin, and english fields."""
    # Matches: traditional simplified [pinyin] /definitions/
    regex = r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$'
    match = re.match(regex, line.strip())
    if match:
        traditional, simplified, pinyin, english = match.groups()
        return {
            "traditional": traditional,
            "simplified": simplified,
            "pinyin": pinyin,
            "english": english
        }
    return None

def load_cedict_simplified_words(cedict_path: str) -> List[str]:
    """Returns a deduplicated list of simplified words from CEDICT, preserving order."""
    words = []
    if os.path.exists(cedict_path):
        with open(cedict_path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('#'): continue
                entry = parse_cedict_line(line)
                if entry:
                    words.append(entry['simplified'])
    return list(dict.fromkeys(words))

def iter_cedict(cedict_path: str) -> Iterator[Dict[str, str]]:
    """Iterates over CEDICT entries as dictionaries."""
    if os.path.exists(cedict_path):
        with open(cedict_path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('#'): continue
                entry = parse_cedict_line(line)
                if entry:
                    yield entry

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

def _group_cedict_entries(entries: List[tuple]) -> Dict[str, List[str]]:
    """Groups (pinyin_with_tones, english_raw) by pinyin, splitting '/' glosses.

    Preserves CEDICT order and drops exact duplicate glosses within a reading.
    """
    grouped: Dict[str, List[str]] = {}
    for pinyin, english_raw in entries:
        glosses = [g.strip() for g in (english_raw or "").split("/") if g.strip()]
        if not glosses:
            continue
        if pinyin not in grouped:
            grouped[pinyin] = []
        for gloss in glosses:
            if gloss not in grouped[pinyin]:
                grouped[pinyin].append(gloss)
    return grouped

def format_cedict_definition(entries: List[tuple]) -> str:
    """Formats grouped CEDICT definitions for a single simplified headword.

    entries: list of (pinyin_with_tones, english_raw) in CEDICT order,
        where english_raw uses '/' as gloss separator.
    Returns a newline-separated definition string:
    - single pinyin + single gloss -> "gloss"
    - single pinyin + N glosses -> "1. gloss1\\n2. gloss2..."
    - multiple pinyins -> "[pinyin] gloss" per line, with "1. / 2. ..."
      numbering when that pinyin has multiple glosses.
    """
    grouped = _group_cedict_entries(entries)

    if not grouped:
        return ""

    if len(grouped) == 1:
        glosses = next(iter(grouped.values()))
        if len(glosses) == 1:
            return glosses[0]
        return "\n".join(f"{i + 1}. {gloss}" for i, gloss in enumerate(glosses))

    lines = []
    for pinyin, glosses in grouped.items():
        if len(glosses) == 1:
            lines.append(f"[{pinyin}] {glosses[0]}")
        else:
            for i, gloss in enumerate(glosses):
                lines.append(f"[{pinyin}] {i + 1}. {gloss}")
    return "\n".join(lines)

def build_cedict_searchable_text(simplified: str, entries: List[tuple]) -> str:
    """Builds aggregated search-index text for a single simplified headword.

    Covers every reading (toneless + concatenated forms) and every gloss in
    plain form: no [pinyin] prefixes, no numbering.
    """
    grouped = _group_cedict_entries(entries)
    variants = list(grouped.keys())
    glosses = [gloss for gloss_list in grouped.values() for gloss in gloss_list]
    toneless_parts = [unidecode(variant) for variant in variants]
    concatenated_parts = [toneless.replace(" ", "") for toneless in toneless_parts]
    hanzi_split = " ".join(list(simplified))
    parts = [simplified, hanzi_split] + glosses + toneless_parts + concatenated_parts
    return " ".join([str(part) for part in parts if part]).lower()

_BRACKET_RE = re.compile(r"\[([^\[\]]+)\]")
_PINYIN_LIKE_RE = re.compile(r"[a-zA-ZāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜüÜ ]+")

def extract_bracketed_pinyins(definition: Optional[str]) -> List[str]:
    """Extracts [pinyin] reading prefixes from a formatted definition.

    Only returns candidates that look like pinyin (letters/tone marks/spaces,
    no digits or CJK), so gloss-internal references like [jun4] are ignored.
    """
    readings = []
    for match in _BRACKET_RE.finditer(definition or ""):
        candidate = match.group(1).strip()
        if candidate and candidate not in readings and _PINYIN_LIKE_RE.fullmatch(candidate):
            readings.append(candidate)
    return readings

def plain_definition_text(definition: Optional[str]) -> str:
    """Strips [pinyin] prefixes and leading 'N.' numbering for search indexing."""
    text = _BRACKET_RE.sub(" ", definition or "")
    text = re.sub(r"(?m)^\s*\d+\.\s*", " ", text)
    return re.sub(r"\s+", " ", text).strip()

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
