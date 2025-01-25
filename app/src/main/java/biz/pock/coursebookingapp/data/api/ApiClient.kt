package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.auth.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val retrofitClient: RetrofitClient,
    private val tokenManager: TokenManager
) {
    val apiServiceLogin: ApiServiceLogin by lazy {
        retrofitClient.retrofit.create(ApiServiceLogin::class.java)
    }

    val apiServiceBookings: ApiServiceBookings by lazy {
        retrofitClient.retrofit.create(ApiServiceBookings::class.java)
    }

    val apiServiceCourses: ApiServiceCourses by lazy {
        retrofitClient.retrofit.create(ApiServiceCourses::class.java)
    }

    val apiServiceUsers: ApiServiceUsers by lazy {
        retrofitClient.retrofit.create(ApiServiceUsers::class.java)
    }

    val apiServiceLocations: ApiServiceLocations by lazy {
        retrofitClient.retrofit.create(ApiServiceLocations::class.java)
    }

    val apiServiceInvoices: ApiServiceInvoices by lazy {
        retrofitClient.retrofit.create(ApiServiceInvoices::class.java)
    }

}