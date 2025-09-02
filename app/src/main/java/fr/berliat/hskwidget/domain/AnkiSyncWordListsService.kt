package fr.berliat.hskwidget.domain

import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import fr.berliat.ankidroidhelper.AnkiSyncService
import fr.berliat.hskwidget.MainActivity
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.WordList
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.AnkiStore
import fr.berliat.hskwidget.data.store.DatabaseHelper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlin.collections.chunked

class AnkiSyncWordListsService: AnkiSyncService() {
    override suspend fun syncToAnki() {
        val wordListDAO = DatabaseHelper.getInstance(this).wordListDAO()
        val annotatedChineseWordDAO = DatabaseHelper.getInstance(this).annotatedChineseWordDAO()
        val ankiStore = AnkiStore(this)

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
        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Creating decks if needed")
        val decks = mutableMapOf<Long, AnkiDeck>()
        for (list in lists) {
            decks[list.id] = AnkiDeck.getOrCreate(this, ankiStore, list.wordList)

            if (decks[list.id]!!.ankiId == WordList.ANKI_ID_EMPTY) {
                nbDeckCreationErrors += 1
            }
        }
        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Created decks if possible: $nbDeckCreationErrors errors")

        Log.i(WordListRepository.TAG, "syncListsToAnki.Anki: Starting iterating through lists")
        for ((index, entry) in entries.withIndex()) {
            val deck = decks[entry.listId]
            if (deck == null) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to any list. Skipping entry")
                nbErrors += 1
                continue
            }

            if (words[entry.simplified] == null) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: found a WordListEntry not linked to an actual word. Skipping entry")
                nbErrors += 1
                continue
            }

            if (deck.ankiId == WordList.ANKI_ID_EMPTY) {
                Log.e(WordListRepository.TAG, "syncListsToAnki.Anki: deck with no ID in Anki. Skipping entry")
                nbErrors += 1
                continue
            }

            val id = ankiStore.importOrUpdateCard(deck, entry, words[entry.simplified]!!)

            if (id == null) {
                nbErrors += 1
            }

            // Report progress
            val progress = index + 1
            var message = resources.getString(R.string.anki_sync_progress_message)
            message = message.format(entry.simplified, progress, nbToImport)

            Log.d(WordListRepository.TAG, message)

            currentCoroutineContext().ensureActive() // let the loop stop IF asked to cancel
            // Update notification and Emit progress event
            updateProgress(progress, nbErrors, nbToImport, message)
            yield() // Ensure other small operations can happen. Shouldn't be needed though.
        }

        Log.i(WordListRepository.TAG, "importOrUpdateAllCards: imported for {$nbToImport-$nbErrors} ouf of $nbToImport")

        if (nbErrors > 0) {
            Result.failure(Exception("$nbErrors ouf of $nbToImport imported to Anki."))
        } else {
            Result.success(Unit)
        }
    }

    override fun getSyncStartMessage(): String {
        return getString(R.string.anki_sync_start_message)
    }

    override fun getActivityClass(): Class<out Any> {
        return MainActivity::class.java
    }

    override fun getNotificationTitle(): String {
        return getString(R.string.app_name)
    }

    override fun getNotificationLargeIcon(): Bitmap? {
        return BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
    }

    override fun getNotificationSmallIcon(): Int {
        return R.mipmap.ic_launcher_monochrome_mini
    }

    override fun getNotificationCancelIcon(): Int {
        return R.drawable.close_24px
    }

    override fun getNotificationCancelText(): String {
        return getString(R.string.cancel)
    }

    override fun getNotificationChannelTitle(): String {
        return getString(R.string.anki_sync_notification_name)
    }

    override fun getNotificationChannelDescription(): String {
        return getString(R.string.anki_sync_notification_description)
    }
}