package fr.berliat.hskwidget.domain

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import fr.berliat.hskwidget.ui.wordlist.WordListSelectionDialog
import androidx.core.view.isVisible
import fr.berliat.hskwidget.HSKHelperApp
import fr.berliat.hskwidget.data.model.ChineseWord.Companion.CN_HSK3
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

typealias CallbackNoParam = () -> Unit

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

        fun getOpenURLIntent(url: String): Intent {
            return Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        fun sendEmail(context: Context, address: String, subject: String = "", body: String = "") {
            val intent = getOpenURLIntent(
                "mailto:$address?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body))

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, context.getString(R.string.about_email_noapp), Toast.LENGTH_LONG).show()
                copyToClipBoard(context, address)
            }
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
                        if (value.state == WorkInfo.State.FAILED) {
                            var errId =
                                value.outputData.getString(BackgroundSpeechService.FAILURE_REASON)
                            when (errId) {
                                BackgroundSpeechService.FAILURE_MUTED
                                    -> errStringId = R.string.speech_failure_toast_muted

                                BackgroundSpeechService.FAILURE_INIT_FAILED
                                    -> errStringId = R.string.speech_failure_toast_init

                                BackgroundSpeechService.FAILURE_LANG_UNSUPPORTED
                                    -> errStringId =
                                    R.string.speech_failure_toast_chinese_unsupported

                                else -> {
                                    errStringId = R.string.speech_failure_toast_unknown
                                    errId = BackgroundSpeechService.FAILURE_UNKNOWN
                                }
                            }

                            logAnalyticsError("SPEECH", errId, "")
                            Toast.makeText(
                                context, context.getString(errStringId),
                                Toast.LENGTH_LONG
                            ).show()
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
                val db = ChineseWordsDatabase.getInstance(context)
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

        suspend fun listFilesInSAFDirectory(context: Context, directoryUri: Uri): List<DocumentFile> {
            val dir = DocumentFile.fromTreeUri(context, directoryUri)
            return dir?.listFiles()?.toList() ?: emptyList()
        }

        suspend fun copyFileUsingSAF(context: Context, sourcePath: String, destinationDir: Uri, fileName: String): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    // Open InputStream from source file
                    val file = File(sourcePath)

                    // Open input stream for the source database file
                    val inputStream: InputStream = FileInputStream(file)

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
        }

        fun copyUriToCacheDir(context: Context, uri: Uri): File {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open input stream from URI")

            val outFile = File(context.cacheDir, "imported_backup.db")
            outFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            return outFile
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
                                        wordListChangedCallback: CallbackNoParam
        ) {
            // Definition vs. HSK definition -- which one to show?
            // Collect then invert if needed
            var definition = word.word?.definition?.get(Locale.ENGLISH) ?: ""
            var annotation = word.annotation?.notes ?: ""
            if (definition == "") {
                definition = word.annotation?.notes ?: ""
                annotation = ""
            }
            var altDef = word.word?.definition?.get(CN_HSK3) ?: ""

            val appConfig = AppPreferencesStore(navController.context)
            if (appConfig.dictionaryShowHSK3Definition && altDef.isNotEmpty()) {
                val tmp = altDef
                altDef = definition
                definition = tmp
            }

            // Populate the "top part"
            var pinyins = word.word?.pinyins.toString()
            if (pinyins == "")
                pinyins = word.annotation?.pinyins?.toString() ?: ""

            with(binding.dictionaryItemChinese) {
                hanziText = word.simplified
                pinyinText = pinyins
            }

            binding.dictionaryItemHskLevel.visibility = hideViewIf(
                word.word?.hskLevel == null || word.word.hskLevel == ChineseWord.HSK_Level.NOT_HSK
            )
            binding.dictionaryItemHskLevel.text = word.word?.hskLevel.toString()

            binding.dictionaryItemDefinition.text = definition
            binding.dictionaryItemAnnotation.text = annotation
            binding.dictionaryItemAnnotation.visibility = hideViewIf(annotation.isEmpty())

            with(binding.dictionaryItemFavorite) {
                if (word.hasAnnotation()) {
                    setImageResource(R.drawable.bookmark_heart_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.md_theme_dark_inversePrimary)
                    )
                } else {
                    setImageResource(R.drawable.bookmark_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.md_theme_dark_surface)
                    )
                }

                setOnClickListener {
                    val action = fr.berliat.hskwidget.ui.dictionary.DictionarySearchFragmentDirections.annotateWord(word.simplified, false)

                    navController.navigate(action)
                }
            }

            val context = navController.context
            binding.dictionaryItemSpeak.setOnClickListener {
                playWordInBackground(context, word.simplified)
            }
            binding.dictionaryItemCopy.setOnClickListener { copyToClipBoard(context, word.simplified) }

            binding.dictionaryItemLists.setOnClickListener {
                val dialog = WordListSelectionDialog.newInstance(word.simplified)
                dialog.onSave = wordListChangedCallback
                dialog.show((context as FragmentActivity).supportFragmentManager, "WordListSelectionDialog")
            }
            // Done with "top part"

            // Populate the "more part"
            binding.dictionaryItemAltdefinition.text = altDef
            binding.dictionaryItemAltdefinitionContainer.visibility = hideViewIf(altDef.isEmpty())

            val examples = word.word?.examples ?: ""
            binding.dictionaryItemExample.text = examples
            binding.dictionaryItemExampleContainer.visibility = hideViewIf(examples.isEmpty())

            val antonym = word.word?.antonym ?: ""
            binding.dictionaryItemAntonyms.text = word.word?.antonym.toString()
            binding.dictionaryItemAntonymContainer.visibility = hideViewIf(antonym.isEmpty())

            val synonyms = word.word?.synonyms ?: ""
            binding.dictionaryItemSynonyms.text = synonyms
            binding.dictionaryItemSynonymsContainer.visibility = hideViewIf(synonyms.isEmpty())

            val modality = word.word?.modality ?: ChineseWord.Modality.UNKNOWN
            binding.dictionaryItemModality.text = capitalizeStr(modality)
            binding.dictionaryItemModality.visibility = hideViewIf(modality == ChineseWord.Modality.UNKNOWN)

            val type = word.word?.type ?: ChineseWord.Type.UNKNOWN
            binding.dictionaryItemType.text = capitalizeStr(type)
            binding.dictionaryItemType.visibility = hideViewIf(type == ChineseWord.Type.UNKNOWN)

            // Hide all if all empty
            val nothingMore = (altDef + examples + antonym + synonyms).isEmpty()
                    && (modality == ChineseWord.Modality.UNKNOWN)
                    && (type == ChineseWord.Type.UNKNOWN)

            binding.dictionaryItemToggle.visibility = hideViewIf(nothingMore)

            if (! nothingMore) {
                binding.dictionaryItemContainer.setOnClickListener {
                    val isMoreShown = binding.dictionaryItemMore.isVisible

                    var evt : ANALYTICS_EVENTS
                    if (isMoreShown) {
                        binding.dictionaryItemMore.visibility = View.GONE
                        binding.dictionaryItemToggle.setImageResource(R.drawable.keyboard_arrow_down_24px)
                        evt = ANALYTICS_EVENTS.WIDGET_COLLAPSE
                    } else {
                        binding.dictionaryItemMore.visibility = View.VISIBLE
                        binding.dictionaryItemToggle.setImageResource(R.drawable.keyboard_arrow_up_24px)
                        evt = ANALYTICS_EVENTS.WIDGET_EXPAND
                    }

                    logAnalyticsEvent(evt)
                }
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
                context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
        WIGDET_ADD,
        WIDGET_EXPAND,
        WIDGET_COLLAPSE,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY,
        WIDGET_COPY_WORD,
        CONFIG_BACKUP_ON,
        CONFIG_BACKUP_OFF,
        CONFIG_BACKUP_RESTORE,
        CONFIG_BACKUPCLOUD_ON,
        CONFIG_BACKUPCLOUD_OFF,
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