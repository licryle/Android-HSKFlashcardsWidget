package fr.berliat.hskwidget.ui.screens.wordlist

import androidx.compose.runtime.Composable
import fr.berliat.hskwidget.AnkiDelegator
import fr.berliat.hskwidget.data.repo.WordListRepository
import fr.berliat.hskwidget.ui.components.rememberSuspendViewModel

@Composable
fun rememberSuspendWordListViewModel(ankiCaller: AnkiDelegator): WordListViewModel? {
    return rememberSuspendViewModel {
        WordListViewModel(WordListRepository.getInstance(), ankiCaller)
    }
}