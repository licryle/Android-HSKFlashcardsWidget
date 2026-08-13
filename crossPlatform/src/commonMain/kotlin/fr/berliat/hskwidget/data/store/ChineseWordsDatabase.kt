package fr.berliat.hskwidget.data.store

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.execSQL
import androidx.room.useWriterConnection

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name

import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.dao.ChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordFrequencyDAO
import fr.berliat.hskwidget.data.dao.WidgetListDAO
import fr.berliat.hskwidget.data.dao.WordListDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.model.ChineseWordFrequency
import fr.berliat.hskwidget.data.model.WidgetListEntry
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.model.WordListEntry
import fr.berliat.hskwidget.data.type.AnnotatedChineseWordsConverter
import fr.berliat.hskwidget.data.type.DefinitionsConverter
import fr.berliat.hskwidget.data.type.InstantConverter
import fr.berliat.hskwidget.data.type.ListTypeConverter
import fr.berliat.hskwidget.data.type.ModalityConverter
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.data.type.WordTypeConverter

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class,
        WordList::class, WordListEntry::class, WidgetListEntry::class],
    version = ChineseWordsDatabase.DATABASE_VERSION, exportSchema = true)
@TypeConverters(
    Pinyins::class,
    WordTypeConverter::class,
    ModalityConverter::class,
    InstantConverter::class,
    DefinitionsConverter::class,
    AnnotatedChineseWordsConverter::class,
    ListTypeConverter::class)

@ConstructedBy(ChineseWordsDatabaseConstructor::class)
abstract class ChineseWordsDatabase: RoomDatabase() {
    companion object {
        const val DATABASE_VERSION = 2
    }
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO
    abstract fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO
    abstract fun wordListDAO(): WordListDAO
    abstract fun widgetListDAO(): WidgetListDAO

    var _databaseFile: PlatformFile? = null
    val databaseFile
        get() = _databaseFile!!

    suspend fun snapshotToFile(): PlatformFile? = try {
        // Flush live WAL to the main file
        this.useWriterConnection { connection ->
            connection.execSQL("PRAGMA wal_checkpoint(truncate)")
        }

        val mainFile = this.databaseFile
        val dest = FileKit.cacheDir / mainFile.name
        mainFile.copyTo(dest)
        dest
    } catch (_: Exception) { null }
}

expect object ChineseWordsDatabaseConstructor : RoomDatabaseConstructor<ChineseWordsDatabase> {
    override fun initialize(): ChineseWordsDatabase
}
