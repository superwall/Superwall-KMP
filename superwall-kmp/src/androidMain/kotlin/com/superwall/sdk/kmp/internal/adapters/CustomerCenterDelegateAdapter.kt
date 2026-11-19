package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.CustomerCenterDelegate
import com.superwall.sdk.kmp.internal.mappers.toKmp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.superwall.sdk.customercenter.CustomerCenterAction as NativeCustomerCenterAction
import com.superwall.sdk.customercenter.CustomerCenterDelegate as NativeCustomerCenterDelegate
import com.superwall.sdk.customercenter.CustomerCenterPurchase as NativeCustomerCenterPurchase
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus as NativeCustomerCenterRefundStatus

/**
 * Maps superwall-android's [NativeCustomerCenterDelegate] onto the common
 * [CustomerCenterDelegate]. One adapter per `presentCustomerCenter` call;
 * the native SDK retains it while the Customer Center is presented.
 *
 * The native SDK already calls every method on the main thread, so the
 * plain callbacks run in place. `customerCenterShouldRestorePurchases` is the
 * only suspending one: it is launched on [scope] (`Main.immediate`) and
 * answers the native `proceed` once. Exceptions from app code are reported
 * through [log] and never reach the native SDK; a throwing restore check lets
 * the restore go ahead, the native default.
 */
internal class CustomerCenterDelegateAdapter(
    private val delegate: CustomerCenterDelegate,
    private val scope: () -> CoroutineScope,
    private val log: (message: String, error: Throwable?) -> Unit,
) : NativeCustomerCenterDelegate {
    private inline fun deliver(
        callbackName: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (throwable: Throwable) {
            log("CustomerCenterDelegate.$callbackName threw an exception", throwable)
        }
    }

    override fun customerCenterShouldRestorePurchases(proceed: (Boolean) -> Unit) {
        scope().launch {
            val shouldRestore =
                try {
                    delegate.customerCenterShouldRestorePurchases()
                } catch (throwable: Throwable) {
                    log("CustomerCenterDelegate.customerCenterShouldRestorePurchases threw an exception", throwable)
                    true
                }
            proceed(shouldRestore)
        }
    }

    override fun customerCenterDidSelectAction(
        action: NativeCustomerCenterAction,
        pathId: String,
        purchase: NativeCustomerCenterPurchase?,
    ) {
        deliver("customerCenterDidSelectAction") {
            delegate.customerCenterDidSelectAction(action.toKmp(), pathId, purchase?.toKmp())
        }
    }

    override fun customerCenterDidCompleteSurvey(
        surveyId: String,
        optionId: String,
        action: NativeCustomerCenterAction,
        pathId: String,
    ) {
        deliver("customerCenterDidCompleteSurvey") {
            delegate.customerCenterDidCompleteSurvey(surveyId, optionId, action.toKmp(), pathId)
        }
    }

    override fun customerCenterDidCompleteRefundRequest(
        productId: String,
        status: NativeCustomerCenterRefundStatus,
    ) {
        deliver("customerCenterDidCompleteRefundRequest") {
            delegate.customerCenterDidCompleteRefundRequest(productId, status.toKmp())
        }
    }

    override fun customerCenterDidDismiss() {
        deliver("customerCenterDidDismiss") { delegate.customerCenterDidDismiss() }
    }
}
