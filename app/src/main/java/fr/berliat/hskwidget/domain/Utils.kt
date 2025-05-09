package fr.berliat.hskwidget.domain

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.repo.ChineseWordFrequencyRepo
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.databinding.FragmentDictionarySearchItemBinding
import fr.berliat.hskwidget.ui.widget.FlashcardWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri


class Utils {
    class DummyWorker(context: Context, workerParams: WorkerParameters)
        : Worker(context, workerParams) {
        override fun doWork(): Result {
            return Result.success()
        }
    }
    companion object {
        fun getDefaultWord(context: Context): ChineseWord {
            return ChineseWord(
                context.getString(R.string.widget_default_chinese),
                "",
                mapOf(Locale.ENGLISH to context.getString(R.string.widget_default_english)),
                ChineseWord.HSK_Level.HSK1,
                ChineseWord.Pinyins(context.getString(R.string.widget_default_pinyin)),
                0
            )
        }

        fun getOpenURLIntent(url: String): Intent {
            return Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        fun sendEmail(context: Context, address: String, subject: String = "", body: String = "") {
            context.startActivity(getOpenURLIntent(
                "mailto:$address?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body)))
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

                            logAnalyticsError(context, "SPEECH", errId, "")
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
        }

        fun incrementConsultedWord(context: Context, word: String) {
            val db = ChineseWordsDatabase.getInstance(context)
            val frequencyWordsRepo = ChineseWordFrequencyRepo(
                db.chineseWordFrequencyDAO(),
                db.annotatedChineseWordDAO()
            )

            GlobalScope.launch {
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

        fun populateDictionaryEntryView(binding: FragmentDictionarySearchItemBinding,
                                        word: AnnotatedChineseWord, navController: NavController) {
            var pinyins = word.word?.pinyins.toString()
            if (pinyins == "")
                pinyins = word.annotation?.pinyins?.toString() ?: ""

            var definition = word.word?.definition?.get(Locale.ENGLISH) ?: ""
            var annotation = word.annotation?.notes ?: ""
            if (definition == "") {
                definition = word.annotation?.notes ?: ""
                annotation = ""
            }

            with(binding.dictionaryItemChinese) {
                hanziText = word.simplified
                pinyinText = pinyins
            }
            var hskViz = View.VISIBLE
            if (word.word?.hskLevel == null || word.word.hskLevel == ChineseWord.HSK_Level.NOT_HSK)
                hskViz = View.INVISIBLE

            binding.dictionaryItemHskLevel.visibility = hskViz
            binding.dictionaryItemHskLevel.text = word.word?.hskLevel.toString()

            binding.dictionaryItemDefinition.text = definition
            binding.dictionaryItemAnnotation.text = annotation

            if (annotation == "")
                binding.dictionaryItemAnnotation.visibility = View.GONE
            else
                binding.dictionaryItemAnnotation.visibility = View.VISIBLE

            with(binding.dictionaryItemFavorite) {
                if (word.hasAnnotation()) {
                    setImageResource(R.drawable.bookmark_heart_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_dark_inversePrimary)
                    )
                } else {
                    setImageResource(R.drawable.bookmark_24px)
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_dark_surface)
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
            context: Context, event: ANALYTICS_EVENTS,
            params: Map<String, String> = mapOf()
        ) {
            val bundle = Bundle()
            params.forEach {
                bundle.putString(it.key, it.value)
            }

            val appMgr = FlashcardWidgetProvider()
            val widgets = appMgr.getWidgetIds(context)
            bundle.putString("WIDGET_TOTAL_NUMBER", widgets.size.toString())

            if (widgets.isEmpty()) {
                bundle.putString("MAX_WIDGET_ID", "0")
            } else {
                bundle.putString("MAX_WIDGET_ID", widgets.last().toString())
            }

            Firebase.analytics.logEvent(event.name, bundle)
        }

        fun logAnalyticsError(context: Context, module: String, error: String, details: String) {
            logAnalyticsEvent(
                context, ANALYTICS_EVENTS.ERROR,
                mapOf(
                    "MODULE" to module,
                    "ERROR_ID" to error,
                    "DETAILS" to details
                )
            )
        }

        fun logAnalyticsWidgetAction(context: Context, event: ANALYTICS_EVENTS, widgetId: Int) {
            val widgets = FlashcardWidgetProvider().getWidgetIds(context)
            val size = WidgetSizeProvider(context).getWidgetsSize(widgetId)

            var hskLevels = ""
            FlashcardManager.getInstance(context, widgetId).getPreferenceStore().getAllowedHSK()
                .forEach() {
                    hskLevels += it.level.toString() + ","
                }
            hskLevels = hskLevels.dropLast(1)

            logAnalyticsEvent(
                context, event,
                mapOf(
                    "WIDGET_NUMBER" to widgets.indexOf(widgetId).toString(),
                    "WIDGET_SIZE" to "${size.first}x${size.second}",
                    "WIDGET_HSK" to hskLevels
                )
            )
        }

        fun logAnalyticsScreenView(context: Context, screenName: String) {
            logAnalyticsEvent(
                context, ANALYTICS_EVENTS.SCREEN_VIEW,
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
        ERROR,
        WIDGET_PLAY_WORD,
        WIDGET_MANUAL_WORD_CHANGE,
        WIDGET_RECONFIGURE,
        WIDGET_CONFIG_VIEW,
        WIGDET_RESIZE,
        WIGDET_ADD,
        WIGDET_REMOVE,
        WIDGET_OPEN_DICTIONARY
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