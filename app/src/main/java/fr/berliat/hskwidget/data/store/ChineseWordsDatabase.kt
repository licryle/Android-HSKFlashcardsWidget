package fr.berliat.hskwidget.data.store

import android.content.Context
import android.util.Log

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord

import java.util.Locale
import java.util.concurrent.Executors

@Database(entities = [AnnotatedChineseWord::class, ChineseWord::class], version = 1, exportSchema = false)
@TypeConverters(ChineseWord.Pinyins::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DateConverter::class,
    fr.berliat.hskwidget.data.store.TypeConverters.DefinitionsConverter::class)
abstract class ChineseWordsDatabase : RoomDatabase() {
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordDAO(): ChineseWordDAO

    companion object {
        //Todo: Monitor for memory leak.
        @Volatile
        private var INSTANCE: ChineseWordsDatabase? = null

        fun getInstance(context: Context): ChineseWordsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                    ChineseWordsDatabase::class.java, "chinese_words.db")
                .createFromAsset("databases/chinese_words.db")
/*            Room.databaseBuilder(context.applicationContext,
                ChineseWordsDatabase::class.java, "chinesewords.db")
                // prepopulate the database after onCreate was called
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // insert the data on the IO Thread
                        GlobalScope.async {
                            getInstance(context).chineseWordDAO().insertAll(*loadData(context))
                        }
                    }
                })*/
                .setQueryCallback(RoomDatabase.QueryCallback { sqlQuery, bindArgs ->
                    Log.d("ChineseWordsDatabase", "SQL Query: $sqlQuery SQL Args: $bindArgs")
                }, Executors.newSingleThreadExecutor())
                .build()
/*
        private fun loadData(context: Context) : Array<ChineseWord> {
            Log.d("ChineseWordsDatabase", "loadData started")
            val dict = mutableListOf<ChineseWord>()
            context.assets.open("cedict_ts.u8").bufferedReader().use {
                var nextLine = it.readLine()
                while (nextLine != null) {
                    if (nextLine.isEmpty() || nextLine[0] == '#') {
                        nextLine = it.readLine()
                        continue
                    }

                    val word = extractChineseEntry(nextLine)
                    if (word != null) {
                        dict.add(word)
                    }
                    else Log.d("ChineseWordsDatabase", "DictLine incorrect: $nextLine")

                    nextLine = it.readLine()
                }
            }
            val size = dict.size
            Log.d("ChineseWordsDatabase", "loadData returned $size words")
            return dict.toTypedArray()
        }

        private fun extractChineseEntry(entry: String): ChineseWord? {
            // Define a regex pattern to capture each part
            val regex = """^(\S+) (\S+) \[([^\]]+)\] /(.+)/${'$'}""".toRegex()

            // Apply the regex pattern on the input string
            val matchResult = regex.find(entry)

            return if (matchResult != null && matchResult.destructured.toList().size == 4) {
                val (traditional, simplified, pinyins, definition) = matchResult.destructured

                ChineseWord(
                    simplified,
                    traditional,
                    mapOf(
                        Locale.ENGLISH to definition
                    ),
                    null,
                    ChineseWord.Pinyins(pinyins),
                    0
                )
            } else {
                // Handle case where the pattern does not match
                null
            }
        }*/
    }
}