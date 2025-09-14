package fr.berliat.hskwidget.crossPlatform

expect object Utils {
    fun openLink(url: String)
    fun sendEmail(email: String, subject: String = "", body: String = "") : Boolean
    fun getPlatform(): String
    // commonMain
    fun getAppVersion(): String

    fun logAnalyticsScreenView(screen: String)
}