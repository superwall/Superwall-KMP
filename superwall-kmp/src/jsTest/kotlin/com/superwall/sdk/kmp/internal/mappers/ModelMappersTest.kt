package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionOfferType
import com.superwall.sdk.kmp.models.entitlements.LatestSubscriptionState
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.paywall.FeatureGatingBehavior
import com.superwall.sdk.kmp.models.paywall.PaywallCloseReason
import com.superwall.sdk.kmp.models.redemption.Ownership
import com.superwall.sdk.kmp.models.redemption.RedemptionResult
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.triggers.VariantType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class ModelMappersTest {
    private val pro: dynamic =
        js(
            "({ id: 'pro', type: 'SERVICE_LEVEL', isActive: true, productIds: ['pro_monthly'], latestProductId: 'pro_monthly', store: 'stripe', startsAt: 1700000000000, expiresAt: 1702592000000, willRenew: true, state: 'subscribed', offerType: 'trial' })",
        )

    @Test
    fun entitlementMapsEveryField() {
        val entitlement = entitlementFromJs(pro)
        assertEquals("pro", entitlement.id)
        assertEquals(listOf("pro_monthly"), entitlement.productIds)
        assertEquals(ProductStore.STRIPE, entitlement.store)
        assertEquals(Instant.fromEpochMilliseconds(1700000000000), entitlement.startsAt)
        assertEquals(Instant.fromEpochMilliseconds(1702592000000), entitlement.expiresAt)
        assertEquals(LatestSubscriptionState.SUBSCRIBED, entitlement.state)
        assertEquals(LatestSubscriptionOfferType.TRIAL, entitlement.offerType)
        assertNull(entitlement.renewedAt)
    }

    @Test
    fun entitlementRoundTripsThroughJs() {
        val entitlement = entitlementFromJs(pro)
        assertEquals(entitlement, entitlementFromJs(entitlement.toJs()))
    }

    @Test
    fun unknownStoreDegradesToOther() {
        assertEquals(ProductStore.OTHER, productStoreFromJs("somethingNew"))
        assertNull(productStoreFromJs(null))
    }

    @Test
    fun subscriptionStatusMapsAllCasesBothWays() {
        val active = subscriptionStatusFromJs(js("({ status: 'ACTIVE', entitlements: [{ id: 'pro', type: 'SERVICE_LEVEL', isActive: true, productIds: [] }] })"))
        assertEquals(setOf("pro"), assertIs<SubscriptionStatus.Active>(active).entitlements.map { it.id }.toSet())
        assertEquals(SubscriptionStatus.Inactive, subscriptionStatusFromJs(js("({ status: 'INACTIVE' })")))
        assertEquals(SubscriptionStatus.Unknown, subscriptionStatusFromJs(js("({ status: 'UNKNOWN' })")))
        assertEquals(SubscriptionStatus.Unknown, subscriptionStatusFromJs(null))

        for (status in listOf(SubscriptionStatus.Active(setOf(Entitlement(id = "pro"))), SubscriptionStatus.Inactive, SubscriptionStatus.Unknown)) {
            assertEquals(status, subscriptionStatusFromJs(status.toJs()))
        }
    }

    @Test
    fun entitlementsSnapshotMirrorsAllIntoWeb() {
        val snapshot = entitlementsSnapshot(arrayOf(pro), emptyArray(), arrayOf(pro))
        assertEquals(snapshot.all, snapshot.web)
        assertEquals(1, snapshot.active.size)
    }

    @Test
    fun customerInfoMapsTransactions() {
        val info =
            customerInfoFromJs(
                js(
                    "({ userId: 'u1', subscriptions: [{ transactionId: 't1', productId: 'pro_monthly', purchaseDate: 1700000000000, willRenew: true, isRevoked: false, isInGracePeriod: false, isInBillingRetryPeriod: false, isActive: true, store: 'stripe' }], nonSubscriptions: [{ transactionId: 't2', productId: 'lifetime', purchaseDate: 1700000000000, isConsumable: false, isRevoked: false }], entitlements: [] })",
                ),
            )
        assertEquals("u1", info.userId)
        assertEquals("t1", info.subscriptions.single().transactionId)
        assertEquals(ProductStore.STRIPE, info.subscriptions.single().store)
        assertEquals("lifetime", info.nonSubscriptions.single().productId)
    }

    @Test
    fun paywallInfoParsesIsoTimestampsAndEnums() {
        val info =
            paywallInfoFromJs(
                js(
                    "({ identifier: 'pw', name: 'Main', url: 'https://x', productIds: ['a'], products: [{ id: 'a', entitlements: [], store: 'stripe' }], presentedByPlacementAt: '2026-01-02T03:04:05.000Z', responseLoadDuration: 12.5, featureGatingBehavior: 'nonGated', closeReason: 'manualClose', experiment: { id: 'e', groupId: 'g', variant: { id: 'v', type: 'treatment', paywallId: 'pw' } }, state: { step: 2 }, webViewLoadStartTime: 'not a date' })",
                ),
            )
        assertEquals("pw", info.identifier)
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), info.presentedByPlacementAt)
        assertNull(info.webViewLoadStartTime)
        assertEquals(12.5, info.responseLoadDuration)
        assertEquals(FeatureGatingBehavior.NON_GATED, info.featureGatingBehavior)
        assertEquals(PaywallCloseReason.MANUAL_CLOSE, info.closeReason)
        assertEquals(VariantType.TREATMENT, info.experiment?.variant?.type)
        assertEquals("a", info.products?.single()?.id)
        assertEquals(mapOf("step" to 2L), info.state)
    }

    @Test
    fun presentationResultMapsEveryCase() {
        assertIs<PresentationResult.Paywall>(presentationResultFromJs(js("({ type: 'paywall', experiment: { id: 'e', groupId: 'g', variant: { id: 'v', type: 'treatment' } } })")))
        assertIs<PresentationResult.Holdout>(presentationResultFromJs(js("({ type: 'holdout', experiment: { id: 'e', groupId: 'g', variant: { id: 'v', type: 'holdout' } } })")))
        assertEquals(PresentationResult.NoAudienceMatch, presentationResultFromJs(js("({ type: 'noAudienceMatch' })")))
        assertEquals(PresentationResult.PlacementNotFound, presentationResultFromJs(js("({ type: 'placementNotFound' })")))
        assertEquals(PresentationResult.PaywallNotAvailable, presentationResultFromJs(js("({ type: 'paywallNotAvailable' })")))
    }

    @Test
    fun paywallResultAndSkipReason() {
        assertEquals(PaywallResult.Purchased("pro_monthly"), paywallResultFromJs(js("({ type: 'purchased', productId: 'pro_monthly' })")))
        assertEquals(PaywallResult.Declined, paywallResultFromJs(js("({ type: 'declined' })")))
        assertEquals(PaywallResult.Restored, paywallResultFromJs(js("({ type: 'restored' })")))
        assertNull(paywallResultFromJs(js("({ type: 'somethingNew' })")))

        assertEquals(PaywallSkippedReason.NoAudienceMatch, paywallSkippedReasonFromJs(js("({ type: 'noAudienceMatch' })")))
        assertEquals(PaywallSkippedReason.PlacementNotFound, paywallSkippedReasonFromJs(js("({ type: 'placementNotFound' })")))
        // Native SDKs have no "already subscribed" skip; web's is not surfaced.
        assertNull(paywallSkippedReasonFromJs(js("({ type: 'userSubscribed' })")))
    }

    @Test
    fun redemptionResultSynthesizesOwnershipFromTheCurrentUser() {
        val success =
            redemptionResultFromJs(
                js("({ type: 'success', code: 'redemption_abc', entitlements: [{ id: 'pro', type: 'SERVICE_LEVEL', isActive: true, productIds: [] }] })"),
                userId = "user_42",
            )
        val info = assertIs<RedemptionResult.Success>(success).redemptionInfo
        assertEquals(Ownership.AppUser("user_42"), info.ownership)
        assertEquals(setOf("pro"), info.entitlements.map { it.id }.toSet())

        assertEquals(RedemptionResult.InvalidCode("c"), redemptionResultFromJs(js("({ type: 'invalid', code: 'c' })"), "u"))
        assertIs<RedemptionResult.ExpiredCode>(redemptionResultFromJs(js("({ type: 'expired', code: 'c' })"), "u"))
        val error = assertIs<RedemptionResult.Error>(redemptionResultFromJs(js("({ type: 'error', code: 'c', error: 'nope' })"), "u"))
        assertEquals("nope", error.error.message)
    }

    @Test
    fun restorationResultCarriesTheErrorMessage() {
        assertEquals(RestorationResult.Restored, restorationResultFromJs(js("({ type: 'restored' })")))
        assertEquals(RestorationResult.Failed("no purchases"), restorationResultFromJs(js("({ type: 'failed', error: new Error('no purchases') })")))
    }
}
