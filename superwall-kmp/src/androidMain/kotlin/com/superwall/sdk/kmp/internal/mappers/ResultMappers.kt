package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.results.RestoreType
import com.superwall.sdk.kmp.models.results.TriggerResult
import com.superwall.sdk.kmp.models.triggers.ConfirmedAssignment
import com.superwall.sdk.kmp.models.triggers.Experiment
import com.superwall.sdk.kmp.models.triggers.Variant
import com.superwall.sdk.kmp.models.triggers.VariantType
import com.superwall.sdk.delegate.PurchaseResult as NativePurchaseResult
import com.superwall.sdk.delegate.RestorationResult as NativeRestorationResult
import com.superwall.sdk.models.assignment.ConfirmedAssignment as NativeConfirmedAssignment
import com.superwall.sdk.models.triggers.Experiment as NativeExperiment
import com.superwall.sdk.models.triggers.TriggerResult as NativeTriggerResult
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatus as NativePaywallPresentationRequestStatus
import com.superwall.sdk.paywall.presentation.internal.PaywallPresentationRequestStatusReason as NativePaywallPresentationRequestStatusReason
import com.superwall.sdk.paywall.presentation.internal.state.PaywallResult as NativePaywallResult
import com.superwall.sdk.paywall.presentation.internal.state.PaywallSkippedReason as NativePaywallSkippedReason
import com.superwall.sdk.paywall.presentation.result.PresentationResult as NativePresentationResult
import com.superwall.sdk.store.transactions.RestoreType as NativeRestoreType

/**
 * Result-family mappers: purchase/restoration outcomes (both directions, for
 * the purchase-controller adapter and `restorePurchases`), plus native → common
 * mappings for trigger, presentation, paywall and skip results, presentation
 * request status, restore type, and the experiment/variant/assignment models
 * they carry. Ports the Flutter host's json/TriggerResultMapper.kt,
 * json/RestoreType+Json.kt, json/PaywallPresentationRequestStatus+Json.kt and
 * the result branches of PaywallPresentationHandlerHost.kt.
 */

// ---- Experiment / Variant / ConfirmedAssignment --------------------------------

internal fun NativeExperiment.toKmp(): Experiment =
    Experiment(
        id = id,
        groupId = groupId,
        variant = variant.toKmp(),
    )

internal fun NativeExperiment.Variant.toKmp(): Variant =
    Variant(
        id = id,
        type =
            when (type) {
                NativeExperiment.Variant.VariantType.TREATMENT -> VariantType.TREATMENT
                NativeExperiment.Variant.VariantType.HOLDOUT -> VariantType.HOLDOUT
            },
        paywallId = paywallId,
    )

internal fun NativeConfirmedAssignment.toKmp(): ConfirmedAssignment =
    ConfirmedAssignment(
        experimentId = experimentId,
        variant = variant.toKmp(),
    )

// ---- PurchaseResult (both directions) -------------------------------------------

internal fun PurchaseResult.toNative(): NativePurchaseResult =
    when (this) {
        is PurchaseResult.Purchased -> NativePurchaseResult.Purchased()
        is PurchaseResult.Cancelled -> NativePurchaseResult.Cancelled()
        is PurchaseResult.Pending -> NativePurchaseResult.Pending()
        is PurchaseResult.Failed -> NativePurchaseResult.Failed(errorMessage = error)
    }

internal fun NativePurchaseResult.toKmp(): PurchaseResult =
    when (this) {
        is NativePurchaseResult.Purchased -> PurchaseResult.Purchased
        is NativePurchaseResult.Cancelled -> PurchaseResult.Cancelled
        is NativePurchaseResult.Pending -> PurchaseResult.Pending
        is NativePurchaseResult.Failed -> PurchaseResult.Failed(errorMessage)
        // Native PurchaseResult is a non-exhaustive sealed class from another
        // module — degrade, never crash.
        else -> PurchaseResult.Failed("Unknown purchase result: $this")
    }

// ---- RestorationResult (both directions) -----------------------------------------

internal fun RestorationResult.toNative(): NativeRestorationResult =
    when (this) {
        is RestorationResult.Restored -> NativeRestorationResult.Restored()
        is RestorationResult.Failed -> NativeRestorationResult.Failed(Exception(error))
    }

internal fun NativeRestorationResult.toKmp(): RestorationResult =
    when (this) {
        is NativeRestorationResult.Restored -> RestorationResult.Restored
        is NativeRestorationResult.Failed ->
            RestorationResult.Failed(
                error?.localizedMessage ?: error?.message ?: "Unknown error",
            )
        else -> RestorationResult.Failed("Unknown restoration result: $this")
    }

// ---- TriggerResult ------------------------------------------------------------------

