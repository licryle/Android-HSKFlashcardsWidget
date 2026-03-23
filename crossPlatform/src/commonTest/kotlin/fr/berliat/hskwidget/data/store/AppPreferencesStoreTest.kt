package fr.berliat.hskwidget.data.store

import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.domain.SearchQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppPreferencesStoreTest {

    private lateinit var fakeDataStore: FakeDataStore

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeDataStore()
        // Clear singleton instances to ensure a fresh start and avoid scope leakage
        AppPreferencesStore.instances.clear()
        PrefixedPreferencesStore.instances.clear()
    }

    @Test
    fun testInstantConverter() = runTest(UnconfinedTestDispatcher()) {
        val store = AppPreferencesStore.getInstance(fakeDataStore, scope = backgroundScope)
        val instant = Instant.fromEpochMilliseconds(123456789L)
        store.dbBackupCloudLastSuccess.value = instant
        advanceUntilIdle()

        val savedLong = fakeDataStore.latestPreferences[longPreferencesKey("database_backup_cloud_lastsuccess")]
        assertEquals(123456789L, savedLong)
        assertEquals(instant, store.dbBackupCloudLastSuccess.value)
    }

    @Test
    fun testClassLevelConverter() = runTest(UnconfinedTestDispatcher()) {
        val store = AppPreferencesStore.getInstance(fakeDataStore, scope = backgroundScope)
        store.lastAnnotatedClassLevel.value = ClassLevel.Intermediate2
        advanceUntilIdle()

        val savedString = fakeDataStore.latestPreferences[stringPreferencesKey("class_level")]
        assertEquals("Intermediate2", savedString)
        assertEquals(ClassLevel.Intermediate2, store.lastAnnotatedClassLevel.value)
    }

    @Test
    fun testClassTypeConverter() = runTest(UnconfinedTestDispatcher()) {
        val store = AppPreferencesStore.getInstance(fakeDataStore, scope = backgroundScope)
        store.lastAnnotatedClassType.value = ClassType.Listening
        advanceUntilIdle()

        val savedString = fakeDataStore.latestPreferences[stringPreferencesKey("class_type")]
        assertEquals("Listening", savedString)
        assertEquals(ClassType.Listening, store.lastAnnotatedClassType.value)
    }

    @Test
    fun testTextSizeConverter() = runTest(UnconfinedTestDispatcher()) {
        val store = AppPreferencesStore.getInstance(fakeDataStore, scope = backgroundScope)
        val size = 42f.sp
        store.readerTextSize.value = size
        advanceUntilIdle()

        val savedFloat = fakeDataStore.latestPreferences[floatPreferencesKey("reader_text_size")]
        assertEquals(42f, savedFloat)
        assertEquals(size, store.readerTextSize.value)
    }

    @Test
    fun testSearchQueryConverter() = runTest(UnconfinedTestDispatcher()) {
        val store = AppPreferencesStore.getInstance(fakeDataStore, scope = backgroundScope)
        val query = SearchQuery.fromString("test query")
        store.searchQuery.value = query
        advanceUntilIdle()

        val savedString = fakeDataStore.latestPreferences[stringPreferencesKey("search_query")]
        assertEquals(query.toString(), savedString)
        assertEquals(query.query, store.searchQuery.value.query)
    }
}
