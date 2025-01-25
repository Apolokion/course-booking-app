package biz.pock.coursebookingapp.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.databinding.ItemGuestTimeEntryBinding
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class GuestTimeEntryAdapter(
    private val context: Context
) : ListAdapter<TimeEntry, GuestTimeEntryAdapter.TimeEntryViewHolder>(TimeEntryDiffCallback()) {

    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    inner class TimeEntryViewHolder(
        private val binding: ItemGuestTimeEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeEntry: TimeEntry) {
            binding.apply {
                try {
                    // Zeitbereich und Datum kombiniert
                    val timeRange = buildTimeRangeText(timeEntry)
                    val date = formatDate(timeEntry.date)

                    textTimeRange.text = context.getString(
                        R.string.time_entry_date_format,
                        timeRange,
                        date
                    )

                    textTimeRange.contentDescription = context.getString(
                        R.string.time_entry_accessibility_format,
                        timeRange,
                        date
                    )

                } catch (e: Exception) {
                    Timber.e(e, ">>> Error binding time entry: ${timeEntry.id}")
                    setFallbackValues()
                }
            }
        }

        private fun buildTimeRangeText(timeEntry: TimeEntry): String {
            val startTime = timeEntry.startTime?.substring(0, 5) ?: "--:--"
            val endTime = timeEntry.endTime?.substring(0, 5) ?: "--:--"
            return "$startTime - $endTime"
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

        private fun setFallbackValues() {
            binding.textTimeRange.text = context.getString(
                R.string.time_entry_date_format,
                "--:-- - --:--",
                context.getString(R.string.unknown_date)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeEntryViewHolder {
        val binding = ItemGuestTimeEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
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