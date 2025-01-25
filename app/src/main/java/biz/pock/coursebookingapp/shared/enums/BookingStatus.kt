package biz.pock.coursebookingapp.shared.enums

import android.content.Context
import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class BookingStatus(@StringRes override val resId: Int) : SpinnerItem {
    PENDING(R.string.booking_status_pending),
    CONFIRMED(R.string.booking_status_confirmed),
    CANCELED(R.string.booking_status_canceled);

    companion object {
        fun fromApiString(value: String): BookingStatus? {
            return when(value.lowercase()) {
                "pending" -> PENDING
                "confirmed" -> CONFIRMED
                "canceled" -> CANCELED
                else -> null
            }
        }

        fun toApiString(status: BookingStatus): String {
            return when(status) {
                PENDING -> "pending"
                CONFIRMED -> "confirmed"
                CANCELED -> "canceled"
            }
        }

        fun fromLocalizedName(context: Context, localizedName: String): BookingStatus? {
            return entries.firstOrNull { status ->
                context.getString(status.resId).equals(localizedName, ignoreCase = true)
            }
        }
    }
}