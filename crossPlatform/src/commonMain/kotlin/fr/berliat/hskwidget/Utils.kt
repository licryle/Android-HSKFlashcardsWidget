package fr.berliat.hskwidget

import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

expect object Utils {
    fun openLink(url: String)
    fun sendEmail(email: String, subject: String = "", body: String = "") : Boolean
    fun getPlatform(): String
    // commonMain
    fun getAppVersion(): String

    fun logAnalyticsScreenView(screen: String)

    suspend fun getDatabaseInstance() : ChineseWordsDatabase

    fun getDatabasePath(): String
}