package fr.berliat.hskwidget.ui.application

import fr.berliat.hskwidget.domain.SearchQuery
import io.github.vinceglb.filekit.PlatformFile

sealed class AppIntent {
    data class Search(val query: SearchQuery) : AppIntent()
    data class SearchTTS(val query: SearchQuery) : AppIntent()
    data class WidgetConfiguration(val widgetId: Int) : AppIntent()
    data class ImageOCR(val file: PlatformFile) : AppIntent() {
        constructor(path: String) : this(PlatformFile(path))
    }
}
