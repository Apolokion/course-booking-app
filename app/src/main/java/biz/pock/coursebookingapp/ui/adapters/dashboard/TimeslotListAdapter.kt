package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.ItemTimeslotBinding
import biz.pock.coursebookingapp.shared.enums.CourseStatus
import biz.pock.coursebookingapp.ui.dialogs.TimeEntriesDialog
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TimeslotViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class TimeslotListAdapter(
    private val viewModel: TimeslotViewModel,
    private val fragment: Fragment,
    private val onTimeslotClick: (Timeslot, View) -> Unit
) : BaseListAdapter<Timeslot, ItemTimeslotBinding>(TimeslotDiffCallback()) {

    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val expandedTimeslots = mutableMapOf<String, Boolean>()
    private val initialVisibleEntries = 2

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemTimeslotBinding {
        return ItemTimeslotBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemTimeslotBinding, item: Timeslot) {
        binding.apply {
            try {
                // Kurs- und Standortinformationen setzen
                setupCourseAndLocation(root.context, item, binding)

                // Datumsbereich formatieren
                setupDateRange(root.context, item, binding)

                // Kapazität anzeigen
                setupCapacityInfo(root.context, item, binding)

                // TimeEntries anzeigen
                setupTimeEntries(root.context, item, binding)

                // Accessibility für die Card
                root.apply {
                    isClickable = true
                    isFocusable = true
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    setOnClickListener { view -> onTimeslotClick(item, view) }

                    // Gesamtbeschreibung für Screenreader
                    contentDescription = buildFullDescription(context, item, binding)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error binding timeslot: ${e.message}")
                setFallbackValues(root.context, binding)
            }
        }
    }

    private fun setupCourseAndLocation(context: Context, timeslot: Timeslot, binding: ItemTimeslotBinding) {
        binding.apply {
            val course = viewModel.dashboardData.value.allCourses.find { it.id == timeslot.courseId }
            val location = viewModel.dashboardData.value.allLocations.find { it.id == timeslot.locationId }

            // Kursinfo
            textCourseTitle.text = course?.title ?: context.getString(R.string.unknown_course)
            textCourseTitle.contentDescription = context.getString(
                R.string.course_title_format,
                course?.title ?: context.getString(R.string.unknown_course)
            )

            // Standortinfo
            textLocation.apply {
                text = location?.name ?: context.getString(R.string.unknown_location)
                contentDescription = context.getString(
                    R.string.location_name_format,
                    location?.name ?: context.getString(R.string.unknown_location)
                )
                visibility = View.VISIBLE
            }

            // Status Chip
            setupStatusChip(context, course, binding)
        }
    }

    private fun setupStatusChip(context: Context, course: Course?, binding: ItemTimeslotBinding) {
        binding.textStatus.apply {
            if (course != null) {
                visibility = View.VISIBLE
                val status = CourseStatus.fromApiString(course.status)
                text = status?.let { context.getString(it.resId) }
                    ?: course.status.replaceFirstChar { it.uppercase() }

                setTextColor(getStatusColor(context, status))
                contentDescription = context.getString(
                    R.string.course_status_desc,
                    text
                )
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun setupDateRange(context: Context, timeslot: Timeslot, binding: ItemTimeslotBinding) {
        val formattedRange = formatDateRange(context, timeslot)
        binding.textDateRange.apply {
            text = formattedRange
            contentDescription = context.getString(
                R.string.date_format_accessibility,
                formattedRange
            )
        }
    }

    private fun setupCapacityInfo(context: Context, timeslot: Timeslot, binding: ItemTimeslotBinding) {
        binding.apply {
            val capacityText = context.getString(
                R.string.capacity_format,
                timeslot.filledParticipants,
                timeslot.maxCapacity
            )
            textCapacity.apply {
                text = capacityText
                contentDescription = context.getString(
                    R.string.course_participants_desc,
                    timeslot.filledParticipants,
                    timeslot.maxCapacity
                )
            }

            // Progress Bar
            val capacityPercentage = calculateCapacityPercentage(timeslot)
            progressCapacity.apply {
                progress = capacityPercentage
                setIndicatorColor(getCapacityColor(context, capacityPercentage))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        }
    }

    private fun setupTimeEntries(context: Context, timeslot: Timeslot, binding: ItemTimeslotBinding) {
        binding.apply {
            // Anzeigen der ProgressBar basierend auf dem Ladezustand der Time Entries
            progressTimeEntries.visibility = if (timeslot.timeEntries == null) View.VISIBLE else View.GONE

            containerTimeEntries.removeAllViews()
            val isExpanded = expandedTimeslots[timeslot.id] ?: false
            val entries = timeslot.timeEntries ?: emptyList()
            val visibleEntries = if (isExpanded) entries else entries.take(initialVisibleEntries)

            visibleEntries.forEach { timeEntry ->
                addTimeEntryView(context, timeEntry, containerTimeEntries)
            }

            setupTimeEntryButtons(context, timeslot, entries, isExpanded, binding)
        }
    }

    private fun setupTimeEntryButtons(
        context: Context,
        timeslot: Timeslot,
        entries: List<TimeEntry>,
        isExpanded: Boolean,
        binding: ItemTimeslotBinding
    ) {
        binding.apply {
            buttonExpandCollapse.apply {
                visibility = if (entries.size > initialVisibleEntries) View.VISIBLE else View.GONE
                text = context.getString(
                    if (isExpanded) R.string.collapse_entries else R.string.expand_entries
                )
                setIconResource(
                    if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                )
                contentDescription = text
                setOnClickListener {
                    expandedTimeslots[timeslot.id] = !isExpanded
                    notifyItemChanged(currentList.indexOf(timeslot))
                }
            }

            buttonManageTimeEntries.apply {
                text = context.getString(R.string.manage_time_entries, entries.size)
                contentDescription = text
                setOnClickListener {
                    showTimeEntriesDialog(timeslot)
                }
                isEnabled = timeslot.timeEntries != null
            }
        }
    }

    private fun addTimeEntryView(context: Context, timeEntry: TimeEntry, container: ViewGroup) {
        val timeEntryView = LayoutInflater.from(context)
            .inflate(R.layout.item_time_entry, container, false)

        val timeRangeView = timeEntryView.findViewById<TextView>(R.id.textTimeRange)
        val startTime = timeEntry.startTime?.substring(0, 5) ?: "--:--"
        val endTime = timeEntry.endTime?.substring(0, 5) ?: "--:--"
        val timeRange = "$startTime - $endTime"

        timeRangeView.apply {
            text = timeRange
            contentDescription = context.getString(
                R.string.time_range_format_accessibility,
                startTime,
                endTime
            )
        }

        val dateView = timeEntryView.findViewById<TextView>(R.id.textDate)
        val formattedDate = formatDate(timeEntry.date, context)
        dateView.apply {
            text = formattedDate
            contentDescription = context.getString(
                R.string.date_format_accessibility,
                formattedDate
            )
        }

        container.addView(timeEntryView)
    }

    private fun buildFullDescription(context: Context, timeslot: Timeslot, binding: ItemTimeslotBinding): String {
        return context.getString(
            R.string.location_details_format,
            binding.textCourseTitle.text,
            binding.textLocation.text,
            binding.textDateRange.text
        )
    }

    private fun formatDate(date: String?, context: Context): String {
        return date?.let { dateString ->
            try {
                apiDateFormatter.parse(dateString)?.let {
                    displayDateFormatter.format(it)
                } ?: dateString
            } catch (e: Exception) {
                Timber.e(e, "Error parsing date")
                context.getString(R.string.unknown_date)
            }
        } ?: context.getString(R.string.unknown_date)
    }

    private fun formatDateRange(context: Context, timeslot: Timeslot): String {
        val startDate = formatDate(timeslot.startDate, context)
        val endDate = formatDate(timeslot.endDate, context)
        return if (startDate == endDate) startDate else "$startDate - $endDate"
    }

    private fun calculateCapacityPercentage(timeslot: Timeslot): Int {
        return if (timeslot.maxCapacity > 0) {
            (timeslot.filledParticipants.toFloat() / timeslot.maxCapacity.toFloat() * 100).toInt()
        } else 0
    }

    private fun getCapacityColor(context: Context, percentage: Int): Int {
        return context.getColor(when {
            percentage >= 90 -> R.color.error
            percentage >= 75 -> R.color.status_pending
            else -> R.color.status_confirmed
        })
    }

    private fun getStatusColor(context: Context, status: CourseStatus?): Int {
        return context.getColor(when (status) {
            CourseStatus.PUBLISHED -> R.color.status_confirmed
            CourseStatus.DRAFT -> R.color.status_pending
            CourseStatus.ARCHIVED -> R.color.status_canceled
            null -> R.color.on_surface
        })
    }

    private fun setFallbackValues(context: Context, binding: ItemTimeslotBinding) {
        binding.apply {
            textCourseTitle.text = context.getString(R.string.unknown_course)
            textLocation.visibility = View.GONE
            textDateRange.text = context.getString(R.string.unknown_date)
            textCapacity.text = "0/0"
            textStatus.visibility = View.GONE
            progressTimeEntries.visibility = View.GONE
        }
    }

    private fun showTimeEntriesDialog(timeslot: Timeslot?) {
        TimeEntriesDialog.newInstance(
            timeslot = timeslot ?: throw IllegalStateException("Timeslot required"),
            timeEntries = timeslot.timeEntries ?: emptyList()
        ).apply {
            setOnCreateTimeEntry { timeslotId, entry ->
                viewModel.handleAction(
                    TimeslotViewModel.TimeslotAction.CreateTimeEntry(
                        timeslotId = timeslotId,
                        timeEntry = entry
                    )
                )
            }
            setOnUpdateTimeEntry { timeslotId, entryId, entry ->
                viewModel.handleAction(
                    TimeslotViewModel.TimeslotAction.UpdateTimeEntry(
                        timeslotId = timeslotId,
                        timeEntryId = entryId,
                        timeEntry = entry
                    )
                )
            }
            setOnDeleteTimeEntry { timeslotId, entryId ->
                viewModel.handleAction(
                    TimeslotViewModel.TimeslotAction.DeleteTimeEntry(
                        timeslotId = timeslotId,
                        timeEntryId = entryId
                    )
                )
            }
        }.show(fragment.childFragmentManager, "time_entries")
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