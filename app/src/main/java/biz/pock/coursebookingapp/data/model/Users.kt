package biz.pock.coursebookingapp.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val user: User,
    val token: String
)

@Parcelize
data class User(
    val id: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val role: String,
    @SerializedName("email_verified_at") val emailVerifiedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    // Password nur für Requests, nicht für Responses
    val password: String = ""
) : Parcelable