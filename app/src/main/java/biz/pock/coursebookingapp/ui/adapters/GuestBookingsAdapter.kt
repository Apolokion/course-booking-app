package biz.pock.coursebookingapp.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.BookingInvoiceContact
import biz.pock.coursebookingapp.data.model.GuestBookingDetails
import biz.pock.coursebookingapp.databinding.ItemGuestBookingBinding
import timber.log.Timber

class GuestBookingAdapter(
    private val context: Context,
    private val onEditBillingContact: (GuestBookingDetails) -> Unit
) : ListAdapter<GuestBookingDetails, GuestBookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    inner class BookingViewHolder(
        private val binding: ItemGuestBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var participantAdapter: ParticipantAdapter? = null

        fun bind(bookingDetails: GuestBookingDetails) {
            binding.apply {
                try {
                    // Header Info
                    setupHeader(bookingDetails)

                    // Click Listener zum Ausklappen
                    headerLayout.setOnClickListener {
                        toggleExpansion(bookingDetails)
                    }
                    buttonExpandCollapse.setOnClickListener {
                        toggleExpansion(bookingDetails)
                    }

                    // Expandable Content - nur wenn die Details geladen wurden
                    setupExpandableContent(bookingDetails)

                    // Accessibility
                    setupAccessibility(bookingDetails)

                } catch (e: Exception) {
                    Timber.e(e, ">>> Error binding booking details: ${bookingDetails.id}")
                    setFallbackValues()
                }
            }
        }

        private fun setupHeader(bookingDetails: GuestBookingDetails) {
            binding.apply {
                // Buchungsnummer
                textBookingId.text = context.getString(R.string.booking_id, bookingDetails.id)

                // Status Chip
                textStatus.apply {
                    text = bookingDetails.status.replaceFirstChar { it.uppercase() }
                    setBackgroundColor(getStatusColor(bookingDetails.status))
                }

                // Kurstitel
                textCourseTitle.text = bookingDetails.course?.title
                    ?: context.getString(R.string.unknown_course)

                // Datum & Zeit
                bookingDetails.timeslot?.let { timeslot ->
                    val startTime = timeslot.startTime?.substring(0, 5) ?: "--:--"
                    val endTime = timeslot.endTime?.substring(0, 5) ?: "--:--"

                    textDateTime.text = context.getString(
                        R.string.time_entry_details_format,
                        startTime,
                        endTime,
                        timeslot.startDate
                    )
                } ?: run {
                    textDateTime.text = context.getString(R.string.unknown_date)
                }

                // Expand/Collapse Button
                buttonExpandCollapse.apply {
                    text = context.getString(
                        if (bookingDetails.isExpanded) R.string.collapse_entries
                        else R.string.expand_entries
                    )
                    setIconResource(
                        if (bookingDetails.isExpanded) R.drawable.ic_arrow_up
                        else R.drawable.ic_arrow_down
                    )
                }
            }
        }

        private fun setupExpandableContent(bookingDetails: GuestBookingDetails) {
            binding.apply {
                expandableContent.visibility =
                    if (bookingDetails.isExpanded) View.VISIBLE else View.GONE

                if (bookingDetails.isExpanded) {
                    // Standort
                    textLocation.text = bookingDetails.timeslot?.location?.name
                        ?: context.getString(R.string.unknown_location)

                    // Preis
                    textAmount.text = context.getString(
                        R.string.booking_amount,
                        bookingDetails.amount
                    )

                    // Teilnehmerliste
                    setupParticipantsList(bookingDetails)

                    // Rechnungskontakt
                    setupBillingContact(bookingDetails)

                    // Edit Button
                    buttonEditContact.setOnClickListener {
                        onEditBillingContact(bookingDetails)
                    }
                }
            }
        }

        private fun setupParticipantsList(bookingDetails: GuestBookingDetails) {
            binding.recyclerViewParticipants.apply {
                if (layoutManager == null) {
                    layoutManager = LinearLayoutManager(context)
                }

                if (participantAdapter == null) {
                    participantAdapter = ParticipantAdapter(
                        onEditClick = { /* Readonly */ },
                        onDeleteClick = { /* Readonly */ },
                        context = context
                    )
                    adapter = participantAdapter
                }

                bookingDetails.participants?.let { participants ->
                    participantAdapter?.submitList(participants)
                }
            }
        }

        private fun setupBillingContact(bookingDetails: GuestBookingDetails) {
            bookingDetails.invoiceContact?.let { contact ->
                binding.apply {
                    // Name
                    textBillingName.text = "${contact.firstname} ${contact.lastname}"

                    // Email
                    textBillingEmail.text = contact.email

                    // Telefon
                    textBillingPhone.text = contact.phone

                    // Adresse
                    textBillingAddress.text = buildAddressString(contact)
                }
            }
        }

        private fun setupAccessibility(bookingDetails: GuestBookingDetails) {
            binding.root.contentDescription = context.getString(
                R.string.booking_details_format,
                bookingDetails.id,
                bookingDetails.course?.title ?: context.getString(R.string.unknown_course),
                bookingDetails.status,
                context.getString(R.string.booking_amount, bookingDetails.amount)
            )
        }

        private fun setFallbackValues() {
            binding.apply {
                textBookingId.text = context.getString(R.string.unknown_course)
                textStatus.visibility = View.GONE
                textCourseTitle.text = context.getString(R.string.unknown_course)
                textDateTime.text = context.getString(R.string.unknown_date)
                expandableContent.visibility = View.GONE
            }
        }

        private fun getStatusColor(status: String): Int {
            return context.getColor(
                when (status.lowercase()) {
                    "pending" -> R.color.status_pending
                    "confirmed" -> R.color.status_confirmed
                    "canceled" -> R.color.status_canceled
                    else -> R.color.status_pending
                }
            )
        }
    }

    private fun toggleExpansion(bookingDetails: GuestBookingDetails) {
        // Neues Item mit umgekehrtem isExpanded Status erstellen
        val updatedItem = bookingDetails.copy(isExpanded = !bookingDetails.isExpanded)

        // Position finden und Item aktualisieren
        val position = currentList.indexOf(bookingDetails)
        if (position != -1) {
            // Neue Liste erstellen mit dem aktualisierten Item
            val newList = currentList.toMutableList().apply {
                set(position, updatedItem)
            }
            submitList(newList)
        }
    }

    private fun buildAddressString(contact: BookingInvoiceContact): String {
        return buildString {
            append(contact.address)
            append(", ")
            append(contact.zip)
            append(" ")
            append(contact.city)
            if (contact.country.isNotBlank()) {
                append(", ")
                append(contact.country)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        return BookingViewHolder(
            ItemGuestBookingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<GuestBookingDetails>() {
        override fun areItemsTheSame(
            oldItem: GuestBookingDetails,
            newItem: GuestBookingDetails
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: GuestBookingDetails,
            newItem: GuestBookingDetails
        ): Boolean {
            return oldItem == newItem
        }
    }
}