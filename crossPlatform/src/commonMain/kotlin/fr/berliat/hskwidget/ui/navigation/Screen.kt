package fr.berliat.hskwidget.ui.navigation

import kotlinx.serialization.Serializable

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.bakery_dining_24px
import hskflashcardswidget.crossplatform.generated.resources.format_list_bulleted_add_24px
import hskflashcardswidget.crossplatform.generated.resources.ic_dictionary_24dp
import hskflashcardswidget.crossplatform.generated.resources.menu_dictionary
import hskflashcardswidget.crossplatform.generated.resources.menu_lists
import hskflashcardswidget.crossplatform.generated.resources.baseline_info_24
import hskflashcardswidget.crossplatform.generated.resources.baseline_widgets_24
import hskflashcardswidget.crossplatform.generated.resources.menu_about
import hskflashcardswidget.crossplatform.generated.resources.menu_ocr
import hskflashcardswidget.crossplatform.generated.resources.menu_settings
import hskflashcardswidget.crossplatform.generated.resources.menu_support
import hskflashcardswidget.crossplatform.generated.resources.menu_widgets
import hskflashcardswidget.crossplatform.generated.resources.photo_camera_24px
import hskflashcardswidget.crossplatform.generated.resources.settings_24px

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Serializable
sealed class Screen() {
    @Serializable
    data class Dictionary(val search: String = ""): Screen()

    @Serializable
    data object Lists : Screen()

    @Serializable
    data class Annotate(val simplifiedWord: String): Screen()

    @Serializable
    data class Widgets(val widgetId: Int? = null, val expectsActivityResult: Boolean = false): Screen()

    @Serializable
    data object Config : Screen()

    @Serializable
    data object About : Screen()

    @Serializable
    data object Support : Screen()

    @Serializable
    data class OCRCapture(val preText: String = "") : Screen()

    @Serializable
    data class OCRDisplay(val preText: String = "", val imageFilePath: String? = null) : Screen()
}

sealed class DecoratedScreen(val screen: Screen, val title: StringResource, val icon: DrawableResource) {
    data object Dictionary: DecoratedScreen(
        Screen.Dictionary(),
        Res.string.menu_dictionary,
        Res.drawable.ic_dictionary_24dp)

    data object Annotate: DecoratedScreen(
        Screen.Annotate(""),
        Res.string.menu_dictionary,
        Res.drawable.ic_dictionary_24dp)

    data object Lists : DecoratedScreen(
        Screen.Lists,
        Res.string.menu_lists,
        Res.drawable.format_list_bulleted_add_24px)

    data object Widgets: DecoratedScreen(
        Screen.Widgets(),
        Res.string.menu_widgets,
        Res.drawable.baseline_widgets_24)

    data object Config : DecoratedScreen(
        Screen.Config,
        Res.string.menu_settings,
        Res.drawable.settings_24px)

    data object Support : DecoratedScreen(
        Screen.Support,
        Res.string.menu_support,
        Res.drawable.bakery_dining_24px)

    data object About : DecoratedScreen(
        Screen.About,
        Res.string.menu_about,
        Res.drawable.baseline_info_24)

    data object OCRCapture : DecoratedScreen(
        Screen.OCRCapture(),
        Res.string.menu_ocr,
        Res.drawable.photo_camera_24px)

    data object OCRDisplay : DecoratedScreen(
        Screen.OCRDisplay(),
        Res.string.menu_ocr,
        Res.drawable.photo_camera_24px)

    companion object {
        fun fromScreen(screen: Screen): DecoratedScreen = when(screen) {
            is Screen.About -> About
            is Screen.Support -> Support
            is Screen.Annotate -> Annotate
            is Screen.Config -> Config
            is Screen.Dictionary -> Dictionary
            is Screen.Lists -> Lists
            is Screen.OCRCapture -> OCRCapture
            is Screen.OCRDisplay -> OCRDisplay
            is Screen.Widgets -> Widgets
        }
    }
}