package fr.berliat.hskwidget.domain

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

import fr.berliat.hskwidget.HSKHelperApp
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import androidx.compose.ui.graphics.Color
import fr.berliat.hskwidget.AnkiDelegator
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.ui.components.DetailedWordView
import fr.berliat.hskwidget.ui.screens.wordlist.WordListSelectionDialog

val hanziStyle = TextStyle(fontSize = 34.sp, color = Color.Black)
val pinyinStyle = TextStyle(fontSize = 20.sp, color = Color.Black)
val hanziClickedBackground = Color.Yellow

fun <T : Parcelable> Intent.getParcelableExtraCompat(key: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }
}

class Utils {
    class DummyWorker(context: Context, workerParams: WorkerParameters)
        : Worker(context, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }

    companion object {
        fun getAppContext() : HSKHelperApp {
            return HSKHelperApp.instance
        }

        fun formatDate(time: Instant) : String {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())

            return formatter.format(time)
        }

        fun playWordInBackground(context: Context, word: String) {
            val speechRequest = OneTimeWorkRequestBuilder<BackgroundSpeechService>()
                .setInputData(workDataOf(Pair("word", word)))
                .build()

            val workMgr = WorkManager.getInstance(context)
            val observer = object : Observer<WorkInfo?> {
                override fun onChanged(value: WorkInfo?) {
                    if (value == null) {
                        // Handle the case where workInfo is null
                        return
                    }

                    if (value.state == WorkInfo.State.SUCCEEDED
                        || value.state == WorkInfo.State.FAILED
                    ) {

                        val errStringId: Int
                        var errRemedyIntent: String? = null
                        if (value.state == WorkInfo.State.FAILED) {
                            var errId =
                                value.outputData.getString(BackgroundSpeechService.FAILURE_REASON)
                            when (errId) {
                                BackgroundSpeechService.FAILURE_MUTED
                                    -> errStringId = R.string.speech_failure_toast_muted

                                BackgroundSpeechService.FAILURE_INIT_FAILED -> {
                                    errStringId = R.string.speech_failure_toast_init
                                    errRemedyIntent = Settings.ACTION_ACCESSIBILITY_SETTINGS
                                }

                                BackgroundSpeechService.FAILURE_LANG_UNSUPPORTED -> {
                                    errStringId = R.string.speech_failure_toast_chinese_unsupported
                                    errRemedyIntent = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                                }

                                else -> {
                                    errStringId = R.string.speech_failure_toast_unknown
                                    errId = BackgroundSpeechService.FAILURE_UNKNOWN
                                }
                            }

                            logAnalyticsError("SPEECH", errId, "")

                            if (errRemedyIntent == null) {
                                Toast.makeText(
                                    context, context.getString(errStringId),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                AlertDialog.Builder(context)
                                    .setTitle(R.string.dialog_tts_error)
                                    .setMessage(errStringId)
                                    .setPositiveButton(R.string.fix_it) { _, _ ->
                                        val intent = Intent(errRemedyIntent)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                            }
                        }

                        workMgr.getWorkInfoByIdLiveData(speechRequest.id)
                            .removeObserver(this)
                    }
                }
            }

            workMgr.getWorkInfoByIdLiveData(speechRequest.id).observeForever(observer)

            workMgr.enqueue(speechRequest)

            incrementConsultedWord(context, word)

            logAnalyticsEvent(ANALYTICS_EVENTS.WIDGET_PLAY_WORD)
        }

        fun capitalizeStr(s: Any?) =
            s?.toString()?.lowercase()?.replaceFirstChar { it.uppercaseChar() } ?: ""

        fun getAppScope(context: Context) = (context.applicationContext as HSKHelperApp).applicationScope

        fun incrementConsultedWord(context: Context, word: String) {
           getAppScope(context).launch(Dispatchers.IO) {
                val db = DatabaseHelper.getInstance(context)
                val frequencyWordsRepo = ChineseWordFrequencyRepo(
                    db.chineseWordFrequencyDAO(),
                    db.annotatedChineseWordDAO()
                )

                frequencyWordsRepo.incrementConsulted(word)
            }
        }

        fun hasFolderWritePermission(context: Context, uri: Uri): Boolean {
            if (uri.toString() == "") return false
            if (!DocumentsContract.isTreeUri(uri)) return false

            val resolver = context.contentResolver
            val persistedUris = resolver.persistedUriPermissions

            for (permission in persistedUris) {
                if (permission.uri == uri && permission.isWritePermission) {
                    return true
                }
            }
            return false
        }

        suspend fun listFilesInSAFDirectory(context: Context, directoryUri: Uri): List<DocumentFile>
            = withContext(Dispatchers.IO) {
            val dir = DocumentFile.fromTreeUri(context, directoryUri)
            dir?.listFiles()?.toList() ?: emptyList()
        }

        suspend fun copyFileUsingSAF(context: Context, sourceFile: File, destinationDir: Uri, fileName: String): Boolean
            = withContext(Dispatchers.IO) {
            try {
                // Open input stream for the source database file
                val inputStream: InputStream = FileInputStream(sourceFile)

                val dir = DocumentFile.fromTreeUri(context, destinationDir)
                val destinationFile = dir?.createFile("application/octet-stream", fileName)

                // Open OutputStream to the destination file
                context.contentResolver.openFileDescriptor(destinationFile!!.uri, "w")
                    ?.use { parcelFileDescriptor ->
                        FileOutputStream(parcelFileDescriptor.fileDescriptor).use { output ->
                            // Copy data from source to destination
                            inputStream.use { input ->
                                val buffer = ByteArray(1024)
                                var length: Int
                                while (input.read(buffer).also { length = it } > 0) {
                                    output.write(buffer, 0, length)
                                }
                            }
                        }
                    }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        suspend fun copyUriToCacheDir(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
            if (uri.scheme == "file") {
                val file = File(uri.path!!)
                if (file.absolutePath.startsWith(context.cacheDir.absolutePath)) {
                    return@withContext file // already in cacheDir, no need to copy
                }
            }

            val inputStream = when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)
                "file" -> File(uri.path!!).inputStream()
                else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
            } ?: throw IllegalArgumentException("Cannot open input stream from URI")

            val outFile = File(context.cacheDir, UUID.randomUUID().toString())
            outFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            inputStream.close()
            outFile.outputStream().close()

            return@withContext outFile
        }

        fun hideKeyboard(context: Context, view: View) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        fun hideViewIf(isTrue: Boolean): Int {
            return if (isTrue) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        fun populateDictionaryEntryView(binding: FragmentDictionarySearchItemBinding,
                                        word: AnnotatedChineseWord, navController: NavController,
                                        ankiCaller: AnkiDelegator
        ) {
            val appConfig = OldAppPreferencesStore(navController.context)
            val context = navController.context

            binding.dictionaryItemContainer.setContent {
                var showWordListDialog by remember { mutableStateOf<ChineseWord?>(null) }

                showWordListDialog?.let {
                    WordListSelectionDialog(
                        ankiCaller = ankiCaller,
                        word = it,
                        onDismiss = { showWordListDialog = null },
                        onSaved = { showWordListDialog = null }
                    )
                }

                DetailedWordView(
                    word,
                    appConfig.dictionaryShowHSK3Definition,
                    {
                        val action = DictionarySearchFragmentDirections.annotateWord(word.simplified, false)

                        navController.navigate(action)
                    },
                    { playWordInBackground(context, word.simplified) },
                    { copyToClipBoard(context, word.simplified) },
                    { showWordListDialog = word.word }
                )
            }
        }

        fun requestPermissionNotification(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        activity.applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        0
                    )
                }
            }
        }

