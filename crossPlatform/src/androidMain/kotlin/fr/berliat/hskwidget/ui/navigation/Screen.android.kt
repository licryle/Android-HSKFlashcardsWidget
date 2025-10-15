package fr.berliat.hskwidget.ui.navigation

import androidx.compose.runtime.Composable
import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.core.capitalize
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.menu_support
import fr.berliat.hskwidget.support_status_tpl
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun supportTitle(): String {
    val totalSpent = HSKAppServices.appPreferences.supportTotalSpent.value
    val tier = SupportDevStore.getSupportTier(totalSpent)

    var tpl = stringResource(Res.string.menu_support)
    if (totalSpent > 0) {
        tpl = stringResource(Res.string.support_status_tpl)
    }

    return tpl.format(tier.toString().capitalize())
}