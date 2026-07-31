package com.superwall.sdk.kmp

import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResultStatus
import com.superwall.sdk.kmp.models.entitlements.Entitlement
import com.superwall.sdk.kmp.models.entitlements.EntitlementType
import com.superwall.sdk.kmp.models.entitlements.SubscriptionStatus
import com.superwall.sdk.kmp.models.identity.IdentityOptions
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import com.superwall.sdk.kmp.models.paywall.PaywallInfo
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.triggers.Experiment
import com.superwall.sdk.kmp.models.triggers.Variant
import com.superwall.sdk.kmp.models.triggers.VariantType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Spot-checks of model defaults and sealed hierarchies: constructor defaults
 * match the documented behavior, and sealed cases carry their payloads with
 * structural equality.
 */
class ModelDefaultsTest {
    private val holdoutVariant = Variant(id = "v1", type = VariantType.HOLDOUT)
    private val treatmentVariant = Variant(id = "v2", type = VariantType.TREATMENT, paywallId = "pw_9")
    private val experiment = Experiment(id = "exp_1", groupId = "grp_1", variant = holdoutVariant)

    // ---- Entitlement defaults ---------------------------------------------------

    @Test
    fun entitlementDefaultsMatchDocumentedBehavior() {
        val entitlement = Entitlement(id = "pro")
        assertEquals("pro", entitlement.id)
        assertEquals(EntitlementType.SERVICE_LEVEL, entitlement.type)
        assertTrue(entitlement.isActive)
        assertEquals(emptyList(), entitlement.productIds)
        assertNull(entitlement.latestProductId)
        assertNull(entitlement.store)
        assertNull(entitlement.startsAt)
        assertNull(entitlement.renewedAt)
        assertNull(entitlement.expiresAt)
        assertNull(entitlement.isLifetime)
        assertNull(entitlement.willRenew)
        assertNull(entitlement.state)
        assertNull(entitlement.offerType)
    }

    // ---- SubscriptionStatus ------------------------------------------------------

    @Test
    fun subscriptionStatusIsActiveOnlyForActive() {
        val active = SubscriptionStatus.Active(setOf(Entitlement(id = "pro")))
        assertTrue(active.isActive)
        assertFalse(SubscriptionStatus.Inactive.isActive)
        assertFalse(SubscriptionStatus.Unknown.isActive)
    }

    @Test
    fun subscriptionStatusActiveCarriesEntitlements() {
        val entitlements = setOf(Entitlement(id = "pro"), Entitlement(id = "plus"))
        val status: SubscriptionStatus = SubscriptionStatus.Active(entitlements)
        assertIs<SubscriptionStatus.Active>(status)
        assertEquals(entitlements, status.entitlements)
    }

    // ---- Experiment / Variant ------------------------------------------------------

    @Test
    fun experimentCarriesVariant() {
        assertEquals(holdoutVariant, experiment.variant)
        assertEquals("exp_1", experiment.id)
        assertEquals("grp_1", experiment.groupId)
    }

    @Test
    fun variantPaywallIdDefaultsToNull() {
        assertNull(holdoutVariant.paywallId)
        assertEquals("pw_9", treatmentVariant.paywallId)
    }

    @Test
    fun variantsWithSamePayloadAreStructurallyEqual() {
        assertEquals(
            Variant(id = "v", type = VariantType.TREATMENT, paywallId = "p"),
            Variant(id = "v", type = VariantType.TREATMENT, paywallId = "p"),
        )
    }

    // ---- PaywallSkippedReason ----------------------------------------------------------

    @Test
    fun paywallSkippedReasonHoldoutCarriesExperiment() {
        val reason: PaywallSkippedReason = PaywallSkippedReason.Holdout(experiment)
        assertIs<PaywallSkippedReason.Holdout>(reason)
        assertEquals(experiment, reason.experiment)
        // ...down to the variant it carries.
        assertEquals(holdoutVariant, reason.experiment.variant)
    }

