package fr.berliat.hskwidget.crossPlatform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform