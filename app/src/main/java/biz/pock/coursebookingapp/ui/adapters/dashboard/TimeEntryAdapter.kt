package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.databinding.ItemTimeEntryBinding
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class TimeEntryAdapter(
    private val onTimeEntryClick: (TimeEntry, View) -> Unit
) : BaseListAdapter<TimeEntry, ItemTimeEntryBinding>(TimeEntryDiffCallback()) {

    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemTimeEntryBinding {
        return ItemTimeEntryBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemTimeEntryBinding, item: TimeEntry) {
        binding.apply {
            try {
                // Zeit formatieren und setzen
                val formattedTimeRange = buildTimeRange(item, root.context)
                textTimeRange.text = formattedTimeRange
                textTimeRange.contentDescription = root.context.getString(
                    R.string.time_range_format_accessibility,
                    item.startTime?.substring(0, 5) ?: "--:--",
                    item.endTime?.substring(0, 5) ?: "--:--"
                )

                // Datum formatieren und setzen
                val formattedDate = formatDate(item.date, root.context)
                textDate.text = formattedDate
                textDate.contentDescription = root.context.getString(
                    R.string.date_format_accessibility,
                    formattedDate
                )

                // Icon Accessibility
                iconTime.contentDescription = root.context.getString(R.string.content_desc_time_icon)

                // Gesamte Item Beschreibung für Screenreader
                root.contentDescription = buildFullDescription(
                    formattedTimeRange,
                    formattedDate,
                    root.context
                )

                // Click Listener
                root.setOnClickListener { view ->
                    onTimeEntryClick(item, view)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error binding time entry: ${item.id}")
                // Fallback für fehlgeschlagenes Binding
                setFallbackValues(binding, root.context)
            }
        }
    }

    private fun buildTimeRange(timeEntry: TimeEntry, context: Context): String {
        val startTime = timeEntry.startTime?.let { time ->
            try {
                time.substring(0, 5)
            } catch (e: Exception) {
                Timber.e(e, "Error formatting start time")
                "--:--"
            }
        } ?: "--:--"

        val endTime = timeEntry.endTime?.let { time ->
            try {
                time.substring(0, 5)
            } catch (e: Exception) {
                Timber.e(e, "Error formatting end time")
                "--:--"
            }
        } ?: "--:--"

        return context.getString(R.string.time_range_format, startTime, endTime)
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

    private fun buildFullDescription(
        timeRange: String,
        date: String,
        context: Context
    ): String {
        return context.getString(
            R.string.time_range_format_accessibility,
            timeRange,
            date
        )
    }

    private fun setFallbackValues(binding: ItemTimeEntryBinding, context: Context) {
        binding.apply {
            textTimeRange.text = "--:-- - --:--"
            textDate.text = context.getString(R.string.unknown_date)
            root.contentDescription = context.getString(
                R.string.time_range_format_accessibility,
                "--:-- - --:--",
                context.getString(R.string.unknown_date)
            )
        }
    }

    private class TimeEntryDiffCallback : DiffUtil.ItemCallback<TimeEntry>() {
        override fun areItemsTheSame(oldItem: TimeEntry, newItem: TimeEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TimeEntry, newItem: TimeEntry): Boolean {
            return oldItem == newItem
        }
    }
}