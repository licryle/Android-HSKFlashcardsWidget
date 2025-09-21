package fr.berliat.hskwidget.core

expect class HSKOCR() {
    fun init()
    suspend fun process(
        imagePath: String,
        successCallback: (String?) -> Unit,
        failureCallBack: (Exception) -> Unit
    )
}