    @Test
    fun paywallSkippedReasonObjectCasesAreSingletons() {
        val noMatch: PaywallSkippedReason = PaywallSkippedReason.NoAudienceMatch
        val notFound: PaywallSkippedReason = PaywallSkippedReason.PlacementNotFound
        assertIs<PaywallSkippedReason.NoAudienceMatch>(noMatch)
        assertIs<PaywallSkippedReason.PlacementNotFound>(notFound)
    }

    // ---- PurchaseResult ----------------------------------------------------------------

    @Test
    fun purchaseResultFactoriesReturnMatchingCases() {
        assertIs<PurchaseResult.Purchased>(PurchaseResult.purchased())
        assertIs<PurchaseResult.Cancelled>(PurchaseResult.cancelled())
        assertIs<PurchaseResult.Pending>(PurchaseResult.pending())
        val failed = PurchaseResult.failed("card declined")
        assertIs<PurchaseResult.Failed>(failed)
        assertEquals("card declined", failed.error)
    }

    @Test
    fun purchaseResultFailedIsStructurallyEqual() {
        assertEquals(PurchaseResult.Failed("x"), PurchaseResult.failed("x"))
    }

    // ---- RestorationResult ------------------------------------------------------------------

    @Test
    fun restorationResultFactoriesReturnMatchingCases() {
        assertIs<RestorationResult.Restored>(RestorationResult.restored())
        val failed = RestorationResult.failed("no receipt")
        assertIs<RestorationResult.Failed>(failed)
        assertEquals("no receipt", failed.error)
    }

    // ---- CustomCallbackResult ------------------------------------------------------------------

    @Test
    fun customCallbackResultFactoriesSetStatusAndData() {
        val success = CustomCallbackResult.success(mapOf("validated" to true))
        assertEquals(CustomCallbackResultStatus.SUCCESS, success.status)
        assertEquals(mapOf<String, Any?>("validated" to true), success.data)

        val failure = CustomCallbackResult.failure()
        assertEquals(CustomCallbackResultStatus.FAILURE, failure.status)
        assertNull(failure.data)
    }

    // ---- PresentationResult / TriggerResult sealed payloads --------------------------------------

    @Test
    fun presentationResultCasesCarryExperiment() {
        val paywall: PresentationResult = PresentationResult.Paywall(experiment)
        val holdout: PresentationResult = PresentationResult.Holdout(experiment)
        assertIs<PresentationResult.Paywall>(paywall)
        assertEquals(experiment, paywall.experiment)
        assertIs<PresentationResult.Holdout>(holdout)
        assertEquals(experiment, holdout.experiment)
    }

    @Test
    fun triggerResultCasesCarryPayloads() {
        val paywall: TriggerResult = TriggerResult.Paywall(experiment)
        assertIs<TriggerResult.Paywall>(paywall)
        assertEquals(experiment, paywall.experiment)

        val error: TriggerResult = TriggerResult.Error("boom")
        assertIs<TriggerResult.Error>(error)
        assertEquals("boom", error.error)
    }

    @Test
    fun paywallResultPurchasedCarriesProductId() {
        val result: PaywallResult = PaywallResult.Purchased("pro_annual")
        assertIs<PaywallResult.Purchased>(result)
        assertEquals("pro_annual", result.productId)
    }

    // ---- Options defaults ----------------------------------------------------------------------------

    @Test
    fun identityOptionsDefaultsToNoAssignmentRestore() {
        assertFalse(IdentityOptions().restorePaywallAssignments)
    }

    @Test
    fun superwallOptionsExperimentalDeviceVariablesDefaultOff() {
        assertFalse(SuperwallOptions().enableExperimentalDeviceVariables)
    }

    // ---- PaywallInfo -----------------------------------------------------------------------------------

    @Test
    fun paywallInfoDefaultsToAllNullFields() {
        val info = PaywallInfo()
        assertNull(info.identifier)
        assertNull(info.name)
        assertNull(info.experiment)
        assertNull(info.productIds)
        assertNull(info.url)
        assertNull(info.featureGatingBehavior)
        assertNull(info.closeReason)
        assertNull(info.state)
    }
}
