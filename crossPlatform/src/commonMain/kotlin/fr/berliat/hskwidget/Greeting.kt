package fr.berliat.hskwidget

class Greeting {
    fun greet(): String {
        return "Hello, ${Utils.getPlatform()}!"
    }
}