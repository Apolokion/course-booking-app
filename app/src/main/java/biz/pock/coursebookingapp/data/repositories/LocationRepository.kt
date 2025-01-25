package biz.pock.coursebookingapp.data.repositories

import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    fun getLocations(): Flow<List<Location>> = flow {
        val response = apiClient.apiServiceLocations.getLocations()
        if (response.isSuccessful) {
            emit(response.body() ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun createLocation(location: Location): Location {
        val response = apiClient.apiServiceLocations.storeLocation(location)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun updateLocation(locationId: String, location: Location): Location {
        val response = apiClient.apiServiceLocations.updateLocation(locationId, location)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun deleteLocation(locationId: String) {
        val response = apiClient.apiServiceLocations.deleteLocation(locationId)
        if (!response.isSuccessful) {
            throw Exception(response.errorBody()?.string())
        }
    }
}