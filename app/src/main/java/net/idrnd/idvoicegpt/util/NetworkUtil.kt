package net.idrnd.idvoicegpt.util

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtil {

    fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
                isNetworkCapabilitiesValid(it)
            } ?: false
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }

    private fun isNetworkCapabilitiesValid(networkCapabilities: NetworkCapabilities): Boolean =
        networkCapabilities.let {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        }
}
