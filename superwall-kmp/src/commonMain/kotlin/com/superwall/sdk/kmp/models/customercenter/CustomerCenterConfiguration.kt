package com.superwall.sdk.kmp.models.customercenter

/**
 * Configures the screens, actions, support options and appearance of the
 * Customer Center, a self-service screen where users can view and restore
 * their purchases, manage, cancel or change a subscription, request a refund
 * and contact support.
 *
 * Set the default with `SuperwallOptions.customerCenter`, or pass one to
 * `Superwall.presentCustomerCenter`. [default] is the configuration the
 * native SDKs use when none is set.
 */
public data class CustomerCenterConfiguration(
    /** The screen shown when the user has at least one subscription (active or expired) or purchase. */
    val managementScreen: Screen,
    /** The screen shown when the user has no purchases at all. */
    val noPurchasesScreen: Screen,
    /** Support settings: email, app update warning and web management URL. */
    val support: Support = Support(),
    /** Colour overrides. */
    val appearance: Appearance = Appearance(),
    /** Shows the account details section (user ID, original download date). */
    val showsAccountDetails: Boolean = true,
    /** Warns when both a store and a web subscription are active. */
    val warnsAboutDuplicateSubscriptions: Boolean = true,
) {
    /** A Customer Center screen: a title, an optional subtitle and an ordered list of paths. */
    public data class Screen(
        /** Title. `null` uses the localized default for the screen. */
        val title: String? = null,
        /** Subtitle. `null` uses the localized default (no-purchases screen) or none (management screen). */
        val subtitle: String? = null,
        /** The paths (actions) shown on the screen, in order. */
        val paths: List<Path>,
    )

    /** An action row in the Customer Center. */
    public data class Path(
        /** What the path does. */
        val type: PathType,
        /** Row title. `null` uses the localized default for [type]; give URL paths a title. */
        val title: String? = null,
        /** An optional survey shown before the action runs. */
        val survey: FeedbackSurvey? = null,
        /**
         * Identifier reported as the path id on Customer Center events and to
         * [com.superwall.sdk.kmp.CustomerCenterDelegate]. `null` uses the
         * native default: the type's name (e.g. `restore`), a URL's host and
         * path, or a custom path's identifier. Only needed to tell apart two
         * paths that would otherwise share an id.
         */
        val id: String? = null,
    )

    /** The kinds of path the Customer Center supports. */
    public sealed interface PathType {
        /** Restores purchases. */
        public data object Restore : PathType

        /** Opens the store's subscription management page. */
        public data object ManageSubscription : PathType

        /**
         * Requests a refund.
         *
         * @property windowMillis How long after the purchase, in milliseconds,
         * a refund may be requested; `null` for no limit.
         */
        public data class Refund(val windowMillis: Long? = null) : PathType

        /**
         * Lets the user switch plans.
         *
         * @property productIds The plans to offer; `null` offers every plan.
         */
        public data class ChangePlan(val productIds: List<String>? = null) : PathType

        /** Contacts support by email ([Support.email]). */
        public data object ContactSupport : PathType

        /**
         * Opens a URL. Set the path's title: a URL has no name the SDK could
         * give it.
         */
        public data class Url(
            val url: String,
            val openMethod: OpenMethod = OpenMethod.IN_APP,
        ) : PathType

        /**
         * Does nothing in the SDK; the tap is reported to
         * [com.superwall.sdk.kmp.CustomerCenterDelegate] as
         * [CustomerCenterAction.Custom] with this [identifier].
         */
        public data class Custom(val identifier: String) : PathType
    }

    /** How a URL path opens. */
    public enum class OpenMethod {
        /** In an in-app browser. */
        IN_APP,

        /** In whichever app handles the URL. */
        EXTERNAL,
    }

    /** A single-choice survey shown before a path's action runs. */
    public data class FeedbackSurvey(
        val id: String,
        /**
         * Question text. `null` uses the localized "Why are you cancelling?"
         * on a manage-subscription path, and no title on any other path.
         */
        val title: String? = null,
        val options: List<Option>,
    ) {
        /** A survey answer. */
        public data class Option(
            val id: String,
            /** Option text. `null` uses the localized default for the built-in options. */
            val title: String? = null,
        ) {
            public companion object {
                /** "Too expensive", localized. */
                public val tooExpensive: Option get() = Option(id = "too_expensive")

                /** "Don't use the app", localized. */
                public val dontUse: Option get() = Option(id = "dont_use")

                /** "Bought by mistake", localized. */
                public val boughtByMistake: Option get() = Option(id = "bought_by_mistake")
            }
        }

        public companion object {
            /** The built-in cancellation survey: "Why are you cancelling?" with the three built-in options. */
            public val cancellation: FeedbackSurvey
                get() =
                    FeedbackSurvey(
                        id = "cancel_survey",
                        options = listOf(Option.tooExpensive, Option.dontUse, Option.boughtByMistake),
                    )
        }
    }

    /** Support settings. */
    public data class Support(
        /** Support email for the contact support path. `null` hides that path. */
        val email: String? = null,
        /** The latest published app version; when newer than the installed one, an update banner shows. */
        val latestAppVersion: String? = null,
        /** Whether to show the update banner. */
        val warnsAboutUpdates: Boolean = true,
        /**
         * Whether the SDK may look up the latest version on the App Store when
         * [latestAppVersion] isn't set. iOS only; ignored on Android.
         */
        val checksAppStoreForUpdates: Boolean = true,
        /** Overrides the web subscription management page for web subscriptions. */
        val webManagementUrl: String? = null,
    )

    /** Colour overrides for the Customer Center. */
    public data class Appearance(
        /** Tints buttons and links. `null` uses the default accent. */
        val accent: ColorPair? = null,
    ) {
        /** A light and dark colour, each a hex string (`#RRGGBB` or `#RRGGBBAA`). */
        public data class ColorPair(
            val light: String,
            val dark: String,
        )
    }

    public companion object {
        /**
         * The native SDKs' default configuration: restore, change plan,
         * refund, manage subscription (with the cancellation survey) and
         * contact support on the management screen; restore on the
         * no-purchases screen.
         */
        public val default: CustomerCenterConfiguration
            get() =
                CustomerCenterConfiguration(
                    managementScreen =
                        Screen(
                            paths =
                                listOf(
                                    Path(PathType.Restore),
                                    Path(PathType.ChangePlan()),
                                    Path(PathType.Refund()),
                                    Path(PathType.ManageSubscription, survey = FeedbackSurvey.cancellation),
                                    Path(PathType.ContactSupport),
                                ),
                        ),
                    noPurchasesScreen = Screen(paths = listOf(Path(PathType.Restore))),
                )
    }
}
