package fr.berliat.hskwidget.ui.widget

expect class FlashcardWidgetProvider() {
    fun updateAllFlashCardWidgets()
    fun redrawAllFlashCardWidgets()
    suspend fun getWidgetIds(): List<Int>
}