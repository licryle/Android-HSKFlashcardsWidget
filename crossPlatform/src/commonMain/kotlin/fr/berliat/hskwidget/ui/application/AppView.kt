package fr.berliat.hskwidget.ui.application

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

import fr.berliat.hskwidget.ui.application.content.AppBar
import fr.berliat.hskwidget.ui.application.content.OCRReminder
import fr.berliat.hskwidget.ui.application.drawer.AppDrawer
import fr.berliat.hskwidget.ui.components.LoadingView
import fr.berliat.hskwidget.ui.navigation.AppNavHost
import fr.berliat.hskwidget.ui.navigation.DecoratedScreen
import fr.berliat.hskwidget.ui.navigation.NavigationManager
import fr.berliat.hskwidget.ui.navigation.Screen

import org.jetbrains.compose.resources.stringResource

@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.clearFocusOnAnyOutsideTap() = composed {
    val focusManager = LocalFocusManager.current
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            focusManager.clearFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppView(
    navigationManager: NavigationManager,
    viewModel: AppViewModel
) {
    val isReady = viewModel.isReady.collectAsState(false)
    val drawerIsOpen = remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(if (drawerIsOpen.value) DrawerValue.Open else DrawerValue.Closed)

    val showOCRReminder = remember { mutableStateOf(true) }

    if (!isReady.value) {
        LoadingView()
        return
    }

    LaunchedEffect(drawerIsOpen.value) {
        if (drawerIsOpen.value) drawerState.open() else drawerState.close()
    }

    // Drive drawerIsOpen from drawerState (like a user swipe)
    LaunchedEffect(drawerState.currentValue) {
        drawerIsOpen.value = (drawerState.currentValue == DrawerValue.Open)
    }

    val currentScreen = navigationManager.currentScreen()
    MaterialTheme() {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    currentScreen = currentScreen
                ) { selectedScreen ->
                    navigationManager.navigate(selectedScreen)
                    drawerIsOpen.value = false
                }
            },
            modifier = Modifier.clearFocusOnAnyOutsideTap()
        ) {
            Scaffold(
                topBar = {
                    AppBar(
                        title = stringResource(DecoratedScreen.fromScreen(currentScreen).title),
                        onOcrClick = { navigationManager.navigate(Screen.OCRCapture()) },
                        onSearch = { s -> navigationManager.navigate(Screen.Dictionary(s)) },
                        onMenuClick = { drawerIsOpen.value = !drawerIsOpen.value }
                    )
                },
                content = { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        // Show OCR Reminder overlay if COR in recent stack, didn't dismiss etc.
                        if (currentScreen !is Screen.OCRDisplay
                            && showOCRReminder.value
                            && navigationManager.inBackStack(Screen.OCRDisplay::class)) {
                            OCRReminder(
                                onClose = { showOCRReminder.value = false },
                                onClick = { navigationManager.navigate(Screen.OCRDisplay("")) }
                            )
                        }

                        // Screen
                        AppNavHost(viewModel = viewModel)
                    }
                }
            )
        }
    }
}