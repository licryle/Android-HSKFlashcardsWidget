package fr.berliat.hskwidget.ui.support

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.Purchase
import fr.berliat.hskwidget.R
import org.json.JSONObject

class SupportViewModel(application: Application) : AndroidViewModel(application),
    PurchasesUpdatedListener {
    private val _totalSpent = MutableLiveData(0.00)
    val totalSpent: LiveData<Double> = _totalSpent

    private val _totalSpentText = MutableLiveData(
        application.applicationContext.getString(R.string.support_total_support).format(totalSpent))
    val totalSpentText: LiveData<String> = _totalSpentText

    private val _supportTier = MutableLiveData("No support yet 😢")
    val supportTier: LiveData<String> = _supportTier

    private val _tierIcon = MutableLiveData(R.drawable.trophy_24px)
    val tierIcon: LiveData<Int> = _tierIcon

    private lateinit var billingClient: BillingClient

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(application.applicationContext)
            .setListener { billingResult, purchases ->
                // Handle new purchases if needed (not used here)
            }
            .setListener(this)  // Set the listener here
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchaseHistory()
                } else {
                    Log.e(TAG, "Billing Setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing Service disconnected")
            }
        })
    }

    private fun queryPurchaseHistory() {
        val params = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchaseHistoryAsync(params) { billingResult, historyList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && historyList != null) {
                val totalMicros = historyList.sumOf {
                    JSONObject(it.originalJson).optLong("priceAmountMicros", 0L)
                }

                val totalDollars = totalMicros / 1_000_000.0
                _totalSpent.postValue(Math.round(totalDollars * 100).toDouble() / 100)
                _totalSpentText.postValue("You’ve spent $${"%.2f".format(totalDollars)} so far.")

                var statusStringId = 0
                when {
                    totalDollars >= 6 -> statusStringId = R.string.support_status_tier3
                    totalDollars >= 3 -> statusStringId = R.string.support_status_tier2
                    totalDollars >= 1 -> statusStringId = R.string.support_status_tier1
                }

                val statusTpl = application.applicationContext.getString(R.string.support_status)
                val statusStr = application.applicationContext.getString(statusStringId)
                _supportTier.postValue(statusTpl.format(statusStr))
            } else {
                Log.e(TAG, "Failed to query purchase history: ${billingResult.debugMessage}")
            }
        }
    }

    fun makePurchase(activity: Activity, productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val productDetails = productDetailsList[0]

                val billingParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, billingParams)
            } else {
                Log.e("Billing", "Product query failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user, e.g. unlock feature or update support tier

            // Acknowledge the purchase if not already acknowledged
            if (!purchase.isAcknowledged) {
                val acknowledgeParams =
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                    }
                }
            }
        }
    }

    // This method listens for purchase updates (success, failure, user cancel)
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User canceled the purchase - handle if needed
            }
            else -> {
                // Handle other error codes
            }
        }
    }

    companion object {
        const val TAG = "SupportViewModel"
    }
}