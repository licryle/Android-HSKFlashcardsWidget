import argparse
from lib.orchestrator import Orchestrator

def main():
    parser = argparse.ArgumentParser(description="Mandarin Assistant Database Generator")
    parser.add_argument('--update', action='store_true', help="Update local source files (download, scrape, etc.)")
    parser.add_argument('--schema', 
                        default="../crossPlatform/schemas/fr.berliat.hskwidget.data.store.ChineseWordsDatabase/3.json",
                        help="Path to the Room schema JSON file")
    parser.add_argument('--db', 
                        default="output/Mandarin_Assistant.db",
                        help="Path to the output SQLite database")
    parser.add_argument('--inputs', 
                        default="inputs",
                        help="Directory containing data providers")
    
    parser.add_argument('--report-only', action='store_true', help="Only output the summary report without generating database")
    
    args = parser.parse_args()
    
    orch = Orchestrator(args.schema, args.db)
    if not args.report_only:
        orch.discover_providers(args.inputs)
        orch.validate_providers()
    orch.run(args.update, report_only=args.report_only)

if __name__ == "__main__":
    main()
