package fr.berliat.hskwidget.crossPlatform

class Greeting {
    fun greet(): String {
        return "Hello, ${Utils.getPlatform()}!"
    }
}