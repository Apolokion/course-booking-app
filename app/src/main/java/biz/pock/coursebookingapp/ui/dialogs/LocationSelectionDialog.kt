package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.CourseLocation
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.data.repositories.LocationRepository
import biz.pock.coursebookingapp.databinding.DialogLocationSelectionBinding
import biz.pock.coursebookingapp.utils.AlertUtils
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LocationSelectionDialog : DialogFragment() {
    private var _binding: DialogLocationSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var courseId: String
    private var selectedLocationIds = mutableSetOf<String>()

    @Inject
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var locationRepository: LocationRepository

    private var onLocationChangeListener: ((Int) -> Unit)? = null

    fun setOnLocationChangeListener(listener: (Int) -> Unit) {
        onLocationChangeListener = listener
    }

    companion object {
        private const val ARG_COURSE_ID = "arg_course_id"
        private const val ARG_SELECTED_LOCATIONS = "arg_selected_locations"

        fun newInstance(courseId: String, selectedLocationIds: Set<String>) =
            LocationSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_ID, courseId)
                    putStringArrayList(ARG_SELECTED_LOCATIONS, ArrayList(selectedLocationIds))
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLocationSelectionBinding.inflate(layoutInflater)

        courseId = arguments?.getString(ARG_COURSE_ID)
            ?: throw IllegalArgumentException("Course ID required")
        selectedLocationIds = arguments?.getStringArrayList(ARG_SELECTED_LOCATIONS)?.toMutableSet()
            ?: mutableSetOf()

        setupLocationList()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_locations)
            .setView(binding.root)
            .setPositiveButton(R.string.close, null)
            .create()
    }

    private fun setupLocationList() {
        lifecycleScope.launch {
            try {
                // Flow zu Liste konvertieren
                val allLocations =
                    locationRepository.getLocations().first()

                allLocations.forEach { location ->
                    val chip = Chip(requireContext()).apply {
                        text = location.name
                        isCheckable = true
                        isChecked = selectedLocationIds.contains(location.id)
                        // Tag für spätere Referenz
                        tag = location.id

                        setOnCheckedChangeListener { _, isChecked ->
                            // Location Objekt übergeben
                            handleLocationSelection(location, isChecked)
                        }
                    }
                    binding.chipGroupLocations.addView(chip)
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading locations")
                AlertUtils.showError(
                    textRes = R.string.error_loading_locations
                )
            }
        }
    }

    private fun handleLocationSelection(location: Location, isChecked: Boolean) {
        lifecycleScope.launch {
            try {
                if (isChecked) {
                    val courseLocation = CourseLocation(location.id)
                    courseRepository.addCourseLocation(courseId, courseLocation)
                    selectedLocationIds.add(location.id)
                } else {
                    courseRepository.removeCourseLocation(courseId, location.id)
                    selectedLocationIds.remove(location.id)
                }
                onLocationChangeListener?.invoke(selectedLocationIds.size)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error managing location selection")

                // Chip-Status zurücksetzen
                val chip = binding.chipGroupLocations.findViewWithTag<Chip>(location.id)
                chip?.isChecked = !isChecked

                AlertUtils.showError(
                    textRes = if (isChecked)
                        R.string.error_adding_location
                    else
                        R.string.error_removing_location
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}