package fr.berliat.hskwidget.ui.application

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.FragmentActivity

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.core.ExpectedUtils.INTENT_SEARCH_WORD
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.StrictModeManager
import fr.berliat.hskwidget.data.store.GoogleDriveBackup
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import fr.berliat.hskwidget.domain.HSKAnkiDelegate
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.app_name
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.data.store.PrefCompat.PrefCompatMigration
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.ui.navigation.NavigationManager

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.init

import kotlinx.coroutines.runBlocking

import org.jetbrains.compose.resources.getString

actual class AppViewModel(navigationManager: NavigationManager, val activityProvider: () -> FragmentActivity)
    : CommonAppViewModel(navigationManager) {

    private lateinit var ankiDelegate : HSKAnkiDelegate

    override fun init() {
        // Enable StrictMode in Debug mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StrictModeManager.init()
        }

        FileKit.init(activityProvider.invoke())
        ExpectedUtils.init(activityProvider.invoke())

        // Todo remove run blocking
        HSKAppServices.registerGoogleBackup(
            GoogleDriveBackup(
                activityProvider.invoke(),
                runBlocking { getString(Res.string.app_name) }
            )
        )

        // HSKAnkiDelegate must be init before onResume, yet HSKAppServices aren't ready
        ankiDelegate = HSKAnkiDelegate(
            activity = activityProvider.invoke(),
            handler = null,
            appConfig = null,
            ankiStore = null
        )

        super.init()
    }

    override suspend fun finishInitialization() {
        // Now we may be after onResume() and HSK AppServices is ready for consumption
        ankiDelegate.ankiStore = HSKAppServices.ankiStore
        ankiDelegate.appConfig = HSKAppServices.appPreferences
        HSKAppServices.registerAnkiDelegators(ankiDelegate)

        FlashcardWidgetProvider.init(activityProvider) // Depends on HSKAppServices

        // Init done
        super.finishInitialization()

        syncPlayPurchases()
    }

    fun syncPlayPurchases() {
        val supportDevStore = SupportDevStore.getInstance(activityProvider.invoke())

        lateinit var listener : SupportDevStore.SupportDevListener
        listener = object : SupportDevStore.SupportDevListener {
            override fun onTotalSpentChange(totalSpent: Float) {
                appConfig.supportTotalSpent.value = totalSpent
                // We're done, bye
                supportDevStore.removeListener(listener = listener)
            }

            override fun onQueryFailure(result: BillingResult) { }

            override fun onPurchaseSuccess(purchase: Purchase) { }

            override fun onPurchaseHistoryUpdate(purchases: Map<SupportDevStore.SupportProduct, Int>) { }

            override fun onPurchaseAcknowledgedSuccess(purchase: Purchase) { }

            override fun onPurchaseFailure(purchase: Purchase?, billingResponseCode: Int) { }
        }

        supportDevStore.addListener(listener)

        supportDevStore.connect()
    }

    override fun handleAppUpdate() {
        if (PrefCompatMigration.shouldMigrate(ExpectedUtils.context, Utils.getAppVersion()))
            PrefCompatMigration.migrate(ExpectedUtils.context)

        super.handleAppUpdate()

        // Hack to fix an Android bug
        FlashcardWidgetProvider().updateAllFlashCardWidgets()
    }

    fun handleIntent(intent: Intent) {
        // Defer intent handling until services are ready to prevent race conditions
        executeWhenReady {
            handleWidgetConfigIntent(intent)
            handleSearchIntent(intent)
            handleTextSearchIntent(intent)
            handleImageOCRIntent(intent)
        }
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

    override fun finalizeWidgetConfiguration(widgetId: Int) {
        val resultIntent = Intent()
        resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        activityProvider.invoke().setResult(Activity.RESULT_OK, resultIntent)
        activityProvider.invoke().finish()
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