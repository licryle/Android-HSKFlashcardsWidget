package fr.berliat.hsktextviews

import fr.berliat.pinyin4kot.Hanzi2Pinyin

object PinyinUtils {
    val hanzi2Pinyin = Hanzi2Pinyin()

    fun String.getPinyins(): List<String> {
        return buildList<String> {
            this@getPinyins.forEach { hanzi: Char ->
                try {
                    add(hanzi2Pinyin.numberedToTonal(hanzi2Pinyin.getPinyin(hanzi)[0]))
                } catch (_: Exception) {
                    add("   ")
                }
            }
        }
    }

    fun Char.isHanzi(): Boolean {
        val code = this.code
        return (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in 0x20000..0x2EBEF)
    }
}



