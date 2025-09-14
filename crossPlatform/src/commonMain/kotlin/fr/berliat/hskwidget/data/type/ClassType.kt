package fr.berliat.hskwidget.data.type

enum class ClassType (val type: String) {
    Speaking("口语"),
    Writing("写作"),
    Reading("精读"),
    Listening("听力"),
    FastReading("阅读"),
    NotFromClass("其他");
    companion object {
        infix fun from(findValue: String): ClassType = ClassType.valueOf(findValue)
    }
}