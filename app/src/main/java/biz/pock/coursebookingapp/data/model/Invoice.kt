package biz.pock.coursebookingapp.data.model

import com.google.gson.annotations.SerializedName

data class Invoice(
    val id: String,
    val index: Int,
    val number: String,
    val amount: Double,
    val status: String,
    @SerializedName("payment_type")
    val paymentType: String?,
    @SerializedName("invoice_contact_id")
    val invoiceContactId: String?,
    @SerializedName("download_token")
    val downloadToken: String?,
    @SerializedName("download_token_expires_at")
    val downloadTokenExpiresAt: String?,
    val items: List<InvoiceItem>?,
    @SerializedName("invoice_contact")
    val invoiceContact: InvoiceContact?
) {
    // Vorbereitet für zukünftige Implementierungen
    // deshalb werden inaktive Parts nicht entfernt!!
    companion object {
        const val STATUS_DRAFT = "draft"
        const val STATUS_PENDING = "pending"
        const val STATUS_PAID = "paid"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_ERROR = "error"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELED_WITH_REVERSAL = "canceled_with_reversal"
        const val STATUS_REVERSAL = "reversal"

        const val PAYMENT_TYPE_CASH = "cash"
        const val PAYMENT_TYPE_CARD = "card"
    }
}

data class InvoiceItem(
    val description: String,
    val quantity: Int,
    @SerializedName("unit_price")
    val unitPrice: Double
)

data class InvoiceContact(
    val id: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String,
    val zip: String,
    val country: String
)