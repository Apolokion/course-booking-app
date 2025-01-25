package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.api.interceptors.AuthInterceptor
import biz.pock.coursebookingapp.data.api.interceptors.LoggingInterceptor
import biz.pock.coursebookingapp.data.api.interceptors.NetworkConnectionInterceptor
import biz.pock.coursebookingapp.shared.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val loggingInterceptor: LoggingInterceptor,
    private val networkConnectionInterceptor: NetworkConnectionInterceptor
) {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(networkConnectionInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}