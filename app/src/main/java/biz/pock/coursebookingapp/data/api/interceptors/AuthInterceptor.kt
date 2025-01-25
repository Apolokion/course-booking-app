package biz.pock.coursebookingapp.data.api.interceptors

import biz.pock.coursebookingapp.data.auth.TokenManager
import biz.pock.coursebookingapp.data.model.BookingToken
import biz.pock.coursebookingapp.data.storage.BookingTokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val bookingTokenStorage: BookingTokenStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Bei Login Endpoint, keinen Token setzen
        if (originalRequest.url.encodedPath.endsWith("/login")) {
            return chain.proceed(originalRequest)
        }

        val modifiedRequest = originalRequest.newBuilder().apply {
            // Standard Auth Token setzen
            val token = tokenManager.getToken()
            token?.let {
                header("Authorization", "Bearer $it")
                Timber.d(">>> Adding auth token to request")
            }

            // Bei Buchungs-Endpoints prüfen ob ein spezifischer Booking Token existiert
            if (originalRequest.url.encodedPath.contains("/bookings/")) {
                val bookingId = extractBookingId(originalRequest.url.encodedPath)
                bookingId?.let { id ->
                    bookingTokenStorage.getValidTokenForBooking(id)?.let { bookingToken ->
                        header("X-Guest-Token", bookingToken)
                        Timber.d(">>> Adding booking token for booking $id")
                    }
                }
            }

            // Standard Headers
            addHeader("Accept", "application/json")
            addHeader("Content-Type", "application/json")
        }.build()

        val response = chain.proceed(modifiedRequest)

        // Nach einer erfolgreichen Buchung nach Token in der Response suchen
        if (response.isSuccessful &&
            originalRequest.method == "POST" &&
            originalRequest.url.encodedPath.endsWith("/bookings")
        ) {
            try {
                val responseBody = response.peekBody(Long.MAX_VALUE).string()
                val jsonObject = JSONObject(responseBody)

                // Token kommt im "guest" Objekt
                if (jsonObject.has("guest")) {
                    val guestObject = jsonObject.getJSONObject("guest")
                    val bookingObject = jsonObject.getJSONObject("booking")

                    if (guestObject.has("token") &&
                        guestObject.has("token_expires_at") &&
                        bookingObject.has("id")
                    ) {

                        val bookingToken = BookingToken(
                            bookingId = bookingObject.getString("id"),
                            token = guestObject.getString("token"),
                            tokenExpiresAt = guestObject.getString("token_expires_at")
                        )
                        bookingTokenStorage.saveBookingToken(bookingToken)
                        Timber.d(">>> Saved booking token for booking ${bookingToken.bookingId}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error processing booking response")
            }
        }

        return response
    }

    private fun extractBookingId(path: String): String? {
        return try {
            // Extrahiert die Booking ID aus URLs wie:
            // /api/v1/bookings/{booking_id}
            // /api/v1/bookings/{booking_id}/participants
            // /api/v1/bookings/{booking_id}/invoice-contact
            val regex = "/bookings/([^/]+)".toRegex()
            regex.find(path)?.groupValues?.get(1)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error extracting booking ID from path: $path")
            null
        }
    }
}