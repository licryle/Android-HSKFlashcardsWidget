package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnotateViewModelFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnnotateViewModel::class.java)) {
            return AnnotateViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AnnotateViewModel(context: Context) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val annotatedWordsDAO: AnnotatedChineseWordDAO =
        ChineseWordsDatabase.getInstance(context).annotatedChineseWordDAO()
    private val annotationDAO: ChineseWordAnnotationDAO =
        ChineseWordsDatabase.getInstance(context).chineseWordAnnotationDAO()
    private val _annotatedWord = MutableLiveData<AnnotatedChineseWord>()
    val annotatedWord: LiveData<AnnotatedChineseWord> get() = _annotatedWord
    val simplified: String get() = _annotatedWord.value?.simplified ?: ""

    fun fetchAnnotatedWord(arguments: Bundle?) {
        val simplifiedWord = arguments?.getString("simplifiedWord") ?: ""
        if (simplifiedWord == "") {
            _annotatedWord.value = AnnotatedChineseWord.getBlank(simplifiedWord)
        } else {
            viewModelScope.launch {
                // Switch to the IO dispatcher to perform background work
                _annotatedWord.value = getAnnotatedChineseWord(simplifiedWord) // Checked just below
            }
        }
    }

    suspend fun getAnnotatedChineseWord(simplifiedWord: String): AnnotatedChineseWord {
        val annot = annotatedWordsDAO.getFromSimplified(simplifiedWord) // Checked just below

        if (annot == null || !annot.hasAnnotation()) { // failure or new word
            return AnnotatedChineseWord(
                annot?.word ?: ChineseWord.getBlank(simplifiedWord),
                ChineseWordAnnotation.getBlank(simplifiedWord)
            )
        }
        return annot
    }

    fun updateAnnotation(annotation: ChineseWordAnnotation, callback: (Exception?) -> Unit) {
        viewModelScope.launch {
            var error: Exception? = null
            try {
                annotationDAO.insertOrUpdate(annotation)
                _annotatedWord.value = AnnotatedChineseWord(_annotatedWord.value!!.word, annotation)
            } catch (e: Exception) {
                error = e
            }

            withContext(Dispatchers.Main) {
                callback(error)
            }
        }
    }

    fun updateAnnotationAnkiId(ankiId: Long, callback: ((Exception?) -> Unit)?) {
        viewModelScope.launch {
            var error: Exception? = null
            try {
                annotationDAO.updateAnkiNoteId(simplified, ankiId)
            } catch (e: Exception) {
                error = e
            }

            if (callback != null) {
                withContext(Dispatchers.Main) {
                    callback(error)
                }
            }
        }
    }

    fun deleteAnnotation(callback: (Exception?) -> Unit) {
        viewModelScope.launch {
            var error: Exception? = null
            var nbRowAffected = 0
            try {
                nbRowAffected = annotationDAO.deleteBySimplified(annotatedWord.value!!.simplified)
            } catch (e: Exception) {
                error = e
            }

            if (error == null && nbRowAffected == 0) {
                error = Exception("No records deleted for $annotatedWord.value!!.simplified")
            }

            withContext(Dispatchers.Main) {
                callback(error)
            }
        }
    }
}
