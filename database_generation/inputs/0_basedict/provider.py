import os
import re
import json
from typing import List, Dict, Optional, Any, Iterator, Tuple
from base_provider import Provider, ProviderType
from utils import convert_pinyin_with_tones, generate_searchable_text

class BaseDictProvider(Provider):
    def update(self):
        # Placeholder for downloading latest CEDICT if needed
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "traditional", "definition", "pinyins", "searchable_text", "hsk_level", "popularity"]
            }
        }

    def _extract_entry(self, entry: str) -> Optional[Dict[str, Any]]:
        regex = r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$'
        match = re.match(regex, entry)
        if match:
            traditional, simplified, pinyins_raw, definition_raw = match.groups()
            pinyins = convert_pinyin_with_tones(pinyins_raw)
            definition = json.dumps({"en": definition_raw}, ensure_ascii=False)
            
            return {
                "simplified": simplified,
                "traditional": traditional,
                "definition": definition,
                "pinyins": pinyins,
                "hsk_level": "NOT_HSK",
                "popularity": 0,
                "searchable_text": generate_searchable_text(simplified, traditional, pinyins, definition_raw)
            }
        return None

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        cedict_path = os.path.join(os.path.dirname(__file__), "cedict_ts.u8")
        if not os.path.exists(cedict_path):
            return

        with open(cedict_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line[0] == '#':
                    continue
                
                record = self._extract_entry(line)
                if record:
                    yield ("chinese_word", record)
