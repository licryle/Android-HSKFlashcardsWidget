package fr.berliat.hskwidget.ui.screens.support

import android.app.Activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode

import fr.berliat.hskwidget.core.ExpectedUtils
import fr.berliat.hskwidget.core.Utils
import fr.berliat.hskwidget.core.HSKAppServices
import fr.berliat.hskwidget.data.store.AppPreferencesStore
import fr.berliat.hskwidget.data.store.SupportDevStore

import hskflashcardswidget.crossplatform.generated.resources.Res
import hskflashcardswidget.crossplatform.generated.resources.support_payment_failed
import hskflashcardswidget.crossplatform.generated.resources.support_payment_success
import hskflashcardswidget.crossplatform.generated.resources.support_review_failed
import hskflashcardswidget.crossplatform.generated.resources.support_reviewed
import hskflashcardswidget.crossplatform.generated.resources.support_total_error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

actual class SupportViewModel(
    val supportDevStore : SupportDevStore = SupportDevStore.getInstance(ExpectedUtils.context),
    val activityProvider: () -> Activity = { ExpectedUtils.activity },
    val reviewManager: ReviewManager = ReviewManagerFactory.create(ExpectedUtils.context),
    val appConfig : AppPreferencesStore = HSKAppServices.appPreferences,
) : ViewModel(),
    SupportDevStore.SupportDevListener {
    val totalSpent: StateFlow<Float> = appConfig.supportTotalSpent.asStateFlow()

    private val _purchaseList =
        MutableStateFlow(supportDevStore.purchases.toMap())
    val purchaseList: StateFlow<Map<SupportDevStore.SupportProduct, Int>> = _purchaseList

    init {
        onTotalSpentChange(appConfig.supportTotalSpent.value)
        supportDevStore.addListener(this)
    }

    fun fetchPurchases() {
        supportDevStore.connect()
    }

    fun supportTier() = supportDevStore.getSupportTier(totalSpent.value)

    fun makePurchase(productId: String) {
        supportDevStore.makePurchase(activityProvider.invoke(), productId)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_CLICK,
            mapOf("product_id" to productId)
        )
    }

    override fun onTotalSpentChange(totalSpent: Float) {
        appConfig.supportTotalSpent.value = totalSpent
    }

    override fun onQueryFailure(result: BillingResult) {
        Utils.toast(Res.string.support_total_error)
    }

    override fun onPurchaseSuccess(purchase: Purchase) {
        Utils.toast(Res.string.support_payment_success)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_SUCCESS,
            mapOf("product_id" to getFirstProductId(purchase))
        )
    }

    override fun onPurchaseHistoryUpdate(purchases: Map<SupportDevStore.SupportProduct, Int>) {
        viewModelScope.launch {
            _purchaseList.emit(purchases)
        }
    }

    override fun onPurchaseAcknowledgedSuccess(purchase: Purchase) { }

    override fun onPurchaseFailure(purchase: Purchase?, billingResponseCode: Int) {
        Utils.toast(Res.string.support_payment_failed)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_FAILED,
            mapOf("product_id" to getFirstProductId(purchase))
        )
    }

    fun triggerReview() {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activityProvider.invoke(), reviewInfo)
                flow.addOnCompleteListener {
                    Utils.toast(Res.string.support_reviewed)
                }
            } else {
                Utils.toast(Res.string.support_review_failed)
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Utils.logAnalyticsError(TAG, "RequestReviewFlow_SetupFailed", reviewErrorCode.toString())
            }
        }
    }

    fun getFirstProductId(purchase: Purchase?): String {
        return purchase?.products?.first() ?: ""
    }

    companion object {
        private const val TAG = "SupportViewModel"
    }
}