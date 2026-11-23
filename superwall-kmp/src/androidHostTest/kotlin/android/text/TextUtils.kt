package android.text

/**
 * Host tests run against the mockable android.jar, where every method throws
 * "not mocked". Play Billing's `Purchase.getOrderId` calls
 * `TextUtils.isEmpty`, so `ObservedPurchaseTest` needs the real behaviour:
 * test classes come ahead of android.jar on the classpath, and this one
 * method is all that is used.
 */
@Suppress("unused") // Called by Play Billing, not by the tests.
object TextUtils {
    @JvmStatic
    fun isEmpty(str: CharSequence?): Boolean = str.isNullOrEmpty()
}
