package fr.berliat.hskwidget.ui.application

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.ui.navigation.NavigationManager

actual class AppViewModel(navigationManager: NavigationManager) : CommonAppViewModel(navigationManager) {

    override suspend fun finishInitialization() {
        HSKAppServices.registerAnkiDelegators(HSKAnkiDelegate())
		super.init()
    }
}
