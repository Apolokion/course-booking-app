package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.model.Location
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiServiceLocations {
    @GET("/api/v1/locations")
    suspend fun getLocations(): Response<List<Location>>

    @POST("/api/v1/locations")
    suspend fun storeLocation(
        @Body location: Location
    ): Response<Location>

    @GET("/api/v1/locations/{location}")
    suspend fun getLocation(
        @Path("location") locationId: String
    ): Response<Location>

    @PUT("/api/v1/locations/{location}")
    suspend fun updateLocation(
        @Path("location") locationId: String,
        @Body location: Location
    ): Response<Location>

    @DELETE("/api/v1/locations/{location}")
    suspend fun deleteLocation(
        @Path("location") locationId: String
    ): Response<Unit>
}