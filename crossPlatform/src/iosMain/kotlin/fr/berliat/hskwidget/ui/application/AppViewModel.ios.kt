package fr.berliat.hskwidget.ui.application

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.domain.HSKAnkiDelegate

actual class AppViewModel : CommonAppViewModel() {

    override fun finishInitialization() {
        HSKAppServices.registerAnkiDelegators(HSKAnkiDelegate())
    }
}