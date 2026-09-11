import os
import json
import sqlite3
import logging
import time
import re
from typing import Dict, Any, Iterator, Tuple, List

from lib.utils_ai import call_llm_api
from lib import Provider, ProviderType, BATCH_SIZE, API_ENDPOINT, MODEL_NAME

# Keep the same directory-based cache convention used by the other AI-backed providers.
FRENCH_CACHE_DB = os.path.join(os.path.dirname(__file__), 'language_french_cache.db')
CEDICT_FILE = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '0_basedict', 'cedict_ts.u8'))
CFDICT_FILE = os.path.abspath(os.path.join(os.path.dirname(__file__), 'cfdict.u8'))


def generate_prompt(items: List[Dict[str, str]]) -> str:
    """Prompt the LLM with richer examples and a Chinese-first French glossing rule.

    The English gloss should act as the lexical anchor only. The source Chinese entry remains
    the meaning authority, so French should be driven by the Chinese word, not a literal
    English-to-French glossing pass.
    """
    lines = []
    for item in items:
        word = item['word']
        english = item['english']
        lines.append(f"{word} | {english}")

    examples = [
        {"word": "做", "english": "to make; to produce/to write; to compose/to do; to engage in; to hold (a party etc)/(of a person) to be (an intermediary, a good student etc); to become (husband and wife, friends etc)/(of a thing) to serve as; to be used for/to assume (an air or manner)",
                       "fr": "faire; fabriquer/produire; écrire; composer/faire; se livrer à; organiser (une fête, etc.)/(pour une personne) être (un intermédiaire, un bon élève, etc.); devenir (mari et femme, amis, etc.)/(pour une chose) servir de; être utilisé pour/prendre (un air ou une attitude)"},
        {"word": "学校", "english": "school; educational institution", "fr": "école; établissement d'enseignement"},
        {"word": "龙虎", "english": "outstanding people; water and fire (in Daoist writing)", "fr": "personnes exceptionnelles; eau et feu (concept Daoïste)"},
        {"word": "小朋友", "english": "child; kid (used to refer to a child, or by an adult to address a child, or, in Taiwan, to refer to sb's own child)/CL:個|个[ge4]", 
                          "fr": "enfant; gamin (utilisé pour désigner un enfant, ou par un adulte pour s'adresser à un enfant, ou, à Taïwan, pour désigner son propre enfant)/CL:個|个[ge4]"},
        {"word": "喜欢", "english": "to like; to be fond of; to enjoy", "fr": "aimer; apprécier; avoir envie de"},
        {"word": "健美运动", "english": "bodybuilding", "fr": "culturisme; sport de musculation"},
        {"word": "捐献", "english": "to donate; to contribute", "fr": "faire un don; contribuer"},
        {"word": "城阳", "english": "Chengyang district of Qingdao city 青島市|青岛市, Shandong", "fr": "district de Chengyang de la ville de Qingdao 青島市|青岛市, Shandong"},
        {"word": "艳如桃李", "english": "lit. beautiful as peach and prune/fig. radiant beauty", "fr": "lit. aussi beau que les fleurs de pêcher et de prunier/fig. beauté radieuse"},
    ]
    example_lines = '\n'.join(f'{ex["word"]} | {ex["english"]} -> {ex["fr"]}' for ex in examples)

    return f"""<|system|>
You are a precise Chinese-to-French lexicography assistant. Preserve the Chinese lexical meaning and produce natural French.
You are given a Chinese word and an English gloss. Use the English gloss only as a sense anchor.
Do not perform a straight English-to-French translation if that would drift from the Chinese word's own meaning.
Return a valid JSON array of objects.
<|user|>
Translate the meanings of the Chinese entries below into French.
For each entry, output exactly:
1. "word": the simplified Chinese word.
2. "fr": the French translation of the definition, concise and dictionary-like.

Important constraints:
- Output MUST be valid JSON.
- No markdown.
- No extra text, no extra commentary.
- Keep the French translation concise and dictionary-like.
- Preserve multi-sense distinctions when they come from the Chinese source.
- Prefer a terms-first translation: use Chinese form as the meaning source, not English word order.

Examples:
{example_lines}

Entries to translate:
{chr(10).join(lines)}

Expected format:
[
  {{"word": "做", "fr": "faire; préparer; fabriquer"}}
]
"""


