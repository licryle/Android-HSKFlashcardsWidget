# python -m venv env
#
# source env/bin/activate
# pip install -r requirements.txt
# python3 dict_gen.py
import json
import re
import sqlite3
import csv 
from datetime import datetime
from typing import List, Optional
from unidecode import unidecode

# Define ChineseWordAnotation
class ChineseWordAnnotation:
    class_level_conv = {
        "初一": "Elementary1",
        "初二": "Elementary2",
        "初三": "Elementary3",
        "初四": "Elementary4",
        "中一": "Intermediate1",
        "中二": "Intermediate2",
        "中B": "Intermediate2",
        "中三": "Intermediate3",
        "高一": "Advanced1",
        "高二": "Advanced2",
        "高三": "Advanced3",
        "其他": "NotFromClass",
        "不课": "NotFromClass"
    }

    class_type_conv = {
        "口语": "Speaking",
        "写作": "Writing",
        "读书": "Reading",
        "精读": "Reading",
        "听力": "Listening",
        "阅读": "FastReading",
        "其他": "NotFromClass",
        "不课": "NotFromClass"
    }

    def __init__(self, simplified, pinyins, notes, class_type, class_level, themes, first_seen, is_exam):
        self.a_simplified = simplified
        self.a_pinyins = pinyins
        self.notes = notes
        self.class_level = self.class_level_conv[class_level]
        self.class_type = self.class_type_conv[class_type]
        self.themes = themes
        self.first_seen = datetime.strptime(first_seen, "%Y-%m-%d").timestamp() * 1000
        self.is_exam = is_exam
        self.searchable_text = unidecode(pinyins).replace(" ", "")  + ' ' + notes + ' ' + themes + ' ' + simplified
        self.anki_id = -1


def load_annotations(csv_file):
# Open the CSV file
    with open(csv_file, mode='r', newline='') as file:
        # Create a CSV reader object
        reader = csv.reader(file)
        words = []
        
        # Loop through each row in the CSV file
        for row in reader:
            if (row[0] == ''):
                return words
            
            # row is a dictionary where the keys are the headers
            annotation = ChineseWordAnnotation(row[0], row[1], row[2], row[3], row[7], row[5],row[4],0)
            words.append(annotation)
        
        return words

# Define the ChineseWord class
class ChineseWord:    
    def __init__(self, simplified, traditional, definition, pinyins, hsk_level=None, popularity=0):
        self.simplified = simplified
        self.traditional = traditional
        self.definition = definition
        self.pinyins = pinyins
        self.popularity = popularity
        self.hsk_level = hsk_level
        self.searchable_text = simplified + ' ' + traditional + ' ' + unidecode(pinyins).replace(" ", "") + ' ' + json.dumps(definition)

# Function to extract ChineseWord from a line in cedict_ts.u8
def extract_chinese_entry(entry: str) -> Optional[ChineseWord]:
    # Define the regex pattern
    regex = r'^(\S+) (\S+) \[([^\]]+)\] /(.+)/$'
    
    # Search for the pattern in the entry
    match = re.match(regex, entry)

    if match:
        traditional, simplified, pinyins, definition = match.groups()
        
        # Create and return a ChineseWord object
        return ChineseWord(
            simplified=simplified,
            traditional=traditional,
            definition={"en": definition},
            pinyins=convert_pinyin_with_tones(pinyins)
        )
    else:
        return None

# Pinyin vowel tone mappings
tone_marks = {
    'a': ['ā', 'á', 'ǎ', 'à'],
    'e': ['ē', 'é', 'ě', 'è'],
    'i': ['ī', 'í', 'ǐ', 'ì'],
    'o': ['ō', 'ó', 'ǒ', 'ò'],
    'u': ['ū', 'ú', 'ǔ', 'ù'],
    'ü': ['ǖ', 'ǘ', 'ǚ', 'ǜ'],
    'A': ['Ā', 'Á', 'Ǎ', 'À'],
    'E': ['Ē', 'É', 'Ě', 'È'],
    'I': ['Ī', 'Í', 'Ǐ', 'Ì'],
    'O': ['Ō', 'Ó', 'Ǒ', 'Ò'],
    'U': ['Ū', 'Ú', 'Ǔ', 'Ù'],
    'Ü': ['Ǖ', 'Ǘ', 'Ǚ', 'Ǜ']
}

def convert_pinyin_with_tones(pinyin_string):
    # Regex to match syllables with tones
    pinyin_pattern = re.compile(r"([a-züÜ]+)([1-5]?)", re.IGNORECASE)
    
    def replace_tone(match):
        syllable, tone = match.groups()
        if not tone or tone == '5':  # No tone number or neutral tone, pass through
            return syllable
        
        tone_num = int(tone) - 1
        # Replace the correct vowel with the corresponding tone mark
        for vowel in 'aAeEiIoOuUüÜ':
            if vowel in syllable:
                return syllable.replace(vowel, tone_marks[vowel][tone_num])
        return syllable  # If no valid vowel found, return unchanged
    
    # Replace all syllables with tones
    return ' '.join(replace_tone(match) for match in pinyin_pattern.finditer(pinyin_string))


