package fr.berliat.hskwidget.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@kotlinx.serialization.Serializable(with = LocaleSerializer::class)
enum class Locale(val code: String) {
    ENGLISH("en"),
    CHINESE("zh"),
    FRENCH("fr"),
    SPANISH("es"),
    CN_HSK3("zh_CN_HSK03"),
    SIMPLIFIED_CHINESE("zh_CN");

    companion object {
        // Get enum from string code
        fun fromCode(code: String): Locale? =
            entries.firstOrNull { it.code == code }

        fun getDefault(): Locale {
            return ENGLISH
        }
    }
}

object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) {
        encoder.encodeString(value.code)
    }

    override fun deserialize(decoder: Decoder): Locale {
        val code = decoder.decodeString()
        return Locale.fromCode(code) ?: Locale.getDefault()
    }
}