@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.NSAnySanitizer
import com.superwall.sdk.kmp.internal.interop.SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_NO_PRESENTATION
import com.superwall.sdk.kmp.internal.interop.SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_PRESENTATION
import com.superwall.sdk.kmp.internal.interop.SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_TIMEOUT
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallback
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusFailure
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomCallbackResultStatusSuccess
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseDebuggerPresented
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseNoAudienceMatch
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseNoConfig
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseNoPaywallViewController
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseNoPresenter
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCasePaywallAlreadyPresented
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCasePlacementNotFound
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallPresentationRequestStatusReasonCaseSubscriptionStatusTimeout
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCaseDeclined
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCasePurchased
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallResultCaseRestored
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReason
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReasonCaseHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReasonCaseNoAudienceMatch
import com.superwall.sdk.kmp.internal.ios.interop.SWBPaywallSkippedReasonCasePlacementNotFound
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResultCaseHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResultCaseNoAudienceMatch
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResultCasePaywall
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResultCasePaywallNotAvailable
import com.superwall.sdk.kmp.internal.ios.interop.SWBPresentationResultCasePlacementNotFound
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCaseCancelled
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCaseFailed
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCasePending
import com.superwall.sdk.kmp.internal.ios.interop.SWBPurchaseResultCasePurchased
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResultCaseFailed
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestorationResultCaseRestored
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestoreType
import com.superwall.sdk.kmp.internal.ios.interop.SWBRestoreTypeCaseViaPurchase
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResult
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResultCaseError
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResultCaseHoldout
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResultCaseNoAudienceMatch
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResultCasePaywall
import com.superwall.sdk.kmp.internal.ios.interop.SWBTriggerResultCasePlacementNotFound
import com.superwall.sdk.kmp.models.callbacks.CustomCallback
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResult
import com.superwall.sdk.kmp.models.callbacks.CustomCallbackResultStatus
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusReason
import com.superwall.sdk.kmp.models.results.PaywallPresentationRequestStatusType
import com.superwall.sdk.kmp.models.results.PaywallResult
import com.superwall.sdk.kmp.models.results.PaywallSkippedReason
import com.superwall.sdk.kmp.models.results.PresentationResult
import com.superwall.sdk.kmp.models.results.PurchaseResult
import com.superwall.sdk.kmp.models.results.RestorationResult
import com.superwall.sdk.kmp.models.results.RestoreType
import com.superwall.sdk.kmp.models.results.TriggerResult
import platform.Foundation.NSNumber

// ---------------------------------------------------------------------------
// Trigger / presentation results (SWB -> common).
// ---------------------------------------------------------------------------

internal fun SWBTriggerResult.toModel(): TriggerResult =
    when (result()) {
        SWBTriggerResultCasePlacementNotFound -> TriggerResult.PlacementNotFound
        SWBTriggerResultCaseNoAudienceMatch -> TriggerResult.NoAudienceMatch
        SWBTriggerResultCasePaywall -> TriggerResult.Paywall(experiment().toModelOrEmpty())
        SWBTriggerResultCaseHoldout -> TriggerResult.Holdout(experiment().toModelOrEmpty(holdout = true))
        SWBTriggerResultCaseError -> TriggerResult.Error(error() ?: "Unknown trigger error")
        else -> TriggerResult.Error("Unknown trigger result case: ${result()}")
    }

internal fun SWBPresentationResult.toModel(): PresentationResult =
    when (result()) {
        SWBPresentationResultCasePlacementNotFound -> PresentationResult.PlacementNotFound
        SWBPresentationResultCaseNoAudienceMatch -> PresentationResult.NoAudienceMatch
        SWBPresentationResultCasePaywall -> PresentationResult.Paywall(experiment().toModelOrEmpty())
        SWBPresentationResultCaseHoldout -> PresentationResult.Holdout(experiment().toModelOrEmpty(holdout = true))
        SWBPresentationResultCasePaywallNotAvailable -> PresentationResult.PaywallNotAvailable
        else -> PresentationResult.PaywallNotAvailable
    }

internal fun SWBPaywallResult.toModel(): PaywallResult =
    when (result()) {
        SWBPaywallResultCasePurchased -> PaywallResult.Purchased(productId() ?: "")
        SWBPaywallResultCaseDeclined -> PaywallResult.Declined
        SWBPaywallResultCaseRestored -> PaywallResult.Restored
        else -> PaywallResult.Declined
    }

internal fun SWBPaywallSkippedReason.toModel(): PaywallSkippedReason =
    when (reason()) {
        SWBPaywallSkippedReasonCaseHoldout ->
            PaywallSkippedReason.Holdout(experiment().toModelOrEmpty(holdout = true))
        SWBPaywallSkippedReasonCaseNoAudienceMatch -> PaywallSkippedReason.NoAudienceMatch
        SWBPaywallSkippedReasonCasePlacementNotFound -> PaywallSkippedReason.PlacementNotFound
        else -> PaywallSkippedReason.NoAudienceMatch
    }

