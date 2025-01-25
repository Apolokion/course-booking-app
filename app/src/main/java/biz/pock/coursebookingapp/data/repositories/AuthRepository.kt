package biz.pock.coursebookingapp.data.repositories

import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.auth.TokenManager
import biz.pock.coursebookingapp.data.model.LoginRequest
import biz.pock.coursebookingapp.data.model.LoginResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager
) {

    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiClient.apiServiceLogin.loginUser(LoginRequest(email, password))
        if (response.isSuccessful) {
            response.body()?.let { loginResponse ->
                // Token speichern
                tokenManager.saveToken(loginResponse.token)
                // Rolle speichern
                loginResponse.user?.role?.let { role ->
                    tokenManager.saveRole(role)
                }
                // User ID speichern
                loginResponse.user?.id?.let { userId ->
                    tokenManager.saveUserId(userId)
                }
                return loginResponse
            } ?: throw Exception(">>> Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun logout() {
        try {
            // API Logout durchführen
            val response = apiClient.apiServiceLogin.logout()
            if (!response.isSuccessful) {
                Timber.e(">>> API Logout failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error during API logout")
        } finally {
            // Lokalen State in jedem Fall löschen
            tokenManager.clearAll()
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.hasToken()

    fun getCurrentRole(): String = tokenManager.getRole() ?: "guest"

    fun getCurrentUserId(): String? = tokenManager.getUserId()

    fun getAuthToken(): String? = tokenManager.getToken()

}