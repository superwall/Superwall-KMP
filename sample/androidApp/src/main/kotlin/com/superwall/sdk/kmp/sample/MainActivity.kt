package com.superwall.sdk.kmp.sample

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Hosts the shared Compose Multiplatform [App] via [setSampleAppContent]
 * (defined in :sample:shared's androidMain — see the note there for why the
 * composable call doesn't live here).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSampleAppContent()
    }
}