        fun copyToClipBoard(context: Context, s: String) {
            // https://stackoverflow.com/a/28780585/3059536
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", s)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                context,
                String.format(context.getString(R.string.copied_to_clipboard), s),
                Toast.LENGTH_SHORT
            ).show()

            logAnalyticsEvent(ANALYTICS_EVENTS.WIDGET_COPY_WORD)

            incrementConsultedWord(context, s)
        }

        /**
         * This is a workaround for a Bug in handling system wide events.
         * An empty WorkManager queue will trigger an APPWIGET_UPDATE event, which is undesired.
         * Read more at: https://www.reddit.com/r/android_devs/comments/llq2mw/question_why_should_it_be_expected_that/
         */
        fun preventUnnecessaryAppWidgetUpdates(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context).getWorkInfosByTag("always_pending_work")
            if (workInfos.get().size > 0) return false

            val alwaysPendingWork = OneTimeWorkRequestBuilder<DummyWorker>()
                .setInitialDelay(5000L, TimeUnit.DAYS)
                .addTag("always_pending_work")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "always_pending_work",
                ExistingWorkPolicy.KEEP,
                alwaysPendingWork
            )

            return true
        }

        fun logAnalyticsEvent(
            event: ANALYTICS_EVENTS,
            params: Map<String, String> = mapOf()
        ) {
            val appContext = getAppContext()
            val bundle = Bundle()
            params.forEach {
                bundle.putString(it.key, it.value)
            }

            val appMgr = FlashcardWidgetProvider()
            val widgets = appMgr.getWidgetIds(appContext)
            bundle.putString("WIDGET_TOTAL_NUMBER", widgets.size.toString())

            if (widgets.isEmpty()) {
                bundle.putString("MAX_WIDGET_ID", "0")
            } else {
                bundle.putString("MAX_WIDGET_ID", widgets.last().toString())
            }

            appContext.applicationScope.launch(Dispatchers.IO) {
                Firebase.analytics.logEvent(event.name, bundle)
            }
        }

        fun logAnalyticsError(module: String, error: String, details: String) {
            logAnalyticsEvent(
                ANALYTICS_EVENTS.ERROR,
                mapOf(
                    "MODULE" to module,
                    "ERROR_ID" to error,
                    "DETAILS" to details
                )
            )
        }

        fun logAnalyticsWidgetAction(event: ANALYTICS_EVENTS, widgetId: Int) {
            val appContext = getAppContext()
            val widgets = FlashcardWidgetProvider().getWidgetIds(appContext)
            val size = WidgetSizeProvider(appContext).getWidgetsSize(widgetId)

            logAnalyticsEvent(
                event,
                mapOf(
                    "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                    "WIDGET_SIZE" to "${size.first}x${size.second}"
                )
            )
        }

        fun logAnalyticsScreenView(screenName: String) {
            logAnalyticsEvent(
                ANALYTICS_EVENTS.SCREEN_VIEW,
                mapOf("SCREEN_NAME" to screenName)
            )
        }

        fun containsChinese(text: String): Boolean {
            val pattern = Regex("[\u4e00-\u9fff]")
            return pattern.containsMatchIn(text)
        }

        fun formatKBToMB(bytesReceived: Long, format: String = "%.2f"): String {
            return String.format(
                format,
                bytesReceived.toDouble() / 1024 / 1024)
        }
    }

    enum class ANALYTICS_EVENTS {
        SCREEN_VIEW,
        AUTO_WORD_CHANGE,
        ERROR, // Use logAnalyticsError for details
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD, // Would be great to add
        WIDGET_EXPAND,
        WIDGET_COLLAPSE,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY,
        WIDGET_COPY_WORD,
        CONFIG_BACKUP_ON,
        CONFIG_BACKUP_OFF,
        CONFIG_BACKUP_RESTORE,
        CONFIG_BACKUPCLOUD_ON, // Reserved for future use
        CONFIG_BACKUPCLOUD_OFF, // Reserved for future use
        CONFIG_BACKUPCLOUD_RESTORE,
        CONFIG_BACKUPCLOUD_BACKUP,
        CONFIG_ANKI_SYNC_ON,
        CONFIG_ANKI_SYNC_OFF,
        ANNOTATION_SAVE,
        ANNOTATION_DELETE,
        LIST_CREATE,
        LIST_DELETE,
        LIST_MODIFY_WORD,
        LIST_RENAME,
        DICT_HSK3_ON,
        DICT_HSK3_OFF,
        DICT_ANNOTATION_ON,
        DICT_ANNOTATION_OFF,
        DICT_SEARCH,
        OCR_WORD_NOTFOUND,
        OCR_WORD_FOUND,
        PURCHASE_CLICK,
        PURCHASE_FAILED,
        PURCHASE_SUCCESS
    }

    /* Thank to https://stackoverflow.com/questions/25153604/get-the-size-of-my-homescreen-widget */
    class WidgetSizeProvider(
        private val context: Context // Do not pass Application context
    ) {

        private val appWidgetManager = AppWidgetManager.getInstance(context)

        fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
            val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
            val width = getWidgetWidth(isPortrait, widgetId)
            val height = getWidgetHeight(isPortrait, widgetId)
            val widthInPx = context.dip(width)
            val heightInPx = context.dip(height)
            return widthInPx to heightInPx
        }

        private fun getWidgetWidth(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            }

        private fun getWidgetHeight(isPortrait: Boolean, widgetId: Int): Int =
            if (isPortrait) {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            } else {
                getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            }

        private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
            appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

        private fun Context.dip(value: Int): Int =
            (value * resources.displayMetrics.density).toInt()
    }
}