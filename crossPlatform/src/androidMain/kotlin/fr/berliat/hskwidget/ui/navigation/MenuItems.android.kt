package fr.berliat.hskwidget.ui.navigation

actual val MenuItems: List<DecoratedScreen>
    get() {
        return listOf(
            DecoratedScreen.Dictionary,
            DecoratedScreen.Lists,
            DecoratedScreen.Widgets,
            DecoratedScreen.Config,
            DecoratedScreen.Support,
            DecoratedScreen.About
        )
    }
