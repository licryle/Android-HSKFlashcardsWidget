package fr.berliat.hskwidget.domain

import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.util.Log
import android.graphics.Canvas
import fr.berliat.ankidroidhelper.AnkiSyncService
import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_notification_description
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_notification_name
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_progress_message
import hskflashcardswidget.crossplatform.generated.resources.anki_sync_start_message
import hskflashcardswidget.crossplatform.generated.resources.app_name
import hskflashcardswidget.crossplatform.generated.resources.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.getString
import kotlin.collections.chunked

@OptIn(InternalResourceApi::class)
actual class AnkiSyncWordListsService: AnkiSyncService() {
    private lateinit var syncStartMessage : String
    private lateinit var syncProgressMessage : String
    private lateinit var notificationTitle : String
    private lateinit var notificationCancelText : String
    private lateinit var notificationChannelDescription : String
    private lateinit var notificationChannelTitle : String
    private lateinit var notificationLargeIcon : Bitmap

    private lateinit var wordListRepository : WordListRepository
    init {
        runBlocking { // Todo, optimize that, refactor parent to load at the beginning of service
            wordListRepository = WordListRepository.getInstance()
            syncStartMessage = getString(Res.string.anki_sync_start_message)
            syncProgressMessage = getString(Res.string.anki_sync_progress_message)
            notificationTitle = getString(Res.string.app_name)
            notificationCancelText = getString(Res.string.cancel)
            notificationChannelDescription = getString(Res.string.anki_sync_notification_description)
            notificationChannelTitle = getString(Res.string.anki_sync_notification_name)
            notificationLargeIcon = loadVectorXmlAsBitmap("drawable/close_24px.xml")//Res.drawable.close_24px)
        }
    }

    suspend fun loadVectorXmlAsBitmap(resourcePath: String): Bitmap {
        // Parse the XML string into a VectorDrawableCompat
        // This requires an XML pull parser, which is more complex than a direct string.
        // A simpler, though less robust way, is to manually draw it.
        // The most robust way is via `ContextCompat`.
        try {
            val resourceId = this.resources.getIdentifier(
                resourcePath.substringAfterLast("/").substringBeforeLast("."),
                "drawable",
                this.packageName
            )

            val drawable = VectorDrawableCompat.create(this.resources, resourceId, null)

            // Ensure the drawable is not null and has a size
            if (drawable == null || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                throw Error()
            }

            // Use the drawable's intrinsic size for the bitmap.
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight

            // Create a new bitmap.
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Create an Android-specific Canvas instance, which takes a Bitmap in its constructor.
            val canvas = Canvas(bitmap)

            // Set the bounds and draw the drawable onto the canvas.
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            return bitmap
        } catch (_: Exception) {
            return Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        }
    }

    override suspend fun syncToAnki() {
        val wordListDAO = Utils.getDatabaseInstance().wordListDAO()
        val annotatedChineseWordDAO = Utils.getDatabaseInstance().annotatedChineseWordDAO()
        val ankiStore = AnkiStore.getInstance()

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
        Log.i(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: Creating decks if needed")
        val decks = mutableMapOf<Long, WordList>()
        for (list in lists) {
            decks[list.id] = wordListRepository.getOrCreate(list.wordList)

            if (decks[list.id]!!.ankiDeckId == WordList.Companion.ANKI_ID_EMPTY) {
                nbDeckCreationErrors += 1
            }
        }
        Log.i(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: Created decks if possible: $nbDeckCreationErrors errors")

        Log.i(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: Starting iterating through lists")
        for ((index, entry) in entries.withIndex()) {
            val deck = decks[entry.listId]
            if (deck == null) {
                Log.e(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to any list. Skipping entry")
                nbErrors += 1
                continue
            }

            if (words[entry.simplified] == null) {
                Log.e(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to an actual word. Skipping entry")
                nbErrors += 1
                continue
            }

            if (deck.ankiDeckId == WordList.Companion.ANKI_ID_EMPTY) {
                Log.e(WordListRepository.Companion.TAG, "syncListsToAnki.Anki: deck with no ID in Anki. Skipping entry")
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

            Log.d(WordListRepository.Companion.TAG, message)

            currentCoroutineContext().ensureActive() // let the loop stop IF asked to cancel
            // Update notification and Emit progress event
            updateProgress(progress, nbErrors, nbToImport, message)
            yield() // Ensure other small operations can happen. Shouldn't be needed though.
        }

        Log.i(WordListRepository.Companion.TAG, "importOrUpdateAllCards: imported for {$nbToImport-$nbErrors} ouf of $nbToImport")

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

    override fun getNotificationSmallIcon() = this.resources.getIdentifier("ic_launcher_monochrome_mini", "mipmap", this.packageName)

    override fun getNotificationCancelIcon() = this.resources.getIdentifier("close_24px", "drawable", this.packageName)

    override fun getNotificationCancelText() = notificationCancelText

    override fun getNotificationChannelTitle() = notificationChannelTitle

    override fun getNotificationChannelDescription() = notificationChannelDescription

    companion object {
        const val TAG = "AnkiSyncWordListsService"
    }
}