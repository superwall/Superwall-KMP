package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterPurchase
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus
import com.superwall.sdk.customercenter.CustomerCenterAction as NativeCustomerCenterAction
import com.superwall.sdk.customercenter.CustomerCenterConfiguration as NativeCustomerCenterConfiguration
import com.superwall.sdk.customercenter.CustomerCenterPurchase as NativeCustomerCenterPurchase
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus as NativeCustomerCenterRefundStatus

/**
 * Customer Center mappers (superwall-android 2.8.5+, `customercenter/`).
 * The configuration goes common → native; actions, purchases and refund
 * statuses come back native → common through the delegate adapter.
 */

// ---- Configuration (common -> native) ----------------------------------------

internal fun CustomerCenterConfiguration.toNative(): NativeCustomerCenterConfiguration =
    NativeCustomerCenterConfiguration(
        managementScreen = managementScreen.toNative(),
        noPurchasesScreen = noPurchasesScreen.toNative(),
        // checksAppStoreForUpdates is an App Store lookup: iOS only.
        support =
            NativeCustomerCenterConfiguration.Support(
                email = support.email,
                latestAppVersion = support.latestAppVersion,
                warnsAboutUpdates = support.warnsAboutUpdates,
                webManagementUrl = support.webManagementUrl,
            ),
        appearance =
            NativeCustomerCenterConfiguration.Appearance(
                accent =
                    appearance.accent?.let {
                        NativeCustomerCenterConfiguration.Appearance.ColorPair(light = it.light, dark = it.dark)
                    },
            ),
        showsAccountDetails = showsAccountDetails,
        warnsAboutDuplicateSubscriptions = warnsAboutDuplicateSubscriptions,
    )

private fun CustomerCenterConfiguration.Screen.toNative(): NativeCustomerCenterConfiguration.Screen =
    NativeCustomerCenterConfiguration.Screen(
        title = title,
        subtitle = subtitle,
        paths = paths.map { it.toNative() },
    )

private fun CustomerCenterConfiguration.Path.toNative(): NativeCustomerCenterConfiguration.Path {
    val nativeType = type.toNative()
    return NativeCustomerCenterConfiguration.Path(
        type = nativeType,
        title = title,
        survey = survey?.toNative(),
        // An unset id is the native default, as for Path(type = ...).
        id = id ?: nativeType.defaultId,
    )
}

private fun CustomerCenterConfiguration.PathType.toNative(): NativeCustomerCenterConfiguration.PathType =
    when (this) {
        CustomerCenterConfiguration.PathType.Restore -> NativeCustomerCenterConfiguration.PathType.Restore
        CustomerCenterConfiguration.PathType.ManageSubscription ->
            NativeCustomerCenterConfiguration.PathType.ManageSubscription
        is CustomerCenterConfiguration.PathType.Refund ->
            NativeCustomerCenterConfiguration.PathType.Refund(windowMillis = windowMillis)
        is CustomerCenterConfiguration.PathType.ChangePlan ->
            NativeCustomerCenterConfiguration.PathType.ChangePlan(productIds = productIds)
        CustomerCenterConfiguration.PathType.ContactSupport -> NativeCustomerCenterConfiguration.PathType.ContactSupport
        is CustomerCenterConfiguration.PathType.Url ->
            NativeCustomerCenterConfiguration.PathType.Url(
                url = url,
                openMethod =
                    when (openMethod) {
                        CustomerCenterConfiguration.OpenMethod.IN_APP -> NativeCustomerCenterConfiguration.OpenMethod.IN_APP
                        CustomerCenterConfiguration.OpenMethod.EXTERNAL -> NativeCustomerCenterConfiguration.OpenMethod.EXTERNAL
                    },
            )
        is CustomerCenterConfiguration.PathType.Custom ->
            NativeCustomerCenterConfiguration.PathType.Custom(identifier = identifier)
    }

private fun CustomerCenterConfiguration.FeedbackSurvey.toNative(): NativeCustomerCenterConfiguration.FeedbackSurvey =
    NativeCustomerCenterConfiguration.FeedbackSurvey(
        id = id,
        title = title,
        options =
            options.map {
                NativeCustomerCenterConfiguration.FeedbackSurvey.Option(id = it.id, title = it.title)
            },
    )

// ---- Delegate payloads (native -> common) -------------------------------------

internal fun NativeCustomerCenterAction.toKmp(): CustomerCenterAction =
    when (this) {
        NativeCustomerCenterAction.Restore -> CustomerCenterAction.Restore
        NativeCustomerCenterAction.ManageSubscription -> CustomerCenterAction.ManageSubscription
        NativeCustomerCenterAction.Refund -> CustomerCenterAction.Refund
        NativeCustomerCenterAction.ChangePlan -> CustomerCenterAction.ChangePlan
        NativeCustomerCenterAction.ContactSupport -> CustomerCenterAction.ContactSupport
        is NativeCustomerCenterAction.Url -> CustomerCenterAction.Url(url)
        is NativeCustomerCenterAction.Custom -> CustomerCenterAction.Custom(identifier)
    }

internal fun NativeCustomerCenterRefundStatus.toKmp(): CustomerCenterRefundStatus =
    when (this) {
        NativeCustomerCenterRefundStatus.SUCCESS -> CustomerCenterRefundStatus.SUCCESS
        NativeCustomerCenterRefundStatus.USER_CANCELLED -> CustomerCenterRefundStatus.USER_CANCELLED
        NativeCustomerCenterRefundStatus.ERROR -> CustomerCenterRefundStatus.ERROR
    }

internal fun NativeCustomerCenterPurchase.toKmp(): CustomerCenterPurchase =
    CustomerCenterPurchase(
        productId = productId,
        store = store.toKmp(),
        entitlements = entitlements.map { it.toKmp() }.toSet(),
        subscription = subscription?.toKmp(),
        nonSubscription = nonSubscription?.toKmp(),
    )
