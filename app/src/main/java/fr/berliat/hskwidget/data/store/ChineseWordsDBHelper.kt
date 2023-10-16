package fr.berliat.hskwidget.data.store

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.opencsv.CSVReader
import fr.berliat.hskwidget.data.model.ChineseWord
import java.util.Locale

class ChineseWordsDBHelper(var context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        Log.i("ChineseWordsDBHelper", "Creating Database")
        db.execSQL(CREATE_TABLE)

        getDictFromCSV().forEach {
            val row = ContentValues()
            row.put(SIMPLIFIED, it.simplified)
            row.put(DEFINITION_EN, it.definition[Locale.ENGLISH])
            row.put(HSK, it.HSK.level)
            row.put(PINYINS, it.pinyins.toString())
            db.insert(TABLE_NAME, null, row)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i("ChineseWordsDBHelper", "Upgrading Database")
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    fun getDictFromCSV(): Array<ChineseWord> {
        Log.i("ChineseWordsStore", "Dictionary is being loaded from disk")

        val fullDict = mutableListOf<ChineseWord>()
        ChineseWord.HSK_Level.values().forEach {
            addFromCSVResource(it, fullDict, getHSKFile(it))
        }

        return fullDict.toTypedArray()
    }

    private fun addFromCSVResource(
        hsk: ChineseWord.HSK_Level, fullDict: MutableList<ChineseWord>,
        reader: CSVReader
    ) {
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

    private fun getHSKFile(hskLevel: ChineseWord.HSK_Level): CSVReader {
        return CSVReader(context.assets.open("hsk_csk/hsk${hskLevel.level}.csv").reader())
    }

    companion object {
        // Table Name
        const val TABLE_NAME = "hsk_words"

        // Table columns
        const val _ID = "_id"
        const val SIMPLIFIED = "simplified"
        const val DEFINITION_EN = "definition_en"
        const val HSK = "hsk_level"
        const val PINYINS = "pinyin"

        // Database Information
        const val DB_NAME = "chinesewords.db"

        // database version
        const val DB_VERSION = 1

        // Creating table query
        private const val CREATE_TABLE = ("CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SIMPLIFIED + " TEXT NOT NULL, "
                + DEFINITION_EN + " TEXT NOT NULL, "
                + HSK + " INTEGER NOT NULL,"
                + PINYINS + " TEXT NOT NULL);")
    }
}