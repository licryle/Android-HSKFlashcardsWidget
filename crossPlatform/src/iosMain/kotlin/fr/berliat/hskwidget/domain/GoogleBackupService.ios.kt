package fr.berliat.hskwidget.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object GoogleBackupService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual fun startBackup() {
        scope.launch {
            GoogleBackupSharedLogic.runBackupInternal()
        }
    }

    actual fun startRestore() {
        scope.launch {
            GoogleBackupSharedLogic.runRestoreInternal()
        }
    }
}
