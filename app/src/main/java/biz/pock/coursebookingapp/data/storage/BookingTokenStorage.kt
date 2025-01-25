package biz.pock.coursebookingapp.data.storage

import android.content.Context
import android.content.SharedPreferences
import biz.pock.coursebookingapp.data.model.BookingToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val PREF_NAME = "booking_tokens"
        private const val KEY_TOKENS = "tokens"
        // ISO-8601 Format für UTC Zeitstempel
        private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveBookingToken(token: BookingToken) {
        try {
            val currentTokens = getBookingTokens().toMutableList()
            currentTokens.add(token)

            // Speichere aktualisierte Liste
            prefs.edit().putString(KEY_TOKENS, gson.toJson(currentTokens)).apply()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error saving booking token")
        }
    }

    fun getBookingTokens(): List<BookingToken> {
        return try {
            val json = prefs.getString(KEY_TOKENS, null)
            if (json != null) {
                val type = object : TypeToken<List<BookingToken>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error getting booking tokens")
            emptyList()
        }
    }

    fun getValidTokenForBooking(bookingId: String): String? {
        return try {
            val token = getBookingTokens()
                .firstOrNull { it.bookingId == bookingId }
                ?: return null

            Timber.w(">>> BOOKINGS IN SHAREDPREFS -> $token ")

            // Parsen des ISO-8601 Zeitstempels
            val expiryDate = ZonedDateTime.parse(token.tokenExpiresAt, dateFormatter)
                .toLocalDateTime()
            val now = LocalDateTime.now()

            if (now.isBefore(expiryDate)) {
                token.token
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error getting valid token for booking $bookingId")
            null
        }
    }
}