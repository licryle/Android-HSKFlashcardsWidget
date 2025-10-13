package fr.berliat.hskwidget.ui.application.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp).padding(top = 15.dp)
    ) {
        AppDrawerHeader()

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 15.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
        )

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
