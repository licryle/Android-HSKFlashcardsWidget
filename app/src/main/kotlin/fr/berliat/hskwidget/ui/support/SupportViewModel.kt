package fr.berliat.hskwidget.ui.support

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.model.ReviewErrorCode
import fr.berliat.hskwidget.R
import fr.berliat.hskwidget.data.store.OldAppPreferencesStore
import fr.berliat.hskwidget.data.store.SupportDevStore
import fr.berliat.hskwidget.domain.Utils

class SupportViewModel(application: Application, private val toastMe: (Int) -> Unit)
    : AndroidViewModel(application),
    SupportDevStore.SupportDevListener {
    private val appConfig = OldAppPreferencesStore(application.applicationContext)
    private val supportDevStore = SupportDevStore.getInstance(application.applicationContext)

    private val _totalSpent = MutableLiveData(appConfig.supportTotalSpent)
    val totalSpent: LiveData<Float> = _totalSpent

    private val _totalSpentText = MutableLiveData(totalSpentStr(totalSpent.value!!))
    val totalSpentText: LiveData<String> = _totalSpentText

    private val _supportTier = MutableLiveData(
        application.applicationContext.getString(R.string.support_status_nosupport))
    val supportTier: LiveData<String> = _supportTier

    private val _tierIcon = MutableLiveData(R.drawable.trophy_24px)
    val tierIcon: LiveData<Int> = _tierIcon

    private val _purchaseList = MutableLiveData(supportDevStore.purchases.toMap())
    val purchaseList: LiveData<Map<SupportDevStore.SupportProduct, Int>> = _purchaseList

    init {
        onTotalSpentChange(appConfig.supportTotalSpent) // Not needed but for ease or reading code
        supportDevStore.addListener(this)
    }

    fun fetchPurchases() {
        supportDevStore.connect()
    }

    fun makePurchase(activity: Activity, productId: String) {
        supportDevStore.makePurchase(activity, productId)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_CLICK,
            mapOf("product_id" to productId)
        )
    }

    override fun onTotalSpentChange(totalSpent: Float) {
        _totalSpent.postValue(totalSpent)
        _totalSpentText.postValue(totalSpentStr(totalSpent))
        _supportTier.postValue(supportTierStr(totalSpent))
    }

    override fun onQueryFailure(result: BillingResult) {
        toastMe(R.string.support_total_error)
    }

    private fun totalSpentStr(totalSpent: Float) : String {
        var strResId = R.string.support_total_error
        if (totalSpent >= 0f) {
            strResId = R.string.support_total_support
        }

        return application.applicationContext.getString(strResId).format(totalSpent)
    }

    private fun supportTierStr(totalSpent: Float) : String {
        val tierStrId = supportDevStore.getSupportTierString(supportDevStore.getSupportTier(totalSpent))
        if (totalSpent <= 0f)
            return application.applicationContext.getString(tierStrId)

        val statusTpl = application.applicationContext.getString(R.string.support_status)
        val statusStr = application.applicationContext.getString(tierStrId)

        return statusTpl.format(statusStr)
    }

    override fun onPurchaseSuccess(purchase: Purchase) {
        toastMe(R.string.support_payment_success)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_SUCCESS,
            mapOf("product_id" to getFirstProductId(purchase))
        )
    }

    override fun onPurchaseHistoryUpdate(purchases: Map<SupportDevStore.SupportProduct, Int>) {
        _purchaseList.postValue(purchases)
    }

    override fun onPurchaseAcknowledgedSuccess(purchase: Purchase) { }

    override fun onPurchaseFailure(purchase: Purchase?, billingResponseCode: Int) {
        toastMe(R.string.support_payment_failed)
        Utils.logAnalyticsEvent(
            Utils.ANALYTICS_EVENTS.PURCHASE_FAILED,
            mapOf("product_id" to getFirstProductId(purchase))
        )
    }

    fun triggerReview(activity : Activity, reviewManager : ReviewManager) {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We got the ReviewInfo object
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    toastMe(R.string.support_reviewed)
                }
            } else {
                toastMe(R.string.support_review_failed)
                // There was some problem, log or handle the error code.
                @ReviewErrorCode val reviewErrorCode = (task.exception as ReviewException).errorCode
                Utils.logAnalyticsError(TAG, "RequestReviewFlow_SetupFailed", reviewErrorCode.toString())
            }
        }
    }

    fun getFirstProductId(purchase: Purchase?): String {
        return purchase?.products?.first() ?: ""
    }

    companion object {
        const val TAG = "SupportViewModel"
    }
}