class LanguageFrenchProvider(Provider):
    """Append a French locale string to the existing JSON definition map in chinese_word.

    Implementation follows the established AI provider cache pattern:
    - update(): query LLM and persist translations in a local cache DB.
    - data(): read the cache DB and emit column updates for the orchestrator.
    """

    def __init__(self):
        self.logger = logging.getLogger(__name__)

    def _get_cache_conn(self):
        os.makedirs(os.path.dirname(FRENCH_CACHE_DB), exist_ok=True)
        conn = sqlite3.connect(FRENCH_CACHE_DB)
        cursor = conn.cursor()
        cursor.execute('''CREATE TABLE IF NOT EXISTS `chinese_word` (
                            `simplified` TEXT NOT NULL,
                            `definition` TEXT NOT NULL,
                            PRIMARY KEY(`simplified`)
                        )''')
        conn.commit()
        return conn

    def _parse_cedict_entry(self, line: str) -> Dict[str, str]:
        """Extract the simplified word and the English gloss from a CEDICT-style line."""
        match = re.match(r'^([^\s]+)\s+([^\s]+)\s+\[([^\]]+)\]\s+/(.+)/$', line.strip())
        if not match:
            return {}

        traditional, simplified, pinyin, english = match.groups()
        return {
            'word': simplified,
            'english': english,
        }

    def _load_cfdict_word_map(self) -> List[Dict[str, str]]:
        """Read the CFDICT French file. It is the first translation lookup layer.

        Each line is a CEDICT-like record, but the glosses are written in French instead of
        English. We normalize those lines into a (simplified word -> French gloss) queue
        and seed the cache DB before any LLM pass.
        """
        queue: List[Dict[str, str]] = []
        if not os.path.exists(CFDICT_FILE):
            return queue

        seen = set()
        with open(CFDICT_FILE, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue

                # Same structure as the language dictionary files: traditional simplified [pinyin] /French gloss/
                match = re.match(r'^([^\s]+)\s+([^\s]+)\s+\[([^\]]+)\]\s+/(.+)/$', line)
                if not match:
                    continue

                traditional, simplified, pinyin, french = match.groups()
                # Split the slash-delimited glosses into a single French gloss string.
                # Preserve the slash-separated sense list as a human-readable French sentence.
                parts = [part.strip() for part in french.split('/') if part.strip()]
                fr = '; '.join(parts)
                if not fr:
                    continue

                if simplified in seen:
                    continue
                seen.add(simplified)
                queue.append({
                    'word': simplified,
                    'fr': fr,
                })

        return queue

    def _load_cedict_word_map(self) -> List[Dict[str, str]]:
        """Read the English CEDICT file and build the LLM fallback queue.

        The queue is a list of source Chinese words and English glosses used only for
        the words that are not already covered by the CFDICT cache.
        """
        queue: List[Dict[str, str]] = []
        if not os.path.exists(CEDICT_FILE):
            return queue

        seen = set()
        with open(CEDICT_FILE, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                row = self._parse_cedict_entry(line)
                if not row:
                    continue
                word = row['word']
                if word in seen:
                    continue
                seen.add(word)
                queue.append({
                    'word': word,
                    'english': row['english'],
                })

        return queue

    def update(self):
        """Populate the French cache in the requested sequence:

        1. Seed the cache from the CFDICT file when a French definition exists.
        2. Use the CEDICT word stream to build the LLM fallback queue.
        3. Call the LLM only for the queued words that remain absent from the cache.
        """
        conn = self._get_cache_conn()
        cursor = conn.cursor()

        # CEDICT is the authoritative source for chinese_word.  CFDICT has additional
        # headwords, but definitions for those cannot satisfy the foreign key and must
        # never enter the cache used for the generated application database.
        cedict_queue = self._load_cedict_word_map()
        base_words = {item['word'] for item in cedict_queue}

        # Step 1: seed only French entries that have a base-dictionary parent.
        cfdict_queue = [item for item in self._load_cfdict_word_map()
                        if item['word'] in base_words]
        if cfdict_queue:
            self.logger.info(f"LanguageFrenchProvider: seeding {len(cfdict_queue)} French entries from CFDICT.")
            for item in cfdict_queue:
                word = item['word']
                fr = item['fr']
                definition_json = json.dumps({'fr': fr}, ensure_ascii=False)
                cursor.execute(
                    "INSERT OR REPLACE INTO chinese_word (simplified, definition) VALUES (?, ?)",
                    (word, definition_json)
                )
            conn.commit()

        # Step 2: use the CEDICT queue for the English anchor, then compare against cache words.
        cursor.execute("SELECT simplified FROM chinese_word")
        cached_words = {row[0] for row in cursor.fetchall()}

        missing = [item for item in cedict_queue if item['word'] not in cached_words]
        if not missing:
            self.logger.info("LanguageFrenchProvider: no missing words left for the LLM fallback.")
            conn.close()
            return

        self.logger.info(f"LanguageFrenchProvider: found {len(missing)} words missing from cache for LLM generation.")

        # Step 3: generate only the words absent from the CFDICT cache.
        for i in range(0, len(missing), BATCH_SIZE):
            batch = missing[i:i + BATCH_SIZE]
            prompt = generate_prompt(batch)
            required_fields = ['word', 'fr']

            ai_results = call_llm_api(API_ENDPOINT, MODEL_NAME, prompt, required_fields)
            if ai_results:
                for res in ai_results:
                    word = res.get('word')
                    fr = res.get('fr')
                    if not word or word not in base_words or not fr:
                        continue

                    # Keep the JSON locale map shape consistent with the rest of the repository.
                    definition_json = json.dumps({'fr': fr}, ensure_ascii=False)
                    cursor.execute(
                        "INSERT OR REPLACE INTO chinese_word (simplified, definition) VALUES (?, ?)",
                        (word, definition_json)
                    )

                conn.commit()
                self.logger.info(f"LanguageFrenchProvider: progress {i + len(batch)}/{len(missing)}")
            else:
                self.logger.warning("LanguageFrenchProvider: LLM returned no valid results for batch. Skipping.")

            time.sleep(1)

        conn.close()

    def schema(self) -> Dict[str, Dict[str, Any]]:
        return {
            "word_definition": {
                "type": ProviderType.TABLE,
                "columns": ["simplified", "language", "definition"]
            }
        }

    def data(self) -> Iterator[Tuple[str, Dict[str, Any]]]:
        if not os.path.exists(FRENCH_CACHE_DB):
            return

        # The cache can predate this constraint.  Filter again at assembly time rather
        # than allowing stale CFDict-only cache rows to break database generation.
        base_words = {item['word'] for item in self._load_cedict_word_map()}
        conn = sqlite3.connect(FRENCH_CACHE_DB)
        cursor = conn.cursor()
        cursor.execute("SELECT simplified, definition FROM chinese_word")
        for row in cursor.fetchall():
            simplified, definition_json = row
            if simplified not in base_words:
                continue
            # The cache DB stores per-word {"fr": ...} JSON. Feed that to the orchestrator.
            definition = json.loads(definition_json).get("fr")
            if not definition:
                continue
            yield ("word_definition", {
                "simplified": simplified,
                "language": "fr",
                "definition": definition
            })
        conn.close()
