package fr.berliat.hskwidget.data.store

import io.github.vinceglb.filekit.BookmarkData
import kotlin.io.encoding.Base64

open class PreferenceConverter<S, T>(
    val fromStore: (S) -> T,
    val toStore: (T) -> S
)

class ByteArrayPreferenceConverter : PreferenceConverter<String, BookmarkData?>(
    fromStore = { stored ->
        try {
            BookmarkData(Base64.decode(stored))
        } catch (_: Exception) {
            null
        }
    },
    toStore = { value -> if (value == null) {
            Base64.encode(byteArrayOf(0))
        } else {
            Base64.encode(value.bytes)
        }
    }
)