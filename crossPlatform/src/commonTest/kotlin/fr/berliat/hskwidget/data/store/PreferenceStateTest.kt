package fr.berliat.hskwidget.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceStateTest {

    private lateinit var fakeDataStore: FakeDataStore
    private val testKey = stringPreferencesKey("test_key")

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeDataStore()
    }

    @Test
    fun testInitialValueLoaded() = runTest(UnconfinedTestDispatcher()) {
        val initialValue = "default"
        val state = PreferenceState(fakeDataStore, testKey, initialValue, coroutineScope = backgroundScope)

        state.ensureLoaded()
        assertEquals(initialValue, state.value)
    }

    @Test
    fun testValueUpdatesPersistToStore() = runTest(UnconfinedTestDispatcher()) {
        val state = PreferenceState(fakeDataStore, testKey, "default", coroutineScope = backgroundScope)
        state.ensureLoaded()

        state.value = "new_value"

        // With UnconfinedTestDispatcher, the optimistic update is immediate
        assertEquals("new_value", state.value)

        // advanceUntilIdle will ensure the write loop (which suspends on DataStore.edit) completes
        advanceUntilIdle()

        // Check the value in DataStore
        assertEquals("new_value", fakeDataStore.latestPreferences[testKey])
    }

    @Test
    fun testStoreUpdatesReflectInState() = runTest(UnconfinedTestDispatcher()) {
        val state = PreferenceState(fakeDataStore, testKey, "default", coroutineScope = backgroundScope)
        state.ensureLoaded()

        // Simulate external change in DataStore
        fakeDataStore.edit { it[testKey] = "external_change" }

        // Wait for the observation loop in PreferenceState to catch the change
        advanceUntilIdle()

        assertEquals("external_change", state.value)
    }

    @Test
    fun testWithConverter() = runTest(UnconfinedTestDispatcher()) {
        val intKey = androidx.datastore.preferences.core.intPreferencesKey("int_key")
        val converter = PreferenceConverter<Int, String>(
            fromStore = { it.toString() },
            toStore = { it.toInt() }
        )

        val state = PreferenceState(fakeDataStore, intKey, "0", converter, coroutineScope = backgroundScope)
        state.ensureLoaded()

        state.value = "42"
        advanceUntilIdle()

        assertEquals(42, fakeDataStore.latestPreferences[intKey])
    }
}

/**
 * A more robust FakeDataStore that correctly handles atomic updates.
 */
class FakeDataStore : DataStore<Preferences> {
    private val _data = MutableStateFlow(emptyPreferences())
    override val data: StateFlow<Preferences> = _data.asStateFlow()

    val latestPreferences: Preferences get() = _data.value

    private val mutex = Mutex()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val current = _data.value
            val next = transform(current)
            _data.value = next
            next
        }
    }
}
