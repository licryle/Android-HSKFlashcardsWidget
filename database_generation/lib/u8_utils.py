"""Generic helpers for CEDICT-format `.u8` dictionary files.

Line format: `traditional simplified [pinyin] /gloss1/gloss2/`.
Used identically by every definition stage (base English, French, HSK3):
providers only declare their file and language (see U8Provider).
"""
import logging
import os
import re
from typing import Dict, Any, Optional, List, Iterator, Set, Tuple

from unidecode import unidecode

from .base_provider import Provider, ProviderType
from .conf import CEDICT_FILE

# A single .u8 line: traditional simplified [numbered pinyin] /glosses/
_U8_LINE_RE = re.compile(r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$')
# [pinyin] reading prefixes added by format_u8_definition (multi-reading words)
_BRACKET_RE = re.compile(r"\[([^\[\]]+)\]")
_PINYIN_LIKE_RE = re.compile(r"[a-zA-ZāáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜüÜ ]+")


def parse_u8_line(line: str) -> Optional[Dict[str, str]]:
    """Parses a single .u8 line into traditional/simplified/pinyin/gloss fields."""
    match = re.match(_U8_LINE_RE, line.strip())
    if match:
        traditional, simplified, pinyin, gloss = match.groups()
        return {
            "traditional": traditional,
            "simplified": simplified,
            "pinyin": pinyin,
            "gloss": gloss
        }
    return None


def iter_u8(u8_path: str) -> Iterator[Dict[str, str]]:
    """Iterates over .u8 entries as dictionaries."""
    if os.path.exists(u8_path):
        with open(u8_path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('#'):
                    continue
                entry = parse_u8_line(line)
                if entry:
                    yield entry


def load_u8_words(u8_path: str) -> List[str]:
    """Returns a deduplicated list of simplified words from a .u8 file, preserving order."""
    words = []
    if os.path.exists(u8_path):
        with open(u8_path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('#'):
                    continue
                entry = parse_u8_line(line)
                if entry:
                    words.append(entry['simplified'])
    return list(dict.fromkeys(words))


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
        if not tone or tone == '5':
            return syllable
        tone_num = int(tone) - 1
        if "iu" in syllable:
            return syllable.replace("u", tone_marks["u"][tone_num])
        if "ui" in syllable:
            return syllable.replace("i", tone_marks["i"][tone_num])
        for vowel in ["a", "A", "o", "O", "e", "E", "i", "I", "u", "U", "ü", "Ü"]:
            if vowel in syllable:
                return syllable.replace(vowel, tone_marks[vowel][tone_num])
        return syllable

    return ' '.join(replace_tone(m) for m in pinyin_pattern.finditer(pinyin_string))


def _group_u8_entries(entries: List[tuple]) -> Dict[str, List[str]]:
    """Groups (pinyin_with_tones, gloss_raw) by pinyin, splitting '/' glosses.

    Preserves file order and drops exact duplicate glosses within a reading.
    """
    grouped: Dict[str, List[str]] = {}
    for pinyin, gloss_raw in entries:
        glosses = [g.strip() for g in (gloss_raw or "").split("/") if g.strip()]
        if not glosses:
            continue
        if pinyin not in grouped:
            grouped[pinyin] = []
        for gloss in glosses:
            if gloss not in grouped[pinyin]:
                grouped[pinyin].append(gloss)
    return grouped


def format_u8_definition(entries: List[tuple]) -> str:
    """Formats grouped .u8 glosses for a single simplified headword.

    entries: list of (pinyin_with_tones, gloss_raw) in file order,
        where gloss_raw uses '/' as separator.
    - single pinyin + single gloss -> "gloss"
    - single pinyin + N glosses -> "1. gloss1\\n2. gloss2..."
    - multiple pinyins -> "[pinyin] gloss" per line, numbered per reading
      when that reading has multiple glosses.
    """
    grouped = _group_u8_entries(entries)

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


def build_u8_searchable_text(simplified: str, entries: List[tuple]) -> str:
    """Builds aggregated search-index text for a single simplified headword.

    Covers every reading (toneless + concatenated forms) without definition glosses.
    """
    grouped = _group_u8_entries(entries)
    variants = list(grouped.keys())
    toneless_parts = [unidecode(variant) for variant in variants]
    concatenated_parts = [toneless.replace(" ", "") for toneless in toneless_parts]
    hanzi_split = " ".join(list(simplified))
    parts = [simplified, hanzi_split] + toneless_parts + concatenated_parts
    return " ".join([str(part) for part in parts if part]).lower()


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


class U8Provider(Provider):
    """Generic provider turning a CEDICT-format .u8 file into word_definition rows.

    Subclasses only declare their source file and language; .u8 files are
    generated externally so update() is a no-op::

        class LanguageFrenchProvider(U8Provider):
            U8_FILE = os.path.join(os.path.dirname(__file__), "cfdict-next-full.u8")
            LANGUAGE = "fr"
    """

    #: Absolute path to the .u8 source file.
    U8_FILE: str = ""
    #: word_definition language tag for every row this provider emits.
    LANGUAGE: str = ""
    #: Restrict emissions to the base-dictionary vocabulary so child tables
    #: never violate the chinese_word foreign key (the orchestrator aborts
    #: the whole build on FK violations). The base stage itself sets False.
    FILTER_TO_BASE: bool = True

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def update(self):
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "word_definition": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "language", "definition"]
            }
        }

    def _read_grouped(self) -> Dict[str, Dict[str, Any]]:
        """Reads U8_FILE into {simplified: {"display": (traditional, pinyins),
        "entries": [(pinyins, gloss_raw), ...]}}, preserving file order."""
        grouped: Dict[str, Dict[str, Any]] = {}
        if not self.U8_FILE or not os.path.exists(self.U8_FILE):
            return grouped
        for entry in iter_u8(self.U8_FILE):
            simplified = entry["simplified"]
            pinyins = convert_pinyin_with_tones(entry["pinyin"])
            if simplified not in grouped:
                grouped[simplified] = {"display": (entry["traditional"], pinyins), "entries": []}
            # Display columns keep last-entry-wins semantics.
            grouped[simplified]["display"] = (entry["traditional"], pinyins)
            grouped[simplified]["entries"].append((pinyins, entry["gloss"]))
        return grouped

    def _allowed_words(self) -> Optional[Set[str]]:
        if not self.FILTER_TO_BASE:
            return None
        return set(load_u8_words(CEDICT_FILE))

    def _yield_definitions(self, grouped: Dict[str, Dict[str, Any]]) -> Iterator[Tuple[str, Dict[str, Any]]]:
        allowed = self._allowed_words()
        skipped = 0
        for simplified, data in grouped.items():
            if allowed is not None and simplified not in allowed:
                skipped += 1
                continue
            yield ("word_definition", {
                "simplified": simplified,
                "language": self.LANGUAGE,
                "definition": format_u8_definition(data["entries"])
            })
        if skipped:
            self.logger.info(
                f"{self.__class__.__name__}: skipped {skipped} words outside the base vocabulary."
            )

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        yield from self._yield_definitions(self._read_grouped())
