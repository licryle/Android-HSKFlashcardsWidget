package fr.berliat.hskwidget.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleTest {

    @Test
    fun testFromCode() {
        assertEquals(Locale.ENGLISH, Locale.fromCode("en"))
        assertEquals(Locale.CHINESE, Locale.fromCode("zh"))
        assertEquals(Locale.FRENCH, Locale.fromCode("fr"))
        assertEquals(Locale.SPANISH, Locale.fromCode("es"))
        assertEquals(Locale.CN_HSK3, Locale.fromCode("zh_CN_HSK03"))
        assertEquals(Locale.SIMPLIFIED_CHINESE, Locale.fromCode("zh_CN"))
        assertEquals(null, Locale.fromCode("unknown"))
    }

    @Test
    fun testGetDefault() {
        assertEquals(Locale.ENGLISH, Locale.getDefault())
    }

    @Test
    fun testSerialization() {
        val json = Json
        assertEquals("\"en\"", json.encodeToString(LocaleSerializer, Locale.ENGLISH))
        assertEquals("\"zh_CN_HSK03\"", json.encodeToString(LocaleSerializer, Locale.CN_HSK3))
    }

    @Test
    fun testDeserialization() {
        val json = Json
        assertEquals(Locale.ENGLISH, json.decodeFromString(LocaleSerializer, "\"en\""))
        assertEquals(Locale.CN_HSK3, json.decodeFromString(LocaleSerializer, "\"zh_CN_HSK03\""))
    }

    @Test
    fun testDeserializationDefault() {
        val json = Json
        assertEquals(Locale.ENGLISH, json.decodeFromString(LocaleSerializer, "\"unknown\""))
    }
}
