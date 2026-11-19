@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_CHANGE_PLAN
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_CONTACT_SUPPORT
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_CUSTOM
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_MANAGE_SUBSCRIPTION
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_RESTORE
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_ACTION_TYPE_URL
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_CHANGE_PLAN
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_CONTACT_SUPPORT
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_CUSTOM
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_MANAGE_SUBSCRIPTION
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_REFUND
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_RESTORE
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_PATH_TYPE_URL
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS
import com.superwall.sdk.kmp.internal.interop.SWB_CUSTOMER_CENTER_REFUND_STATUS_USER_CANCELLED
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterAction
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterConfiguration
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterPath
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterPurchase
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterScreen
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterSurvey
import com.superwall.sdk.kmp.internal.ios.interop.SWBCustomerCenterSurveyOption
import com.superwall.sdk.kmp.internal.ios.interop.SWBEntitlement
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterPurchase
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus
import platform.Foundation.NSNumber

// ---------------------------------------------------------------------------
// Customer Center (CustomerCenter.swift). The configuration envelopes are
// mutable, like SWBSuperwallOptions: create, then set every field. Path types
// travel as SWBCustomerCenterPathType raw values with their type-specific
// fields alongside.
// ---------------------------------------------------------------------------

internal fun CustomerCenterConfiguration.toSWB(): SWBCustomerCenterConfiguration {
    val swb = SWBCustomerCenterConfiguration()
    swb.setManagementScreen(managementScreen.toSWB())
    swb.setNoPurchasesScreen(noPurchasesScreen.toSWB())
    swb.setSupportEmail(support.email)
    swb.setLatestAppVersion(support.latestAppVersion)
    swb.setWarnsAboutUpdates(NSNumber(bool = support.warnsAboutUpdates))
    swb.setChecksAppStoreForUpdates(NSNumber(bool = support.checksAppStoreForUpdates))
    swb.setWebManagementUrl(support.webManagementUrl)
    swb.setAccentLight(appearance.accent?.light)
    swb.setAccentDark(appearance.accent?.dark)
    swb.setShowsAccountDetails(NSNumber(bool = showsAccountDetails))
    swb.setWarnsAboutDuplicateSubscriptions(NSNumber(bool = warnsAboutDuplicateSubscriptions))
    return swb
}

private fun CustomerCenterConfiguration.Screen.toSWB(): SWBCustomerCenterScreen {
    val swb = SWBCustomerCenterScreen()
    swb.setTitle(title)
    swb.setSubtitle(subtitle)
    swb.setPaths(paths.map { it.toSWB() })
    return swb
}

private fun CustomerCenterConfiguration.Path.toSWB(): SWBCustomerCenterPath {
    val swb = SWBCustomerCenterPath()
    swb.setId(id)
    swb.setTitle(title)
    swb.setSurvey(survey?.toSWB())
    when (val pathType = type) {
        CustomerCenterConfiguration.PathType.Restore -> swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_RESTORE)
        CustomerCenterConfiguration.PathType.ManageSubscription ->
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_MANAGE_SUBSCRIPTION)
        is CustomerCenterConfiguration.PathType.Refund -> {
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_REFUND)
            swb.setRefundWindowMillis(pathType.windowMillis?.let { NSNumber(longLong = it) })
        }
        is CustomerCenterConfiguration.PathType.ChangePlan -> {
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_CHANGE_PLAN)
            swb.setChangePlanProductIds(pathType.productIds)
        }
        CustomerCenterConfiguration.PathType.ContactSupport ->
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_CONTACT_SUPPORT)
        is CustomerCenterConfiguration.PathType.Url -> {
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_URL)
            swb.setUrl(pathType.url)
            swb.setOpensExternally(pathType.openMethod == CustomerCenterConfiguration.OpenMethod.EXTERNAL)
        }
        is CustomerCenterConfiguration.PathType.Custom -> {
            swb.setTypeRawValue(SWB_CUSTOMER_CENTER_PATH_TYPE_CUSTOM)
            swb.setCustomIdentifier(pathType.identifier)
        }
    }
    return swb
}

private fun CustomerCenterConfiguration.FeedbackSurvey.toSWB(): SWBCustomerCenterSurvey {
    val swb = SWBCustomerCenterSurvey()
    swb.setId(id)
    swb.setTitle(title)
    swb.setOptions(
        options.map { option ->
            SWBCustomerCenterSurveyOption().also {
                it.setId(option.id)
                it.setTitle(option.title)
            }
        },
    )
    return swb
}

/** An unknown raw type (a native case newer than the bridge) reads as a custom action. */
internal fun SWBCustomerCenterAction.toModel(): CustomerCenterAction =
    when (typeRawValue()) {
        SWB_CUSTOMER_CENTER_ACTION_TYPE_RESTORE -> CustomerCenterAction.Restore
        SWB_CUSTOMER_CENTER_ACTION_TYPE_MANAGE_SUBSCRIPTION -> CustomerCenterAction.ManageSubscription
        SWB_CUSTOMER_CENTER_ACTION_TYPE_REFUND -> CustomerCenterAction.Refund
        SWB_CUSTOMER_CENTER_ACTION_TYPE_CHANGE_PLAN -> CustomerCenterAction.ChangePlan
        SWB_CUSTOMER_CENTER_ACTION_TYPE_CONTACT_SUPPORT -> CustomerCenterAction.ContactSupport
        SWB_CUSTOMER_CENTER_ACTION_TYPE_URL -> CustomerCenterAction.Url(url() ?: "")
        SWB_CUSTOMER_CENTER_ACTION_TYPE_CUSTOM -> CustomerCenterAction.Custom(customIdentifier() ?: "")
        else -> CustomerCenterAction.Custom(customIdentifier() ?: "")
    }

internal fun SWBCustomerCenterPurchase.toModel(): CustomerCenterPurchase =
    CustomerCenterPurchase(
        productId = productId(),
        store = productStoreFromSWB(store()),
        entitlements = entitlements().mapNotNull { (it as? SWBEntitlement)?.toModel() }.toSet(),
        subscription = subscription()?.toModel(),
        nonSubscription = nonSubscription()?.toModel(),
    )

internal fun customerCenterRefundStatusFromSWBRaw(raw: Long): CustomerCenterRefundStatus =
    when (raw) {
        SWB_CUSTOMER_CENTER_REFUND_STATUS_SUCCESS -> CustomerCenterRefundStatus.SUCCESS
        SWB_CUSTOMER_CENTER_REFUND_STATUS_USER_CANCELLED -> CustomerCenterRefundStatus.USER_CANCELLED
        else -> CustomerCenterRefundStatus.ERROR
    }
