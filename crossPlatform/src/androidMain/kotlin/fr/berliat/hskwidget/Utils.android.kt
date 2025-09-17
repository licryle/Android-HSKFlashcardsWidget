package fr.berliat.hskwidget

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.room.Room
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    actual fun logAnalyticsEvent(event: ANALYTICS_EVENTS) {
    }


    private var databasePath = ""
    actual suspend fun getDatabaseInstance(): ChineseWordsDatabase = withContext(
        Dispatchers.IO) {
            val dbBuilder = Room.databaseBuilder(
                contextProvider!!.invoke().applicationContext,
                ChineseWordsDatabase::class.java,
                DATABASE_FILENAME
            )
                .createFromAsset(DATABASE_ASSET_PATH)

            /*if (BuildConfig.DEBUG) {
                dbBuilder.setQueryCallback(
                    { sqlQuery, bindArgs ->
                        Logger.d(tag = TAG, messageString = "SQL Query: $sqlQuery SQL Args: $bindArgs")
                    }, Executors.newSingleThreadExecutor()
                )
            }*/

            val db = dbBuilder.build()

            databasePath = db.openHelper.writableDatabase.path.toString()

            return@withContext db
        }

    actual fun getDatabasePath(): String {
        return databasePath
    }

    const val DATABASE_FILENAME = "Mandarin_Assistant.db"
    const val DATABASE_ASSET_PATH = "databases/$DATABASE_FILENAME"
}