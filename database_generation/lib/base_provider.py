from abc import ABC, abstractmethod
from enum import Enum
from typing import List, Dict, Any, Iterator, Tuple

class ProviderType(Enum):
    TABLE = "table"   # For primary providers creating whole rows
    COLUMN = "column" # For enrichers updating specific columns in existing rows

class Provider(ABC):
    @abstractmethod
    def update(self):
        """Update local source files (download, scrape, etc.)"""
        pass

    @abstractmethod
    def schema(self) -> Dict[str, Dict[str, Any]]:
        """
        Returns the tables/columns to be added or updated.
        Example:
        {
            "chinese_word": {
                "type": ProviderType.COLUMN,
                "columns": ["hsk_level", "popularity"],
                "index": "simplified"
            }
        }
        """
        pass

    @abstractmethod
    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        """
        Yields data records for assembly.
        Yields: (table_name, record_dict)
        """
        pass
