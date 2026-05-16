# Database Generation

This folder contains the scripts and source data required to generate the `Mandarin_Assistant.db` SQLite database used by the application.

## Prerequisites

The easiest way to run the generation script is using [Nix](https://nixos.org/). This ensures you have the correct Python version and all dependencies installed.

If you have `direnv` installed, simply running `direnv allow` in this directory will set up the environment automatically.

Otherwise, you can manually enter the development shell:

```bash
nix develop
```

## Configuration

The script uses an Orchestrator to discover and run data providers.

1.  **Environment Variables**: Configure your settings in `.env` (used by some providers like AI enrichment):
    *   `LLM_API_ENDPOINT`: The URL of your OpenAI-compatible API.
    *   `LLM_MODEL_NAME`: The name of the model to use.
2.  **Schema**: The database structure is driven by the Room export schema located in `crossPlatform/schemas/`.

## Usage

To generate the database, run:

```bash
python3 main.py
```

Optional arguments:
*   `--update`: Triggers the `update()` method on all providers (e.g., to download new source files).
*   `--db <path>`: Specify output database path.
*   `--schema <path>`: Specify the Room schema JSON file.

### How it works

The `Orchestrator` performs the following steps:
1.  **Schema Creation**: Initializes a new SQLite database based on the Room schema.
2.  **Provider Discovery**: Automatically loads providers from the `inputs/` directory.
3.  **Data Assembly**: 
    *   Runs `TABLE` providers (e.g., `0_basedict`) to create primary word entries.
    *   Runs `COLUMN` providers (e.g., `1_HSK`, `2_Popularity`, `3_AiFields1`) to enrich existing entries or populate secondary tables.

## Inputs & Providers

Data is managed by modular providers in the `inputs/` directory:

*   **`0_basedict`**: Parses CC-CEDICT (`cedict_ts.u8`) to populate the core `chinese_word` table.
*   **`1_HSK`**: Assigns HSK levels to words and generates system HSK word lists.
*   **`2_Popularity`**: Uses word frequency data (e.g., BCC corpus) to populate the `popularity` field.
*   **`3_AiFields1`**: Enriches words with AI-generated examples, synonyms, and antonyms.
*   **`4_Annotations`**: Imports user-defined annotations from `annotations.csv`.

## Files Overview

*   `main.py`: The main entry point for database generation.
*   `lib/orchestrator.py`: Logic for database creation and provider execution.
*   `lib/base_provider.py`: Abstract base class and types for data providers.
*   `flake.nix`: Nix flake defining the reproducible development environment.
*   `requirements.txt`: Python dependencies (for non-Nix users).
