package fr.berliat.hskwidget.ui.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlin.reflect.KClass

object NavigationManager {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _navigationEvents = MutableSharedFlow<Screen>(replay = 0)
    val navigationEvents: SharedFlow<Screen> = _navigationEvents.asSharedFlow()

    const val STACK_SCREEN_DEPTH = 20
    val stackScreen = ArrayDeque<Screen>(listOf(Screen.Dictionary()))

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dictionary())
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow() // Expose as StateFlow

    fun registerScreenVisit(screen: Screen) {
        _currentScreen.update { screen }

        if (stackScreen.lastOrNull() != screen) {
            if (stackScreen.size == STACK_SCREEN_DEPTH) {
                stackScreen.removeFirst()
            }
            stackScreen.add(screen)
        }
        logCurrentScreen()
    }

    fun navigate(screen: Screen) {
        scope.launch(Dispatchers.Main) {
            _navigationEvents.emit(screen)
        }
    }

    fun inBackStack(screenClass: KClass<out Screen>): Boolean {
        return getFromBackStack(screenClass).isNotEmpty()
    }

    fun getFromBackStack(screenClass: KClass<out Screen>): List<Screen> {
        return stackScreen.filter { it::class == screenClass }
    }

    private fun logCurrentScreen() {
        println("Current screen: $currentScreen") // TODo
    }
}