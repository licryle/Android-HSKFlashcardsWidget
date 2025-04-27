package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWord
import fr.berliat.hskwidget.data.dao.AnnotatedChineseWordDAO
import fr.berliat.hskwidget.data.dao.ChineseWordAnnotationDAO
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.store.ChineseWordsDatabase

class AnnotateViewModelFactory(val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnnotateViewModel::class.java)) {
            return AnnotateViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AnnotateViewModel(context: Context) : ViewModel() {
    private val annotatedWordsDAO: AnnotatedChineseWordDAO =
        ChineseWordsDatabase.getInstance(context).annotatedChineseWordDAO()
    private val annotationDAO: ChineseWordAnnotationDAO =
        ChineseWordsDatabase.getInstance(context).chineseWordAnnotationDAO()

    suspend fun getAnnotatedChineseWord(simplified: String): AnnotatedChineseWord? {
        return annotatedWordsDAO.getFromSimplified(simplified)
    }

    suspend fun updateAnnotation(annotation: ChineseWordAnnotation): Exception? {
        try {
            annotationDAO.insertOrUpdate(annotation)
        } catch (e: Exception) {
            return e
        }

        return null
    }

    suspend fun updateAnnotationAnkiId(annotation: ChineseWordAnnotation, ankiId: Long): Exception? {
        try {
            annotationDAO.updateAnkiNoteId(annotation.simplified, ankiId)
        } catch (e: Exception) {
            return e
        }

        return null
    }

    suspend fun deleteAnnotation(simplified: String): Exception? {
        val nbRowAffected: Int
        try {
            nbRowAffected = annotationDAO.deleteBySimplified(simplified)
        } catch (e: Exception) {
            return e
        }

        return if (nbRowAffected == 1)
            null
        else
            Exception("No records deleted for $simplified")
    }
}
