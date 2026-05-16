import os
import csv
from typing import List, Dict, Any, Iterator, Tuple
from lib import Provider, ProviderType

class PopularityProvider(Provider):
    def update(self):
        pass

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["popularity"],
                "index": "simplified",
                "defaults": {
                    "popularity": 0
                }
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        freq_file = os.path.join(os.path.dirname(__file__), 'bcc.blcu.edu.cn.multi_domain_total_word_freq.txt')
        
        if not os.path.exists(freq_file):
            return

        with open(freq_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            
            for row in reader:
                word = row['token']
                count = int(row['count'])
                
                yield ("chinese_word", {
                    "simplified": word,
                    "popularity": count
                })
