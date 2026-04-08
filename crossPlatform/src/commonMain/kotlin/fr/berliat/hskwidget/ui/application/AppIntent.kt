package fr.berliat.hskwidget.ui.application

import io.github.vinceglb.filekit.PlatformFile

sealed class AppIntent {
    data class Search(val query: String) : AppIntent()
    data class WidgetConfiguration(val widgetId: Int) : AppIntent()
    data class ImageOCR(val file: PlatformFile) : AppIntent()
}
