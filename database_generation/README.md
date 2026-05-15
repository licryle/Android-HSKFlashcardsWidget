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

The script can optionally use an LLM (Large Language Model) to populate extra fields like synonyms, antonyms, and usage examples.

1.  **Environment Variables**: Configure your settings in `.env`:
    *   `LLM_API_ENDPOINT`: The URL of your OpenAI-compatible API (e.g., LM Studio: `http://localhost:1234/v1/chat/completions`).
    *   `LLM_MODEL_NAME`: The name of the model to use.
2.  **Configuration File**: Further settings like file paths and database schema are defined in `conf.py`.

## Usage

To generate the database, run:

```bash
python3 dict_gen.py
```

This will:
1.  Delete any existing database file at the path specified in `conf.py` (defaulting to `../app/src/main/assets/databases/Mandarin_Assistant.db`).
2.  Parse `cedict_ts.u8` for base dictionary definitions.
3.  Parse HSK lists in `new_hsk/` to assign HSK levels and popularity rankings.
4.  Import user annotations from `annotations.csv`.
5.  Populate system word lists (HSK levels and Annotated words).
6.  (Optional) Call `dict_gen_ai.py` to fill in AI-generated columns, using `ai_fields_cache.db` to avoid re-querying the API for known words.

## Files Overview

*   `dict_gen.py`: The main entry point for database generation.
*   `dict_gen_ai.py`: Handles interaction with the LLM API to enrich dictionary entries.
*   `conf.py`: Configuration constants (paths, API settings, database schema).
*   `cedict_ts.u8`: The CC-CEDICT source file.
*   `annotations.csv`: CSV file containing personal annotations and study progress.
*   `new_hsk/`: Folder containing the HSK word lists.
*   `flake.nix`: Nix flake defining the reproducible development environment.
*   `requirements.txt`: Python dependencies (for non-Nix users).
*   `.env.example`: Template for environment variables.
