package fr.berliat.hskwidget.ui.screens.annotate

import androidx.lifecycle.ViewModel

import fr.berliat.hskwidget.Utils
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.getAppPreferencesStore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AnnotateViewModel(
    prefsStore: AppPreferencesStore = getAppPreferencesStore()
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
        val annot = Utils.getDatabaseInstance().annotatedChineseWordDAO().getFromSimplified(simplifiedWord)
        return@withContext if (annot == null || !annot.hasAnnotation()) {
            AnnotatedChineseWord(
                annot?.word ?: ChineseWord.getBlank(simplifiedWord),
                ChineseWordAnnotation.getBlank(simplifiedWord)
            )
        } else {
            annot
        }
    }
}