package fr.berliat.hskwidget.data.store

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PrefixedPreferencesStoreTest {

    private lateinit var fakeDataStore: FakeDataStore

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeDataStore()
        // Ensure clean caches for each test
        PrefixedPreferencesStore.instances.clear()
        PrefixedPreferencesStore.dataStoreInstances.clear()
    }

    @AfterTest
    fun tearDown() {
        PrefixedPreferencesStore.instances.clear()
        PrefixedPreferencesStore.dataStoreInstances.clear()
    }

    @Test
    fun testGetInstanceCaching() = runTest {
        val i1 = PrefixedPreferencesStore.getInstance(fakeDataStore, "p1", backgroundScope)
        val i2 = PrefixedPreferencesStore.getInstance(fakeDataStore, "p1", backgroundScope)
        val i3 = PrefixedPreferencesStore.getInstance(fakeDataStore, "p2", backgroundScope)
        assertSame(i1, i2, "Should return same instance for same prefix")
        assertNotSame(i1, i3, "Should return different instances for different prefixes")
    }

    @Test
    fun testPrefixKeyLogic() = runTest {
        val store = PrefixedPreferencesStore.getInstance(fakeDataStore, "myPref", backgroundScope)
        assertEquals("myPref_myKey", store.prefixKey("myKey"))
        
        val emptyStore = PrefixedPreferencesStore.getInstance(fakeDataStore, "", backgroundScope)
        assertEquals("myKey", emptyStore.prefixKey("myKey"))
    }

    @Test
    fun testClearRemovesOnlyPrefixedKeys() = runTest {
        val store1 = PrefixedPreferencesStore.getInstance(fakeDataStore, "prefix1", backgroundScope)
        store1.registerStringPref("myPref", "")

        val store2 = PrefixedPreferencesStore.getInstance(fakeDataStore, "prefix2", backgroundScope)
        store2.registerStringPref("myPref", "")

        // Pre-populate DataStore
        fakeDataStore.edit {
            it[stringPreferencesKey("prefix1_myPref")] = "value1"
            it[stringPreferencesKey("prefix2_myPref")] = "value2"
            it[stringPreferencesKey("unrelated")] = "unrelatedValue"
        }

        store1.clear()

        val prefs = fakeDataStore.data.first()
        assertNull(prefs[stringPreferencesKey("prefix1_myPref")], "prefix1_myPref should be cleared")
        assertEquals("value2", prefs[stringPreferencesKey("prefix2_myPref")], "prefix2_myPref should remain")
        assertEquals("unrelatedValue", prefs[stringPreferencesKey("unrelated")], "unrelated key should remain")
    }

    @Test
    fun testClearWithOverlappingPrefixes() = runTest {
        val parentStore = PrefixedPreferencesStore.getInstance(fakeDataStore, "user", backgroundScope)
        val childStore = PrefixedPreferencesStore.getInstance(fakeDataStore, "user_profile", backgroundScope)

        parentStore.registerStringPref("name", "none")
        childStore.registerStringPref("bio", "none")

        fakeDataStore.edit {
            it[stringPreferencesKey("user_name")] = "John"
            it[stringPreferencesKey("user_profile_bio")] = "Developer"
        }

        parentStore.clear()

        val prefs = fakeDataStore.data.first()
        assertNull(prefs[stringPreferencesKey("user_name")], "Parent pref should be cleared")
        assertEquals("Developer", prefs[stringPreferencesKey("user_profile_bio")], "Child pref should NOT be cleared")
    }
    
    @Test
    fun testValuePersistence() = runTest {
        val store = PrefixedPreferencesStore.getInstance(fakeDataStore, "a_prefix", backgroundScope)
        val myStringPref = store.registerStringPref("a_string", "default")
        store.ensureAllLoaded()

        assertEquals("default", myStringPref.value)

        myStringPref.value = "new_value"
        
        // In-memory update is immediate with FakeDataStore if we don't need to wait for disk
        assertEquals("new_value", myStringPref.value)

        // Clear instance cache to force recreation of PrefixedPreferencesStore
        PrefixedPreferencesStore.instances.clear()

        val newStore = PrefixedPreferencesStore.getInstance(fakeDataStore, "a_prefix", backgroundScope)
        val newMyStringPref = newStore.registerStringPref("a_string", "default")
        newStore.ensureAllLoaded()

        assertEquals("new_value", newMyStringPref.value)
    }

    @Test
    fun testEnsureAllLoaded() = runTest {
        // Pre-populate datastore
        fakeDataStore.edit {
            it[stringPreferencesKey("loadTest_pref1")] = "value1"
            it[stringPreferencesKey("loadTest_pref2")] = "value2"
        }

        val store = PrefixedPreferencesStore.getInstance(fakeDataStore, "loadTest", backgroundScope)
        val pref1 = store.registerStringPref("pref1", "default1")
        val pref2 = store.registerStringPref("pref2", "default2")

        // Values should be defaults initially (before loading)
        assertEquals("default1", pref1.value)
        assertEquals("default2", pref2.value)

        store.ensureAllLoaded()

        // After loading, values should be updated from DataStore
        assertEquals("value1", pref1.value)
        assertEquals("value2", pref2.value)
    }
}
