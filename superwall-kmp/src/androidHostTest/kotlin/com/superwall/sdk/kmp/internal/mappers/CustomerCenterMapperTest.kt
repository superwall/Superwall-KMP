package com.superwall.sdk.kmp.internal.mappers

import com.superwall.sdk.kmp.models.customercenter.CustomerCenterAction
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration.FeedbackSurvey
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration.OpenMethod
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration.Path
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration.PathType
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterConfiguration.Screen
import com.superwall.sdk.kmp.models.customercenter.CustomerCenterRefundStatus
import com.superwall.sdk.kmp.models.entitlements.ProductStore
import com.superwall.sdk.kmp.models.options.SuperwallOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.superwall.sdk.customercenter.CustomerCenterAction as NativeAction
import com.superwall.sdk.customercenter.CustomerCenterConfiguration as NativeConfiguration
import com.superwall.sdk.customercenter.CustomerCenterPurchase as NativePurchase
import com.superwall.sdk.customercenter.CustomerCenterRefundStatus as NativeRefundStatus
import com.superwall.sdk.models.entitlements.Entitlement as NativeEntitlement
import com.superwall.sdk.models.product.Store as NativeStore

/**
 * Conversion tests for CustomerCenterMapper.kt against the REAL
 * superwall-android Customer Center types.
 */
class CustomerCenterMapperTest {
    @Test
    fun defaultConfiguration_mapsToTheNativeDefault() {
        assertEquals(NativeConfiguration.default, CustomerCenterConfiguration.default.toNative())
    }

    @Test
    fun configuration_everyPathTypeAndSetting_mapsToNative() {
        val common =
            CustomerCenterConfiguration(
                managementScreen =
                    Screen(
                        title = "Manage",
                        subtitle = "Your plan",
                        paths =
                            listOf(
                                Path(PathType.Refund(windowMillis = 86_400_000)),
                                Path(PathType.ChangePlan(listOf("annual"))),
                                Path(PathType.Url("https://example.com/faq?token=x", OpenMethod.EXTERNAL), title = "FAQ"),
                                Path(PathType.Custom("vip"), id = "vip_path"),
                                Path(
                                    PathType.ManageSubscription,
                                    survey = FeedbackSurvey(id = "why", title = "Why?", options = listOf(FeedbackSurvey.Option("a", "A"))),
                                ),
                            ),
                    ),
                noPurchasesScreen = Screen(paths = listOf(Path(PathType.Restore), Path(PathType.ContactSupport))),
                support =
                    CustomerCenterConfiguration.Support(
                        email = "help@example.com",
                        latestAppVersion = "2.0",
                        warnsAboutUpdates = false,
                        checksAppStoreForUpdates = false,
                        webManagementUrl = "https://example.com/manage",
                    ),
                appearance =
                    CustomerCenterConfiguration.Appearance(
                        accent = CustomerCenterConfiguration.Appearance.ColorPair(light = "#112233", dark = "#445566"),
                    ),
                showsAccountDetails = false,
                warnsAboutDuplicateSubscriptions = false,
            )

        val native = common.toNative()

        val paths = native.managementScreen.paths
        assertEquals("Manage", native.managementScreen.title)
        assertEquals("Your plan", native.managementScreen.subtitle)
        assertEquals(NativeConfiguration.PathType.Refund(86_400_000), paths[0].type)
        assertEquals("refund", paths[0].id)
        assertEquals(NativeConfiguration.PathType.ChangePlan(listOf("annual")), paths[1].type)
        assertEquals(
            NativeConfiguration.PathType.Url("https://example.com/faq?token=x", NativeConfiguration.OpenMethod.EXTERNAL),
            paths[2].type,
        )
        // An unset id falls back to the native default (host and path, no query).
        assertEquals("example.com/faq", paths[2].id)
        assertEquals("FAQ", paths[2].title)
        assertEquals(NativeConfiguration.PathType.Custom("vip"), paths[3].type)
        assertEquals("vip_path", paths[3].id)
        assertEquals(
            NativeConfiguration.FeedbackSurvey(id = "why", title = "Why?", options = listOf(NativeConfiguration.FeedbackSurvey.Option("a", "A"))),
            paths[4].survey,
        )
        assertEquals(
            listOf(NativeConfiguration.PathType.Restore, NativeConfiguration.PathType.ContactSupport),
            native.noPurchasesScreen.paths.map { it.type },
        )
        assertEquals(
            NativeConfiguration.Support(
                email = "help@example.com",
                latestAppVersion = "2.0",
                warnsAboutUpdates = false,
                webManagementUrl = "https://example.com/manage",
            ),
            native.support,
        )
        assertEquals(NativeConfiguration.Appearance.ColorPair("#112233", "#445566"), native.appearance.accent)
        assertEquals(false, native.showsAccountDetails)
        assertEquals(false, native.warnsAboutDuplicateSubscriptions)
    }

    @Test
    fun superwallOptions_customerCenter_unsetKeepsNativeDefaultAndSetIsApplied() {
        assertEquals(NativeConfiguration.default, SuperwallOptions().toNative().customerCenter)

        val configuration = CustomerCenterConfiguration.default.copy(showsAccountDetails = false)
        assertEquals(false, SuperwallOptions(customerCenter = configuration).toNative().customerCenter.showsAccountDetails)
    }

    @Test
    fun actions_mapEveryNativeCase() {
        assertEquals(CustomerCenterAction.Restore, NativeAction.Restore.toKmp())
        assertEquals(CustomerCenterAction.ManageSubscription, NativeAction.ManageSubscription.toKmp())
        assertEquals(CustomerCenterAction.Refund, NativeAction.Refund.toKmp())
        assertEquals(CustomerCenterAction.ChangePlan, NativeAction.ChangePlan.toKmp())
        assertEquals(CustomerCenterAction.ContactSupport, NativeAction.ContactSupport.toKmp())
        assertEquals(CustomerCenterAction.Url("https://example.com"), NativeAction.Url("https://example.com").toKmp())
        assertEquals(CustomerCenterAction.Custom("vip"), NativeAction.Custom("vip").toKmp())
    }

    @Test
    fun refundStatuses_mapEveryNativeCase() {
        assertEquals(
            NativeRefundStatus.entries.map { it.name },
            NativeRefundStatus.entries.map { it.toKmp().name },
        )
        assertEquals(CustomerCenterRefundStatus.USER_CANCELLED, NativeRefundStatus.USER_CANCELLED.toKmp())
    }

    @Test
    fun purchase_mapsStoreAndEntitlements() {
        val purchase =
            NativePurchase(
                productId = "annual",
                store = NativeStore.PLAY_STORE,
                entitlements = setOf(NativeEntitlement("pro")),
            ).toKmp()

        assertEquals("annual", purchase.productId)
        assertEquals(ProductStore.PLAY_STORE, purchase.store)
        assertEquals(setOf("pro"), purchase.entitlements.map { it.id }.toSet())
        assertNull(purchase.subscription)
        assertNull(purchase.nonSubscription)
    }
}
