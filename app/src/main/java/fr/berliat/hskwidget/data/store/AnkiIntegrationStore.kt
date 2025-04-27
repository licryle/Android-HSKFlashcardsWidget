package fr.berliat.hskwidget.data.store


import android.content.Context
import android.util.Log
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import java.util.Locale
import kotlin.reflect.KSuspendFunction2

class AnkiIntegrationStore(val context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "anki") {
    private val api: AddContentApi = AddContentApi(context)

    fun addModelId(modelName: String, modelId: Long) {
        putLong("model:${modelName}", modelId)
    }

    fun getModelId(modelName: String): Long {
        return getLong("model:${modelName}", -1)
    }

    fun addDeckId(deckName: String, modelId: Long) {
        putLong("deck:${deckName}", modelId)
    }

    fun getDeckId(deckName: String): Long {
        return getLong("deck:${deckName}", -1)
    }

    fun findOrCreateDeckIdByName(deckName: String): Long? {
        var did: Long? = findDeckIdByName(deckName)
        if (did == null) {
            did = api.addNewDeck(deckName)

            Log.i(TAG, "findOrCreateDeckIdByName: Inserted new Deck into Anki")

            addDeckId(deckName, did)
        }

        return did
    }

    /**
     * Try to find the given deck by name, accounting for potential renaming of the deck by the user as follows:
     * If there's a deck with deckName then return it's ID
     * If there's no deck with deckName, but a ref to deckName is stored in SharedPreferences, and that deck exist in
     * AnkiDroid (i.e. it was renamed), then use that deck.Note: this deck will not be found if your app is re-installed
     * If there's no reference to deckName anywhere then return null
     * @param deckName the name of the deck to find
     * @return the did of the deck in Anki
     */
    fun findDeckIdByName(deckName: String): Long? {
        // Look for deckName in the deck list
        var did = lookupDeckId(deckName)
        if (did != null) {
            // If the deck was found then return it's id
            return did
        } else {
            // Otherwise try to check if we have a reference to a deck that was renamed and return that
            did = getDeckId(deckName)
            return if (did != -1L && api.getDeckName(did) != null) {
                did
            } else {
                // If the deck really doesn't exist then return null
                null
            }
        }
    }

    /**
     * Get the ID of the deck which matches the name
     * @param deckName Exact name of deck (note: deck names are unique in Anki)
     * @return the ID of the deck that has given name, or null if no deck was found or API error
     */
    private fun lookupDeckId(deckName: String): Long? {
        val deckList: MutableMap<Long, String> = api.getDeckList() ?: return null

        for ((key, value) in deckList) {
            if (value.equals(deckName, ignoreCase = true)) {
                return key
            }
        }
        return null
    }

    fun findOrCreateModelIdByName(deckName: String, modelName: String): Long? {
        var mid: Long? = findModelIdByName(modelName, FIELDS.size)

        if (mid == null) {
            mid = api.addNewCustomModel(
                MODEL_NAME,
                FIELDS,
                CARD_NAMES,
                arrayOf(context.getString(R.string.anki_card_CNEN_front)),
                arrayOf(context.getString(R.string.anki_card_CNEN_back)),
                context.getString(R.string.anki_card_css),
                findDeckIdByName(deckName),
                null
            )

            Log.i(TAG, "findOrCreateModelIdByName: Inserted new Model into Anki")

            addModelId(MODEL_NAME, mid)
        }

        return mid
    }


    /**
     * Try to find the given model by name, accounting for renaming of the model:
     * If there's a model with this modelName that is known to have previously been created (by this app)
     * and the corresponding model ID exists and has the required number of fields
     * then return that ID (even though it may have since been renamed)
     * If there's a model from #getModelList with modelName and required number of fields then return its ID
     * Otherwise return null
     * @param modelName the name of the model to find
     * @param numFields the minimum number of fields the model is required to have
     * @return the model ID or null if something went wrong
     */
    fun findModelIdByName(modelName: String, numFields: Int): Long? {
        val prefsModelId = getModelId(modelName)
        // if we have a reference saved to modelName and it exists and has at least numFields then return it
        if ((prefsModelId != -1L)
            && (api.getModelName(prefsModelId) != null)
            && (api.getFieldList(prefsModelId) != null)
            && (api.getFieldList(prefsModelId).size >= numFields)
        ) { // could potentially have been renamed
            return prefsModelId
        }
        val modelList: Map<Long?, String> = api.getModelList(numFields)
        for ((key, value) in modelList) {
            if (value == modelName) {
                return key // first model wins
            }
        }
        // model no longer exists (by name nor old id), the number of fields was reduced, or API error
        return null
    }

