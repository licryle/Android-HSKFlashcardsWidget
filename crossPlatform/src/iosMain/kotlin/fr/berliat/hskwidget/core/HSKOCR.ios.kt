package fr.berliat.hskwidget.core

import io.github.vinceglb.filekit.PlatformFile

actual class HSKOCR actual constructor() {
    actual fun init() {
    }

    actual suspend fun process(
        imagePath: PlatformFile,
        successCallback: (String?) -> Unit,
        failureCallBack: (Exception) -> Unit
    ) {
    }
}