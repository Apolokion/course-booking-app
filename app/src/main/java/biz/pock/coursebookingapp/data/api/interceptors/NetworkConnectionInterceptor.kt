package biz.pock.coursebookingapp.data.api.interceptors

import biz.pock.coursebookingapp.data.api.interfaces.NetworkConnection
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class NetworkConnectionInterceptor @Inject constructor(
    private val networkConnection: NetworkConnection
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkConnection.isNetworkAvailable()) {
            throw NoConnectivityException()
        }

        return chain.proceed(chain.request())
    }
}

class NoConnectivityException : IOException("No network available, please check your internet connection")