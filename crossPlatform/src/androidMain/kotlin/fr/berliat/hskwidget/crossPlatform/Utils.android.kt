package fr.berliat.hskwidget.crossPlatform

import android.content.Intent
import android.content.Context
import android.net.Uri

actual object Utils {
    private var contextProvider: (() -> Context)? = null

    // Initialize once from Compose or Activity
    fun init(contextProvider: () -> Context) {
        this.contextProvider = contextProvider
    }

    actual fun openLink(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        contextProvider!!.invoke().startActivity(intent)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // use only "mailto:", don't put address here
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        val context = contextProvider!!.invoke()
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            try {
                context.startActivity(Intent.createChooser(intent, "Send email with..."))
            } catch (_: Exception) {
                return false
            }
        }

        return true
    }

    actual fun getPlatform(): String {
        return "Android ${getAppVersion()}"
    }

    // androidMain
    actual fun getAppVersion(): String = "Not supported yet"

    actual fun logAnalyticsScreenView(screen: String) {
    }
}