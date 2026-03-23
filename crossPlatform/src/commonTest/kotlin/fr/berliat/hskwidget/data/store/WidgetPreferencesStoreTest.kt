package fr.berliat.hskwidget.data.store

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetPreferencesStoreTest {

    private lateinit var fakeDataStore: FakeDataStore
    private val widgetId = 42

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeDataStore()
        // Clear instances to ensure a clean state for each test since they are singletons in-memory
        WidgetPreferencesStore.instances.clear()
        PrefixedPreferencesStore.instances.clear()
    }

    @Test
    fun testWidgetIdAndPrefix() = runTest(UnconfinedTestDispatcher()) {
        val store = WidgetPreferencesStore.getInstance(fakeDataStore, widgetId, scope = backgroundScope)
        assertEquals(widgetId, store.widgetId)
        
        store.currentWord.value = "你好"
        advanceUntilIdle()

        // Verify the key in DataStore has the correct prefix
        val expectedKey = stringPreferencesKey("widget_${widgetId}_current_word")
        assertEquals("你好", fakeDataStore.latestPreferences[expectedKey])
    }

    @Test
    fun testMultipleWidgetInstances() = runTest(UnconfinedTestDispatcher()) {
        val store1 = WidgetPreferencesStore.getInstance(fakeDataStore, 1, scope = backgroundScope)
        val store2 = WidgetPreferencesStore.getInstance(fakeDataStore, 2, scope = backgroundScope)

        store1.currentWord.value = "Word1"
        store2.currentWord.value = "Word2"
        advanceUntilIdle()

        assertEquals("Word1", fakeDataStore.latestPreferences[stringPreferencesKey("widget_1_current_word")])
        assertEquals("Word2", fakeDataStore.latestPreferences[stringPreferencesKey("widget_2_current_word")])
    }
}
