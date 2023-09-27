package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale

class ChineseWordsStore private constructor(val context: Context) {
    private val fullDict = mutableListOf<ChineseWord>()

    init {
        Log.i("ChineseWordsStore", "A new dictionary is being loaded")
        ChineseWord.HSK_Level.values().forEach {
            addFromCSVResource(it, getHSKFile(it))
        }
    }

    fun getOnlyHSKLevels(levels: Set<ChineseWord.HSK_Level>) : List<ChineseWord> {
        return fullDict.filter {
            levels.contains(it.HSK)
        }
    }

    fun getRandomWord(
        levels: Set<ChineseWord.HSK_Level>,
        bannedWords: ArrayList<ChineseWord>
    ) : ChineseWord? {
        val dict = getOnlyHSKLevels(levels)
        if (dict.isEmpty() || dict.size == bannedWords.size) return null

        var word : ChineseWord
        do {
            word = dict.random()
        } while (bannedWords.contains(word))

        Log.i("ChineseWordsStore", "New random word: $word")
        return word
    }

    fun findWordFromSimplified(simplifiedWord: String?): ChineseWord? {
        val word = fullDict.filter {
            it.simplified == simplifiedWord
        }

        if (word.isEmpty())
            return null

        return word.first()
    }

    private fun addFromCSVResource(hsk: ChineseWord.HSK_Level, reader: CSVReader) {
        var nextLine: Array<String>?
        nextLine = reader.readNext()
        while (nextLine != null) {
            fullDict.add(
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

    private fun getHSKFile(hskLevel: ChineseWord.HSK_Level) : CSVReader {
        //ToDo: optimize into a database to avoid loading all just to pull a random word
        return CSVReader(context.assets.open("hsk_csk/hsk${hskLevel.level}.csv").reader())
    }

    companion object {
        // @Todo: monitor for possible memory leak
        private var instance: ChineseWordsStore? = null

        fun getInstance(context: Context) : ChineseWordsStore {
            if (instance == null) instance = ChineseWordsStore(context)

            return instance!!
        }
    }
}