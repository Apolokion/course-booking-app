package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.databinding.ItemBookingBinding
import biz.pock.coursebookingapp.shared.enums.BookingStatus

class BookingListAdapter(
    private val onBookingClick: (Booking, View) -> Unit
) : BaseListAdapter<Booking, ItemBookingBinding>(BookingDiffCallback()) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemBookingBinding {
        return ItemBookingBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemBookingBinding, item: Booking) {
        binding.apply {
            // Buchungsnummer mit Accessibility
            val bookingId = item.id.take(8)
            textBookingId.text = root.context.getString(R.string.booking_id, bookingId)
            textBookingId.contentDescription = root.context.getString(
                R.string.booking_id_format,
                bookingId
            )

            // Kursinfo mit Accessibility
            val courseTitle = item.course?.title ?: item.courseId
            textCourseInfo.text = root.context.getString(
                R.string.booking_course,
                courseTitle
            )
            textCourseInfo.contentDescription = root.context.getString(
                R.string.booking_course_format,
                courseTitle
            )

            // Betrag mit Accessibility
            textAmount.text = root.context.getString(R.string.booking_amount, item.amount)
            textAmount.contentDescription = root.context.getString(
                R.string.booking_amount_format,
                item.amount
            )

            // Status mit Accessibility
            setupBookingStatus(root.context, item)

            // Kommentar mit Accessibility
            setupCommentSection(root.context, item)

            // Gesamtbeschreibung für das Item
            root.contentDescription = root.context.getString(
                R.string.booking_details_format,
                bookingId,
                courseTitle,
                textStatus.text,
                textAmount.text
            )

            // Card Accessibility
            root.apply {
                isClickable = true
                isFocusable = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                setOnClickListener { view ->
                    onBookingClick(item, view)
                }
            }

            // Alle wichtigen TextViews als wichtig für Accessibility markieren
            textBookingId.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textCourseInfo.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textAmount.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textStatus.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textComment.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    private fun ItemBookingBinding.setupBookingStatus(context: Context, booking: Booking) {
        val status = BookingStatus.fromApiString(booking.status)
        textStatus.text = status?.let {
            context.getString(it.resId)
        } ?: booking.status.replaceFirstChar { it.uppercase() }

        textStatus.contentDescription = context.getString(
            R.string.content_desc_booking_status,
            textStatus.text
        )

        // Status Styling
        textStatus.setBackgroundResource(R.drawable.bg_status_chip)
        textStatus.setTextColor(getStatusColor(context, status))
    }

    private fun ItemBookingBinding.setupCommentSection(context: Context, booking: Booking) {
        textComment.apply {
            if (!booking.comment.isNullOrBlank()) {
                visibility = View.VISIBLE
                text = context.getString(R.string.booking_comment, booking.comment)
                contentDescription = context.getString(
                    R.string.booking_comment_format,
                    booking.comment
                )
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun getStatusColor(context: Context, status: BookingStatus?): Int {
        return when (status) {
            BookingStatus.PENDING -> context.getColor(R.color.status_pending)
            BookingStatus.CONFIRMED -> context.getColor(R.color.status_confirmed)
            BookingStatus.CANCELED -> context.getColor(R.color.status_canceled)
            null -> context.getColor(R.color.on_surface)
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}