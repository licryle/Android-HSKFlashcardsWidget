package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.berliat.ankihelper.AnkiDelegator
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.data.store.DatabaseHelper
import fr.berliat.hskwidget.domain.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotateViewModel(val context: Context, val wordListRepo: WordListRepository, val ankiCaller: AnkiDelegator) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _annotatedWord = MutableLiveData<AnnotatedChineseWord>()
    val annotatedWord: LiveData<AnnotatedChineseWord> get() = _annotatedWord
    val simplified: String get() = _annotatedWord.value?.simplified ?: ""

    private suspend fun database() = DatabaseHelper.getInstance(context)

    // Fetch annotated word for a given simplified word
    fun fetchAnnotatedWord(arguments: Bundle?) {
        val simplifiedWord = arguments?.getString("simplifiedWord") ?: ""
        if (simplifiedWord == "") {
            _annotatedWord.value = AnnotatedChineseWord.getBlank(simplifiedWord)
        } else {
            viewModelScope.launch {
                val word = withContext(Dispatchers.IO) {
                    getAnnotatedChineseWord(simplifiedWord)
                }

                withContext(Dispatchers.Main) {
                    _annotatedWord.value = word
                }
            }
        }
    }

    suspend fun getAnnotatedChineseWord(simplifiedWord: String): AnnotatedChineseWord {
        val annot = database().annotatedChineseWordDAO().getFromSimplified(simplifiedWord)
        return if (annot == null || !annot.hasAnnotation()) {
            AnnotatedChineseWord(
                annot?.word ?: ChineseWord.getBlank(simplifiedWord),
                ChineseWordAnnotation.getBlank(simplifiedWord)
            )
        } else {
            annot
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
                ankiCaller(wordListRepo.updateInAllLists(simplified))
            } catch (e: Exception) {
                error = e
            }
            withContext(Dispatchers.Main) {
                callback(error)
            }
        }
    }

    // Delete annotation
    fun deleteAnnotation(callback: (Exception?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var error: Exception? = null
            try {
                val nbRowAffected = database().chineseWordAnnotationDAO().deleteBySimplified(annotatedWord.value!!.simplified)
                if (nbRowAffected == 0) throw Exception("No records deleted")

                Utils.logAnalyticsEvent(Utils.ANALYTICS_EVENTS.ANNOTATION_DELETE)

                wordListRepo.touchAnnotatedList()
                ankiCaller(wordListRepo.removeWordFromAllLists(simplified))
            } catch (e: Exception) {
                error = e
            }

            withContext(Dispatchers.Main) {
                callback(error)
            }
        }
    }
}
