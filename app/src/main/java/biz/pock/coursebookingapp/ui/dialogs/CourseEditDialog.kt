package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.data.repositories.LocationRepository
import biz.pock.coursebookingapp.databinding.DialogCourseEditBinding
import biz.pock.coursebookingapp.shared.enums.AgeGroup
import biz.pock.coursebookingapp.shared.enums.CourseType
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.validators.CourseValidator
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CourseEditDialog : DialogFragment() {

    private var totalLocationCount = 0


    private var _binding: DialogCourseEditBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var courseValidator: CourseValidator

    @Inject
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    private var course: Course? = null

    private var onSaveListener: ((Course) -> Unit)? = null

    fun setOnSaveListener(listener: (Course) -> Unit) {
        onSaveListener = listener
    }

    companion object {
        private const val ARG_COURSE = "arg_course"

        fun newInstance(course: Course? = null) = CourseEditDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_COURSE, course)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCourseEditBinding.inflate(layoutInflater)
        @Suppress("DEPRECATION")
        course = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_COURSE, Course::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_COURSE)
        }

        // Initial Locations laden
        loadInitialLocations()

        setupViews()


        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (course == null) R.string.create_course else R.string.edit_course)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonManageLocations.setOnClickListener {
            showLocationSelectionDialog()
        }
    }

    private fun loadInitialLocations() {
        lifecycleScope.launch {
            try {
                // Locations in bestimmter Reihenfolge laden
                val allLocationsDeferred = async {
                    locationRepository.getLocations().first()
                }
                val selectedLocationsDeferred = async {
                    course?.let { c ->
                        courseRepository.getCourseLocations(c.id)
                    } ?: emptyList()
                }

                // Warten bis beide Ergebnisse da sind
                val allLocations = allLocationsDeferred.await()
                val selectedLocations = selectedLocationsDeferred.await()

                totalLocationCount = allLocations.size
                updateLocationButton(selectedLocations.size)

            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading initial locations: ${e.message}")
                AlertUtils.showError(textRes = R.string.error_loading_locations)
            }
        }
    }

    private fun setupViews() {
        DropDownUtils.setupEnumDropdown(
            requireContext(),
            binding.dropdownType,
            CourseType.entries.toTypedArray()
        )

        DropDownUtils.setupEnumDropdown(
            requireContext(),
            binding.dropdownAgeGroup,
            AgeGroup.entries.toTypedArray()
        )

        // Button, um den LocationSelectionDialog zu öffnen
        binding.buttonManageLocations.setOnClickListener {
            showLocationSelectionDialog()
        }

        // Button für Location Management nur anzeigen wenn der Kurs bereits existiert
        binding.buttonManageLocations.visibility = if (course != null) View.VISIBLE else View.GONE

        // Bestehende Kursdaten ins UI laden (falls wir bearbeiten)
        course?.let { c ->
            binding.editTextTitle.setText(c.title)
            binding.editTextDescription.setText(c.description)
            binding.editTextPrice.setText(c.pricePerParticipant.toString())

            val courseType = CourseType.fromApiString(c.type)
            courseType?.let { type ->
                binding.dropdownType.setText(getString(type.resId), false)
            }

            val ageGroup = AgeGroup.fromApiString(c.ageGroup ?: "mixed")
            ageGroup?.let { group ->
                binding.dropdownAgeGroup.setText(getString(group.resId), false)
            }
        }
    }


    private fun showLocationSelectionDialog() {
        course?.let { c ->
            lifecycleScope.launch {
                try {
                    val currentLocations = courseRepository.getCourseLocations(c.id)
                    val currentLocationIds = currentLocations.map { it.id }.toSet()

                    LocationSelectionDialog.newInstance(
                        courseId = c.id,
                        selectedLocationIds = currentLocationIds
                    ).apply {
                        setOnLocationChangeListener { selectedCount ->
                            updateLocationButton(selectedCount)
                        }
                    }.show(childFragmentManager, "location_selection")
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error showing location selection: ${e.message}")
                    AlertUtils.showError(textRes = R.string.error_loading_locations)
                }
            }
        } ?: run {
            // Wenn kein Kurs existiert, Fehlermeldung anzeigen mit
            // Verweis auf Admin
            AlertUtils.showInfo(textRes = R.string.save_course_contact_admin)
        }
    }

    private fun updateLocationButton(selectedCount: Int) {
        binding.buttonManageLocations.post {
            binding.buttonManageLocations.text = getString(
                R.string.manage_locations_with_count,
                selectedCount,
                totalLocationCount
            )
        }
    }

    private fun validateAndSave(): Boolean {
        val title = binding.editTextTitle.text.toString()
        val price = binding.editTextPrice.text.toString()
        val selectedTypeText = binding.dropdownType.text.toString()
        val selectedAgeGroupText = binding.dropdownAgeGroup.text.toString()
        val description = binding.editTextDescription.text.toString()

        val validationResult = courseValidator.validate(
            title = title,
            price = price,
            type = selectedTypeText
        )

        return when (validationResult) {
            is CourseValidator.ValidationResult.Valid -> {
                val courseType = CourseType.fromLocalizedName(requireContext(), selectedTypeText)
                    ?: return false
                val ageGroup = AgeGroup.fromLocalizedName(requireContext(), selectedAgeGroupText)
                    ?: AgeGroup.MIXED

                val courseToSave = Course(
                    id = course?.id ?: "",
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    pricePerParticipant = price.toDouble(),
                    type = CourseType.toApiString(courseType),
                    status = course?.status ?: "draft",
                    ageGroup = AgeGroup.toApiString(ageGroup),
                    locations = emptyList(),
                    timeslots = emptyList()
                )

                onSaveListener?.invoke(courseToSave)
                true
            }

            is CourseValidator.ValidationResult.Invalid -> {
                // Eventuell Fehlermeldung anzeigen
                false
            }
        }
    }

    private fun setupLocationChips(locations: List<Location>) {
        binding.chipGroupLocations.removeAllViews()

        locations.forEach { location ->
            val chip = Chip(requireContext()).apply {
                text = location.name
                isChecked = true
                isCheckable = false
                setChipBackgroundColorResource(R.color.primary_container)
                setTextColor(ContextCompat.getColor(context, R.color.on_primary_container))
                setOnCloseIconClickListener {
                    // Später entfernen, weil wir das Management
                    // in den LocationSelectionDialog verschieben
                    removeLocation(location)
                }
                isCloseIconVisible = true
            }
            binding.chipGroupLocations.addView(chip)
        }
    }

    private fun loadCourseLocations() {
        course?.let { c ->
            lifecycleScope.launch {
                try {
                    val locations = courseRepository.getCourseLocations(c.id)
                    setupLocationChips(locations)
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error loading course locations: ${e.message}")
                    AlertUtils.showError(textRes = R.string.error_loading_locations)
                }
            }
        }
    }

    private fun removeLocation(location: Location) {
        course?.let { c ->
            lifecycleScope.launch {
                try {
                    courseRepository.removeCourseLocation(c.id, location.id)
                    // Chips neu laden
                    loadCourseLocations()
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error removing location: ${e.message}")
                    AlertUtils.showError(textRes = R.string.error_removing_location)
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}