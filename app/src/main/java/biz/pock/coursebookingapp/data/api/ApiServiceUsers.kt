package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiServiceUsers {
    @GET("/api/v1/users")
    suspend fun getUsers(
        @Query("role") role: String? = null
    ): Response<List<User>>

    @POST("/api/v1/users")
    suspend fun storeUser(
        @Body user: User
    ): Response<User>

    @GET("/api/v1/users/{user}")
    suspend fun getUser(
        @Path("user") userId: String
    ): Response<User>

    @PUT("/api/v1/users/{user}")
    suspend fun updateUser(
        @Path("user") userId: String,
        @Body user: User
    ): Response<User>

    @DELETE("/api/v1/users/{user}")
    suspend fun deleteUser(
        @Path("user") userId: String
    ): Response<Unit>
}