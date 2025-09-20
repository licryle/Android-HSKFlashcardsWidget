package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.runtime.Composable
import fr.berliat.hskwidget.AnkiDelegator
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.ui.components.rememberSuspendViewModel

@Composable
fun rememberSuspendWordListViewModel(ankiCaller: AnkiDelegator): WordListViewModel? {
    return rememberSuspendViewModel {
        WordListViewModel(HSKAppServices.wordListRepo, ankiCaller)
    }
}