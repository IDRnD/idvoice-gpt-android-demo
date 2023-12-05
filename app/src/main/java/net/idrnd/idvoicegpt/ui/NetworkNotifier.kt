package net.idrnd.idvoicegpt.ui

/**
 * Abstraction for network connectivity updates.
 */
interface NetworkNotifier {

    /**
     * Notifies network connectivity.
     *
     * @param isConnected boolean that represents connectivity.
     */
    fun notifyNetworkStatus(isConnected: Boolean)
}
