package fr.berliat.hskwidget.ui.screens.annotate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import fr.berliat.hskwidget.data.model.AnnotatedChineseWord

import fr.berliat.hskwidget.data.type.ClassLevel
import fr.berliat.hskwidget.data.type.ClassType
import fr.berliat.hskwidget.ui.components.DetailedWordView
import fr.berliat.hskwidget.ui.components.DropdownSelector

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_class_level_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_class_type_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_message
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_delete_confirm_title
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_is_exam_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_notes_hint
import hskflashcardswidget.crossplatform.generated.resources.annotation_edit_themes_hint
import hskflashcardswidget.crossplatform.generated.resources.cancel
import hskflashcardswidget.crossplatform.generated.resources.delete
import hskflashcardswidget.crossplatform.generated.resources.save

import org.jetbrains.compose.resources.stringResource

@Composable
fun AnnotateScreen(
    word: String,
    viewModel: AnnotateViewModel,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onSave: (word: AnnotatedChineseWord, pinyins: String, notes: String, themes: String, isExam: Boolean, cType: ClassType, cLevel: ClassLevel) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedWord by viewModel.annotatedWord.collectAsState()

    var pinyins by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var themes by remember { mutableStateOf("") }
    var isExam by remember { mutableStateOf(false) }
    var selectedClassType by remember { mutableStateOf(ClassType.entries.first()) }
    var selectedClassLevel by remember { mutableStateOf(ClassLevel.entries.first()) }

    var confirmDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(word) {
        viewModel.fetchAnnotatedWord(word) { word ->
            notes = word.annotation?.notes.orEmpty()
            themes = word.annotation?.themes.orEmpty()
            isExam = word.annotation?.isExam ?: false
            selectedClassType = word.annotation?.classType ?: ClassType.entries.first()
            selectedClassLevel = word.annotation?.level ?: ClassLevel.entries.first()
        }
    }

    if (annotatedWord == null) {
        // loading screen
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DetailedWordView(
            word = annotatedWord!!,
            onSpeakClick = { onSpeak(annotatedWord!!.simplified) },
            onCopyClick = { onCopy(annotatedWord!!.simplified) },
            showHSK3Definition = false,
            onFavoriteClick = null,
            onListsClick = null
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.annotation_edit_notes_hint)) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = modifier.fillMaxWidth()) {
            DropdownSelector(
                label = stringResource(Res.string.annotation_edit_class_type_hint),
                options = ClassType.entries,
                selected = selectedClassType,
                onSelected = { selectedClassType = it },
                modifier = modifier.weight(1f).padding(end = 3.dp)
            )

            DropdownSelector(
                label = stringResource(Res.string.annotation_edit_class_level_hint),
                options = ClassLevel.entries,
                selected = selectedClassLevel,
                onSelected = { selectedClassLevel = it },
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
                onClick = { onSave(viewModel.annotatedWord.value!!, pinyins, notes, themes, isExam, selectedClassType, selectedClassLevel) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }

    if (confirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            title = {
                Text(text = stringResource(Res.string.annotation_edit_delete_confirm_title))
            },
            text = {
                Text(text = stringResource(Res.string.annotation_edit_delete_confirm_message))
            },
            confirmButton = {
                TextButton(onClick = { onDelete(annotatedWord!!.simplified) }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick =  { confirmDeleteDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}