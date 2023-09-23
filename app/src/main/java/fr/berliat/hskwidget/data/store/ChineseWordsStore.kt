package fr.berliat.hskwidget.data.store

import android.content.Context
import com.opencsv.CSVReader
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWords
import java.util.Locale

class ChineseWordsStore {
    companion object {
        fun getHSKFile(context: Context, hskLevel: ChineseWord.HSK_Level) : CSVReader {
            //ToDo: optimize into a database to avoid loading all just to pull a random word
            return CSVReader(context.assets.open("hsk_csk/hsk${hskLevel.level}.csv").reader())
        }

        fun getByHSKLevels(context: Context, levels: Array<ChineseWord.HSK_Level>) : ChineseWords {
            val words = ChineseWords()

            levels.forEach {
                addFromCSVResource(words, it, getHSKFile(context, it))
            }

            return words
        }

        fun getRandomWord(context: Context, levels: Array<ChineseWord.HSK_Level>,
                          bannedWords: ChineseWords) : ChineseWord? {
            val words = getByHSKLevels(context, levels)
            if (words.size == 0) return null

            var newWord : ChineseWord?

            do {
                newWord = words.random()
            } while (bannedWords.contains(newWord))

            return newWord
        }

        fun addFromCSVResource(words: ChineseWords, hsk: ChineseWord.HSK_Level, reader: CSVReader) {
            var nextLine: Array<String>?
            nextLine = reader.readNext()
            while (nextLine != null) {
                words.add(
                    ChineseWord(
                        nextLine[0],
                        "",
                        mapOf(Locale.ENGLISH to nextLine[2]),
                        hsk,
                        ChineseWord.Pinyins(nextLine[1])
                    )
                )

                nextLine = reader.readNext()
            }
        }
    }
}