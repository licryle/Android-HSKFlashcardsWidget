package fr.berliat.hskwidget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun <T> rememberSuspendViewModel(
    create: suspend () -> T
): T? {
    val viewModel = remember { mutableStateOf<T?>(null) }

    LaunchedEffect(true) {
        viewModel.value = create()
    }

    return viewModel.value
}