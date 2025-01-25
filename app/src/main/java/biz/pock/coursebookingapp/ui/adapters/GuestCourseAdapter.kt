package biz.pock.coursebookingapp.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.ItemGuestCourseBinding
import biz.pock.coursebookingapp.shared.enums.AgeGroup
import biz.pock.coursebookingapp.utils.AlertUtils
import com.google.android.material.chip.Chip
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class GuestCourseAdapter(
    private val onCourseClick: (Course) -> Unit,
    private val onLocationClick: (Course, Location) -> Unit,
    private val context: Context
) : ListAdapter<Course, GuestCourseAdapter.CourseViewHolder>(CourseDiffCallback()) {

    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private data class DateRange(
        val startDate: Date,
        val endDate: Date
    )

    inner class CourseViewHolder(
        private val binding: ItemGuestCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            binding.apply {
                // Grundlegende Kursinformationen
                textTitle.text = course.title
                textDescription.text = course.description

                // Preis formatieren
                textPrice.text = root.context.getString(
                    R.string.price_per_participant,
                    course.pricePerParticipant
                )

                // Altersgruppe
                setupAgeGroup(course)

                // Verfügbarkeit und Kapazität
                setupAvailabilityAndCapacity(course)

                // Teilnehmerinfo
                setupParticipantsInfo(course)

                // Location Chips
                setupLocationChips(course)

                // Click Listener für gesamtes Item
                root.setOnClickListener {
                    handleCourseClick(course)
                }

                // Accessibility
                setupAccessibility(course)

                // Verfügbare Dates anzeigen
                setupDateRanges(course)
            }
        }

        private fun setupDateRanges(course: Course) {
            try {
                // Alle Timeslots des Kurses sammeln und nach Datum sortieren
                val dateRanges = course.timeslots
                    ?.filter { isTimeslotValid(it) }
                    ?.mapNotNull { timeslot ->
                        // Startdatum und Enddatum parsen
                        val startDate = parseDate(timeslot.startDate)
                        val endDate = parseDate(timeslot.endDate)
                        if (startDate != null && endDate != null) {
                            DateRange(startDate, endDate)
                        } else null
                    }
                    ?.sortedBy { it.startDate }
                    ?.mergeDateRanges() // Überlappende Bereiche zusammenfügen

                if (!dateRanges.isNullOrEmpty()) {
                    binding.textDateRanges.apply {
                        visibility = View.VISIBLE
                        text = buildDateRangesText(dateRanges)
                        contentDescription = context.getString(
                            R.string.course_date_ranges_desc,
                            text
                        )
                    }
                } else {
                    binding.textDateRanges.visibility = View.GONE
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error setting up date ranges")
                binding.textDateRanges.visibility = View.GONE
            }
        }

        private fun isTimeslotValid(timeslot: Timeslot): Boolean {
            val today = LocalDate.now()
            return try {
                val endDate = LocalDate.parse(timeslot.endDate)
                endDate >= today
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing date for timeslot ${timeslot.id}")
                false
            }
        }

        private fun parseDate(dateStr: String?): Date? {
            return try {
                dateStr?.let { apiDateFormatter.parse(it) }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing date: $dateStr")
                null
            }
        }

        private fun buildDateRangesText(dateRanges: List<DateRange>): String {
            return dateRanges.joinToString("\n") { range ->
                if (range.startDate == range.endDate) {
                    displayDateFormatter.format(range.startDate)
                } else {
                    "${displayDateFormatter.format(range.startDate)} - ${
                        displayDateFormatter.format(
                            range.endDate
                        )
                    }"
                }
            }
        }

        private fun List<DateRange>.mergeDateRanges(): List<DateRange> {
            if (isEmpty()) return emptyList()

            val merged = mutableListOf<DateRange>()
            var current = this[0]

            for (i in 1 until size) {
                val next = this[i]
                if (current.endDate >= next.startDate) {
                    // Bereiche überlappen sich
                    current = current.copy(endDate = maxOf(current.endDate, next.endDate))
                } else {
                    // Kein überlappen, also weiter und saven
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged
        }


        private fun setupAgeGroup(course: Course) {
            val ageGroup = AgeGroup.fromApiString(course.ageGroup ?: "mixed")
            binding.textAgeGroup.apply {
                text = ageGroup?.let { context.getString(it.resId) }
                    ?: course.ageGroup?.replaceFirstChar { it.uppercase() }
                            ?: context.getString(R.string.age_group_mixed)
            }
        }

        private fun setupAvailabilityAndCapacity(course: Course) {
            val isFullyBooked = !checkAvailability(course)
            binding.textAvailability.apply {
                text = context.getString(
                    if (isFullyBooked) R.string.course_fully_booked
                    else R.string.course_available
                )
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (isFullyBooked) R.color.status_canceled
                        else R.color.status_confirmed
                    )
                )
            }
        }

        private fun setupParticipantsInfo(course: Course) {
            var totalCapacity = 0
            var totalFilledSpots = 0

            course.timeslots?.forEach { timeslot ->
                if (!isTimeslotExpired(timeslot)) {
                    totalCapacity += timeslot.maxCapacity
                    totalFilledSpots += timeslot.filledParticipants
                }
            }

            if (totalCapacity > 0) {
                binding.apply {
                    // Kapazitätstext
                    textCapacity.apply {
                        visibility = View.VISIBLE
                        text = context.getString(
                            R.string.spots_available,
                            totalCapacity - totalFilledSpots,
                            totalCapacity
                        )
                    }

                    // Progress Bar
                    progressCapacity.apply {
                        visibility = View.VISIBLE
                        progress =
                            ((totalFilledSpots.toFloat() / totalCapacity.toFloat()) * 100).toInt()
                        progressTintList = ColorStateList.valueOf(
                            context.getColor(
                                when {
                                    totalFilledSpots >= totalCapacity -> R.color.error
                                    totalFilledSpots >= totalCapacity * 0.8f -> R.color.status_pending
                                    else -> R.color.status_confirmed
                                }
                            )
                        )
                    }
                }
            } else {
                binding.apply {
                    textCapacity.visibility = View.GONE
                    progressCapacity.visibility = View.GONE
                }
            }
        }

        private fun setupLocationChips(course: Course) {
            binding.chipGroupLocations.removeAllViews()

            val isAvailable = checkAvailability(course)
            course.locations?.forEach { location ->
                val chip = Chip(binding.root.context).apply {
                    text = location.name
                    isCheckable = false
                    isClickable = true
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setChipBackgroundColorResource(R.color.filter_button_background)

                    setOnClickListener {
                        if (isAvailable) {
                            onLocationClick(course, location)
                        } else {
                            showFullyBookedDialog()
                        }
                    }
                }
                binding.chipGroupLocations.addView(chip)
            }

            binding.chipGroupLocations.visibility =
                if (course.locations.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        private fun setupAccessibility(course: Course) {
            val courseType = when (course.type.lowercase()) {
                "public" -> context.getString(R.string.course_type_public)
                "private" -> context.getString(R.string.course_type_private)
                else -> course.type
            }

            binding.root.contentDescription = context.getString(
                R.string.course_details_format,
                course.title,                                    // Course name
                courseType,                                      // Type
                if (checkAvailability(course))                    // Status
                    context.getString(R.string.course_available)
                else
                    context.getString(R.string.course_fully_booked),
                binding.textPrice.text                          // Price
            )
        }

        private fun handleCourseClick(course: Course) {
            if (checkAvailability(course)) {
                onCourseClick(course)
            } else {
                showFullyBookedDialog()
            }
        }

        private fun showFullyBookedDialog() {
            AlertUtils.showInfo(
                titleRes = R.string.course_fully_booked,
                textRes = R.string.course_fully_booked_message
            )
        }

        private fun checkAvailability(course: Course): Boolean {
            var totalSpots = 0
            var bookedSpots = 0

            course.timeslots?.forEach { timeslot ->
                if (!isTimeslotExpired(timeslot)) {
                    totalSpots += timeslot.maxCapacity
                    bookedSpots += timeslot.filledParticipants
                }
            }

            return totalSpots > bookedSpots
        }

        private fun isTimeslotExpired(timeslot: Timeslot): Boolean {
            val today = LocalDate.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            return try {
                val endDate = LocalDate.parse(timeslot.endDate, dateFormatter)
                endDate.isBefore(today)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing date for timeslot ${timeslot.id}")
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemGuestCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class CourseDiffCallback : DiffUtil.ItemCallback<Course>() {
        override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem == newItem
        }
    }
}