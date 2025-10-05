package fr.berliat.hskwidget.ui.application

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.FragmentActivity
import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.core.ExpectedUtils.INTENT_SEARCH_WORD
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.StrictModeManager
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.domain.WidgetProvider
import fr.berliat.hskwidget.ui.HSKAnkiDelegate
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.app_name
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

actual class AppViewModel(val activity: () -> FragmentActivity)
    : CommonAppViewModel() {

    // Needs instantiation before onResume
    private var ankiDelegate : HSKAnkiDelegate

    init {
        // Enable StrictMode in Debug mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictModeManager.init()
        }

        FileKit.init(activity.invoke())
        ExpectedUtils.init(activity.invoke())

        // Todo remove run blocking
        HSKAppServices.registerGoogleBackup(
            GoogleDriveBackup(
                activity.invoke(),
                runBlocking { getString(Res.string.app_name)
                }
            )
        )

        // HSKAnkiDelegate must be init before onResume, yet HSKAppServices aren't ready
        ankiDelegate = HSKAnkiDelegate(
            activity = activity.invoke(),
            handler = null,
            appConfig = null,
            ankiStore = null
        )
    }

    override fun finishInitialization() {
        super.finishInitialization()

        // Now we may be after onResume() and HSK AppServices is ready for consumption
        ankiDelegate.ankiStore = HSKAppServices.ankiStore
        ankiDelegate.appConfig = HSKAppServices.appPreferences
        HSKAppServices.registerAnkiDelegators(ankiDelegate)

        WidgetProvider.init(activity) // Depends on HSKAppServices
    }

    override fun handleAppUpdate() {
        super.handleAppUpdate()

        // Hack to fix an Android bug
        WidgetProvider().updateAllFlashCardWidgets()
    }

    fun handleIntent(intent: Intent) {
        handleWidgetConfigIntent(intent)
        handleSearchIntent(intent)
        handleTextSearchIntent(intent)
        handleImageOCRIntent(intent)
    }

    private fun handleWidgetConfigIntent(intent: Intent?) {
        intent?.let {
            if (it.action == ACTION_APPWIDGET_CONFIGURE) {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    configureWidget(widgetId)
                }
            }
        }
    }

    private fun handleSearchIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(INTENT_SEARCH_WORD)) {
                val searchWord = it.getStringExtra(INTENT_SEARCH_WORD)
                Log.i(TAG, "Received a search intent: $searchWord")
                if (searchWord != null && searchWord != "") {
                    search(searchWord)
                }
            }
        }
    }

    private fun handleTextSearchIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_PROCESS_TEXT && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            Log.i(TAG, "Received a shared text intent: $sharedText")
            if (sharedText != null) {
                search(sharedText)
            }
        }
    }

    private fun handleImageOCRIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            Log.i(TAG, "Received a shared image intent")
            intent.getParcelableExtraCompat(Intent.EXTRA_STREAM, Uri::class.java)?.let { imageUri ->
                // Handle the image URI here
                ocrImage(PlatformFile(imageUri))
            }
        }
    }

    private fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? T
        }
    }

    companion object {
        const val TAG = "AppViewModel"
    }
}