internal fun SWBRestoreType.toModel(): RestoreType =
    when (type()) {
        SWBRestoreTypeCaseViaPurchase -> RestoreType.ViaPurchase(storeTransaction()?.toModel())
        else -> RestoreType.ViaRestore
    }

// ---------------------------------------------------------------------------
// Presentation-request status / reason (event payloads).
// ---------------------------------------------------------------------------

internal fun presentationRequestStatusFromNSNumber(value: NSNumber?): PaywallPresentationRequestStatusType? =
    value?.let {
        when (it.longLongValue) {
            SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_PRESENTATION -> PaywallPresentationRequestStatusType.PRESENTATION
            SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_NO_PRESENTATION -> PaywallPresentationRequestStatusType.NO_PRESENTATION
            SWB_PAYWALL_PRESENTATION_REQUEST_STATUS_TIMEOUT -> PaywallPresentationRequestStatusType.TIMEOUT
            else -> null
        }
    }

internal fun SWBPaywallPresentationRequestStatusReason.toModel(): PaywallPresentationRequestStatusReason? =
    when (reason()) {
        SWBPaywallPresentationRequestStatusReasonCaseDebuggerPresented ->
            PaywallPresentationRequestStatusReason.DebuggerPresented
        SWBPaywallPresentationRequestStatusReasonCasePaywallAlreadyPresented ->
            PaywallPresentationRequestStatusReason.PaywallAlreadyPresented
        SWBPaywallPresentationRequestStatusReasonCaseHoldout ->
            PaywallPresentationRequestStatusReason.Holdout(experiment().toModelOrEmpty(holdout = true))
        SWBPaywallPresentationRequestStatusReasonCaseNoAudienceMatch ->
            PaywallPresentationRequestStatusReason.NoAudienceMatch
        SWBPaywallPresentationRequestStatusReasonCasePlacementNotFound ->
            PaywallPresentationRequestStatusReason.PlacementNotFound
        SWBPaywallPresentationRequestStatusReasonCaseNoPaywallViewController ->
            PaywallPresentationRequestStatusReason.NoPaywallViewController
        SWBPaywallPresentationRequestStatusReasonCaseNoPresenter ->
            PaywallPresentationRequestStatusReason.NoPresenter
        SWBPaywallPresentationRequestStatusReasonCaseNoConfig ->
            PaywallPresentationRequestStatusReason.NoConfig
        SWBPaywallPresentationRequestStatusReasonCaseSubscriptionStatusTimeout ->
            PaywallPresentationRequestStatusReason.SubscriptionStatusTimeout
        else -> null
    }

// ---------------------------------------------------------------------------
// Restoration / purchase results (both directions).
// ---------------------------------------------------------------------------

internal fun SWBRestorationResult.toModel(): RestorationResult =
    when (result()) {
        SWBRestorationResultCaseRestored -> RestorationResult.Restored
        else -> RestorationResult.Failed(errorMessage() ?: "Unknown restoration error")
    }

internal fun RestorationResult.toSWB(): SWBRestorationResult =
    when (this) {
        RestorationResult.Restored -> SWBRestorationResult(SWBRestorationResultCaseRestored, null)
        is RestorationResult.Failed -> SWBRestorationResult(SWBRestorationResultCaseFailed, error)
    }

internal fun PurchaseResult.toSWB(): SWBPurchaseResult =
    when (this) {
        PurchaseResult.Purchased -> SWBPurchaseResult(SWBPurchaseResultCasePurchased, null)
        PurchaseResult.Pending -> SWBPurchaseResult(SWBPurchaseResultCasePending, null)
        PurchaseResult.Cancelled -> SWBPurchaseResult(SWBPurchaseResultCaseCancelled, null)
        is PurchaseResult.Failed -> SWBPurchaseResult(SWBPurchaseResultCaseFailed, error)
    }

// ---------------------------------------------------------------------------
// Custom callbacks (both directions).
// ---------------------------------------------------------------------------

internal fun SWBCustomCallback.toModel(): CustomCallback =
    CustomCallback(
        name = name(),
        variables = NSAnySanitizer.fromMapOrNull(variables()),
    )

internal fun CustomCallbackResult.toSWB(): SWBCustomCallbackResult =
    SWBCustomCallbackResult(
        when (status) {
            CustomCallbackResultStatus.SUCCESS -> SWBCustomCallbackResultStatusSuccess
            CustomCallbackResultStatus.FAILURE -> SWBCustomCallbackResultStatusFailure
        },
        data?.let { NSAnySanitizer.toNSParams(it) },
    )
