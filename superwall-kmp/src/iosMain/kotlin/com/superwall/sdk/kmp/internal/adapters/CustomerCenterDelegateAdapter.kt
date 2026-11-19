@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.CustomerCenterDelegate
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterAction
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterDelegateProtocol
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterPurchase
import com.superwall.sdk.kmp.internal.mappers.customerCenterRefundStatusFromSWBRaw
import com.superwall.sdk.kmp.internal.mappers.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.darwin.NSObject

/**
 * The Kotlin implementation of the bridge's `@objc(SWBCustomerCenterDelegate)`
 * protocol, forwarding to the app's [CustomerCenterDelegate]. One adapter per
 * `presentCustomerCenter` call.
 *
 * SuperwallKit calls these on the main actor. The plain callbacks are
 * delivered through [scope] (`Main.immediate`, so in place); the restore
 * check launches the app's suspend function and completes once. Exceptions
 * from app code are reported through [log]; a throwing restore check lets the
 * restore go ahead, the native default. [onDismissed] lets the bridge drop its
 * Kotlin-side reference (never rely on ObjC retaining Kotlin objects).
 */
internal class CustomerCenterDelegateAdapter(
    private val delegate: CustomerCenterDelegate,
    private val scope: CoroutineScope,
    private val log: (message: String, error: Throwable?) -> Unit,
    private val onDismissed: (CustomerCenterDelegateAdapter) -> Unit,
) : NSObject(), SWBCustomerCenterDelegateProtocol {
    private fun deliver(
        callbackName: String,
        block: () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (throwable: Throwable) {
                log("CustomerCenterDelegate.$callbackName threw an exception", throwable)
            }
        }
    }

    override fun customerCenterShouldRestorePurchasesWithCompletion(completion: (Boolean) -> Unit) {
        scope.launch {
            val shouldRestore =
                try {
                    delegate.customerCenterShouldRestorePurchases()
                } catch (throwable: Throwable) {
                    log("CustomerCenterDelegate.customerCenterShouldRestorePurchases threw an exception", throwable)
                    true
                }
            completion(shouldRestore)
        }
    }

    override fun customerCenterDidSelectAction(
        action: SWBCustomerCenterAction,
        pathId: String,
        purchase: SWBCustomerCenterPurchase?,
    ) {
        val mappedAction = action.toModel()
        val mappedPurchase = purchase?.toModel()
        deliver("customerCenterDidSelectAction") {
            delegate.customerCenterDidSelectAction(mappedAction, pathId, mappedPurchase)
        }
    }

    override fun customerCenterDidCompleteSurveyWithSurveyId(
        surveyId: String,
        optionId: String,
        action: SWBCustomerCenterAction,
        pathId: String,
    ) {
        val mappedAction = action.toModel()
        deliver("customerCenterDidCompleteSurvey") {
            delegate.customerCenterDidCompleteSurvey(surveyId, optionId, mappedAction, pathId)
        }
    }

    override fun customerCenterDidCompleteRefundRequestWithProductId(
        productId: String,
        statusRawValue: Long,
    ) {
        val status = customerCenterRefundStatusFromSWBRaw(statusRawValue)
        deliver("customerCenterDidCompleteRefundRequest") {
            delegate.customerCenterDidCompleteRefundRequest(productId, status)
        }
    }

    override fun customerCenterDidDismiss() {
        deliver("customerCenterDidDismiss") {
            try {
                delegate.customerCenterDidDismiss()
            } finally {
                onDismissed(this)
            }
        }
    }
}
