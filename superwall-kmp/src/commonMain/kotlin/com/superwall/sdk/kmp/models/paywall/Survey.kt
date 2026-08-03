package com.superwall.sdk.kmp.models.paywall

/**
 * A survey attached to a paywall.
 */
public data class Survey(
    /** The id of the survey. */
    val id: String,
    /**
     * The assigned key for the survey.
     *
     * A user will only see one survey per assignment key.
     */
    val assignmentKey: String,
    /** The title of the survey's alert controller. */
    val title: String,
    /** The message of the survey's alert controller. */
    val message: String,
    /** The options to display in the alert controller. */
    val options: List<SurveyOption>,
    /** When the survey should show. */
    val presentationCondition: SurveyShowCondition,
    /** The probability that the survey will present to the user. */
    val presentationProbability: Double,
    /** Whether an "Other" option should appear to allow a user to provide a custom response. */
    val includeOtherOption: Boolean,
    /** Whether a close button should appear to allow users to skip the survey. */
    val includeCloseOption: Boolean,
)

/**
 * An option to display in a paywall [Survey].
 */
public data class SurveyOption(
    /** The id of the survey option. */
    val id: String? = null,
    /** The text of the survey option. */
    val text: String? = null,
)

/**
 * Indicates when a [Survey] should show.
 */
public enum class SurveyShowCondition {
    /** Shows the survey when the user manually closes the paywall. */
    ON_MANUAL_CLOSE,

    /** Shows the survey after the user purchases. */
    ON_PURCHASE,
}
