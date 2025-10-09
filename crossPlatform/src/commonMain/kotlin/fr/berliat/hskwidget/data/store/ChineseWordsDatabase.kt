package fr.berliat.hskwidget.data.store

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
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
import io.github.vinceglb.filekit.PlatformFile

@Database(
    entities = [ChineseWordAnnotation::class, ChineseWord::class, ChineseWordFrequency::class,
        WordList::class, WordListEntry::class, WidgetListEntry::class],
    version = 1, exportSchema = true)
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
    abstract fun annotatedChineseWordDAO(): AnnotatedChineseWordDAO
    abstract fun chineseWordAnnotationDAO(): ChineseWordAnnotationDAO
    abstract fun chineseWordDAO(): ChineseWordDAO
    abstract fun chineseWordFrequencyDAO(): ChineseWordFrequencyDAO
    abstract fun wordListDAO(): WordListDAO
    abstract fun widgetListDAO(): WidgetListDAO

    var _databaseFile : PlatformFile? = null
    val databaseFile
        get() = _databaseFile!!
}

@Suppress("KotlinNoActualForExpect")
expect object ChineseWordsDatabaseConstructor : RoomDatabaseConstructor<ChineseWordsDatabase> {
    override fun initialize(): ChineseWordsDatabase
}

expect suspend fun ChineseWordsDatabase.snapshotToFile(): PlatformFile?