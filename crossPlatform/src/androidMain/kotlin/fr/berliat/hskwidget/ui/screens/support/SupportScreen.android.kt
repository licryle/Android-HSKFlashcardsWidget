package fr.berliat.hskwidget.ui.screens.support

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.data.store.SupportDevStore.SupportTier

import fr.berliat.hskwidget.Res
import fr.berliat.hskwidget.bakery_dining_24px
import fr.berliat.hskwidget.core.findActivity
import fr.berliat.hskwidget.star_shine_24px
import fr.berliat.hskwidget.support_devintro
import fr.berliat.hskwidget.support_purchase_tier1
import fr.berliat.hskwidget.support_purchase_tier2
import fr.berliat.hskwidget.support_purchase_tier3
import fr.berliat.hskwidget.support_review_btn
import fr.berliat.hskwidget.support_status
import fr.berliat.hskwidget.support_status_error
import fr.berliat.hskwidget.support_status_nosupport
import fr.berliat.hskwidget.support_status_tier1
import fr.berliat.hskwidget.support_status_tier2
import fr.berliat.hskwidget.support_status_tier3
import fr.berliat.hskwidget.support_status_tier4
import fr.berliat.hskwidget.support_total_error
import fr.berliat.hskwidget.support_total_support
import fr.berliat.hskwidget.trophy_24px
import fr.berliat.hskwidget.ui.components.IconButton
import fr.berliat.hskwidget.ui.theme.AppColors

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun SupportScreen(
    modifier: Modifier
) {
    _SupportScreen(
        modifier = modifier
    )
}

/*
 * Breaking it down into 2 to add the vieModel that uses platform specific elements that wouldn't
 * make sense to expect/actual.
 */

@Composable
private fun _SupportScreen(
    modifier: Modifier,
    viewModel: SupportViewModel = SupportViewModel()
) {
    val activity = LocalContext.current.findActivity()

    val totalSpent by viewModel.totalSpent.collectAsState(0f)
    val purchaseList by viewModel.purchaseList.collectAsState(emptyMap())

    val SupportTierMap = mapOf(
        SupportTier.ERROR to stringResource(Res.string.support_status_error),
        SupportTier.NONE to stringResource(Res.string.support_status_nosupport),
        SupportTier.BRONZE to stringResource(Res.string.support_status_tier1),
        SupportTier.SILVER to stringResource(Res.string.support_status_tier2),
        SupportTier.GOLDEN to stringResource(Res.string.support_status_tier3),
        SupportTier.PLATINUM to stringResource(Res.string.support_status_tier4)
    )

    val supportTier = viewModel.supportTier()
    val supportTierStr = SupportTierMap[supportTier]!!

    // ensure purchases refresh when entering
    LaunchedEffect(Unit) {
        viewModel.fetchPurchases()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = stringResource(Res.string.support_devintro),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        IconButton(
            text = stringResource(Res.string.support_review_btn),
            onClick = { viewModel.triggerReview(activity) },
            modifier = Modifier.fillMaxWidth(),
            drawable = Res.drawable.star_shine_24px
        )

        Spacer(Modifier.height(26.dp))

        val supportTpl = if (totalSpent >= 0f) {
            Res.string.support_total_support
        } else {
            Res.string.support_total_error
        }

        Text(
            text = stringResource(supportTpl).format(totalSpent.toDouble()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val txt = stringResource(Res.string.support_status).format(supportTierStr)
            Icon(
                painter = painterResource(Res.drawable.trophy_24px),
                contentDescription = txt,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = txt,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tier 1
        if (!purchaseList.containsKey(SupportDevStore.SupportProduct.TIER1)) {
            TieredPurchaseButton(
                onClick = { viewModel.makePurchase(activity, "support_tier1") },
                btnColor = AppColors.Bronze,
                icon = Res.drawable.bakery_dining_24px,
                text = Res.string.support_purchase_tier1,
                modifier = modifier
            )
        }

        // Tier 2
        if (!purchaseList.containsKey(SupportDevStore.SupportProduct.TIER2)) {
            TieredPurchaseButton(
                onClick = { viewModel.makePurchase(activity, "support_tier2") },
                btnColor = AppColors.Silver,
                icon = Res.drawable.bakery_dining_24px,
                text = Res.string.support_purchase_tier2,
                modifier = modifier
            )
        }

        // Tier 3
        if (!purchaseList.containsKey(SupportDevStore.SupportProduct.TIER3)) {
            TieredPurchaseButton(
                onClick = { viewModel.makePurchase(activity, "support_tier3") },
                btnColor = AppColors.Gold,
                icon = Res.drawable.bakery_dining_24px,
                text = Res.string.support_purchase_tier3,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TieredPurchaseButton(
    onClick: () -> Unit,
    btnColor: Color,
    icon: DrawableResource,
    text: StringResource,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(8.dp))
    IconButton(
        text = stringResource(text),
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        drawable = icon,
        btnColor = ButtonDefaults.buttonColors().copy(containerColor = btnColor)
    )
}
