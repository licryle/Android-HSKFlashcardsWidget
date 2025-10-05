package fr.berliat.hskwidget.ui.screens.annotate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKAppServices

import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.ui.components.ConfirmDialog
import fr.berliat.hskwidget.ui.components.DetailedWordView
import fr.berliat.hskwidget.ui.components.DropdownSelector
import fr.berliat.hskwidget.ui.components.LoadingView

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_class_level_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_class_type_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_message
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_title
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_failure
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_success
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_is_exam_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_notes_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_save_failure
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_save_success
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_themes_hint
import hskflashcardswidget.crossplatform.generated.resources.delete
import hskflashcardswidget.crossplatform.generated.resources.save

import org.jetbrains.compose.resources.stringResource

@Composable
fun AnnotateScreen(
    word: String,
    onSaveSuccess: (String) -> Unit,
    onDeleteSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnnotateViewModel = remember { AnnotateViewModel(
        prefsStore = HSKAppServices.appPreferences,
        database = HSKAppServices.database,
        wordListRepo = HSKAppServices.wordListRepo,
        ankiCaller = HSKAppServices.ankiDelegator
    ) }
) {
    val annotatedWord by viewModel.annotatedWord.collectAsState()

    var pinyins by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var themes by remember { mutableStateOf("") }
    var isExam by remember { mutableStateOf(false) }
    var selectedClassType by remember { mutableStateOf(viewModel.lastAnnotatedClassType.value) }
    var selectedClassLevel by remember { mutableStateOf(viewModel.lastAnnotatedClassLevel.value) }

    val showHSK3Definition by viewModel.showHSK3Definition.collectAsState()

    var confirmDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(word) {
        viewModel.fetchAnnotatedWord(word) { word ->
            notes = word.annotation?.notes.orEmpty()
            themes = word.annotation?.themes.orEmpty()
            isExam = word.annotation?.isExam ?: false
            selectedClassType = if (word.hasAnnotation()) {
                word.annotation!!.classType!!
            } else {
                viewModel.lastAnnotatedClassType.value
            }
            selectedClassLevel = if (word.hasAnnotation()) {
                word.annotation!!.level!!
            } else {
                viewModel.lastAnnotatedClassLevel.value
            }
        }
    }

    fun toastAndAssessSuccess(word: String, action: ACTION, e: Exception?): Boolean {
        val msgRes = when {
            action == ACTION.DELETE && e == null -> Res.string.annotation_edit_delete_success
            action == ACTION.DELETE && e != null -> Res.string.annotation_edit_delete_failure
            action == ACTION.UPDATE && e == null -> Res.string.annotation_edit_save_success
            action == ACTION.UPDATE && e != null -> Res.string.annotation_edit_save_failure
            else -> throw (Exception()) // We'll crash
        }

        if (e == null) {
            Utils.toast(msgRes, listOf(word))
            return true
        } else {
            Utils.toast(msgRes, listOf(word, e.message ?: ""))
            return false
        }
    }

    if (annotatedWord == null) {
        LoadingView()
        return
    }

    if (confirmDeleteDialog) {
        ConfirmDialog(
            title = Res.string.annotation_edit_delete_confirm_title,
            message = stringResource(Res.string.annotation_edit_delete_confirm_message),
            onDismiss = { confirmDeleteDialog = false },
            onConfirm = {
                viewModel.deleteAnnotation(word) { word, e ->
                    if (toastAndAssessSuccess(word, ACTION.DELETE, e)) onDeleteSuccess(word)
                }
                confirmDeleteDialog = false
            },
            confirmButtonLabel = Res.string.delete,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DetailedWordView(
            word = annotatedWord!!,
            onSpeakClick = { viewModel.speakWord(annotatedWord!!.simplified) },
            onCopyClick = { viewModel.copyWord(annotatedWord!!.simplified) },
            showHSK3Definition = false,
            onFavoriteClick = null,
            onListsClick = null,
            onPinyinChange = { pinyins = it }
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.annotation_edit_notes_hint)) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Spacer(Modifier.height(12.dp))



        Row(modifier = modifier.fillMaxWidth()) {
            val labelProvider: (ClassType) -> String = if (showHSK3Definition) {
                { it.type }
            } else {
                { it.name }
            }

            DropdownSelector(
                label = stringResource(Res.string.annotation_edit_class_type_hint),
                options = ClassType.entries,
                selected = selectedClassType,
                onSelected = { selectedClassType = it },
                labelProvider = labelProvider,
                modifier = modifier.weight(1f).padding(end = 3.dp)
            )

            val labelProvider2: (ClassLevel) -> String = if (showHSK3Definition) {
                { it.lvl }
            } else {
                { it.name }
            }

            DropdownSelector(
                label = stringResource(Res.string.annotation_edit_class_level_hint),
                options = ClassLevel.entries,
                selected = selectedClassLevel,
                onSelected = { selectedClassLevel = it },
                labelProvider = labelProvider2,
                modifier = modifier.weight(1f).padding(start = 3.dp)
            )
        }

        OutlinedTextField(
            value = themes,
            onValueChange = { themes = it },
            label = { Text(stringResource(Res.string.annotation_edit_themes_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.annotation_edit_is_exam_hint))
            Switch(checked = isExam, onCheckedChange = { isExam = it })
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (annotatedWord!!.hasAnnotation()) {
                OutlinedButton(onClick = { confirmDeleteDialog = true }) { Text(stringResource(Res.string.delete)) }
            }

            Button(
                onClick = { viewModel.saveWord(
                    annotatedWord = viewModel.annotatedWord.value!!,
                    pinyins,
                    notes, themes,
                    isExam,
                    cType = selectedClassType,
                    cLevel = selectedClassLevel) { word, e ->
                        if (toastAndAssessSuccess(word.simplified, ACTION.UPDATE, e)) onSaveSuccess(word.simplified)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}

private enum class ACTION {
    UPDATE,
    DELETE
}