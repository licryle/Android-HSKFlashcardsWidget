package fr.berliat.hskwidget.data.store

import android.content.Context
import android.database.Cursor
import android.util.Log
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale


class ChineseWordsStore private constructor(val context: Context) {
    private val dbHelper = ChineseWordsDBHelper(context)
    private val database = dbHelper.readableDatabase

    private val projection = arrayOf(
        ChineseWordsDBHelper.SIMPLIFIED,
        ChineseWordsDBHelper.PINYINS,
        ChineseWordsDBHelper.HSK,
        ChineseWordsDBHelper.DEFINITION_EN
    )

    fun getOnlyHSKLevels(levels: Set<ChineseWord.HSK_Level>): Array<ChineseWord> {
        return _getOnlyHSKLevels(levels, arrayListOf(), "", "")
    }

    private fun _getOnlyHSKLevels(
        levels: Set<ChineseWord.HSK_Level>, bannedWord: ArrayList<ChineseWord>,
        orderBy: String, limit: String
    ): Array<ChineseWord> {
        // Filter results WHERE "title" = 'My Title'
        val sel =
            "${ChineseWordsDBHelper.HSK} IN (" + levels.map { it.level }.joinToString() + ") " +
                    "AND ${ChineseWordsDBHelper.SIMPLIFIED} NOT IN (?)"

        val cursor = database.query(
            ChineseWordsDBHelper.TABLE_NAME,             // The table to query
            projection,                                  // The array of columns to return (pass null to get all)
            sel,                                         // The columns for the WHERE clause
            bannedWord.map { it.simplified }.toTypedArray(), // The values for the WHERE clause
            null,                                // don't group the rows
            null,                                 // don't filter by row groups
            orderBy,
            limit
        )

        val dict = mutableSetOf<ChineseWord>()
        with(cursor) {
            while (moveToNext()) {
                dict.add(cursorToWord(cursor))
            }
        }
        cursor.close()

        return dict.toTypedArray()
    }

    fun getRandomWord(
        levels: Set<ChineseWord.HSK_Level>,
        bannedWords: ArrayList<ChineseWord>
    ) : ChineseWord? {
        val dict = _getOnlyHSKLevels(levels, bannedWords, "RANDOM()", "1")
        if (dict.isEmpty()) return null

        var word: ChineseWord
        do {
            word = dict.random()
        } while (bannedWords.contains(word))

        Log.i("ChineseWordsStore", "New random word: $word")
        return word
    }

    fun findWordFromSimplified(simplifiedWord: String?): ChineseWord? {
        // Filter results WHERE "title" = 'My Title'
        val simpSel = "${ChineseWordsDBHelper.SIMPLIFIED} IN (?)"

        val cursor = database.query(
            ChineseWordsDBHelper.TABLE_NAME,             // The table to query
            projection,                                  // The array of columns to return (pass null to get all)
            simpSel,                                     // The columns for the WHERE clause
            arrayOf(simplifiedWord),                     // The values for the WHERE clause
            null,                                // don't group the rows
            null,                                 // don't filter by row groups
            ""
        )

        if (!cursor.moveToNext())
            return null

        val word = cursorToWord(cursor)
        cursor.close()

        return word
    }

    private fun cursorToWord(cursor: Cursor): ChineseWord {
        with(ChineseWordsDBHelper) {
            return ChineseWord(
                cursor.getString(cursor.getColumnIndexOrThrow(SIMPLIFIED)),
                "",
                mapOf(
                    Locale.ENGLISH to cursor.getString(
                        cursor.getColumnIndexOrThrow(DEFINITION_EN)
                    )
                ),
                ChineseWord.HSK_Level.from(
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(HSK)
                    )
                ),
                ChineseWord.Pinyins(
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(PINYINS)
                    )
                )
            )
        }
    }

    companion object {
        //Todo: Monitor for memory leak.
        @Volatile
        private var instance: ChineseWordsStore? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: ChineseWordsStore(context).also { instance = it }
            }
    }
}