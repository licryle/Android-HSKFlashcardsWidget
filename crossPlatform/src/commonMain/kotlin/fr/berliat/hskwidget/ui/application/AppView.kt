package fr.berliat.hskwidget.ui.application

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.core.SnackbarManager
import fr.berliat.hskwidget.ui.application.content.AppBar
import fr.berliat.hskwidget.ui.application.content.OCRReminder
import fr.berliat.hskwidget.ui.application.drawer.AppDrawer
import fr.berliat.hskwidget.ui.application.snackbar.AppSnackbarHost
import fr.berliat.hskwidget.ui.components.AppLoadingView
import fr.berliat.hskwidget.ui.navigation.AppNavHost
import fr.berliat.hskwidget.ui.navigation.DecoratedScreen
import fr.berliat.hskwidget.ui.navigation.Screen
import fr.berliat.hskwidget.ui.theme.AppTheme

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
    viewModel: AppViewModel
) {
    val isReady = viewModel.isReady.collectAsState(false)
    val drawerIsOpen = remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(if (drawerIsOpen.value) DrawerValue.Open else DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val mutedOCRReminders = remember { mutableStateOf(listOf<Screen>()) }

    LaunchedEffect(drawerIsOpen.value) {
        if (drawerIsOpen.value) drawerState.open() else drawerState.close()
    }

    // Drive drawerIsOpen from drawerState (like a user swipe)
    LaunchedEffect(drawerState.currentValue) {
        drawerIsOpen.value = (drawerState.currentValue == DrawerValue.Open)
    }

    val currentScreen by viewModel.navigationManager.currentScreen.collectAsState()

    AppTheme {
        if (!isReady.value) {
            AppLoadingView()
            return@AppTheme
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    currentScreen = currentScreen,
                ) { selectedScreen ->
                    viewModel.navigationManager.navigate(selectedScreen)
                    drawerIsOpen.value = false
                }
            },
            gesturesEnabled = ! currentScreen.decoratedScreen().disableDrawer,
            modifier = Modifier.clearFocusOnAnyOutsideTap()
        ) {
            Scaffold(
                snackbarHost = {
                    AppSnackbarHost(
                        snackbarHostState = snackbarHostState,
                        snackbarManager = SnackbarManager
                    )
                },
                contentWindowInsets = WindowInsets.ime, // Respect keyboard insets
                topBar = {
                    AppBar(
                        onOcrClick = { viewModel.navigationManager.navigate(Screen.OCRCapture()) },
                        onSearch = { s -> viewModel.navigationManager.navigate(Screen.Dictionary(s)) },
                        onMenuClick = { drawerIsOpen.value = !drawerIsOpen.value }
                    )
                },
                content = { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        val ocrScreens = viewModel.navigationManager.getFromBackStack(Screen.OCRDisplay::class)
                        // Show OCR Reminder overlay if OCR in recent stack, not in muted stack
                        if (currentScreen !is Screen.OCRDisplay
                            && ocrScreens.any { !mutedOCRReminders.value.contains(it) }
                            && viewModel.navigationManager.inBackStack(Screen.OCRDisplay::class)) {
                            OCRReminder(
                                onClose = { mutedOCRReminders.value = ocrScreens }, // Muting all OCR screens in backstack
                                onClick = { viewModel.navigationManager.navigate(ocrScreens.last()) }
                            )
                        }

                        Text(
                            text = DecoratedScreen.fromScreen(currentScreen).title(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 15.dp, top = 15.dp, bottom = 2.dp)
                        )

                        // Screen
                        AppNavHost(viewModel = viewModel)
                    }
                }
            )
        }
    }
}
