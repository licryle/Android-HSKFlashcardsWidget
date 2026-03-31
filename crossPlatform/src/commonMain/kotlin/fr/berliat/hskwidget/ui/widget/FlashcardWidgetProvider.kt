package fr.berliat.hskwidget.ui.widget

expect class FlashcardWidgetProvider() {
    fun updateAllFlashCardWidgets()
    suspend fun getWidgetIds(): List<Int>
}