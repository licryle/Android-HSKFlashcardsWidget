import os
from typing import List, Dict, Any, Iterator, Tuple
from base_provider import Provider, ProviderType

class HskProvider(Provider):
    SYSTEM_LISTS_CREATION_DATE = 1746863357780

    def update(self):
        # HSK data is usually in a submodule, update if needed
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["hsk_level", "popularity"],
                "index": "simplified"
            },
            "word_list": {
                "type": ProviderType.TABLE,
                "columns": ["name", "creation_date", "last_modified", "list_type", "anki_deck_id"]
            },
            "word_list_entry": {
                "type": ProviderType.TABLE,
                "columns": ["list_id", "simplified", "anki_note_id"]
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        hsk_submodule = os.path.join(os.path.dirname(__file__), 'new_hsk')
        hsk_freq_dir = os.path.join(hsk_submodule, 'HSK List (Frequency)')
        
        if not os.path.exists(hsk_freq_dir):
            return

        hsk_files = [
            ('HSK 7-9.txt', 'HSK7-9', 7),
            ('HSK 6.txt', 'HSK6', 6),
            ('HSK 5.txt', 'HSK5', 5),
            ('HSK 4.txt', 'HSK4', 4),
            ('HSK 3.txt', 'HSK3', 3),
            ('HSK 2.txt', 'HSK2', 2),
            ('HSK 1.txt', 'HSK1', 1),
        ]

        popularity = 0
        # For word_list, we need IDs. But since we are yielding, 
        # the Orchestrator might need to handle ID generation for TABLE types 
        # or we assume we can provide them.
        # Actually, if word_list uses AUTOINCREMENT, we might have an issue 
        # if multiple providers yield to it.
        # However, HSK levels are distinct.
        
        for filename, level_name, level_num in hsk_files:
            file_path = os.path.join(hsk_freq_dir, filename)
            if not os.path.exists(file_path):
                continue
            
            # Yield the list record
            list_id = level_num # Simple mapping for system lists
            yield ("word_list", {
                "id": list_id,
                "name": level_name,
                "creation_date": self.SYSTEM_LISTS_CREATION_DATE,
                "last_modified": self.SYSTEM_LISTS_CREATION_DATE + 8 - level_num,
                "list_type": 'SYSTEM',
                "anki_deck_id": 0
            })

            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()[::-1]
                for line in lines:
                    word = line.strip()
                    if not word: continue
                    
                    # Update word metadata
                    yield ("chinese_word", {
                        "simplified": word,
                        "hsk_level": level_name,
                        "popularity": popularity
                    })
                    
                    # Link to list
                    yield ("word_list_entry", {
                        "list_id": list_id,
                        "simplified": word,
                        "anki_note_id": 0
                    })
                    
                    popularity += 1
