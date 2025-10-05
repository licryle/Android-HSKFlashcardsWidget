package fr.berliat.hskwidget.ui.application.drawer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.berliat.hskwidget.ui.navigation.DecoratedScreen
import fr.berliat.hskwidget.ui.navigation.MenuItems
import fr.berliat.hskwidget.ui.navigation.Screen
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppDrawer(
    menuItems: List<DecoratedScreen> = MenuItems,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    ModalDrawerSheet {
        AppDrawerHeader()

        Spacer(Modifier.height(16.dp))

        menuItems.forEach { menuItem ->
            val title = stringResource(menuItem.title)
            NavigationDrawerItem(
                label = { Text(title) },
                icon = { Icon(
                    painter = painterResource(menuItem.icon),
                    contentDescription = title
                ) },
                selected = menuItem.screen::class == currentScreen::class,
                onClick = { onNavigate(menuItem.screen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
