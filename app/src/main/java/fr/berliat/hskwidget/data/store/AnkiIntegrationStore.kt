package fr.berliat.hskwidget.data.store


import android.content.Context
import android.util.Log
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.dao.AnkiNotesDAO
import fr.berliat.hskwidget.data.model.WordListEntry

class AnkiIntegrationStore(val context: Context):
    PrefixedPreferenceDataStoreBridge(context.dataStore, "anki") {
    val api: AnkiNotesDAO = AnkiNotesDAO(context)

    suspend fun isStoreReady() : Boolean {
        return api.getDeckList() != null
    }

    suspend fun getOrCreateModelId(): Long? {
        var mid : Long? = getModelId()

        if (mid == null || mid.toInt() == 0 || !api.isModelExist(mid)) {
            // Not found, let's add
            mid = createModel()

            if (mid == null) {
                Log.i(TAG, "findOrCreateModelIdByName: Couldn't add model to Anki")
                return null
            }

            Log.i(TAG, "findOrCreateModelIdByName: Inserted new Model into Anki")
            setModelId(mid)
        }

        return mid
    }

    suspend fun deleteCard(word: WordListEntry): Boolean {
        return word.ankiNoteId.let { api.deleteNote(it) }
    }

    private fun setModelId(modelId: Long) {
        putLong("model", modelId)
    }

    private fun getModelId(): Long {
        return getLong("model", -1)
    }

    private fun getModelName() : String { return context.getString(R.string.app_name) }

    private suspend fun createModel() : Long? {
        return api.addNewCustomModel(
            getModelName(),
            FIELDS,
            CARD_NAMES,
            arrayOf(
                context.getString(R.string.anki_card_ENCN_front),
                context.getString(R.string.anki_card_CNEN_front)
            ),
            arrayOf(
                context.getString(R.string.anki_card_ENCN_back),
                context.getString(R.string.anki_card_CNEN_back)
            ),
            context.getString(R.string.anki_card_css),
            null,
            null
        )
    }

    companion object {
        const val TAG = "AnkiDroidHelper"

        // List of field names that will be used in AnkiDroid model
        val FIELDS: Array<String> = arrayOf(
            "Hanzi", "Pinyin", "Definition", "Notes", "FirstSeen", "HSK", "Level", "Class", "Themes"
        )

        // List of card names that will be used in AnkiDroid (one for each direction of learning)
        val CARD_NAMES: Array<String> = arrayOf("English>Chinese", "Chinese>English")
    }
}