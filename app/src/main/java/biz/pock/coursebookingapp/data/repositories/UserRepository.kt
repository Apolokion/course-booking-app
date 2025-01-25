package biz.pock.coursebookingapp.data.repositories

import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    fun getUsers(role: String? = null): Flow<List<User>> = flow {
        val response = apiClient.apiServiceUsers.getUsers(role)
        if (response.isSuccessful) {
            emit(response.body() ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun createUser(user: User): User {
        val response = apiClient.apiServiceUsers.storeUser(user)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun updateUser(userId: String, user: User): User {
        val response = apiClient.apiServiceUsers.updateUser(userId, user)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun deleteUser(userId: String) {
        val response = apiClient.apiServiceUsers.deleteUser(userId)
        if (!response.isSuccessful) {
            throw Exception(response.errorBody()?.string())
        }
    }
}