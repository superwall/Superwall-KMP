# androidx.startup discovers Initializer implementations reflectively from
# manifest <meta-data>; R8 full mode can otherwise strip or rename them,
# which would silently disable Superwall's automatic Application capture.
-keep class com.superwall.sdk.kmp.internal.SuperwallInitializer { *; }
