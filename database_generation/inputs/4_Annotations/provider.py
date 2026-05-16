import csv
import os
from datetime import datetime
from typing import List, Dict, Any, Iterator, Tuple
from base_provider import Provider, ProviderType
from utils import generate_searchable_text

class AnnotationsProvider(Provider):
    SYSTEM_LISTS_CREATION_DATE = 1746863357780

    class_level_conv = {
        "初一": "Elementary1", "初二": "Elementary2", "初三": "Elementary3", "初四": "Elementary4",
        "中一": "Intermediate1", "中二": "Intermediate2", "中B": "Intermediate2", "中三": "Intermediate3",
        "高一": "Advanced1", "高二": "Advanced2", "高三": "Advanced3", "其他": "NotFromClass", "不课": "NotFromClass"
    }

    class_type_conv = {
        "口语": "Speaking", "写作": "Writing", "读书": "Reading", "精读": "Reading",
        "听力": "Listening", "阅读": "FastReading", "其他": "NotFromClass", "不课": "NotFromClass"
    }

    def update(self):
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word_annotation": {
                "type": ProviderType.TABLE,
                "columns": ["a_simplified", "a_pinyins", "notes", "class_type", "class_level", "themes", "first_seen", "is_exam", "a_searchable_text"]
            },
            "word_list": {
                "type": ProviderType.TABLE,
                "columns": ["id", "name", "creation_date", "last_modified", "list_type", "anki_deck_id"]
            },
            "word_list_entry": {
                "type": ProviderType.TABLE,
                "columns": ["list_id", "simplified", "anki_note_id"]
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        csv_path = os.path.join(os.path.dirname(__file__), "annotations.csv")
        if not os.path.exists(csv_path):
            return

        # System lists for annotations
        annotated_list_id = 100 # Arbitrary ID for system lists

        yield ("word_list", {
            "id": annotated_list_id,
            "name": 'Annotated words',
            "creation_date": self.SYSTEM_LISTS_CREATION_DATE,
            "last_modified": self.SYSTEM_LISTS_CREATION_DATE + 10,
            "list_type": 'SYSTEM',
            "anki_deck_id": 0
        })

        with open(csv_path, mode='r', encoding='utf-8-sig') as file:
            # Robustly skip comments and empty lines at the file level
            # This handles cases where the comment character might be preceded by whitespace or BOM
            filtered_lines = (line for line in file if line.strip() and not line.lstrip().startswith('#'))
            reader = csv.reader(filtered_lines)
            
            for row in reader:
                if len(row) < 8:
                    continue
                
                simplified = row[0].strip()
                pinyins = row[1].strip()
                notes = row[2].strip()
                class_type = self.class_type_conv.get(row[3].strip(), "NotFromClass")
                first_seen_str = row[4].strip()
                themes = row[5].strip()
                # row[6] is unused
                class_level = self.class_level_conv.get(row[7].strip(), "NotFromClass")
                
                try:
                    first_seen = int(datetime.strptime(first_seen_str, "%Y-%m-%d").timestamp() * 1000)
                except (ValueError, TypeError):
                    first_seen = 0

                searchable_text = generate_searchable_text(pinyins, notes, themes, simplified)

                yield ("chinese_word_annotation", {
                    "a_simplified": simplified,
                    "a_pinyins": pinyins,
                    "notes": notes,
                    "class_type": class_type,
                    "class_level": class_level,
                    "themes": themes,
                    "first_seen": first_seen,
                    "is_exam": 0,
                    "a_searchable_text": searchable_text
                })
                
                yield ("word_list_entry", {
                    "list_id": annotated_list_id,
                    "simplified": simplified,
                    "anki_note_id": 0
                })
