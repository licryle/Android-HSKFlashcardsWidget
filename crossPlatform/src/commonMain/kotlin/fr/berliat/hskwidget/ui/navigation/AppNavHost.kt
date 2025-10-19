package fr.berliat.hskwidget.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import fr.berliat.hskwidget.domain.SearchQuery
import fr.berliat.hskwidget.ui.application.AppViewModel
import fr.berliat.hskwidget.ui.screens.OCR.CaptureImageScreen
import fr.berliat.hskwidget.ui.screens.OCR.DisplayOCRScreen
import fr.berliat.hskwidget.ui.screens.about.AboutScreen
import fr.berliat.hskwidget.ui.screens.annotate.AnnotateScreen
import fr.berliat.hskwidget.ui.screens.config.ConfigScreen
import fr.berliat.hskwidget.ui.screens.dictionary.DictionarySearchScreen
import fr.berliat.hskwidget.ui.screens.support.SupportScreen
import fr.berliat.hskwidget.ui.screens.widget.WidgetsListScreen
import fr.berliat.hskwidget.ui.screens.wordlist.WordListScreen

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path

@Composable
fun AppNavHost(viewModel : AppViewModel) {
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        viewModel.navigationManager.navigationEvents.collect { route ->
            navController.navigate(route)
        }
    }

    NavHost(navController = navController, startDestination = Screen.Dictionary()) {
        composable<Screen.Dictionary> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Dictionary>()

            if (args.search != null && args.search != viewModel.appConfig.searchQuery.value.toString()) { // default is from the storage
                viewModel.appConfig.searchQuery.value = SearchQuery.processSearchQuery(args.search)
            }

            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            DictionarySearchScreen(
                onAnnotate = { word ->
                    navController.navigate(Screen.Annotate(word))
                }
            )
        }

        composable<Screen.Annotate> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Annotate>()

            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            AnnotateScreen(
                word = args.simplifiedWord,
                onSaveSuccess = { navController.popBackStack() },
                onDeleteSuccess = { navController.popBackStack() },
            )
        }

        composable<Screen.Lists> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Lists>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            WordListScreen(
                onClickList = { list ->
                    val sq = SearchQuery()
                    sq.inListName = list.name
                    navController.navigate(Screen.Dictionary(sq.toString()))
                }
            )
        }

        composable<Screen.Support> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Support>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            SupportScreen()
        }

        composable<Screen.About> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.About>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            AboutScreen()
        }

        composable<Screen.Widgets> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Widgets>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            WidgetsListScreen(
                selectedWidgetId = args.widgetId,
                onWidgetPreferenceSaved = {
                    if (args.expectsActivityResult && args.widgetId != null) {
                        viewModel.finalizeWidgetConfiguration(args.widgetId)
                    }
                },
                expectsActivityResult = args.expectsActivityResult
            )
        }

        composable<Screen.Config> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Config>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            ConfigScreen()
        }

        composable<Screen.OCRCapture> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OCRCapture>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            CaptureImageScreen(onImageReady = { imageFile: PlatformFile ->
                navController.navigate(Screen.OCRDisplay(
                    preText = args.preText,
                    imageFilePath = imageFile.path
                ))
            })
        }

        composable<Screen.OCRDisplay> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OCRDisplay>()
            LaunchedEffect(args) {
                viewModel.navigationManager.registerScreenVisit(args)
            }

            val imageFile = args.imageFilePath?.let {
                val f = PlatformFile(it)
                if (f.exists()) {
                    f
                } else {
                    null
                }
            }

            DisplayOCRScreen(
                preText = args.preText,
                imageFile = imageFile,
                onFavoriteClick = { word -> navController.navigate(Screen.Annotate(word.simplified)) },
                onClickOCRAdd = { preText -> navController.navigate(Screen.OCRCapture(preText)) }
            )
        }
    }
}