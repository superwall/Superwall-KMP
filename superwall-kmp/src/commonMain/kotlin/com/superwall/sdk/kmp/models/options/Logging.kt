package com.superwall.sdk.kmp.models.options

/**
 * Configuration for printing to the console.
 */
public data class Logging(
    /**
     * The minimum log level to print to the console. `null`, the default,
     * leaves each native SDK's own default in place: [LogLevel.INFO] on iOS
     * and [LogLevel.WARN] on Android.
     */
    val level: LogLevel? = null,
    /** The scope of logs to print to the console. Defaults to [LogScope.ALL]. */
    val scopes: Set<LogScope> = setOf(LogScope.ALL),
) {
    /**
     * Prints [message] (and, if present, [error]) to the console when [level]
     * is at or above this configuration's minimum [Logging.level].
     *
     * Use the [debug]/[info]/[warn]/[error] shorthands for the common levels.
     */
    public fun handleLogRecord(
        level: LogLevel,
        message: String,
        error: Throwable? = null,
    ) {
        // An unset level prints from INFO up, as it did when INFO was the default.
        if (level.ordinal < (this.level ?: LogLevel.INFO).ordinal) {
            return
        }
        println("[$level] $message")
        if (error != null) {
            println(error.stackTraceToString())
        }
    }

    /** Logs [message] (and optional [error]) at [LogLevel.DEBUG]. */
    public fun debug(message: String, error: Throwable? = null): Unit =
        handleLogRecord(LogLevel.DEBUG, message, error)

    /** Logs [message] (and optional [error]) at [LogLevel.INFO]. */
    public fun info(message: String, error: Throwable? = null): Unit =
        handleLogRecord(LogLevel.INFO, message, error)

    /** Logs [message] (and optional [error]) at [LogLevel.WARN]. */
    public fun warn(message: String, error: Throwable? = null): Unit =
        handleLogRecord(LogLevel.WARN, message, error)

    /** Logs [message] (and optional [error]) at [LogLevel.ERROR]. */
    public fun error(message: String, error: Throwable? = null): Unit =
        handleLogRecord(LogLevel.ERROR, message, error)
}
