import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Input Directories
INPUTS_DIR = 'inputs'
BASE_DICT_DIR = os.path.join(INPUTS_DIR, '0_basedict')
HSK_DIR = os.path.join(INPUTS_DIR, '1_HSK')
AI_FIELDS_DIR = os.path.join(INPUTS_DIR, '2_AiFields1')
ANNOTATIONS_DIR = os.path.join(INPUTS_DIR, '3_Annotations')

# Configuration
CEDICT_FILE = os.path.join(BASE_DICT_DIR, 'cedict_ts.u8')
ANNOTATIONS_FILE = os.path.join(ANNOTATIONS_DIR, 'annotations.csv')

# HSK Submodule is now under inputs/1_HSK/new_hsk
HSK_SUBMODULE_PATH = os.path.join(HSK_DIR, 'new_hsk')
HSK_FILES = [
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 7-9.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 6.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 5.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 4.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 3.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 2.txt'),
    os.path.join(HSK_SUBMODULE_PATH, 'HSK List (Frequency)/HSK 1.txt')
]

# The database is generated locally in this folder.
# It is copied to the application's assets by the Gradle build task 'copyGeneratedDatabase'.
DB_FILE = 'output/Mandarin_Assistant.db'

SYSTEM_LISTS_CREATION_DATE = 1746863357780

# AI Configuration
BATCH_SIZE = 10  # Number of words to process in each batch
API_ENDPOINT = os.getenv('LLM_API_ENDPOINT', 'http://localhost:1234/v1/chat/completions')  # LM Studio OpenAI-compatible endpoint
MODEL_NAME = os.getenv('LLM_MODEL_NAME', 'Yi-1.5-6B-Chat-Q6_K')  # Default model name
MAX_CONSECUTIVE_FAILURES = 10  # Maximum number of consecutive API failures before exiting
AI_CACHE_DB = os.path.join(AI_FIELDS_DIR, 'ai_fields_cache.db')
AI_WORDS_WHERE_CLAUSE = "(hsk_level != 'NOT_HSK')"

DEFINITION_AI_LOCALE = 'zh_CN_HSK03'

# Chinese to English mappings for modality and type
MODALITY_MAPPING = {
    # English values
    "ORAL": "ORAL",
    "WRITTEN": "WRITTEN",
    "ORAL_WRITTEN": "ORAL_WRITTEN",
    "oral": "ORAL",
    "written": "WRITTEN",
    "oral and written": "ORAL_WRITTEN",
    "written and oral": "ORAL_WRITTEN",
    "N/A": "N/A",
    # Chinese values
    "口语": "ORAL",
    "书面": "WRITTEN",
    "口语和书面": "ORAL_WRITTEN",
    "不适用": "N/A",
    "未知": "N/A",
    "": "N/A"
}

TYPE_MAPPING = {
    # English values
    "NOUN": "NOUN",
    "VERB": "VERB",
    "ADJECTIVE": "ADJECTIVE",
    "CONJUNCTION": "CONJUNCTION",
    "PREPOSITION": "PREPOSITION",
    "INTERJECTION": "INTERJECTION",
    "IDIOM": "IDIOM",
    "noun": "NOUN",
    "verb": "VERB",
    "adjective": "ADJECTIVE",
    "adverb": "ADVERB",
    "ADVERB": "ADVERB",
    "conjunction": "CONJUNCTION",
    "preposition": "PREPOSITION",
    "interjection": "INTERJECTION",
    "idiom": "IDIOM",
    "N/A": "N/A",
    # Chinese values
    "名词": "NOUN",
    "动词": "VERB",
    "形容词": "ADJECTIVE",
    "副词": "ADVERB",
    "连词": "CONJUNCTION",
    "介词": "PREPOSITION",
    "感叹词": "INTERJECTION",
    "成语": "IDIOM",
    "俗话": "IDIOM",
    "不适用": "N/A",
    "未知": "N/A",
    "": "N/A"
}

# Database schema
AI_COLUMNS = {
    'definition': 'TEXT',
    'examples': 'TEXT',
    'modality': 'TEXT CHECK(modality IN ("ORAL", "WRITTEN", "ORAL_WRITTEN", "N/A"))',
    'type': 'TEXT CHECK(type IN ("NOUN", "VERB", "ADJECTIVE", "ADVERB", "CONJUNCTION", "PREPOSITION", "INTERJECTION",  "IDIOM", "N/A"))',
    'synonyms': 'TEXT',
    'antonym': 'TEXT'
}

REQUIRED_AI_FIELDS = list(AI_COLUMNS.keys()) + ['word']
