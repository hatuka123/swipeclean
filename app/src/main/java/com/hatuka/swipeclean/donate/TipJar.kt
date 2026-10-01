package com.hatuka.swipeclean.donate

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A donation amount; [price] is Google Play's localized price. */
data class Tip(val productId: String, val price: String)

sealed interface TipJarState {
    data object Loading : TipJarState
    data class Ready(val tips: List<Tip>) : TipJarState

    /** Google Play billing is not available: no Play Store, or the app is not installed from Play yet. */
    data object Unavailable : TipJarState
}

/**
 * Donations through Google Play billing (Play requires it for tips to the developer). The tips
 * are consumable in-app products, so the same amount can be given again. Billing talks to the
 * Play Store app on the device; the app itself still has no internet permission.
 */
@Singleton
class TipJar @Inject constructor(@ApplicationContext context: Context) : PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _state = MutableStateFlow<TipJarState>(TipJarState.Loading)
    val state: StateFlow<TipJarState> = _state.asStateFlow()

    private val _thanks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits after a donation went through. */
    val thanks: SharedFlow<Unit> = _thanks.asSharedFlow()

    private var details: Map<String, ProductDetails> = emptyMap()

    fun connect() {
        if (client.isReady) {
            queryTips()
            return
        }
        _state.value = TipJarState.Loading
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryTips()
                    finishLeftovers()
                } else {
                    _state.value = TipJarState.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryTips() {
        val products = TIP_IDS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, found ->
            val list = found.productDetailsList
            if (result.responseCode != BillingClient.BillingResponseCode.OK || list.isEmpty()) {
                _state.value = TipJarState.Unavailable
            } else {
                details = list.associateBy { it.productId }
                _state.value = TipJarState.Ready(
                    TIP_IDS.mapNotNull { id -> details[id]?.oneTimePurchaseOfferDetails?.let { Tip(id, it.formattedPrice) } },
                )
            }
        }
    }

    /** Opens Google Play's payment sheet; false if the amount is not available. */
    fun donate(activity: Activity, productId: String): Boolean {
        val product = details[productId] ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.forEach(::consume)
    }

    /** A donation that was still pending (or paid while the app was closed) is completed now. */
    private fun finishLeftovers() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach(::consume)
        }
    }

    private fun consume(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _thanks.tryEmit(Unit)
        }
    }

    companion object {
        /** In-app products to create in Play Console (consumable), smallest first. */
        val TIP_IDS = listOf("tip_small", "tip_medium", "tip_large")
    }
}
