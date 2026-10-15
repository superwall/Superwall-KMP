package com.superwall.sdk.kmp.models.options

/**
 * A running `superwall dev` server for [SuperwallOptions.devServer] to connect
 * to, so paywalls render from your live, local paywall code. For development
 * builds only.
 *
 * @platform iOS — ignored on Android, which has no dev server support yet.
 */
public sealed class DevServer {
    /**
     * Finds the dev server on `localhost`, which reaches a server running on
     * the same machine from a simulator.
     */
    public data object Default : DevServer()

    /**
     * The dev server at an exact address — the `Device` URL's origin that
     * `superwall dev` prints, e.g. `http://192.168.1.10:6100`. Use this on a
     * physical device, which can't reach your machine via `localhost`. An
     * unparseable URL leaves the dev server off.
     */
    public data class Url(
        val url: String,
    ) : DevServer()
}
