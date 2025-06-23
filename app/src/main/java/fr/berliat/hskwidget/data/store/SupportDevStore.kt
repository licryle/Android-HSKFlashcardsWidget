package fr.berliat.hskwidget.data.store

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import fr.berliat.hskwidget.R

class SupportDevStore private constructor(private val context: Context) : PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient
    private val appConfig = AppPreferencesStore(context)
    private val listeners = mutableListOf<SupportDevListener>()

    var purchases: MutableMap<SupportProduct, Int> = mutableMapOf()
        private set

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)  // Set the listener here
            .enablePendingPurchases()
            .build()
    }

    fun connect() {
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

    enum class SupportTier(val minSpend: Float) {
        ERROR(-1.0f),
        NONE(0.0f),
        BRONZE(1.0f),
        SILVER(3.0f),
        GOLDEN(6.0f),
        PLATINUM(10.0f);
    }

    private val SupportTierStrings = mapOf<SupportTier, Int>(
        SupportTier.ERROR to R.string.support_status_error,
        SupportTier.NONE to R.string.support_status_nosupport,
        SupportTier.BRONZE to R.string.support_status_tier1,
        SupportTier.SILVER to R.string.support_status_tier2,
        SupportTier.GOLDEN to R.string.support_status_tier3,
        SupportTier.PLATINUM to R.string.support_status_tier4
    )

    enum class SupportProduct(val productId: String) {
        TIER1("support_tier1"),
        TIER2("support_tier2"),
        TIER3("support_tier3");

        companion object {
            fun fromProductId(id: String): SupportProduct? =
                entries.find { it.productId == id }
        }

    }

    private val SupportProductPrices = mapOf<SupportProduct, Double>(
        SupportProduct.TIER1 to 1.0, // in USD
        SupportProduct.TIER2 to 3.0,
        SupportProduct.TIER3 to 6.0
    )

    fun isPurchased(product: SupportProduct) : Boolean {
        return purchases.contains(product)
    }

    fun getSupportTier(totalSpent: Float): SupportTier {
        val tiers = SupportTier.entries
        for (i in tiers.size -1 downTo 0) {
            if (totalSpent >= tiers[i].minSpend) return tiers[i]
        }

        return SupportTier.ERROR
    }

    fun getSupportTierString(tier : SupportTier): Int {
        return SupportTierStrings[tier]!!
    }

    fun queryPurchaseHistory() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, historyList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchaseHistory(historyList)

                val totSpent = getTotalSpent()
                if (totSpent >= 0) {
                    appConfig.supportTotalSpent = totSpent
                }

                triggerOnTotalSpentChange(totSpent)
            } else {
                triggerOnQueryFailure(billingResult)
            }
        }
    }

    private fun handlePurchaseHistory(historyList: MutableList<Purchase>?) {
        val history = historyList?.mapNotNull {
            runCatching {
                if (it.purchaseState != Purchase.PurchaseState.PURCHASED) {
                    null
                } else {
                    Pair<SupportProduct, Int>(
                        SupportProduct.fromProductId(it.skus.first().toString())!!,
                        it.quantity
                    )
                }
            }.getOrNull()
        } ?: emptyList()

        val oldPurchases = purchases.toMap()
        purchases.clear()
        history.forEach {
            purchases[it.first] = (purchases[it.first] ?: 0) + it.second
        }

        if (oldPurchases != purchases) {
            triggerOnPurchaseHistoryUpdate()
        }
    }

    private fun getTotalSpent(): Float {
        return purchases.entries.sumOf { (sku, quantity) ->
            val price = SupportProductPrices[sku] ?: 0.0
            quantity * price
        }.toFloat()
    }

    // This method listens for purchase updates (success, failure, user cancel)
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                handlePurchase(purchase)
            }
        } else {
            triggerOnPurchaseFailed(null, billingResult.responseCode)
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
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
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
                triggerOnPurchaseFailed(null, billingResult.responseCode)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user, e.g. unlock feature or update support tier
            triggerOnPurchaseSuccess(purchase)

            runCatching {
                val product = SupportProduct.fromProductId(purchase.skus.first())!!
                purchases[product] = (purchases[product] ?:0) + purchase.quantity
            }
            triggerOnPurchaseHistoryUpdate()
            triggerOnTotalSpentChange(getTotalSpent())

            // Acknowledge the purchase if not already acknowledged
            if (!purchase.isAcknowledged) {
                val acknowledgeParams =
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                        triggerOnPurchaseAcknowledged(purchase)
                    }
                }
            }
        } else {
            triggerOnPurchaseFailed(purchase, BillingClient.BillingResponseCode.ERROR)
        }
    }

    /** Listeners **/
    fun addListener(listener: SupportDevListener) {
        if (listeners.contains(listener)) return

        listeners.add(listener)
    }

    fun removeListener(listener: SupportDevListener) {
        if (listeners.contains(listener)) listeners.remove(listener)
    }

    private fun triggerOnTotalSpentChange(totalSpent: Float) {
        listeners.forEach {
            it.onTotalSpentChange(totalSpent)
        }
    }

    private fun triggerOnPurchaseSuccess(purchase: Purchase) {
        listeners.forEach {
            it.onPurchaseSuccess(purchase)
        }
    }

    private fun triggerOnPurchaseAcknowledged(purchase: Purchase) {
        listeners.forEach {
            it.onPurchaseAcknowledgedSuccess(purchase)
        }
    }

    private fun triggerOnPurchaseFailed(purchase: Purchase?, billingResponseCode: Int) {
        listeners.forEach {
            it.onPurchaseFailure(purchase, billingResponseCode)
        }
    }

    private fun triggerOnPurchaseHistoryUpdate() {
        listeners.forEach {
            it.onPurchaseHistoryUpdate(purchases)
        }
    }

    private fun triggerOnQueryFailure(result: BillingResult) {
        listeners.forEach {
            it.onQueryFailure(result)
        }
    }

    interface SupportDevListener {
        fun onTotalSpentChange(totalSpent: Float)
        fun onQueryFailure(result: BillingResult)
        fun onPurchaseSuccess(purchase: Purchase)
        fun onPurchaseHistoryUpdate(purchases: Map<SupportProduct, Int>)
        fun onPurchaseAcknowledgedSuccess(purchase: Purchase)
        fun onPurchaseFailure(purchase: Purchase?, billingResponseCode: Int)
    }

    companion object {
        const val TAG = "SupportDevStore"

        @SuppressLint("StaticFieldLeak") // always using applicationContext
        @Volatile
        private var instance: SupportDevStore? = null

        fun getInstance(context: Context): SupportDevStore {
            return instance ?: synchronized(this) {
                instance ?: SupportDevStore(context.applicationContext).also { instance = it }
            }
        }
    }
}