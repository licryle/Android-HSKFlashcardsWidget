import os
import sys
import importlib.util
import traceback
import random
from collections import defaultdict

# Ensure the root of database_generation is in sys.path so 'lib' can be imported
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

from lib import Provider

def test_providers(inputs_dir: str):
    print(f"Scanning for providers in {os.path.abspath(inputs_dir)}...\n")
    
    if not os.path.exists(inputs_dir):
        print(f"Error: Inputs directory '{inputs_dir}' not found.")
        return

    for item in sorted(os.listdir(inputs_dir)):
        item_path = os.path.join(inputs_dir, item)
        provider_file = os.path.join(item_path, "provider.py")
        
        if os.path.isdir(item_path) and os.path.exists(provider_file):
            print(f"=== Testing Provider in {item} ===")
            
            try:
                # Load the provider module
                module_name = f"inputs.{item}.provider"
                spec = importlib.util.spec_from_file_location(module_name, provider_file)
                module = importlib.util.module_from_spec(spec)
                
                # We don't necessarily need to add the provider_dir to sys.path 
                # if they import from 'lib', as 'lib' is in the root path.
                
                spec.loader.exec_module(module)
                
                # Find the Provider class
                provider_instance = None
                for attr_name in dir(module):
                    attr = getattr(module, attr_name)
                    if isinstance(attr, type) and issubclass(attr, Provider) and attr is not Provider:
                        provider_instance = attr()
                        break
                
                if not provider_instance:
                    print(f"  [ERROR] No Provider class found in {provider_file}")
                    continue

                # 1. Test Schema
                schema = provider_instance.schema()
                print(f"  [OK] Schema: {list(schema.keys())}")

                # 2. Test Data
                print("  [INFO] Processing full dataset...")
                all_data = defaultdict(list)
                
                for table, record in provider_instance.data():
                    all_data[table].append(record)
                
                # Summary and Random Sampling
                counts = {table: len(records) for table, records in all_data.items()}
                
                for table in sorted(all_data.keys()):
                    records = all_data[table]
                    if not records:
                        print(f"    [WARNING] Table '{table}' has no records.")
                        continue
                        
                    # Draw a random sample from the set
                    idx = random.randrange(len(records))
                    sample_record = records[idx]
                    
                    print(f"    Random sample for '{table}' (record #{idx + 1} of {len(records)}):")
                    for k, v in sample_record.items():
                        val_str = str(v)
                        if len(val_str) > 100:
                            val_str = val_str[:97] + "..."
                        print(f"      {k}: {val_str}")
                
                print(f"  [OK] Data summary: {counts}")

            except Exception as e:
                print(f"  [ERROR] Failed to test provider {item}: {e}")
                traceback.print_exc()
            print()

if __name__ == "__main__":
    test_providers("inputs")
