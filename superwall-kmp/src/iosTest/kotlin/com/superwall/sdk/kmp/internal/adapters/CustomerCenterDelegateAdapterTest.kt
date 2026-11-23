@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.adapters

import com.superwall.sdk.kmp.CustomerCenterDelegate
import com.superwall.sdk.kmp.internal.RecordingLog
import com.superwall.sdk.kmp.internal.inPlaceScope
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_URL
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_REFUND_STATUS_ERROR
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_REFUND_STATUS_USER_CANCELLED
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterAction
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterPurchase
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.internal.ios.interop.SWBProductStoreAppStore
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterPurchase
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CustomerCenterDelegateAdapterTest {
    private val delegate = RecordingDelegate()
    private val log = RecordingLog()
    private val dismissed = mutableListOf<CustomerCenterDelegateAdapter>()
    private val adapter = CustomerCenterDelegateAdapter(delegate, inPlaceScope(), log.log) { dismissed += it }

    @Test
    fun restoreCheckAnswersWithTheDelegatesDecisionOnceItReturns() {
        val decision = CompletableDeferred<Boolean>()
        delegate.shouldRestore = { decision.await() }
        val answers = mutableListOf<Boolean>()

        adapter.customerCenterShouldRestorePurchasesWithCompletion { answers += it }
        assertTrue(answers.isEmpty())

        decision.complete(false)
        assertEquals(listOf(false), answers)
    }

    @Test
    fun aThrowingRestoreCheckLetsTheRestoreGoAheadAndIsLogged() {
        val failure = IllegalStateException("app bug")
        delegate.shouldRestore = { throw failure }
        val answers = mutableListOf<Boolean>()

        adapter.customerCenterShouldRestorePurchasesWithCompletion { answers += it }

        assertEquals(listOf(true), answers)
        assertEquals(
            "CustomerCenterDelegate.customerCenterShouldRestorePurchases threw an exception",
            log.entries.single().first,
        )
        assertSame(failure, log.entries.single().second)
    }

    @Test
    fun selectedActionIsForwardedWithItsPathAndPurchase() {
        adapter.customerCenterDidSelectAction(
            SWBCustomerCenterAction(SWB_CUSTOMER_CENTER_ACTION_TYPE_URL, "https://example.com/help", null),
            "path_help",
            null,
        )
        adapter.customerCenterDidSelectAction(
            SWBCustomerCenterAction(SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND, null, null),
            "path_refund",
            SWBCustomerCenterPurchase("annual", SWBProductStoreAppStore, emptyList<SWBEntitlement>(), null, null),
        )

        assertEquals(
            listOf<Any?>(
                Triple(CustomerCenterAction.Url("https://example.com/help"), "path_help", null),
                Triple(
                    CustomerCenterAction.Refund,
                    "path_refund",
                    CustomerCenterPurchase("annual", ProductStore.APP_STORE, emptySet()),
                ),
            ),
            delegate.calls,
        )
    }

    @Test
    fun completedSurveyIsForwarded() {
        adapter.customerCenterDidCompleteSurveyWithSurveyId(
            "survey_1",
            "option_2",
            SWBCustomerCenterAction(SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND, null, null),
            "path_refund",
        )

        assertEquals(
            listOf<Any?>(listOf("survey_1", "option_2", CustomerCenterAction.Refund, "path_refund")),
            delegate.calls,
        )
    }

    @Test
    fun refundRequestIsForwardedWithTheMappedStatus() {
        adapter.customerCenterDidCompleteRefundRequestWithProductId("a", SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS)
        adapter.customerCenterDidCompleteRefundRequestWithProductId("b", SWB_CUSTOMER_CENTER_REFUND_STATUS_USER_CANCELLED)
        adapter.customerCenterDidCompleteRefundRequestWithProductId("c", SWB_CUSTOMER_CENTER_REFUND_STATUS_ERROR)
        // A status newer than the wrapper must not read as a successful refund.
        adapter.customerCenterDidCompleteRefundRequestWithProductId("d", 99)

        assertEquals(
            listOf<Any?>(
                "a" to CustomerCenterRefundStatus.SUCCESS,
                "b" to CustomerCenterRefundStatus.USER_CANCELLED,
                "c" to CustomerCenterRefundStatus.ERROR,
                "d" to CustomerCenterRefundStatus.ERROR,
            ),
            delegate.calls,
        )
    }

    @Test
    fun dismissIsForwardedThenTheAdapterIsReleased() {
        // The delegate must hear the dismiss before the bridge lets go.
        delegate.onDismiss = { assertTrue(dismissed.isEmpty()) }

        adapter.customerCenterDidDismiss()

        assertEquals(listOf<Any?>("dismissed"), delegate.calls)
        assertSame(adapter, dismissed.single())
        assertTrue(log.entries.isEmpty())
    }

    @Test
    fun aThrowingDismissIsLoggedAndStillReleasesTheAdapter() {
        val failure = IllegalStateException("app bug")
        delegate.onDismiss = { throw failure }

        adapter.customerCenterDidDismiss()

        assertSame(adapter, dismissed.single())
        assertEquals("CustomerCenterDelegate.customerCenterDidDismiss threw an exception", log.entries.single().first)
        assertSame(failure, log.entries.single().second)
    }

    @Test
    fun aThrowingCallbackIsLoggedByNameAndDoesNotReachTheCaller() {
        val failure = IllegalStateException("app bug")
        delegate.onRefund = { throw failure }

        adapter.customerCenterDidCompleteRefundRequestWithProductId("a", SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS)

        assertEquals(
            "CustomerCenterDelegate.customerCenterDidCompleteRefundRequest threw an exception",
            log.entries.single().first,
        )
        assertSame(failure, log.entries.single().second)
        assertTrue(dismissed.isEmpty())
    }

    private class RecordingDelegate : CustomerCenterDelegate {
        val calls = mutableListOf<Any?>()
        var shouldRestore: suspend () -> Boolean = { true }
        var onDismiss: () -> Unit = {}
        var onRefund: () -> Unit = {}

        override suspend fun customerCenterShouldRestorePurchases(): Boolean = shouldRestore()

        override fun customerCenterDidSelectAction(
            action: CustomerCenterAction,
            pathId: String,
            purchase: CustomerCenterPurchase?,
        ) {
            calls += Triple(action, pathId, purchase)
        }

        override fun customerCenterDidCompleteSurvey(
            surveyId: String,
            optionId: String,
            action: CustomerCenterAction,
            pathId: String,
        ) {
            calls.add(listOf(surveyId, optionId, action, pathId))
        }

        override fun customerCenterDidCompleteRefundRequest(
            productId: String,
            status: CustomerCenterRefundStatus,
        ) {
            onRefund()
            calls += productId to status
        }

        override fun customerCenterDidDismiss() {
            onDismiss()
            calls += "dismissed"
        }
    }
}
