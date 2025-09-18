package fr.berliat.hskwidget.ui.dictionary

import android.content.Context

import fr.berliat.ankidroidhelper.AnkiDelegator
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.data.type.Pinyins
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class OldAnnotateViewModel(val context: Context, val wordListRepo: WordListRepository, val ankiCaller: AnkiDelegator) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private suspend fun database() = withContext(Dispatchers.IO) { DatabaseHelper.getInstance(context) }

    suspend fun getAnnotatedChineseWord(simplifiedWord: String): AnnotatedChineseWord
            = withContext(Dispatchers.IO) {
        val annot = database().annotatedChineseWordDAO().getFromSimplified(simplifiedWord)
        return@withContext if (annot == null || !annot.hasAnnotation()) {
            AnnotatedChineseWord(
                annot?.word ?: ChineseWord.getBlank(simplifiedWord),
                ChineseWordAnnotation.getBlank(simplifiedWord)
            )
        } else {
            annot
        }
    }

    fun saveWord(annotatedWord: AnnotatedChineseWord, pinyins: String, notes: String, themes: String, isExam: Boolean, cType: ClassType, cLevel: ClassLevel, resultCallback: ((AnnotatedChineseWord, Exception?) -> Unit)) {
        var firstSeen = annotatedWord.annotation?.firstSeen
        if (firstSeen == null)
            firstSeen = Clock.System.now()

        val updatedAnnotation = ChineseWordAnnotation(
            simplified = annotatedWord.simplified.trim(),
            pinyins = Pinyins.fromString(pinyins),
            notes = notes,
            classType = cType,
            level = cLevel,
            themes = themes,
            firstSeen = firstSeen,  // Handle date logic
            isExam = isExam
        )

        val annotatedWord = AnnotatedChineseWord(annotatedWord.word, updatedAnnotation)
        updateAnnotation(annotatedWord) { err -> resultCallback.invoke(annotatedWord, err) }

        Utils.incrementConsultedWord(context, annotatedWord.simplified)

        if (annotatedWord.hasAnnotation()) {
            OldAppPreferencesStore(context).lastAnnotatedClassType = updatedAnnotation.classType!!
            OldAppPreferencesStore(context).lastAnnotatedClassLevel = updatedAnnotation.level!!
        }
    }

    // Save or update annotation
    fun updateAnnotation(annotatedWord: AnnotatedChineseWord, callback: (Exception?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var error: Exception? = null
            try {
                database().chineseWordAnnotationDAO().insertOrUpdate(annotatedWord.annotation!!)

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.ANNOTATION_SAVE)

                ankiCaller(wordListRepo.addWordToSysAnnotatedList(annotatedWord))
                ankiCaller(wordListRepo.updateInAllLists(annotatedWord.simplified))
            } catch (e: Exception) {
                error = e
            }
            withContext(Dispatchers.Main) {
                callback(error)
            }
        }
    }

    // Delete annotation
    fun deleteAnnotation(simplified: String, callback: (String, Exception?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var error: Exception? = null
            try {
                val nbRowAffected = database().chineseWordAnnotationDAO().deleteBySimplified(simplified)
                if (nbRowAffected == 0) throw Exception("No records deleted")

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.ANNOTATION_DELETE)

                wordListRepo.touchAnnotatedList()
                ankiCaller(wordListRepo.removeWordFromAllLists(simplified))
            } catch (e: Exception) {
                error = e
            }

            withContext(Dispatchers.Main) {
                callback(simplified, error)
            }
        }
    }
}
