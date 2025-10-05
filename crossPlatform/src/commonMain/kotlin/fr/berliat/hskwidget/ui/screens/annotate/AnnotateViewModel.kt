package fr.berliat.hskwidget.ui.screens.annotate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.berliat.hskwidget.core.KAnkiDelegator
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.Utils.incrementConsultedWord

import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.data.type.Pinyins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class AnnotateViewModel(
    prefsStore: AppPreferencesStore = HSKAppServices.appPreferences,
    private val database: ChineseWordsDatabase = HSKAppServices.database,
    private val wordListRepo: WordListRepository = HSKAppServices.wordListRepo,
    private val ankiCaller : KAnkiDelegator
) : ViewModel() {
    private val _annotatedWord = MutableStateFlow<AnnotatedChineseWord?>(null)
    val annotatedWord: StateFlow<AnnotatedChineseWord?> get() = _annotatedWord

    val showHSK3Definition: StateFlow<Boolean> = prefsStore.dictionaryShowHSK3Definition.asStateFlow()
    val lastAnnotatedClassType: StateFlow<ClassType> = prefsStore.lastAnnotatedClassType.asStateFlow()
    val lastAnnotatedClassLevel: StateFlow<ClassLevel> = prefsStore.lastAnnotatedClassLevel.asStateFlow()

    suspend fun fetchAnnotatedWord(word: String, callBack: ((AnnotatedChineseWord) -> Unit)?) {
        if (word == "") {
            _annotatedWord.value = AnnotatedChineseWord.getBlank()
        } else {
            _annotatedWord.value = getAnnotatedChineseWord(word)

            callBack?.invoke(annotatedWord.value!!)
        }
    }
    suspend fun getAnnotatedChineseWord(simplifiedWord: String): AnnotatedChineseWord
            = withContext(Dispatchers.IO) {
        val annot = HSKAppServices.database.annotatedChineseWordDAO().getFromSimplified(simplifiedWord)
        return@withContext if (annot == null || !annot.hasAnnotation()) {
            AnnotatedChineseWord(
                annot?.word ?: ChineseWord.getBlank(simplifiedWord),
                ChineseWordAnnotation.getBlank(simplifiedWord)
            )
        } else {
            annot
        }
    }

    fun saveWord(annotatedWord: AnnotatedChineseWord, pinyins: String, notes: String, themes: String, isExam: Boolean, cType: ClassType, cLevel: ClassLevel, callback: ((AnnotatedChineseWord, Exception?) -> Unit)? = null) {
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
        updateAnnotation(annotatedWord) { err -> callback?.invoke(annotatedWord, err) }

        incrementConsultedWord(annotatedWord.simplified)
    }

    // Save or update annotation
    fun updateAnnotation(annotatedWord: AnnotatedChineseWord, callback: (Exception?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var error: Exception? = null
            try {
                database.chineseWordAnnotationDAO().insertOrUpdate(annotatedWord.annotation!!)

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
    fun deleteAnnotation(simplified: String, callback: ((String, Exception?) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            var error: Exception? = null
            try {
                val nbRowAffected = database.chineseWordAnnotationDAO().deleteBySimplified(simplified)
                if (nbRowAffected == 0) throw Exception("No records deleted")

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.ANNOTATION_DELETE)

                wordListRepo.touchAnnotatedList()
                ankiCaller(wordListRepo.removeWordFromAllLists(simplified))
            } catch (e: Exception) {
                error = e
            }

            withContext(Dispatchers.Main) {
                callback?.invoke(simplified, error)
            }
        }
    }

    fun speakWord(word: String) {
        Utils.playWordInBackground(word)
    }

    fun copyWord(word: String) {
        Utils.copyToClipBoard(word)
    }
}