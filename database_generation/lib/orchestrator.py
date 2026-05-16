import json
import os
import sqlite3
import importlib.util
import logging
import sys
from datetime import datetime
from typing import List, Dict, Any, Set, Iterator, Tuple, Optional
from .base_provider import Provider, ProviderType
from .utils import merge_json_strings

class Orchestrator:
    def __init__(self, schema_path: str, db_path: str):
        self.schema_path = schema_path
        self.db_path = db_path
        self.providers: List[Provider] = []
        self.schema_data = self._load_schema()
        self.table_definitions = self._parse_room_entities()
        self._setup_logging()

    def _setup_logging(self):
        os.makedirs('logs', exist_ok=True)
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        log_file = f'logs/generation_{timestamp}.log'
        
        logging.basicConfig(
            level=logging.DEBUG,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        self.logger = logging.getLogger(__name__)
        self.logger.info(f"Logging initialized. Log file: {log_file}")

    def _load_schema(self):
        with open(self.schema_path, 'r') as f:
            return json.load(f)

    def _parse_room_entities(self) -> Dict[str, Dict[str, Any]]:
        entities = {}
        for entity in self.schema_data['database']['entities']:
            table_name = entity['tableName']
            fields = {field['columnName']: field for field in entity['fields']}
            entities[table_name] = fields
        return entities

    def create_database(self):
        self.logger.info(f"Initializing database: {self.db_path}")
        if os.path.exists(self.db_path):
            os.remove(self.db_path)
        
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        for entity in self.schema_data['database']['entities']:
            create_sql = entity['createSql'].replace('${TABLE_NAME}', entity['tableName'])
            cursor.execute(create_sql)
            
            for index in entity.get('indices', []):
                index_sql = index['createSql'].replace('${TABLE_NAME}', entity['tableName'])
                cursor.execute(index_sql)
        
        for query in self.schema_data['database']['setupQueries']:
            cursor.execute(query)
            
        conn.commit()
        conn.close()
        self.logger.info("Database schema created.")

    def discover_providers(self, inputs_dir: str):
        self.logger.info(f"Discovering providers in {inputs_dir}...")
        for item in sorted(os.listdir(inputs_dir)):
            item_path = os.path.join(inputs_dir, item)
            provider_file = os.path.join(item_path, "provider.py")
            if os.path.isdir(item_path) and os.path.exists(provider_file):
                spec = importlib.util.spec_from_file_location(f"provider_{item}", provider_file)
                module = importlib.util.module_from_spec(spec)
                spec.loader.exec_module(module)
                
                for attr_name in dir(module):
                    attr = getattr(module, attr_name)
                    if isinstance(attr, type) and issubclass(attr, Provider) and attr is not Provider:
                        self.providers.append(attr())
                        self.logger.info(f"Discovered provider: {attr_name} in {item}")

    def validate_providers(self):
        self.logger.info("Validating providers...")
        errors = []
        table_owners = {}
        
        for provider in self.providers:
            p_name = provider.__class__.__name__
            p_schema = provider.schema()
            
            for table, info in p_schema.items():
                if table not in self.table_definitions:
                    errors.append(f"Provider {p_name} targets unknown table '{table}'")
                    continue
                
                provider_cols = set(info['columns'])
                if info['type'] == ProviderType.TABLE:
                    if table in table_owners:
                        owner_name, owner_cols = table_owners[table]
                        if provider_cols != owner_cols:
                            errors.append(f"Conflict: Table '{table}' has multiple primary providers with DIFFERENT columns: {p_name} and {owner_name}")
                        else:
                            self.logger.warning(f"Table '{table}' has multiple primary providers ({p_name}, {owner_name}), but their schema matches. Allowing.")
                    else:
                        table_owners[table] = (p_name, provider_cols)

                valid_cols = self.table_definitions[table].keys()
                for col in info['columns']:
                    if col not in valid_cols:
                        errors.append(f"Provider {p_name} targets unknown column '{col}' in table '{table}'")
        
        if errors:
            for err in errors:
                self.logger.error(err)
            logging.shutdown()
            sys.exit(1)
        self.logger.info("Validation successful.")

    def _assemble_data(self, conn: sqlite3.Connection):
        cursor = conn.cursor()
        
        table_providers = [p for p in self.providers if any(s['type'] == ProviderType.TABLE for s in p.schema().values())]
        column_providers = [p for p in self.providers if p not in table_providers]

        for provider in table_providers + column_providers:
            p_name = provider.__class__.__name__
            p_schema = provider.schema()
            self.logger.info(f"Assembling data from {p_name}...")
            
            for table_name, record in provider.data():
                if table_name not in p_schema:
                    continue
                
                info = p_schema[table_name]
                
                if info['type'] == ProviderType.TABLE:
                    data_to_insert = record.copy()
                    table_fields = self.table_definitions[table_name]
                    for col_name, field_info in table_fields.items():
                        if col_name not in data_to_insert and not field_info.get('notNull', False):
                            data_to_insert[col_name] = None
                    
                    cols = list(data_to_insert.keys())
                    placeholders = ', '.join(['?'] * len(cols))
                    sql = f"INSERT OR REPLACE INTO {table_name} ({', '.join(cols)}) VALUES ({placeholders})"
                    cursor.execute(sql, list(data_to_insert.values()))
                
                elif info['type'] == ProviderType.COLUMN:
                    index_col = info['index']
                    if index_col not in record:
                        continue
                    
                    cols = list(record.keys())
                    update_cols = [c for c in cols if c != index_col]
                    
                    if 'definition' in update_cols:
                        cursor.execute(f"SELECT definition FROM {table_name} WHERE {index_col} = ?", (record[index_col],))
                        row = cursor.fetchone()
                        if row:
                            current_def = row[0]
                            record['definition'] = merge_json_strings(current_def, record['definition'])

                    set_clause = ', '.join([f"{c} = ?" for c in update_cols])
                    sql = f"UPDATE {table_name} SET {set_clause} WHERE {index_col} = ?"
                    
                    values = [record[c] for c in update_cols]
                    values.append(record[index_col])
                    cursor.execute(sql, values)
            
            conn.commit()

    def run(self, run_update: bool):
        if run_update:
            for provider in self.providers:
                p_name = provider.__class__.__name__
                self.logger.info(f"Updating {p_name}...")
                try:
                    provider.update()
                except Exception as e:
                    self.logger.critical(f"FATAL: Update failed for {p_name}: {e}", exc_info=True)
                    logging.shutdown()
                    sys.exit(1)

        self.create_database()
        conn = sqlite3.connect(self.db_path)
        try:
            self._assemble_data(conn)
        finally:
            conn.close()
        self.logger.info("Database generation complete.")
