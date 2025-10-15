package fr.berliat.hskwidget.ui.navigation

import androidx.navigation.NavController

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlin.reflect.KClass

object NavigationManager {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private lateinit var navControllerProvider: () -> NavController
    val navController
        get() = { navControllerProvider.invoke() }

    private val _navigationEvents = MutableSharedFlow<Screen>(replay = 0)
    val navigationEvents: SharedFlow<Screen> = _navigationEvents.asSharedFlow()
    val currentScreen: StateFlow<Screen> = _navigationEvents.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = Screen.Dictionary())

    const val STACK_SCREEN_DEPTH = 5
    val stackScreen = ArrayDeque<Screen>(listOf(Screen.Dictionary()))

    fun init(navController: NavController) {
        navControllerProvider = { navController }
    }


    fun navigate(screen: Screen) {
        if (stackScreen.size == STACK_SCREEN_DEPTH) {
            stackScreen.removeFirst() // remove oldest
        }
        stackScreen.add(screen)

        scope.launch(Dispatchers.Main) {
            _navigationEvents.emit(screen)
        }

        logCurrentScreen()
    }

    fun pop() {
        stackScreen.removeLastOrNull()
        if (!navController().popBackStack()) {
            // Exit app fallback if stack is empty
            println("Backstack empty → quit app")
        } else {
            logCurrentScreen()
        }
    }

    fun inBackStack(screenClass: KClass<out Screen>): Boolean {
        return stackScreen.any { it::class == screenClass }
    }

    private fun logCurrentScreen() {
        println("Current screen: $currentScreen")
    }
}