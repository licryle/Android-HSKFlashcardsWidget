package fr.berliat.hskwidget.ui.dictionary

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ResultCallback
import fr.berliat.ankidroidhelper.AnkiDelegator
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord
import fr.berliat.hskwidget.data.model.ChineseWord
import fr.berliat.hskwidget.data.model.ChineseWordAnnotation
import fr.berliat.hskwidget.data.repo.WordListRepository
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
/*
class AnnotateViewModel(val context: Context, val wordListRepo: WordListRepository, val ankiCaller: AnkiDelegator) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _annotatedWord = MutableLiveData<AnnotatedChineseWord>()
    val annotatedWord: LiveData<AnnotatedChineseWord> get() = _annotatedWord
    val simplified: String get() = _annotatedWord.value?.simplified ?: ""

    private suspend fun database() = withContext(Dispatchers.IO) { DatabaseHelper.getInstance(context) }

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

    fun saveWord(resultCallback: ((Error) -> Unit)) {
        var firstSeen = annotateViewModel.annotatedWord.value?.annotation?.firstSeen
        if (firstSeen == null)
            firstSeen = Clock.System.now()

        val updatedAnnotation = ChineseWordAnnotation(
            simplified = annotateViewModel.simplified.trim(),
            pinyins = Pinyins.fromString(pinyins),
            notes = binding.annotationEditNotes.text.toString(),
            classType = ClassType.entries[binding.annotationEditClassType.selectedItemPosition],
            level = ClassLevel.entries[binding.annotationEditClassLevel.selectedItemPosition],
            themes = binding.annotationEditThemes.text.toString(),
            firstSeen = firstSeen,  // Handle date logic
            isExam = binding.annotationEditIsExam.isChecked
        )

        val annotatedWord = AnnotatedChineseWord(annotateViewModel.annotatedWord.value!!.word, updatedAnnotation)
        annotateViewModel.updateAnnotation(annotatedWord) { err -> resultCallback?.invoke(err) }

        Utils.incrementConsultedWord(requireContext(), annotateViewModel.simplified)

        if (annotatedWord.hasAnnotation()) {
            AppPreferencesStore(requireContext()).lastAnnotatedClassType = updatedAnnotation.classType!!
            AppPreferencesStore(requireContext()).lastAnnotatedClassLevel = updatedAnnotation.level!!
        }
    }
}*/
