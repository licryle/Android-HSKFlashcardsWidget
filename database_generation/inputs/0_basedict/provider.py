import os
import re
import json
import sqlite3
from typing import List, Dict, Optional, Any, Iterator, Tuple
from lib import Provider, ProviderType, convert_pinyin_with_tones, unidecode

class BaseDictProvider(Provider):
    def update(self):
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "traditional", "pinyins", "hsk_level"]
            },
            "word_definition": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "language", "definition"]
            }
        }

    def _extract_entry(self, entry: str) -> Optional[Dict[str, Any]]:
        regex = r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$'
        match = re.match(regex, entry)
        if match:
            traditional, simplified, pinyins_raw, definition_raw = match.groups()
            pinyins = convert_pinyin_with_tones(pinyins_raw)
            return {
                "simplified": simplified,
                "traditional": traditional,
                "pinyins": pinyins,
                "hsk_level": "NOT_HSK",
                "_definition": definition_raw
            }
        return None

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        cedict_path = os.path.join(os.path.dirname(__file__), "cedict_ts.u8")
        if not os.path.exists(cedict_path):
            return

        cedict_words = set()
        with open(cedict_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line[0] == '#':
                    continue
                
                record = self._extract_entry(line)
                if record:
                    cedict_words.add(record["simplified"])
                    yield ("chinese_word", {key: value for key, value in record.items() if key != "_definition"})
                    yield ("word_definition", {
                        "simplified": record["simplified"],
                        "language": "en",
                        "definition": record["_definition"]
                    })

        # CEDICT occasionally removes headwords. Preserve records from the previous
        # generated dictionary so an update never turns existing definitions into
        # foreign-key orphans merely because the upstream source changed.
        previous_path = os.path.join(os.path.dirname(__file__), "../../output/Mandarin_Assistant - Copy.db")
        if not previous_path or not os.path.exists(previous_path):
            return

        with sqlite3.connect(previous_path) as conn:
            columns = {row[1] for row in conn.execute("PRAGMA table_info(chinese_word)")}
            word_columns = [
                "simplified", "traditional", "hsk_level", "pinyins", "popularity",
                "examples", "collocations", "modality", "type", "synonyms", "antonym"
            ]
            available_columns = [column for column in word_columns if column in columns]
            if "simplified" not in available_columns:
                return

            select_columns = ", ".join(available_columns)
            for row in conn.execute(f"SELECT {select_columns} FROM chinese_word"):
                record = dict(zip(available_columns, row))
                simplified = record["simplified"]
                if simplified in cedict_words:
                    continue
                yield ("chinese_word", record)

                # v3 source database: definitions are already normalized.
                if conn.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='word_definition'").fetchone():
                    for language, definition in conn.execute(
                        "SELECT language, definition FROM word_definition WHERE simplified = ?", (simplified,)
                    ):
                        yield ("word_definition", {
                            "simplified": simplified,
                            "language": language,
                            "definition": definition
                        })
                # v1/v2 source database: unpack its legacy JSON map only for preserved words.
                elif "definition" in columns:
                    legacy = conn.execute("SELECT definition FROM chinese_word WHERE simplified = ?", (simplified,)).fetchone()
                    if legacy and legacy[0]:
                        try:
                            definitions = json.loads(legacy[0])
                        except json.JSONDecodeError:
                            definitions = {}
                        for language, definition in definitions.items():
                            if isinstance(definition, str):
                                yield ("word_definition", {
                                    "simplified": simplified,
                                    "language": language,
                                    "definition": definition
                                })
