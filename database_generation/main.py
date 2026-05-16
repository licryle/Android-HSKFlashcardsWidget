import argparse
from lib.orchestrator import Orchestrator

def main():
    parser = argparse.ArgumentParser(description="Mandarin Assistant Database Generator")
    parser.add_argument('--update', action='store_true', help="Update local source files (download, scrape, etc.)")
    parser.add_argument('--schema', 
                        default="../crossPlatform/schemas/fr.berliat.hskwidget.data.store.ChineseWordsDatabase/1.json",
                        help="Path to the Room schema JSON file")
    parser.add_argument('--db', 
                        default="output/Mandarin_Assistant.db",
                        help="Path to the output SQLite database")
    parser.add_argument('--inputs', 
                        default="inputs",
                        help="Directory containing data providers")
    
    args = parser.parse_args()
    
    orch = Orchestrator(args.schema, args.db)
    orch.discover_providers(args.inputs)
    orch.validate_providers()
    orch.run(args.update)

if __name__ == "__main__":
    main()
