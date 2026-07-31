package com.superwall.sdk.kmp.models.paywall

/**
 * A request to compute a device property associated with a placement at runtime.
 */
public data class ComputedPropertyRequest(
    /** The type of device property to compute. */
    val type: ComputedPropertyRequestType,
    /** The name of the placement used to compute the device property. */
    val eventName: String,
)

/**
 * The type of device property to compute for a [ComputedPropertyRequest].
 */
public enum class ComputedPropertyRequestType {
    /** The number of minutes since the placement occurred. */
    MINUTES_SINCE,

    /** The number of hours since the placement occurred. */
    HOURS_SINCE,

    /** The number of days since the placement occurred. */
    DAYS_SINCE,

    /** The number of months since the placement occurred. */
    MONTHS_SINCE,

    /** The number of years since the placement occurred. */
    YEARS_SINCE,

    /** The number of times the placement occurred in the last hour. */
    PLACEMENTS_IN_HOUR,

    /** The number of times the placement occurred in the last day. */
    PLACEMENTS_IN_DAY,

    /** The number of times the placement occurred in the last week. */
    PLACEMENTS_IN_WEEK,

    /** The number of times the placement occurred in the last month. */
    PLACEMENTS_IN_MONTH,

    /** The number of times the placement occurred since install. */
    PLACEMENTS_SINCE_INSTALL,
}
