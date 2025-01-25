package biz.pock.coursebookingapp.data.api

import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.data.model.BookingInvoiceContact
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.data.model.CreateBookingRequest
import biz.pock.coursebookingapp.data.model.CreateBookingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiServiceBookings {

    @GET("api/v1/bookings")
    suspend fun getBookings(
        @Query("status") status: String?,
        @Query("with") with: String?
    ): Response<List<Booking>>

    @PUT("api/v1/bookings/{booking}")
    suspend fun updateBooking(
        @Path("booking") bookingId: String,
        @Body booking: Booking
    ): Response<Booking>

    @DELETE("api/v1/bookings/{booking}")
    suspend fun deleteBooking(
        @Path("booking") bookingId: String
    ): Response<Unit>

    // Für den Gast
    @POST("api/v1/bookings")
    suspend fun createBooking(
        @Body createBookingRequest: CreateBookingRequest
    ): Response<CreateBookingResponse>

    @GET("api/v1/bookings/{booking}")
    suspend fun getBookingDetails(
        @Path("booking") bookingId: String,
        @Query("with") with: String?,
        @Header("X-Guest-Token") guestToken: String
    ): Response<Booking>

    @GET("api/v1/bookings/{booking}")
    suspend fun getBookingDetailsAdmin(
        @Path("booking") bookingId: String,
        @Query("with") with: String?,
    ): Response<Booking>

    @GET("api/v1/bookings/{booking}/participants")
    suspend fun getBookingParticipants(
        @Path("booking") bookingId: String,
        @Header("X-Guest-Token") guestToken: String
    ): Response<List<BookingParticipant>>

    @POST("api/v1/bookings/{booking}/participants")
    suspend fun createBookingParticipant(
        @Path("booking") bookingId: String,
        @Body participant: BookingParticipant,
        @Header("X-Guest-Token") guestToken: String
    ): Response<BookingParticipant>

    @POST("api/v1/bookings/{booking}/invoice-contact")
    suspend fun createBookingInvoiceContact(
        @Path("booking") bookingId: String,
        @Body invoiceContact: BookingInvoiceContact,
        @Header("X-Guest-Token") guestToken: String
    ): Response<BookingInvoiceContact>

    @GET("api/v1/bookings/{booking}/invoice-contact")
    suspend fun getBookingInvoiceContact(
        @Path("booking") bookingId: String,
        @Header("X-Guest-Token") guestToken: String
    ): Response<BookingInvoiceContact>

}