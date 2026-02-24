package fr.berliat.hskwidget.data.store

import io.github.vinceglb.filekit.BookmarkData
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferenceConverterTest {

    @Test
    fun testPreferenceConverterCustom() {
        val converter = PreferenceConverter<Int, String>(
            fromStore = { it.toString() },
            toStore = { it.toInt() }
        )

        assertEquals("42", converter.fromStore(42))
        assertEquals(42, converter.toStore("42"))
    }

    @Test
    fun testByteArrayPreferenceConverter_toStore() {
        val converter = FileKitBookmarkPreferenceConverter()
        
        // Null should be converted to empty string
        assertEquals("", converter.toStore(null))
        
        // Data should be Base64 encoded
        val bytes = byteArrayOf(1, 2, 3)
        val bookmark = BookmarkData(bytes)
        assertEquals(Base64.encode(bytes), converter.toStore(bookmark))
    }

    @Test
    fun testByteArrayPreferenceConverter_fromStore() {
        val converter = FileKitBookmarkPreferenceConverter()
        
        // Empty string should be null
        assertNull(converter.fromStore(""))
        
        // Valid Base64 should be decoded
        val bytes = byteArrayOf(4, 5, 6)
        val encoded = Base64.encode(bytes)
        val decoded = converter.fromStore(encoded)

        // THe bookmark can t be resolved.
        assertEquals(null, decoded?.bytes?.toList())
        
        // Invalid Base64 should be null
        assertNull(converter.fromStore("invalid base64!"))
    }

    //TODO: Add a test for happy case, but that suggests we have a real BookMark.
}
