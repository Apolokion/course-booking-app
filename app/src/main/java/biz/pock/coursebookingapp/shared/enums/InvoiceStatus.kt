// InvoiceStatus.kt
package biz.pock.coursebookingapp.shared.enums

import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class InvoiceStatus(@StringRes override val resId: Int) : SpinnerItem {
    PENDING(R.string.invoice_status_pending),                  // "pending"
    DRAFT(R.string.invoice_status_draft),                      // "draft"
    PAID(R.string.invoice_status_paid),                        // "paid"
    CONFIRMED(R.string.invoice_status_confirmed),              // "confirmed"
    CANCELED(R.string.invoice_status_cancelled),              // "canceled"
    CANCELED_WITH_REVERSAL(R.string.invoice_status_cancelled_with_reversal), // "canceled_with_reversal"
    REVERSAL(R.string.invoice_status_reversal);              // "reversal"

    companion object {
        fun fromApiString(value: String): InvoiceStatus? {
            return when(value.lowercase()) {
                "pending" -> PENDING
                "draft" -> DRAFT
                "paid" -> PAID
                "confirmed" -> CONFIRMED
                "canceled" -> CANCELED
                "canceled_with_reversal" -> CANCELED_WITH_REVERSAL
                "reversal" -> REVERSAL
                else -> null
            }
        }

        fun toApiString(status: InvoiceStatus): String {
            return when(status) {
                PENDING -> "pending"
                DRAFT -> "draft"
                PAID -> "paid"
                CONFIRMED -> "confirmed"
                CANCELED -> "canceled"
                CANCELED_WITH_REVERSAL -> "canceled_with_reversal"
                REVERSAL -> "reversal"
            }
        }
    }
}