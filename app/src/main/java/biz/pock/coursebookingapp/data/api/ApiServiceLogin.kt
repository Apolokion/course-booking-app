package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.model.LoginRequest
import biz.pock.coursebookingapp.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiServiceLogin {
    @POST("/api/v1/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/v1/logout")
    suspend fun logout(): Response<Unit>

}