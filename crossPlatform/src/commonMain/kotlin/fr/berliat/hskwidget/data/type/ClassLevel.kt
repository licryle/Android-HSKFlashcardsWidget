package fr.berliat.hskwidget.data.type

enum class ClassLevel (val lvl: String) {
    Elementary1("初一"),
    Elementary2("初二"),
    Elementary3("初三"),
    Elementary4("初四"),
    Intermediate1("中一"),
    Intermediate2("中二"),
    Intermediate3("中三"),
    Advanced1("高一"),
    Advanced2("高二"),
    Advanced3("高三"),
    NotFromClass("其他");
    companion object {
        infix fun from(findValue: String): ClassLevel = ClassLevel.valueOf(findValue)
    }

    override fun toString(): String {
        return "$lvl / $name"
    }
}