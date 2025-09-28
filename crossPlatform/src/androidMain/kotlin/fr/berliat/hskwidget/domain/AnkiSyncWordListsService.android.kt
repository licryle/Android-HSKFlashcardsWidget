package fr.berliat.hskwidget.domain

import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import fr.berliat.ankidroidhelper.AnkiSyncService
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.repo.WordListRepository
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_notification_description
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_notification_name
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_progress_message
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_start_message
import hskflashcardswidget.crossplatform.generated.resources.app_name
import hskflashcardswidget.crossplatform.generated.resources.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.getString
import kotlin.collections.chunked

actual class AnkiSyncWordListsService: AnkiSyncService() {
    private lateinit var syncStartMessage : String
    private lateinit var syncProgressMessage : String
    private lateinit var notificationTitle : String
    private lateinit var notificationCancelText : String
    private lateinit var notificationChannelDescription : String
    private lateinit var notificationChannelTitle : String
    private lateinit var notificationLargeIcon : Bitmap

    private lateinit var wordListRepository : WordListRepository


    override suspend fun initResources() {
        wordListRepository = HSKAppServices.wordListRepo
        syncStartMessage = getString(Res.string.anki_sync_start_message)
        syncProgressMessage = getString(Res.string.anki_sync_progress_message)
        notificationTitle = getString(Res.string.app_name)
        notificationCancelText = getString(Res.string.cancel)
        notificationChannelDescription = getString(Res.string.anki_sync_notification_description)
        notificationChannelTitle = getString(Res.string.anki_sync_notification_name)
        notificationLargeIcon = drawableToBitmap(resources.getDrawable(resources.getIdentifier("anki_icon", "drawable", packageName)))
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override suspend fun syncToAnki() {
        val wordListDAO = HSKAppServices.database.wordListDAO()
        val annotatedChineseWordDAO = HSKAppServices.database.annotatedChineseWordDAO()
        val ankiStore = HSKAppServices.ankiStore

        val lists = wordListDAO.getAllLists()
        val entries = wordListDAO.getAllListEntries()

        val annotatedChineseWords = mutableListOf<AnnotatedChineseWord>()
        val wordList = entries.map { it.simplified }

        /**
         * Some devices like Huawei P20 or Samsung Galaxy A11 have a low limit on variables per
         * SQLite queries. Most devices don't, so to be safe, we will iterate at max 12 times with
         * lowering the number of words per query, hence lowering the number of variables each time.
         */
        var chunkSize = 2048
        var formerException : SQLiteException? = SQLiteException()
        while (formerException != null && chunkSize >= 1) {
            annotatedChineseWords.clear()

            try {
                // Safeguard against too many SQL vars error for a big migration
                val chunks = wordList.chunked(chunkSize)
                for (chunk in chunks) {
                    val partialResult = annotatedChineseWordDAO.getFromSimplified(chunk)
                    annotatedChineseWords.addAll(partialResult)
                }
                formerException = null

            } catch (e: SQLiteException) {
                formerException = e
                chunkSize /= 2
            }
        }

        if (formerException != null) {
            throw formerException
        }

        val words = annotatedChineseWords.associateBy { it.simplified }

        if (lists.isEmpty() || entries.isEmpty() || words.isEmpty()) return
        val nbToImport = entries.size

        var nbErrors = 0
        var nbDeckCreationErrors = 0
        Log.i(TAG, "syncListsToAnki.Anki: Creating decks if needed")
        val decks = mutableMapOf<Long, WordList>()
        for (list in lists) {
            decks[list.id] = wordListRepository.getOrCreate(list.wordList)

            if (decks[list.id]!!.ankiDeckId == WordList.Companion.ANKI_ID_EMPTY) {
                nbDeckCreationErrors += 1
            }
        }
        Log.i(TAG, "syncListsToAnki.Anki: Created decks if possible: $nbDeckCreationErrors errors")

        Log.i(TAG, "syncListsToAnki.Anki: Starting iterating through lists")
        for ((index, entry) in entries.withIndex()) {
            val deck = decks[entry.listId]
            if (deck == null) {
                Log.e(TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to any list. Skipping entry")
                nbErrors += 1
                continue
            }

            if (words[entry.simplified] == null) {
                Log.e(TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to an actual word. Skipping entry")
                nbErrors += 1
                continue
            }

            if (deck.ankiDeckId == WordList.Companion.ANKI_ID_EMPTY) {
                Log.e(TAG, "syncListsToAnki.Anki: deck with no ID in Anki. Skipping entry")
                nbErrors += 1
                continue
            }

            val id = ankiStore.importOrUpdateCard(deck, entry, words[entry.simplified]!!)

            if (id == null) {
                nbErrors += 1
            }

            // Report progress
            val progress = index + 1
            var message = syncProgressMessage
            message = message.format(entry.simplified, progress, nbToImport)

            Log.d(TAG, message)

            currentCoroutineContext().ensureActive() // let the loop stop IF asked to cancel
            // Update notification and Emit progress event
            updateProgress(progress, nbErrors, nbToImport, message)
            yield() // Ensure other small operations can happen. Shouldn't be needed though.
        }

        Log.i(TAG, "importOrUpdateAllCards: imported for {$nbToImport-$nbErrors} ouf of $nbToImport")

        if (nbErrors > 0) {
            Result.failure(Exception("$nbErrors ouf of $nbToImport imported to Anki."))
        } else {
            Result.success(Unit)
        }
    }

    override fun getSyncStartMessage() = syncStartMessage

    override fun getActivityClass(): Class<out Any> {
        return this.javaClass
    }

    override fun getNotificationTitle() = notificationTitle

    override fun getNotificationLargeIcon(): Bitmap? = notificationLargeIcon

    override fun getNotificationSmallIcon() = this.resources.getIdentifier("ic_launcher", "mipmap", this.packageName)

    override fun getNotificationCancelIcon() = this.resources.getIdentifier("close_24px", "drawable", this.packageName)

    override fun getNotificationCancelText() = notificationCancelText

    override fun getNotificationChannelTitle() = notificationChannelTitle

    override fun getNotificationChannelDescription() = notificationChannelDescription

    companion object {
        private const val TAG = "AnkiSyncWordListsService"
    }
}