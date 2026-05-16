import os
import re
import json
from typing import List, Dict, Optional, Any, Iterator, Tuple
from lib import Provider, ProviderType, convert_pinyin_with_tones, unidecode

class BaseDictProvider(Provider):
    def update(self):
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "traditional", "definition", "pinyins", "searchable_text", "hsk_level"]
            }
        }

    def _extract_entry(self, entry: str) -> Optional[Dict[str, Any]]:
        regex = r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$'
        match = re.match(regex, entry)
        if match:
            traditional, simplified, pinyins_raw, definition_raw = match.groups()
            pinyins = convert_pinyin_with_tones(pinyins_raw)
            definition_dict = {"en": definition_raw}
            definition_json = json.dumps(definition_dict, ensure_ascii=False)

            searchable_text = simplified + ' ' + traditional + ' ' + unidecode(pinyins).replace(" ", "") + ' ' + definition_json
            
            return {
                "simplified": simplified,
                "traditional": traditional,
                "definition": definition_json,
                "pinyins": pinyins,
                "hsk_level": "NOT_HSK",
                "searchable_text": searchable_text
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
