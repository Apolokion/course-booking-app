package biz.pock.coursebookingapp.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.DialogTimeEntryEditBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimeEntryDialog : DialogFragment() {

    private var _binding: DialogTimeEntryEditBinding? = null
    private val binding get() = _binding!!

    private var timeEntry: TimeEntry? = null
    private var timeslot: Timeslot? = null
    private var onSave: ((TimeEntry) -> Unit)? = null

    // Formatter für Datum und Zeit
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val displayTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        private const val ARG_TIMESLOT = "arg_timeslot"
        private const val ARG_TIME_ENTRY = "arg_time_entry"

        fun newInstance(timeslot: Timeslot, timeEntry: TimeEntry? = null) = TimeEntryDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMESLOT, timeslot)
                putParcelable(ARG_TIME_ENTRY, timeEntry)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTimeEntryEditBinding.inflate(layoutInflater)

        timeslot = arguments?.getParcelable(ARG_TIMESLOT)
            ?: throw IllegalArgumentException(getString(R.string.error_timeslot_required))
        timeEntry = arguments?.getParcelable(ARG_TIME_ENTRY)

        setupViews()
        setupDateTimePickers()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (timeEntry == null) R.string.create_time_entry else R.string.edit_time_entry)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        if (validateAndSave()) {
                            dialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun setupViews() {
        // Datums-Range Info anzeigen
        binding.textDateRange.text = buildDateRangeText()

        // Default Zeiten Info anzeigen falls vorhanden
        setupDefaultTimesInfo()

        timeEntry?.let { entry ->
            binding.apply {
                // API Format zu Display Format konvertieren für existierenden Eintrag
                try {
                    val date = apiDateFormatter.parse(entry.date)
                    editTextDate.setText(date?.let { displayDateFormatter.format(it) })
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error parsing date")
                    editTextDate.setText(entry.date)
                }

                // Start- und Endzeit
                try {
                    editTextStartTime.setText(entry.startTime?.substring(0, 5))
                    editTextEndTime.setText(entry.endTime?.substring(0, 5))
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error parsing time")
                    editTextStartTime.setText(entry.startTime)
                    editTextEndTime.setText(entry.endTime)
                }
            }
        } ?: setupDefaultValues()

        // Clear Icons Click Listener
        setupClearButtons()
    }


    private fun buildDateRangeText(): String {
        return try {
            val startDate = timeslot?.startDate?.let {
                apiDateFormatter.parse(it)?.let { date ->
                    displayDateFormatter.format(date)
                }
            } ?: return ""

            val endDate = timeslot?.endDate?.let {
                apiDateFormatter.parse(it)?.let { date ->
                    displayDateFormatter.format(date)
                }
            } ?: startDate

            return if (startDate == endDate) {
                getString(R.string.valid_date, startDate)
            } else {
                getString(R.string.valid_range, startDate, endDate)
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error building date range text")
            ""
        }
    }

    private fun setupDefaultTimesInfo() {
        if (!timeslot?.startTime.isNullOrEmpty() && !timeslot?.endTime.isNullOrEmpty()) {
            binding.textDefaultTimes.apply {
                visibility = View.VISIBLE
                text = getString(
                    R.string.default_times_from_timeslot,
                    timeslot?.startTime?.substring(0, 5),
                    timeslot?.endTime?.substring(0, 5)
                )
            }
        } else {
            binding.textDefaultTimes.visibility = View.GONE
        }
    }

    @SuppressLint("DefaultLocale")
    private fun setupDefaultValues() {
        binding.apply {
            // Aktuelles Datum als Standard, wenn im gültigen Bereich
            val today = Calendar.getInstance().time
            if (isDateInRange(today)) {
                editTextDate.setText(displayDateFormatter.format(today))
            } else {
                // Wenn heute außerhalb der Range, dann Timeslot-Startdatum
                timeslot?.startDate?.let { startDate ->
                    try {
                        val date = apiDateFormatter.parse(startDate)
                        editTextDate.setText(date?.let { displayDateFormatter.format(it) })
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error parsing timeslot start date")
                    }
                }
            }

            // Zeiten vom Timeslot übernehmen falls vorhanden
            if (!timeslot?.startTime.isNullOrEmpty() && !timeslot?.endTime.isNullOrEmpty()) {
                editTextStartTime.setText(timeslot?.startTime?.substring(0, 5))
                editTextEndTime.setText(timeslot?.endTime?.substring(0, 5))
            } else {
                // Aktuelle Zeit + 1h
                val cal = Calendar.getInstance()
                editTextStartTime.setText(String.format("%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)))

                cal.add(Calendar.HOUR_OF_DAY, 1)
                editTextEndTime.setText(String.format("%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE)))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Aktualisiere die Range-Info falls sich das Datum ändert
        binding.textDateRange.text = buildDateRangeText()
    }

    private fun setupClearButtons() {
        binding.apply {
            // Date Clear Button
            textInputDate.setEndIconOnClickListener {
                editTextDate.text?.clear()
            }

            // Start Time Clear Button
            textInputStartTime.setEndIconOnClickListener {
                editTextStartTime.text?.clear()
                // Endzeit auch löschen
                editTextEndTime.text?.clear()
            }

            // End Time Clear Button
            textInputEndTime.setEndIconOnClickListener {
                editTextEndTime.text?.clear()
            }
        }
    }

    private fun setupDateTimePickers() {
        // Date Picker
        binding.editTextDate.setOnClickListener {
            val currentDate = try {
                binding.editTextDate.text?.toString()?.let { dateStr ->
                    displayDateFormatter.parse(dateStr)?.time
                }
            } catch (e: Exception) {
                null
            } ?: MaterialDatePicker.todayInUtcMilliseconds()

            // Constraints für Datums-Range vom Timeslot
            val startDate = timeslot?.startDate?.let { dateStr ->
                apiDateFormatter.parse(dateStr)?.time
            } ?: currentDate

            val endDate = timeslot?.endDate?.let { dateStr ->
                apiDateFormatter.parse(dateStr)?.time
            } ?: currentDate

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(currentDate)
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build()
                )
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                binding.editTextDate.setText(displayDateFormatter.format(date))
            }

            picker.show(childFragmentManager, "date_picker")
        }

        // Time Pickers
        setupTimePicker(isStartTime = true)
        setupTimePicker(isStartTime = false)
    }

    @SuppressLint("DefaultLocale")
    private fun setupTimePicker(isStartTime: Boolean) {
        val editText = if (isStartTime) binding.editTextStartTime else binding.editTextEndTime

        editText.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Aktuelle Zeit aus EditText parsen
            editText.text?.toString()?.let { timeStr ->
                try {
                    val time = displayTimeFormatter.parse(timeStr)
                    calendar.time = time
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error parsing time")
                }
            }

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText(if (isStartTime) R.string.select_start_time else R.string.select_end_time)
                .build()

            picker.addOnPositiveButtonClickListener {
                val time = String.format("%02d:%02d", picker.hour, picker.minute)
                editText.setText(time)

                // Bei Startzeit automatisch Endzeit +1h setzen
                if (isStartTime && binding.editTextEndTime.text.isNullOrEmpty()) {
                    calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
                    calendar.set(Calendar.MINUTE, picker.minute)
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                    binding.editTextEndTime.setText(
                        String.format("%02d:%02d",
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE))
                    )
                }
            }

            picker.show(childFragmentManager, "time_picker")
        }
    }

    private fun validateAndSave(): Boolean {
        val dateStr = binding.editTextDate.text.toString()
        val startTimeStr = binding.editTextStartTime.text.toString()
        val endTimeStr = binding.editTextEndTime.text.toString()

        // Validierung Datum
        if (dateStr.isBlank()) {
            binding.textInputDate.error = getString(R.string.error_date_required)
            return false
        }

        // Datum im Timeslot-Bereich validieren
        try {
            val date = displayDateFormatter.parse(dateStr)
            if (!isDateInRange(date)) {
                binding.textInputDate.error = getString(R.string.error_date_range)
                binding.textDateRange.setTextColor(resources.getColor(R.color.error, null))
                return false
            } else {
                binding.textInputDate.error = null
                binding.textDateRange.setTextColor(resources.getColor(R.color.on_surface_variant, null))
            }
        } catch (e: Exception) {
            binding.textInputDate.error = getString(R.string.error_invalid_date_format)
            return false
        }

        // Zeiten validieren
        if (startTimeStr.isBlank()) {
            binding.textInputStartTime.error = getString(R.string.error_start_time_required)
            return false
        }
        if (endTimeStr.isBlank()) {
            binding.textInputEndTime.error = getString(R.string.error_end_time_required)
            return false
        }

        // Validiere dass Endzeit nach Startzeit liegt
        try {
            val startTime = displayTimeFormatter.parse(startTimeStr)
            val endTime = displayTimeFormatter.parse(endTimeStr)
            if (startTime != null && endTime != null && !endTime.after(startTime)) {
                binding.textInputEndTime.error = getString(R.string.error_end_time_after_start_time)
                return false
            }
        } catch (e: Exception) {
            binding.textInputStartTime.error = getString(R.string.error_invalid_time_format)
            binding.textInputEndTime.error = getString(R.string.error_invalid_time_format)
            return false
        }

        // API Format konvertieren und TimeEntry erstellen
        val apiDate = try {
            val date = displayDateFormatter.parse(dateStr)
            date?.let { apiDateFormatter.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }

        val timeEntryToSave = TimeEntry(
            id = timeEntry?.id ?: "",
            date = apiDate,
            startTime = "$startTimeStr:00",
            endTime = "$endTimeStr:00",
            timeslotId = timeslot?.id ?: ""
        )

        onSave?.invoke(timeEntryToSave)
        return true
    }

    private fun isDateInRange(date: Date?): Boolean {
        if (date == null) return false

        try {
            val startDate = timeslot?.startDate?.let { apiDateFormatter.parse(it) }
            val endDate = timeslot?.endDate?.let { apiDateFormatter.parse(it) }

            return when {
                startDate == null || endDate == null -> true
                date.before(startDate) -> false
                date.after(endDate) -> false
                else -> true
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error validating date range")
            return false
        }
    }

    fun setOnSaveListener(listener: (TimeEntry) -> Unit) {
        onSave = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}