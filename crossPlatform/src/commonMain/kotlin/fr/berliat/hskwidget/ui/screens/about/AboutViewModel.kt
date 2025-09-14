package fr.berliat.hskwidget.ui.screens.about

import co.touchlab.kermit.Logger
import fr.berliat.hskwidget.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AboutViewModel {
    val version = Utils.getPlatform()

    private val viewModelScope = CoroutineScope(SupervisorJob())

    class Stats(val wordsCnt: Int, val annotationCnt: Int)
    // Internal mutable state
    private val _stats = MutableStateFlow(Stats(0, 0))
    val stats: StateFlow<Stats> = _stats.asStateFlow() // expose as read-only

    // Actions
    fun onClickWebsite() {
        Utils.openLink("https://github.com/licryle/Android-HSKFlashcardsWidget")
        Utils.logAnalyticsScreenView("Github")
    }

    fun onClickEmail() : Boolean {
        Utils.logAnalyticsScreenView("Email")
        return Utils.sendEmail("cyrille.berliat+hsk@gmail.com")
    }

    fun registerVisit() {
        Utils.logAnalyticsScreenView("About")
    }

    fun fetchStats() {
        Logger.d(tag = TAG, messageString = "fetching stats")

        viewModelScope.launch(Dispatchers.IO) {
            val db = Utils.getDatabaseInstance()
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
        const val TAG = "AboutFragment"
    }
}