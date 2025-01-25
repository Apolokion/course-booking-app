package biz.pock.coursebookingapp.data.repositories

import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.data.model.BookingInvoiceContact
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.data.model.CreateBookingRequest
import biz.pock.coursebookingapp.data.model.CreateBookingResponse
import biz.pock.coursebookingapp.data.storage.BookingTokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val bookingTokenStorage: BookingTokenStorage
) {
    fun getBookings(status: String? = null): Flow<List<Booking>> = flow {
        val response = apiClient.apiServiceBookings.getBookings(
            status = status,
            // Mit Kursdetails laden
            with = "course"
        )
        if (response.isSuccessful) {
            emit(response.body() ?: emptyList())
        } else {
            throw HttpException(response)
        }
    }

    suspend fun getBookingDetailsAdmin(bookingId: String): Booking {
        val response = apiClient.apiServiceBookings.getBookingDetailsAdmin(bookingId, null)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)//Exception(response.errorBody()?.string())
        }
    }

    suspend fun createBooking(request: CreateBookingRequest): CreateBookingResponse {
        try {
            val response = apiClient.apiServiceBookings.createBooking(request)
            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating booking")
            throw e
        }
    }

    suspend fun getBookingDetails(
        bookingId: String,
        includeDetails: Boolean = true
    ): Booking {
        try {
            // Token für diese spezifische Buchung holen
            val guestToken = bookingTokenStorage.getValidTokenForBooking(bookingId)
                ?: throw Exception("No valid token found for booking $bookingId")

            val withParam = if (includeDetails) {
                "course,participants,timeslot.location,invoicecontact"
            } else null

            val response = apiClient.apiServiceBookings.getBookingDetails(
                bookingId = bookingId,
                with = withParam,
                guestToken = guestToken
            )

            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error getting booking details for $bookingId")
            throw e
        }
    }

    suspend fun createBookingParticipant(
        bookingId: String,
        participant: BookingParticipant,
        bookingToken: String
    ): BookingParticipant {
        try {

            val response = apiClient.apiServiceBookings.createBookingParticipant(
                bookingId = bookingId,
                participant = participant,
                guestToken = bookingToken
            )

            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating participant for booking $bookingId")
            throw e
        }
    }

    suspend fun getBookingParticipants(bookingId: String): List<BookingParticipant> {
        try {
            val guestToken = bookingTokenStorage.getValidTokenForBooking(bookingId)
                ?: throw Exception("No valid token found for booking $bookingId")

            val response = apiClient.apiServiceBookings.getBookingParticipants(
                bookingId = bookingId,
                guestToken = guestToken
            )

            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error getting participants for booking $bookingId")
            throw e
        }
    }

    suspend fun createBookingInvoiceContact(
        bookingId: String,
        invoiceContact: BookingInvoiceContact,
        bookingToken: String,
    ): BookingInvoiceContact {
        try {

            val response = apiClient.apiServiceBookings.createBookingInvoiceContact(
                bookingId = bookingId,
                invoiceContact = invoiceContact,
                guestToken = bookingToken
            )

            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating invoice contact for booking $bookingId")
            throw e
        }
    }

    suspend fun getGuestBookings(): List<Booking> {
        try {
            // Hole alle gespeicherten Booking Tokens
            val bookingTokens = bookingTokenStorage.getBookingTokens()

            // Für jeden Token die Buchungsdetails abrufen
            return bookingTokens.mapNotNull { token ->
                try {
                    getBookingDetails(token.bookingId)
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching booking ${token.bookingId}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error fetching guest bookings")
            throw e
        }
    }

    suspend fun getBookingInvoiceContact(
        bookingId: String,
        contactId: String
    ): BookingInvoiceContact? {
        return try {
            // Hole den gespeicherten Token für diese Buchung
            val guestToken = bookingTokenStorage.getValidTokenForBooking(bookingId)
                ?: throw Exception("No valid token found for booking $bookingId")

            val response = apiClient.apiServiceBookings.getBookingInvoiceContact(
                bookingId = bookingId,
                guestToken = guestToken
            )

            if (response.isSuccessful) {
                response.body()
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error getting invoice contact ($contactId) for booking $bookingId")
            null
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Booking {
        try {
            // Hole zuerst die vollständigen Booking-Details
            val existingBooking = getBookingDetailsAdmin(bookingId)

            // Erstelle ein neues Booking-Objekt mit dem aktualisierten Status
            val updatedBooking = existingBooking.copy(
                status = status
            )

            // Sende das Update an die API
            val response = apiClient.apiServiceBookings.updateBooking(bookingId, updatedBooking)
            if (response.isSuccessful) {
                return response.body() ?: throw Exception("Empty response body")
            } else {
                // HttpException für Status Code Erkennung
                throw HttpException(response)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating booking status")
            throw e
        }
    }

    suspend fun deleteBooking(bookingId: String) {
        val response = apiClient.apiServiceBookings.deleteBooking(bookingId)
        if (!response.isSuccessful) {
            // HttpException für Fehler-Code Erkennung
            throw HttpException(response)
        }
    }

}