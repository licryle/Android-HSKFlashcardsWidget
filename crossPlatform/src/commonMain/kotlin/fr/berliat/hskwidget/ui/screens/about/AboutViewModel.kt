package fr.berliat.hskwidget.ui.screens.about

import co.touchlab.kermit.Logger

import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.about_email_noapp
import fr.berliat.hskwidget.core.AppDispatchers
import fr.berliat.hskwidget.core.Logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AboutViewModel {
    val version = Utils.getAppVersion()

    private val viewModelScope = CoroutineScope(SupervisorJob())

    class Stats(val wordsCnt: Int, val annotationCnt: Int)
    // Internal mutable state
    private val _stats = MutableStateFlow(Stats(0, 0))
    val stats: StateFlow<Stats> = _stats.asStateFlow() // expose as read-only

    // Actions
    fun onClickWebsite() {
        Utils.openLink("https://github.com/licryle/Android-HSKFlashcardsWidget")
        Logging.logAnalyticsScreenView("Github")
    }

    fun openEmail() {
        Logging.logAnalyticsScreenView("Email")

        if (!Utils.sendEmail("cyrille.berliat+hsk@gmail.com", "About Mandarin Assistant App", "")) {
            HSKAppServices.snackbar.show(Res.string.about_email_noapp)
        }
    }

    fun fetchStats() {
        Logger.d(tag = TAG, messageString = "fetching stats")

        viewModelScope.launch(AppDispatchers.IO) {
            val db = HSKAppServices.database
            val words = db.chineseWordDAO()
            val annotations = db.chineseWordAnnotationDAO()
            // Here we executed in the coRoutine Scope
            val wordsCnt = words.getCount()
            val annotationsCnt = annotations.getCount()

            // Switch back to the main thread to update UI
            // Update the UI with the result
            Logger.d(tag = TAG, messageString = "stats fetched")

            _stats.value = Stats(wordsCnt, annotationsCnt)
        }
    }

    companion object {
        private const val TAG = "AboutFragment"
    }
}