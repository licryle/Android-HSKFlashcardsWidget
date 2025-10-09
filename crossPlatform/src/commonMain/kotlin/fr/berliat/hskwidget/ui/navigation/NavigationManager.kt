package fr.berliat.hskwidget.ui.navigation

import androidx.navigation.NavController
import kotlin.reflect.KClass

object NavigationManager {
    private lateinit var navController: NavController

    const val STACK_SCREEN_DEPTH = 5
    val stackScreen = ArrayDeque<Screen>(listOf(Screen.Dictionary()))

    fun currentScreen() = stackScreen.last()

    fun init(controller: NavController) {
        navController = controller
    }

    fun navigate(screen: Screen) {
        navController.navigate(screen)

        if (stackScreen.size == STACK_SCREEN_DEPTH) {
            stackScreen.removeFirst() // remove oldest
        }
        stackScreen.add(screen)

        logCurrentScreen()
    }

    fun pop() {
        stackScreen.removeLastOrNull()
        if (!navController.popBackStack()) {
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
        val current = navController.currentBackStackEntry?.destination?.route
        println("Current screen: $current")
    }
}