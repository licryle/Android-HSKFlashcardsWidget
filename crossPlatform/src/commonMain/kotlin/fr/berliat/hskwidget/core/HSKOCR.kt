package fr.berliat.hskwidget.core

import io.github.vinceglb.filekit.PlatformFile

expect class HSKOCR() {
    fun init()
    suspend fun process(
        imagePath: PlatformFile,
        successCallback: (String?) -> Unit,
        failureCallBack: (Exception) -> Unit
    )
}