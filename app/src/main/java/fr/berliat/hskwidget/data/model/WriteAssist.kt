package fr.berliat.hskwidget.data.model

data class WriteAssist(
    val originalCN: String,
    val correctedCN: String,
    val confidence: Double,
    val explanations: String,
    val originalEN: String,
    val correctedEN: String,
    val grade: Double
) {
}