# Function to load Chinese words from cedict
def load_words_dict(files: List[str]) -> List[ChineseWord]:
    words = {}
    popularity = 0

    for file in files:
        with open(file, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and line[0] != '#':
                    word = extract_chinese_entry(line)
                    word.popularity = popularity
                    word.hsk_level = "NOT_HSK"
                    if word:
                        if word.simplified in words:
                            # @todo(Licryle): add special handling
                            pass

                        words[word.simplified] = word
                    else:
                        print("Issues with line: ", line)

    return words

# Function to load HSK Chinese words and add popularity ranking based on file position (but in reverse)
def load_words_hsk_popularity(files: List[str]) -> List[ChineseWord]:
    words = {}
    popularity = 0

    for file in files:
        hsk_level = int(file[33::34])
        with open(file, 'r', encoding='utf-8') as f:
            lines = f.readlines()[::-1]

            for line in lines:
                word = line.strip()
                if line:
                    words[word] = {
                        'popularity': popularity,
                        'hsk_level': "HSK" + str(hsk_level)
                    }
                    popularity += 1

    return words

# Merge words based on position (first appearance in the list is most popular)
def build_dictionary(cedict_file: str, other_files: List[str], db_file: str):
    # Parse cedict_ts.u8
    cedict_words = load_words_dict([cedict_file])
    print(f'Loaded {len(cedict_words)} words from cedict')
    
    # Parse the other files for popularity ranking
    hsk_words = load_words_hsk_popularity(other_files)
    print(f'Loaded {len(hsk_words)} words from HSK')

    for w in cedict_words.keys():
        if w in hsk_words:
            cedict_words[w].hsk_level = hsk_words[w]['hsk_level']
            cedict_words[w].popularity = hsk_words[w]['popularity']

    annotations = load_annotations('annotations.csv')
    print(f'Loaded {len(annotations)} annotations')

    # Set up SQLite database
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()

    # Create the table for Chinese words
    cursor.execute("DROP TABLE IF EXISTS chinese_word_annotation")
    cursor.execute("DROP TABLE IF EXISTS chinese_word")
    cursor.execute("CREATE TABLE IF NOT EXISTS `chinese_word_annotation` (`a_simplified` TEXT NOT NULL, `a_pinyins` TEXT, `notes` TEXT, `class_level` TEXT, `class_type` TEXT, `themes` TEXT, `first_seen` INTEGER, `is_exam` INTEGER, `a_searchable_text` TEXT DEFAULT '' NOT NULL, PRIMARY KEY(`a_simplified`))");
    cursor.execute("CREATE TABLE IF NOT EXISTS `chinese_word` (`simplified` TEXT NOT NULL, `traditional` TEXT, `definition` TEXT NOT NULL, `hsk_level` TEXT, `pinyins` TEXT, `popularity` INTEGER, `searchable_text` TEXT DEFAULT '' NOT NULL, PRIMARY KEY(`simplified`))");
    cursor.execute("CREATE INDEX IF NOT EXISTS `index_chinese_word_searchable_text` ON `chinese_word` (`searchable_text`)");
    cursor.execute("CREATE INDEX IF NOT EXISTS `index_chinese_word_annotation_searchable_text` ON `chinese_word_annotation` (`a_searchable_text`)");
    cursor.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
    cursor.execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8c11d8c0110e00f9938fbe598f89bced')");
    
    cursor.execute("DELETE FROM chinese_word")
    print("Truncated tables")

    for word in cedict_words.values():
        cursor.execute('''
        INSERT INTO chinese_word (simplified, traditional,  hsk_level, pinyins, definition, popularity, searchable_text, anki_id)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (word.simplified, word.traditional,  word.hsk_level, 
              word.pinyins, json.dumps(word.definition), word.popularity,
              word.searchable_text))

    for word in annotations:
        cursor.execute('''
        INSERT INTO chinese_word_annotation (a_simplified, a_pinyins,  notes, class_level, class_type, themes, first_seen, is_exam, a_searchable_text)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (word.a_simplified, word.a_pinyins,  word.notes, 
              word.class_level, word.class_type, word.themes, word.first_seen,
              0, word.searchable_text, word.anki_id))

    # Commit and close
    conn.commit()
    conn.close()

    print(f'Wrote {len(cedict_words)} rows into Truncated ChineseWord')
    print(f'Wrote {len(annotations)} rows into Truncated ChineseWordAnnotations')

# Example usage
cedict_file = 'cedict_ts.u8'
other_files = ['new_hsk/HSK List (Frequency)/HSK 7-9.txt', 'new_hsk/HSK List (Frequency)/HSK 6.txt', 
               'new_hsk/HSK List (Frequency)/HSK 5.txt', 'new_hsk/HSK List (Frequency)/HSK 4.txt', 
               'new_hsk/HSK List (Frequency)/HSK 3.txt', 'new_hsk/HSK List (Frequency)/HSK 2.txt', 
               'new_hsk/HSK List (Frequency)/HSK 1.txt']  # List of other files containing Chinese words
db_file = '../app/src/main/assets/databases/chinese_words.db'

build_dictionary(cedict_file, other_files, db_file)
