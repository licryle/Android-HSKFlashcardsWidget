package fr.berliat.hskwidget.ui.application

import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.runBlocking

import fr.berliat.googledrivebackup.GoogleDriveBackup

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.app_name
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.ui.navigation.NavigationManager

actual class AppViewModel(navigationManager: NavigationManager) : CommonAppViewModel(navigationManager) {

    override suspend fun finishInitialization() {
        HSKAppServices.registerAnkiDelegators(HSKAnkiDelegate())

		val gDrive = GoogleDriveBackup(
            runBlocking { getString(Res.string.app_name) } )
		HSKAppServices.registerGoogleBackup(gDrive)

		super.finishInitialization()
    }
}
