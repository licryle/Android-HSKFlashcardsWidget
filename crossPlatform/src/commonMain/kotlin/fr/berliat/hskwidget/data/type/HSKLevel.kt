package fr.berliat.hskwidget.data.type

enum class HSK_Level (val level: Int) {
    HSK1(1),
    HSK2(2),
    HSK3(3),
    HSK4(4),
    HSK5(5),
    HSK6(6),
    HSK7(7),
    HSK8(8),
    HSK9(9),
    NOT_HSK(10);
    companion object {
        fun from(findValue: Int): HSK_Level {
            return if (findValue == 10)
                NOT_HSK
            else
                HSK_Level.valueOf("HSK$findValue")
        }
    }
}