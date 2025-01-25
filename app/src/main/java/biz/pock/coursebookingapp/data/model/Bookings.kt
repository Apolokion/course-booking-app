package biz.pock.coursebookingapp.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class Booking(
    val id: String,
    val amount: Double,
    val comment: String?,
    val status: String,
    @SerializedName("course_id")
    val courseId: String,
    @SerializedName("timeslot_id")
    val timeslotId: String,
    @SerializedName("invoice_contact_id")
    val invoiceContactId: String?,
    @SerializedName("invoice_id")
    val invoiceId: String?,
    val course: Course? = null,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val token: String?,
    @SerializedName("token_expires_at")
    val tokenExpiresAt: String?
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELED = "canceled"
    }
}

data class CreateBookingRequest(
    @SerializedName("timeslot_id")
    val timeslotId: String,
    @SerializedName("course_id")
    val courseId: String,
    val comment: String?
)

data class CreateBookingResponse(
    val booking: Booking,
    val guest: GuestToken
)

data class GuestToken(
    val token: String,
    @SerializedName("token_expires_at")
    val tokenExpiresAt: String
)

data class BookingToken(
    val bookingId: String,
    val token: String,
    @SerializedName("token_expires_at")
    val tokenExpiresAt: String
)

@Parcelize
data class BookingParticipant(
    val id: String? = null,
    val firstname: String,
    val lastname: String,
    val birthdate: String,
    val email: String,
    @SerializedName("skill_level")
    val skillLevel: String,
    @SerializedName("booking_id")
    val bookingId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class BookingInvoiceContact(
    val id: String? = null,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String,
    val zip: String,
    val country: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class GuestBookingDetails(
    // Basis Buchungsdaten
    val id: String,
    val amount: Double,
    val comment: String?,
    val status: String,
    val courseId: String,
    val timeslotId: String,
    val invoiceContactId: String?,
    val invoiceId: String?,
    val createdAt: String?,
    val updatedAt: String?,

    // Erweiterte Details, die separat geladen werden
    val course: Course?,
    val timeslot: Timeslot?,
    val participants: List<BookingParticipant>?,
    val invoiceContact: BookingInvoiceContact?,

    // Zusätzliche Helper-Funktion für UI-Status
    val isExpanded: Boolean = false
) : Parcelable {
    companion object {
        fun fromBooking(
            booking: Booking,
            participants: List<BookingParticipant>? = null,
            invoiceContact: BookingInvoiceContact? = null,
            timeslot: Timeslot? = null,
            isExpanded: Boolean = false
        ): GuestBookingDetails {
            return GuestBookingDetails(
                id = booking.id,
                amount = booking.amount,
                comment = booking.comment,
                status = booking.status,
                courseId = booking.courseId,
                timeslotId = booking.timeslotId,
                invoiceContactId = booking.invoiceContactId,
                invoiceId = booking.invoiceId,
                createdAt = booking.createdAt,
                updatedAt = booking.updatedAt,
                course = booking.course,
                timeslot = timeslot,
                participants = participants,
                invoiceContact = invoiceContact,
                isExpanded = isExpanded
            )
        }
    }
}