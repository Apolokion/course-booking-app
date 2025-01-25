package biz.pock.coursebookingapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import biz.pock.coursebookingapp.data.api.interfaces.NetworkConnection
import javax.inject.Inject

class NetworkUtils @Inject constructor(
    private val context: Context
) : NetworkConnection {

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}