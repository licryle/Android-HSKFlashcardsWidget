package fr.berliat.hskwidget.core

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {

    @Test
    fun testGetRandomString() {
        val length = 10
        val randomString = Utils.getRandomString(length)
        assertEquals(length, randomString.length)
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        randomString.forEach { char ->
            assertTrue(char in allowedChars, "Character $char is not allowed")
        }
    }

    @Test
    fun testCapitalize() {
        assertEquals("Hello", "hello".capitalize())
        assertEquals("Hello", "HELLO".capitalize())
        assertEquals("Hello", "HeLLo".capitalize())
        assertEquals("", "".capitalize())
        assertEquals("A", "a".capitalize())
    }

    @Test
    fun testToSafeFileName() {
        assertEquals("safe_file_name.txt", "safe file name.txt".toSafeFileName())
        assertEquals("file_with_special_chars_____", "file with special chars !@#$".toSafeFileName())
        assertEquals("alreadySafe_123.jpg", "alreadySafe_123.jpg".toSafeFileName())
        assertEquals("dot.dot.dot", "dot.dot.dot".toSafeFileName())
    }

    @Test
    fun testFromKBToMB() {
        // Based on implementation: this.toDouble() / (1024 * 1024)
        // If this is bytes: 1024 * 1024 bytes = 1 MB
        assertEquals("1.0", (1024L * 1024L).fromKBToMB())
        assertEquals("0.5", (512L * 1024L).fromKBToMB())
        assertEquals("1.23", 1294220L.fromKBToMB(2))
    }

    @Test
    fun testYYMMDD() {
        val instant = Instant.fromEpochMilliseconds(1704067200000L) // 2024-01-01T00:00:00Z
        val tz = TimeZone.UTC
        assertEquals("2024/01/01", instant.YYMMDD(tz))
    }

    @Test
    fun testYYMMDDHHMMSS() {
        val instant = Instant.fromEpochMilliseconds(1704067200000L) // 2024-01-01T00:00:00Z
        val tz = TimeZone.UTC
        assertEquals("2024/01/01 00:00:00", instant.YYMMDDHHMMSS(tz))
    }

    @Test
    fun testContainsHanzi() {
        assertEquals(ContainHanziResult.EMPTY, "".containsHanzi())
        assertEquals(ContainHanziResult.NONE, "Hello".containsHanzi())
        assertEquals(ContainHanziResult.NONE, "123 !?".containsHanzi())
        assertEquals(ContainHanziResult.NONE, " ".containsHanzi())
        
        assertEquals(ContainHanziResult.ALL, "你好".containsHanzi())
        assertEquals(ContainHanziResult.ALL, "我是一个好人".containsHanzi())
        
        assertEquals(ContainHanziResult.NONE, "〇".containsHanzi()) // U+3007 is not in the Hanzi ranges in isHanzi()
        
        assertEquals(ContainHanziResult.SOME, "Hello你好".containsHanzi())
        assertEquals(ContainHanziResult.SOME, "你好Hello".containsHanzi())
        assertEquals(ContainHanziResult.SOME, "你a好".containsHanzi())
        assertEquals(ContainHanziResult.SOME, "a你好b".containsHanzi())
        assertEquals(ContainHanziResult.SOME, "你好。".containsHanzi())
    }
}
