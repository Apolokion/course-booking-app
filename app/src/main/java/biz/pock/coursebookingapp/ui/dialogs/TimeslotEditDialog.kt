package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.DialogTimeslotEditBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class TimeslotEditDialog : DialogFragment() {

    private var _binding: DialogTimeslotEditBinding? = null
    private val binding get() = _binding!!

    private var courses: List<Course> = emptyList()
    private var locations: List<Location> = emptyList()
    private var selectedCourseId: String? = null
    private var selectedLocationId: String? = null

    private var courseId: String? = null
    private var locationId: String? = null
    private var onSave: ((Timeslot) -> Unit)? = null
    private var isEditMode = false

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val displayTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun setOnSaveListener(listener: (Timeslot) -> Unit) {
        onSave = listener
    }

    companion object {
        private const val ARG_TIMESLOT = "arg_timeslot"
        private const val ARG_SELECTED_COURSE_ID = "arg_selected_course_id"
        private const val ARG_SELECTED_LOCATION_ID = "arg_selected_location_id"
        private const val ARG_COURSES = "arg_courses"
        private const val ARG_ALL_LOCATIONS = "arg_all_locations"

        fun newInstance(
            timeslot: Timeslot? = null,
            selectedCourseId: String? = null,
            selectedLocationId: String? = null,
            courses: List<Course>,
            locations: List<Location>
        ) = TimeslotEditDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMESLOT, timeslot)
                putString(ARG_SELECTED_COURSE_ID, selectedCourseId)
                putString(ARG_SELECTED_LOCATION_ID, selectedLocationId)
                putParcelableArrayList(ARG_COURSES, ArrayList(courses))
                putParcelableArrayList(ARG_ALL_LOCATIONS, ArrayList(locations))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTimeslotEditBinding.inflate(layoutInflater)

        val timeslot = arguments?.getParcelable<Timeslot>(ARG_TIMESLOT)
        courseId = arguments?.getString(ARG_SELECTED_COURSE_ID)
        locationId = arguments?.getString(ARG_SELECTED_LOCATION_ID)
        isEditMode = timeslot != null

        @Suppress("DEPRECATION")
        courses = arguments?.getParcelableArrayList(ARG_COURSES) ?: emptyList()
        @Suppress("DEPRECATION")
        locations = arguments?.getParcelableArrayList(ARG_ALL_LOCATIONS) ?: emptyList()

        // Wichtig: Erst Dropdowns setup, dann Views
        setupDropdowns()
        setupViews(timeslot)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEditMode) R.string.edit_timeslot else R.string.create_timeslot)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    val positiveButton = (dialog as AlertDialog)
                        .getButton(AlertDialog.BUTTON_POSITIVE)
                    positiveButton.setOnClickListener {
                        if (validateAndSave()) {
                            dialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun setupDropdowns() {
        if (isEditMode) return

        binding.apply {
            // --- KURS-DROPDOWN ---
            val courseAdapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                courses.map { it.title }
            )
            dropdownCourse.setAdapter(courseAdapter)

            // Location-Dropdown zuerst deaktivieren
            dropdownLocation.isEnabled = false
            dropdownLocation.setText("", false)

            // Falls schon ein vorausgewählter Kurs existiert (zB via selectedCourseId),
            // dann direkt die Location-Liste aktualisieren:
            courseId?.let { preselectedId ->
                val idx = courses.indexOfFirst { it.id == preselectedId }
                if (idx != -1) {
                    // Text setzen und Location-Dropdown laden
                    dropdownCourse.setText(courses[idx].title, false)
                    updateLocationDropdown(courses[idx])
                }
            }

            // REAGIERT AUF KURS-AUSWAHL
            dropdownCourse.setOnItemClickListener { _, _, position, _ ->
                val selectedCourse = courses[position]
                selectedCourseId = selectedCourse.id
                updateLocationDropdown(selectedCourse)
            }
        }
    }

    private fun updateLocationDropdown(course: Course) {
        binding.apply {
            val courseLocations = course.locations ?: emptyList()

            if (courseLocations.isEmpty()) {
                // Keine Locations: Deaktivieren, Text zurücksetzen
                dropdownLocation.isEnabled = false
                dropdownLocation.setText("", false)
                selectedLocationId = null
                return
            }

            // Ansonsten: Dropdown aktivieren und befüllen
            dropdownLocation.isEnabled = true

            // Adapter neu erstellen
            val locationAdapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                courseLocations.map { it.name }
            )
            dropdownLocation.setAdapter(locationAdapter)

            // Standardmäßig den 1. Eintrag auswählen
            val firstLocation = courseLocations[0]
            dropdownLocation.setText(firstLocation.name, false)
            selectedLocationId = firstLocation.id


            dropdownLocation.setOnItemClickListener { _, _, pos, _ ->
                val chosen = courseLocations[pos]
                selectedLocationId = chosen.id
            }
        }
    }

    private fun setupViews(timeslot: Timeslot?) {
        binding.apply {
            // DropDown Felder für Kurs und Location nur anzeigen, wenn
            // neuer Timeslot erstellt wird
            val showDropdowns = !isEditMode
            textInputCourse.visibility = if (showDropdowns) View.VISIBLE else View.GONE
            textInputLocation.visibility = if (showDropdowns) View.VISIBLE else View.GONE


            // Checkbox für optionale Zeitangaben
            // Wichtig: Erst die Visibility der Zeitfelder setzen basierend auf existierenden Zeiten
            timeslot?.let { slot ->
                editTextStartDate.setText(slot.startDate)
                editTextEndDate.setText(slot.endDate)
                editTextCapacity.setText(slot.maxCapacity.toString())

                // Zeitfelder nur anzeigen wenn sie im Timeslot vorhanden sind
                if (!slot.startTime.isNullOrEmpty() && !slot.endTime.isNullOrEmpty()) {
                    textInputStartTime.visibility = View.VISIBLE
                    textInputEndTime.visibility = View.VISIBLE
                    checkboxUseTime.isChecked = true
                    editTextStartTime.setText(displayTimeFormatter.format(timeFormatter.parse(slot.startTime)))
                    editTextEndTime.setText(displayTimeFormatter.format(timeFormatter.parse(slot.endTime)))
                } else {
                    textInputStartTime.visibility = View.GONE
                    textInputEndTime.visibility = View.GONE
                    checkboxUseTime.isChecked = false
                }
            } ?: run {
                // Bei neuem Timeslot das aktuelle Datum vorbelegen
                val currentDate = dateFormatter.format(Calendar.getInstance().time)
                editTextStartDate.setText(currentDate)
                editTextEndDate.setText(currentDate)
                checkboxUseTime.isChecked = false
                textInputStartTime.visibility = View.GONE
                textInputEndTime.visibility = View.GONE
            }

            // Checkbox Listener NACH dem Setup der initialen Werte
            checkboxUseTime.setOnCheckedChangeListener { _, isChecked ->
                textInputStartTime.visibility = if (isChecked) View.VISIBLE else View.GONE
                textInputEndTime.visibility = if (isChecked) View.VISIBLE else View.GONE

                // Wenn eingeblendet und Zeitwerte vorhanden, diese wieder setzen
                if (isChecked) {
                    timeslot?.let { slot ->
                        if (!slot.startTime.isNullOrEmpty() && !slot.endTime.isNullOrEmpty()) {
                            editTextStartTime.setText(
                                displayTimeFormatter.format(
                                    timeFormatter.parse(
                                        slot.startTime
                                    )
                                )
                            )
                            editTextEndTime.setText(
                                displayTimeFormatter.format(
                                    timeFormatter.parse(
                                        slot.endTime
                                    )
                                )
                            )
                        }
                    }
                    setupTimePickers()
                }
            }

            setupDatePickers()
            // TimePickers immer setup'en, Sichtbarkeit wird über Checkbox gesteuert
            setupTimePickers()
        }
    }

    private fun validateAndSave(): Boolean {
        val startDate = binding.editTextStartDate.text.toString()
        val endDate = binding.editTextEndDate.text.toString()
        val capacity = binding.editTextCapacity.text.toString()

        if (startDate.isBlank()) {
            binding.textInputStartDate.error = getString(R.string.error_start_date_required)
            return false
        }
        if (endDate.isBlank()) {
            binding.textInputEndDate.error = getString(R.string.error_end_date_required)
            return false
        }
        if (capacity.isBlank() || capacity.toIntOrNull() == null || capacity.toInt() <= 0) {
            binding.textInputCapacity.error = getString(R.string.error_capacity_required)
            return false
        }

        // Im Edit-Mode verwenden wir die bestehenden IDs
        val finalCourseId = if (isEditMode) {
            courseId
        } else {
            // Im Create-Mode nehmen wir die aus den Dropdowns gewählten Werte
            selectedCourseId ?: courseId
        }

        val finalLocationId = if (isEditMode) {
            locationId
        } else {
            // Im Create-Mode nehmen wir die aus den Dropdowns gewählten Werte
            selectedLocationId ?: locationId
        }

        // Validierung der IDs
        if (finalCourseId == null) {
            binding.textInputCourse.error = getString(R.string.error_select_course_and_location)
            return false
        }
        if (finalLocationId == null) {
            binding.textInputLocation.error = getString(R.string.error_select_course_and_location)
            return false
        }

        // Optional Zeitfelder validieren
        var startTime: String? = null
        var endTime: String? = null

        if (binding.checkboxUseTime.isChecked) {
            val startTimeText = binding.editTextStartTime.text.toString()
            val endTimeText = binding.editTextEndTime.text.toString()

            if (startTimeText.isBlank()) {
                binding.textInputStartTime.error = getString(R.string.error_start_time_required)
                return false
            }
            if (endTimeText.isBlank()) {
                binding.textInputEndTime.error = getString(R.string.error_end_time_required)
                return false
            }

            startTime = "$startTimeText:00"
            endTime = "$endTimeText:00"
        }

        val timeslotToSave = Timeslot(
            id = if (isEditMode) (arguments?.getParcelable<Timeslot>(ARG_TIMESLOT)?.id
                ?: "") else "",
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            maxCapacity = capacity.toInt(),
            courseId = finalCourseId,
            locationId = finalLocationId
        )

        Timber.d(">>> DDD timeslot = $timeslotToSave")

        onSave?.invoke(timeslotToSave)
        return true
    }

    private fun setupDatePickers() {
        binding.apply {
            // Start Date Picker
            editTextStartDate.setOnClickListener {
                val startDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_start_date))
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build()

                startDatePicker.addOnPositiveButtonClickListener { selection ->
                    val date = dateFormatter.format(Date(selection))
                    editTextStartDate.setText(date)

                    // Wenn noch kein Enddatum gesetzt ist, setzen wir es auf das gleiche Datum
                    if (editTextEndDate.text.isNullOrEmpty()) {
                        editTextEndDate.setText(date)
                    }
                }

                startDatePicker.show(childFragmentManager, "start_date_picker")
            }

            // End Date Picker
            editTextEndDate.setOnClickListener {
                // Parse start date für Min-Date Constraint
                val startDate = editTextStartDate.text?.toString()?.let { dateStr ->
                    try {
                        dateFormatter.parse(dateStr)?.time
                    } catch (e: Exception) {
                        null
                    }
                } ?: MaterialDatePicker.todayInUtcMilliseconds()

                val endDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_end_date))
                    .setSelection(startDate)
                    .setCalendarConstraints(
                        CalendarConstraints.Builder()
                            .setStart(startDate)
                            .build()
                    )
                    .build()

                endDatePicker.addOnPositiveButtonClickListener { selection ->
                    val date = dateFormatter.format(Date(selection))
                    editTextEndDate.setText(date)
                }

                endDatePicker.show(childFragmentManager, "end_date_picker")
            }
        }
    }

    private fun setupTimePickers() {
        binding.apply {
            // Start Time Picker
            editTextStartTime.setOnClickListener {
                val calendar = Calendar.getInstance()

                // Aktuelles Datum parsen falls vorhanden
                editTextStartTime.text?.toString()?.let { time ->
                    try {
                        val parsed = displayTimeFormatter.parse(time)
                        if (parsed != null) {
                            calendar.time = parsed
                        }
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error parsing start time")
                    }
                }

                val picker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText(getString(R.string.select_start_time))
                    .build()

                picker.addOnPositiveButtonClickListener {
                    val time = String.format("%02d:%02d", picker.hour, picker.minute)
                    editTextStartTime.setText(time)

                    // Wenn noch keine Endzeit gesetzt ist, setzen sie auf eine Stunde später
                    if (editTextEndTime.text.isNullOrEmpty()) {
                        calendar.set(Calendar.HOUR_OF_DAY, picker.hour)
                        calendar.set(Calendar.MINUTE, picker.minute)
                        calendar.add(Calendar.HOUR_OF_DAY, 1)
                        editTextEndTime.setText(
                            String.format(
                                "%02d:%02d",
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE)
                            )
                        )
                    }
                }

                picker.show(childFragmentManager, "start_time_picker")
            }

            // End Time Picker
            editTextEndTime.setOnClickListener {
                val calendar = Calendar.getInstance()

                // Aktuelle Zeit parsen falls vorhanden
                editTextEndTime.text?.toString()?.let { time ->
                    try {
                        val parsed = displayTimeFormatter.parse(time)
                        if (parsed != null) {
                            calendar.time = parsed
                        }
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error parsing end time")
                    }
                }

                val picker = MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText(getString(R.string.select_end_time))
                    .build()

                picker.addOnPositiveButtonClickListener {
                    val time = String.format("%02d:%02d", picker.hour, picker.minute)
                    editTextEndTime.setText(time)
                }

                picker.show(childFragmentManager, "end_time_picker")
            }
        }
    }

}