    suspend fun importOrUpdateAllCards(
        throwOnError: Boolean = true,
        progressCallBack: KSuspendFunction2<Int, Int, Unit>,
        cardInsertCallBack: KSuspendFunction2<AnnotatedChineseWord, Long, Unit>
    ) : Array<Int> {
        Log.d(TAG, "importOrUpdateAllCards: starting import")
        val annotationDAO = ChineseWordsDatabase.getInstance(context).annotatedChineseWordDAO()
        val words = annotationDAO.getAllAnnotated()

        val nbToImport = words.size
        var nbImported = 0

        for (word in words) {
            val id = importOrUpdateCard(word)
            if (id != null) {
                nbImported += 1

                progressCallBack(nbImported, nbToImport)

                if (id != word.annotation!!.ankiId) {
                    cardInsertCallBack(word, id)
                }
            } else {
                if (throwOnError) {
                    throw IllegalAccessError("Couldn't import note on ${word.simplified}.")
                }
            }
        }
        Log.d(TAG, "importOrUpdateAllCards: import done for ${nbImported} ouf of ${nbToImport}")

        return arrayOf(nbToImport, nbImported)
    }

    fun importOrUpdateCard(word: AnnotatedChineseWord): Long? {
        Log.d(TAG, "importOrUpdateCard: ${word.simplified} to Anki")
        val deckId = findOrCreateDeckIdByName(DECK_NAME)
        val modelId = findOrCreateModelIdByName(DECK_NAME, MODEL_NAME)

        if (deckId == null || modelId == null) {
            return null
        }

        with (word.annotation) {
            val fields = arrayOf(
                word.simplified,
                word.word?.pinyins?.toString() ?: "",
                word.word?.definition?.get(Locale.ENGLISH) ?: "",
                this?.notes ?: "",
                this?.firstSeen?.toString() ?: "",
                word.word?.hskLevel?.toString() ?: "",
                this?.level?.toString() ?: "",
                this?.classType?.toString() ?: "",
                this?.themes ?: ""
            )

            val tags: MutableSet<String> = mutableSetOf(
                word.word?.hskLevel?.toString() ?: "",
                this?.level?.toString() ?: "",
                this?.classType?.toString() ?: ""
            ).apply {
                addAll(this@with?.themes?.split(",") ?: emptyList())
            }

            var note : NoteInfo? = null
            if ((word.annotation?.ankiId
                    ?: ChineseWordAnnotation.ANKI_ID_EMPTY) != ChineseWordAnnotation.ANKI_ID_EMPTY
            ) {
                note = api.getNote(word.annotation!!.ankiId)
            }

            if (note != null && note.fields.size > 1 && note.fields[0] == word.simplified.trim().replace("\r\n","\n")) {
                api.updateNoteFields(note.id, fields)
                api.updateNoteTags(note.id, tags)
                return note.id
            } else {
                return api.addNote(modelId, deckId, fields, tags)
            }
        }
    }

    companion object {
        const val TAG = "AnkiDroidHelper"

        // Name of deck which will be created in AnkiDroid
        const val DECK_NAME: String = "HSK Helper"

        // Name of model which will be created in AnkiDroid
        const val MODEL_NAME: String = "fr.berliat.hskhelper"

        // Optional space separated list of tags to add to every note
        //val TAGS: Set<String> = HashSet(listOf("API_Sample_App"))

        // List of field names that will be used in AnkiDroid model
        val FIELDS: Array<String> = arrayOf(
            "Hanzi", "Pinyin", "Definition", "Notes", "FirstSeen", "HSK", "Level", "Class", "Themes"
        )

        // List of card names that will be used in AnkiDroid (one for each direction of learning)
        val CARD_NAMES: Array<String> = arrayOf("English>Chinese")
    }
}