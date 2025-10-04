package fr.berliat.hskwidget.ui.application.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore

import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.ui.screens.OCR.CaptureImageScreen
import fr.berliat.hskwidget.ui.screens.OCR.DisplayOCRScreen
import fr.berliat.hskwidget.ui.screens.about.AboutScreen
import fr.berliat.hskwidget.ui.screens.annotate.AnnotateScreen
import fr.berliat.hskwidget.ui.screens.config.ConfigScreen
import fr.berliat.hskwidget.ui.screens.dictionary.DictionarySearchScreen
import fr.berliat.hskwidget.ui.screens.widget.WidgetsListScreen
import fr.berliat.hskwidget.ui.screens.wordlist.WordListScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier,
               prefsStore : AppPreferencesStore = HSKAppServices.appPreferences) {
    val navController = rememberNavController()
    NavigationManager.init(navController)

    NavHost(navController = navController, startDestination = Screen.Dictionary()) {
        composable<Screen.Dictionary> { backStackEntry ->
            LaunchedEffect(backStackEntry) {
                val args = backStackEntry.toRoute<Screen.Dictionary>()
                prefsStore.searchQuery.value = SearchQuery.fromString(args.search)
            }

            DictionarySearchScreen(
                onAnnotate = { word ->
                    navController.navigate(Screen.Annotate)
                }
            )
        }

        composable<Screen.Lists> {
            WordListScreen(
                onClickList = { list ->
                    val sq = SearchQuery()
                    sq.inListName = list.name
                    navController.navigate(Screen.Dictionary(sq.toString()))
                }
            )
        }

        composable<Screen.About> {
            AboutScreen()
        }

        composable<Screen.Widgets> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Widgets>()
            WidgetsListScreen(
                widgetIds = emptyList<Int>().toIntArray(),
                selectedWidgetId = args.widgetId ?: 0,
                onAddNewWidget = {},
                onWidgetPreferenceSaved = {  },
                expectsActivityResult = false
            )
        }

        composable<Screen.Config> {
            ConfigScreen()
        }

        composable<Screen.OCRCapture> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Annotate>()
            CaptureImageScreen(

            )
        }

        composable<Screen.Annotate> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Annotate>()
            AnnotateScreen(
                word = args.simplifiedWord,
                onSave = { s: String, e: Exception? -> navController.popBackStack() },
                onDelete = { s: String, e: Exception? -> navController.popBackStack() },
            )
        }

        composable<Screen.OCRDisplay> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OCRDisplay>()
            val preText = args.preText
            DisplayOCRScreen(
            )
        }
    }
}