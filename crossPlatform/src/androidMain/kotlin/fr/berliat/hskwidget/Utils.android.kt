package fr.berliat.hskwidget

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import fr.berliat.hskwidget.data.dao.AnkiDAO
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import okio.Path.Companion.toPath

actual object Utils {
    private var _contextProvider: (() -> Context)? = null
    private fun context() = _contextProvider!!.invoke()


    // Initialize once from Compose or Activity
    fun init(contextProvider: () -> Context) {
        this._contextProvider = contextProvider
    }

    actual fun openLink(url: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context().startActivity(intent)
    }

    actual fun sendEmail(email: String, subject: String, body: String) : Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // use only "mailto:", don't put address here
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        if (intent.resolveActivity(context().packageManager) != null) {
            context().startActivity(intent)
        } else {
            try {
                context().startActivity(Intent.createChooser(intent, "Send email with..."))
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
    private var databaseInstance : ChineseWordsDatabase? = null
    private var databaseMutex = Mutex()
    actual suspend fun getDatabaseInstance(): ChineseWordsDatabase = withContext(
        Dispatchers.IO) {
            val instance = databaseInstance
            instance?.let { return@withContext instance }

            databaseMutex.withLock {
                databaseInstance ?: run {

                    val dbBuilder = Room.databaseBuilder(
                        context().applicationContext,
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
                    databaseInstance = db

                    return@withContext db
                }
            }
        }

    actual fun getDatabasePath(): String {
        return databasePath
    }

    const val DATABASE_FILENAME = "Mandarin_Assistant.db"
    const val DATABASE_ASSET_PATH = "databases/$DATABASE_FILENAME"


    actual fun getDataStore(file: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { context().filesDir.resolve(file).absolutePath.toPath() }
        )
    }

    actual fun getAnkiDAO(): AnkiDAO {
        return AnkiDAO(context())
    }
}