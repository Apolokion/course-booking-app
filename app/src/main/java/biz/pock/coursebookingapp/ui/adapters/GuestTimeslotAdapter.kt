package biz.pock.coursebookingapp.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.ItemGuestTimeslotBinding
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class GuestTimeslotAdapter(
    private val onTimeslotBook: (Timeslot) -> Unit,
    private val viewModel: GuestDashboardViewModel,
    private val context: Context
) : ListAdapter<Timeslot, GuestTimeslotAdapter.TimeslotViewHolder>(TimeslotDiffCallback()) {

    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private val expandedTimeslots = mutableMapOf<String, Boolean>()
    private val initialVisibleEntries = 2

    inner class TimeslotViewHolder(
        private val binding: ItemGuestTimeslotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var timeEntryAdapter: GuestTimeEntryAdapter? = null

        fun bind(timeslot: Timeslot) {
            binding.apply {
                try {
                    // Kurs- und Standortinformationen
                    val course = viewModel.guestCourses.value.find { it.id == timeslot.courseId }
                    val location = viewModel.availableLocations.value.find { it.id == timeslot.locationId }

                    // Kursinfos setzen
                    textCourseTitle.text = course?.title ?: context.getString(R.string.unknown_course)
                    textCourseTitle.contentDescription = context.getString(
                        R.string.course_title_format,
                        course?.title ?: context.getString(R.string.unknown_course)
                    )

                    // Standort setzen
                    textLocation.text = location?.name ?: context.getString(R.string.unknown_location)
                    textLocation.contentDescription = context.getString(
                        R.string.location_name_format,
                        location?.name ?: context.getString(R.string.unknown_location)
                    )

                    // Datumsbereich
                    textDateRange.text = formatDateRange(timeslot)
                    textDateRange.contentDescription = context.getString(
                        R.string.date_format_accessibility,
                        formatDateRange(timeslot)
                    )

                    // Kapazitätsinfos
                    setupCapacityInfo(timeslot)

                    // Buchungs-Button
                    setupBookButton(timeslot)

                    // TimeEntries RecyclerView
                    setupTimeEntriesList(timeslot)

                    // Accessibility für die Card
                    root.contentDescription = buildFullDescription(context, timeslot)

                } catch (e: Exception) {
                    Timber.e(e, ">>> Error binding timeslot: ${timeslot.id}")
                    setFallbackValues()
                }
            }
        }

        private fun setupCapacityInfo(timeslot: Timeslot) {
            binding.apply {
                val capacityText = context.getString(
                    R.string.capacity_format,
                    timeslot.filledParticipants,
                    timeslot.maxCapacity
                )
                textCapacity.text = capacityText
                textCapacity.contentDescription = context.getString(
                    R.string.course_participants_desc,
                    timeslot.filledParticipants,
                    timeslot.maxCapacity
                )

                val percentage = calculateCapacityPercentage(timeslot)
                progressCapacity.apply {
                    progress = percentage
                    progressTintList = ColorStateList.valueOf(getCapacityColor(percentage))
                }
            }
        }

        private fun setupBookButton(timeslot: Timeslot) {
            val availableSpots = timeslot.maxCapacity - timeslot.filledParticipants
            binding.buttonBook.apply {
                isEnabled = availableSpots > 0
                text = if (availableSpots > 0) {
                    context.getString(R.string.book_course_preview)
                } else {
                    context.getString(R.string.no_spots_available)
                }
                contentDescription = if (availableSpots > 0) {
                    context.getString(R.string.button_reserve_desc, availableSpots)
                } else {
                    context.getString(R.string.no_spots_available)
                }
                setOnClickListener {
                    if (availableSpots > 0) {
                        onTimeslotBook(timeslot)
                    }
                }
            }
        }

        private fun setupTimeEntriesList(timeslot: Timeslot) {
            binding.recyclerViewTimeEntries.apply {
                if (layoutManager == null) {
                    layoutManager = LinearLayoutManager(context)
                }

                if (timeEntryAdapter == null) {
                    timeEntryAdapter = GuestTimeEntryAdapter(context)
                    adapter = timeEntryAdapter
                }

                timeslot.timeEntries?.let { entries ->
                    val isExpanded = expandedTimeslots[timeslot.id] ?: false
                    val visibleEntries =
                        if (isExpanded) entries else entries.take(initialVisibleEntries)
                    timeEntryAdapter?.submitList(visibleEntries)

                    // Expand/Collapse Button
                    setupExpandCollapseButton(timeslot, entries, isExpanded)
                } ?: run {
                    timeEntryAdapter?.submitList(emptyList())
                    binding.buttonExpandCollapse.visibility = View.GONE
                }
            }
        }

        private fun setupExpandCollapseButton(
            timeslot: Timeslot,
            entries: List<TimeEntry>,
            isExpanded: Boolean
        ) {
            binding.buttonExpandCollapse.apply {
                visibility = if (entries.size > initialVisibleEntries) View.VISIBLE else View.GONE
                text = context.getString(
                    if (isExpanded) R.string.collapse_entries
                    else R.string.expand_entries
                )
                setIconResource(
                    if (isExpanded) R.drawable.ic_arrow_up
                    else R.drawable.ic_arrow_down
                )
                setOnClickListener {
                    expandedTimeslots[timeslot.id] = !isExpanded
                    notifyItemChanged(currentList.indexOf(timeslot))
                }
            }
        }

        private fun formatDateRange(timeslot: Timeslot): String {
            val startDate = formatDate(timeslot.startDate)
            val endDate = formatDate(timeslot.endDate)
            return if (startDate == endDate) startDate else "$startDate - $endDate"
        }

        private fun formatDate(date: String?): String {
            return date?.let { dateString ->
                try {
                    apiDateFormatter.parse(dateString)?.let {
                        displayDateFormatter.format(it)
                    } ?: dateString
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error parsing date")
                    context.getString(R.string.unknown_date)
                }
            } ?: context.getString(R.string.unknown_date)
        }

        private fun calculateCapacityPercentage(timeslot: Timeslot): Int {
            return if (timeslot.maxCapacity > 0) {
                ((timeslot.filledParticipants.toFloat() / timeslot.maxCapacity.toFloat()) * 100).toInt()
            } else 0
        }

        private fun getCapacityColor(percentage: Int): Int {
            return context.getColor(
                when {
                    percentage >= 90 -> R.color.error
                    percentage >= 75 -> R.color.status_pending
                    else -> R.color.status_confirmed
                }
            )
        }

        private fun buildFullDescription(context: Context, timeslot: Timeslot): String {
            return context.getString(
                R.string.timeslot_details_format,
                binding.textCourseTitle.text,
                binding.textLocation.text,
                binding.textDateRange.text,
                binding.textCapacity.text
            )
        }

        private fun setFallbackValues() {
            binding.apply {
                textCourseTitle.text = context.getString(R.string.unknown_course)
                textLocation.text = context.getString(R.string.unknown_location)
                textDateRange.text = context.getString(R.string.unknown_date)
                textCapacity.text = context.getString(R.string.no_spots_available)
                progressCapacity.progress = 0
                buttonBook.isEnabled = false
                timeEntryAdapter?.submitList(emptyList())
                buttonExpandCollapse.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeslotViewHolder {
        val binding = ItemGuestTimeslotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeslotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeslotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class TimeslotDiffCallback : DiffUtil.ItemCallback<Timeslot>() {
        override fun areItemsTheSame(oldItem: Timeslot, newItem: Timeslot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Timeslot, newItem: Timeslot): Boolean {
            return oldItem == newItem
        }
    }
}