internal fun NativeTriggerResult.toKmp(): TriggerResult =
    when (this) {
        is NativeTriggerResult.PlacementNotFound -> TriggerResult.PlacementNotFound
        is NativeTriggerResult.NoAudienceMatch -> TriggerResult.NoAudienceMatch
        is NativeTriggerResult.Paywall -> TriggerResult.Paywall(experiment.toKmp())
        is NativeTriggerResult.Holdout -> TriggerResult.Holdout(experiment.toKmp())
        is NativeTriggerResult.Error ->
            TriggerResult.Error(error.localizedMessage ?: "Unknown error")
    }

// ---- PresentationResult ---------------------------------------------------------------

internal fun NativePresentationResult.toKmp(): PresentationResult =
    when (this) {
        is NativePresentationResult.PlacementNotFound -> PresentationResult.PlacementNotFound
        is NativePresentationResult.NoAudienceMatch -> PresentationResult.NoAudienceMatch
        is NativePresentationResult.Paywall -> PresentationResult.Paywall(experiment.toKmp())
        is NativePresentationResult.Holdout -> PresentationResult.Holdout(experiment.toKmp())
        is NativePresentationResult.PaywallNotAvailable -> PresentationResult.PaywallNotAvailable
        else -> PresentationResult.PaywallNotAvailable
    }

// ---- PaywallResult ---------------------------------------------------------------------

internal fun NativePaywallResult.toKmp(): PaywallResult =
    when (this) {
        is NativePaywallResult.Purchased -> PaywallResult.Purchased(productId)
        is NativePaywallResult.Declined -> PaywallResult.Declined
        is NativePaywallResult.Restored -> PaywallResult.Restored
        else -> PaywallResult.Declined
    }

// ---- PaywallSkippedReason -------------------------------------------------------------------

internal fun NativePaywallSkippedReason.toKmp(): PaywallSkippedReason =
    when (this) {
        is NativePaywallSkippedReason.Holdout ->
            PaywallSkippedReason.Holdout(experiment.toKmp())
        is NativePaywallSkippedReason.NoAudienceMatch -> PaywallSkippedReason.NoAudienceMatch
        is NativePaywallSkippedReason.PlacementNotFound -> PaywallSkippedReason.PlacementNotFound
        // Native still carries a legacy UserIsSubscribed case with no common
        // counterpart (the Pigeon contract has three cases); degrade to
        // NoAudienceMatch — Flutter host parity (PaywallPresentationHandlerHost.kt).
        else -> PaywallSkippedReason.NoAudienceMatch
    }

// ---- RestoreType -----------------------------------------------------------------------------

internal fun NativeRestoreType.toKmp(): RestoreType =
    when (this) {
        is NativeRestoreType.ViaPurchase -> RestoreType.ViaPurchase(transaction?.toKmp())
        is NativeRestoreType.ViaRestore -> RestoreType.ViaRestore
    }

// ---- PaywallPresentationRequestStatus / Reason -----------------------------------------------------

internal fun NativePaywallPresentationRequestStatus.toKmp(): PaywallPresentationRequestStatusType =
    when (this) {
        is NativePaywallPresentationRequestStatus.Presentation ->
            PaywallPresentationRequestStatusType.PRESENTATION
        is NativePaywallPresentationRequestStatus.NoPresentation ->
            PaywallPresentationRequestStatusType.NO_PRESENTATION
        is NativePaywallPresentationRequestStatus.Timeout ->
            PaywallPresentationRequestStatusType.TIMEOUT
        else -> PaywallPresentationRequestStatusType.NO_PRESENTATION
    }

internal fun NativePaywallPresentationRequestStatusReason.toKmp(): PaywallPresentationRequestStatusReason =
    when (this) {
        is NativePaywallPresentationRequestStatusReason.DebuggerPresented ->
            PaywallPresentationRequestStatusReason.DebuggerPresented
        is NativePaywallPresentationRequestStatusReason.PaywallAlreadyPresented ->
            PaywallPresentationRequestStatusReason.PaywallAlreadyPresented
        is NativePaywallPresentationRequestStatusReason.Holdout ->
            PaywallPresentationRequestStatusReason.Holdout(experiment.toKmp())
        is NativePaywallPresentationRequestStatusReason.NoAudienceMatch ->
            PaywallPresentationRequestStatusReason.NoAudienceMatch
        is NativePaywallPresentationRequestStatusReason.PlacementNotFound ->
            PaywallPresentationRequestStatusReason.PlacementNotFound
        is NativePaywallPresentationRequestStatusReason.NoPaywallView ->
            PaywallPresentationRequestStatusReason.NoPaywallViewController
        is NativePaywallPresentationRequestStatusReason.NoPresenter ->
            PaywallPresentationRequestStatusReason.NoPresenter
        is NativePaywallPresentationRequestStatusReason.NoConfig ->
            PaywallPresentationRequestStatusReason.NoConfig
        is NativePaywallPresentationRequestStatusReason.SubscriptionStatusTimeout ->
            PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout
        else -> PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout
    }
