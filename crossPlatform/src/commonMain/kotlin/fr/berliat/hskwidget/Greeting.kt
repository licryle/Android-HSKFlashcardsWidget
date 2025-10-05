package fr.berliat.hskwidget

import fr.berliat.hskwidget.core.Utils

class Greeting {
    fun greet(): String {
        return "Hello, ${Utils.getPlatform()}!"
    }
}