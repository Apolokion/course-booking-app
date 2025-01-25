package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.FragmentTrainerTimeslotsBinding
import biz.pock.coursebookingapp.ui.adapters.dashboard.TimeslotListAdapter
import biz.pock.coursebookingapp.ui.dialogs.TimeslotEditDialog
import biz.pock.coursebookingapp.ui.dialogs.TrainerSelectionDialog
import biz.pock.coursebookingapp.ui.swiper.TrainerSwipeActionCallback
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TrainerDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TrainerTimeslotsFragment : FilterableFragment() {

    private var _binding: FragmentTrainerTimeslotsBinding? = null
    private val binding get() = _binding!!

    override val viewModel: TrainerDashboardViewModel by activityViewModels()

    private val timeslotAdapter by lazy {
        TimeslotListAdapter(
            viewModel = viewModel,
            fragment = this,
            onTimeslotClick = { timeslot, view -> showTimeslotOptions(timeslot, view) }
        )
    }


    override fun applyCurrentFilters() {
        viewModel.onTabSelected(2)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                courseId = viewModel.selectedCourseId.value,
                locationId = viewModel.selectedLocationId.value
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerTimeslotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilter()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTimeslots.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            Timber.w(">>> SPAN $spanCount")
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = timeslotAdapter

            ItemTouchHelper(
                TrainerSwipeActionCallback(
                    context = requireContext(),
                    onEdit = { timeslot ->
                        showTimeslotDialog(timeslot)
                    },
                    getItem = { position -> timeslotAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    private fun setupFab() {
        binding.fabAddTimeslot.setOnClickListener {
            showTimeslotDialog(null)
        }
    }

    private fun setupFilter() {
        binding.apply {
            buttonFilterCourse.apply {
                val currentCourse = viewModel.dashboardData.value.courses.find {
                    it.id == viewModel.selectedCourseId.value
                }

                if (currentCourse != null) {
                    text = currentCourse.title
                    updateFilterButtonState(this, true)
                } else {
                    text = getString(R.string.hint_select_course)
                    updateFilterButtonState(this, false)
                }
                setOnClickListener { view -> showCourseFilterMenu(view) }
            }

            buttonFilterLocation.apply {
                val currentLocation = if (viewModel.selectedCourseId.value != null) {
                    viewModel.dashboardData.value.selectedCourseLocations.find {
                        it.id == viewModel.selectedLocationId.value
                    }
                } else {
                    viewModel.dashboardData.value.locations.find {
                        it.id == viewModel.selectedLocationId.value
                    }
                }

                if (currentLocation != null) {
                    text = currentLocation.name
                    updateFilterButtonState(this, true)
                } else {
                    text = getString(R.string.hint_select_location)
                    updateFilterButtonState(this, false)
                }
                setOnClickListener { view -> showLocationFilterMenu(view) }
            }

            buttonResetFilters.setOnClickListener {
                clearFilters()
            }
        }
        updateActiveFiltersVisibility()
    }

    private fun updateFilterButtonState(button: MaterialButton, isSelected: Boolean) {
        button.setStyle(
            if (isSelected) R.style.Widget_App_Button_Filter_Selected
            else R.style.Widget_App_Button_Filter
        )
    }

    private fun MaterialButton.setStyle(@StyleRes styleResId: Int) {
        val typedArray = context.obtainStyledAttributes(styleResId, R.styleable.FilterButton)
        try {
            val backgroundTint = typedArray.getColor(
                R.styleable.FilterButton_filterBackgroundTint,
                context.getColor(R.color.filter_button_background)
            )
            val textColor = typedArray.getColor(
                R.styleable.FilterButton_filterTextColor,
                context.getColor(R.color.on_surface)
            )
            val iconTintColor = typedArray.getColor(
                R.styleable.FilterButton_filterIconTint,
                context.getColor(R.color.on_surface)
            )

            backgroundTintList = ColorStateList.valueOf(backgroundTint)
            setTextColor(textColor)
            iconTint = ColorStateList.valueOf(iconTintColor)
        } finally {
            typedArray.recycle()
        }
    }

    private fun showCourseFilterMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_timeslot_course_filter, popup.menu)
        MaterialPopupMenuHelper.stylePopupMenu(popup, requireContext())

        viewModel.dashboardData.value.allCourses.forEach { course ->
            popup.menu.add(course.title)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when {
                menuItem.itemId == R.id.filter_course_all -> {
                    viewModel.handleAction(
                        TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                            courseId = null,
                            locationId = null
                        )
                    )
                    binding.buttonFilterCourse.apply {
                        text = getString(R.string.hint_select_course)
                        updateFilterButtonState(this, false)
                    }
                    binding.buttonFilterLocation.apply {
                        text = getString(R.string.hint_select_location)
                        updateFilterButtonState(this, false)
                    }
                    true
                }

                else -> {
                    val selectedCourse = viewModel.dashboardData.value.allCourses
                        .find { it.title == menuItem.title }

                    selectedCourse?.let { course ->
                        viewModel.loadLocationsForCourse(course.id)

                        viewModel.handleAction(
                            TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                                courseId = course.id,
                                locationId = null
                            )
                        )
                        binding.buttonFilterCourse.apply {
                            text = course.title
                            updateFilterButtonState(this, true)
                        }
                        binding.buttonFilterLocation.apply {
                            text = getString(R.string.hint_select_location)
                            updateFilterButtonState(this, false)
                        }
                        true
                    } ?: false
                }
            }
        }
        popup.show()
    }

    private fun showLocationFilterMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        MaterialPopupMenuHelper.stylePopupMenu(popup, requireContext())

        val availableLocations = if (viewModel.selectedCourseId.value != null) {
            viewModel.dashboardData.value.selectedCourseLocations
        } else {
            viewModel.dashboardData.value.locations
        }

        // Standard "Alle" Option
        popup.menu.add(getString(R.string.hint_select_location))

        // Alle verfügbaren Locations hinzufügen
        availableLocations.forEach { location ->
            popup.menu.add(location.name)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title.toString()) {
                getString(R.string.hint_select_location) -> {
                    viewModel.handleAction(
                        TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                            courseId = viewModel.selectedCourseId.value,
                            locationId = null
                        )
                    )
                    binding.buttonFilterLocation.apply {
                        text = menuItem.title
                        updateFilterButtonState(this, false)
                    }
                    true
                }

                else -> {
                    val location = availableLocations.find { it.name == menuItem.title }
                    location?.let {
                        viewModel.handleAction(
                            TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                                courseId = viewModel.selectedCourseId.value,
                                locationId = it.id
                            )
                        )
                        binding.buttonFilterLocation.apply {
                            text = it.name
                            updateFilterButtonState(this, true)
                        }
                        true
                    } ?: false
                }
            }
        }
        popup.show()
    }

    private fun clearFilters() {
        binding.apply {
            buttonFilterCourse.apply {
                text = getString(R.string.hint_select_course)
                updateFilterButtonState(this, false)
            }
            buttonFilterLocation.apply {
                text = getString(R.string.hint_select_location)
                updateFilterButtonState(this, false)
            }
        }

        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.FilterTimeslots(
                courseId = null,
                locationId = null
            )
        )
        updateActiveFiltersVisibility()
    }

    private fun updateActiveFiltersVisibility() {
        binding.buttonResetFilters.visibility = if (
            viewModel.selectedCourseId.value != null ||
            viewModel.selectedLocationId.value != null
        ) View.VISIBLE else View.GONE
    }

    private fun showTimeslotOptions(timeslot: Timeslot, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_timeslot_options, menu)
            MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())

            // Icons / Farben anpassen
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_edit,
                R.color.on_surface
            )
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_manage_trainers,
                R.color.on_surface
            )
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_delete,
                R.color.error
            )
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showTimeslotDialog(timeslot)
                    true
                }

                R.id.action_manage_trainers -> {
                    showTrainerSelectionDialog(timeslot)
                    true
                }

                R.id.action_delete -> {
                    confirmDeleteTimeslot(timeslot)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showTrainerSelectionDialog(timeslot: Timeslot) {
        lifecycleScope.launch {
            try {
                // TimeEntries mit Users laden
                val timeEntries =
                    viewModel.courseRepository.getTimeslotTimeEntriesWithUsers(timeslot.id)

                // Alle Trainer IDs sammeln
                val selectedTrainerIds =
                    timeEntries.flatMap { it.users?.map { user -> user.id } ?: emptyList() }.toSet()

                // Dialog mit bereits ausgewählten Trainern anzeigen
                TrainerSelectionDialog.newInstance(
                    timeslotId = timeslot.id,
                    selectedTrainerIds = selectedTrainerIds
                ).apply {
                    setOnTrainerChangeListener { trainerCount ->
                        dialog?.setTitle(
                            getString(R.string.manage_trainers_with_count, trainerCount)
                        )
                    }
                }.show(childFragmentManager, "trainer_selection")

            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading timeslot time entries")
                AlertUtils.showError(textRes = R.string.error_loading_trainers)
            }
        }
    }

    private fun showTimeslotDialog(timeslot: Timeslot?) {
        val courseId = timeslot?.courseId ?: viewModel.selectedCourseId.value
        val locationId = timeslot?.locationId ?: viewModel.selectedLocationId.value

        // Debug Logs
        Timber.d(">>> Showing TimeslotDialog")
        Timber.d(">>> Available courses: ${viewModel.dashboardData.value.courses.size}")
        Timber.d(">>> Available locations: ${viewModel.dashboardData.value.allLocations}")
        Timber.d(">>> Selected Course ID: $courseId")
        Timber.d(">>> Selected Location ID: $locationId")

        TimeslotEditDialog.newInstance(
            timeslot = timeslot,
            selectedCourseId = courseId,
            selectedLocationId = locationId,
            courses = viewModel.dashboardData.value.allCourses,
            locations = viewModel.dashboardData.value.allLocations
        ).apply {
            setOnSaveListener { savedTimeslot ->
                if (timeslot == null) {
                    viewModel.handleAction(
                        TrainerDashboardViewModel.TrainerAction.CreateTimeslot(savedTimeslot)
                    )
                } else {
                    viewModel.handleAction(
                        TrainerDashboardViewModel.TrainerAction.UpdateTimeslot(savedTimeslot)
                    )
                }
            }
        }.show(childFragmentManager, "timeslot_edit")
    }

    private fun confirmDeleteTimeslot(timeslot: Timeslot) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_timeslot_title)
            .setMessage(getString(R.string.dialog_delete_timeslot_message))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.handleAction(
                    TrainerDashboardViewModel.TrainerAction.DeleteTimeslot(timeslot.id)
                )
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Timeslots beobachten
                launch {
                    viewModel.dashboardData
                        .map { it.timeslots }
                        .collect { timeslots ->
                            timeslotAdapter.submitList(timeslots)
                            updateEmptyState(timeslots)
                        }
                }

                // Filter-States beobachten
                launch {
                    viewModel.selectedCourseId.collect { courseId ->
                        val course = viewModel.dashboardData.value.allCourses.find {
                            it.id == courseId
                        }
                        binding.buttonFilterCourse.apply {
                            text = course?.title ?: getString(R.string.hint_select_course)
                            updateFilterButtonState(this, course != null)
                        }
                    }
                }

                launch {
                    viewModel.selectedLocationId.collect { locationId ->
                        val location = if (viewModel.selectedCourseId.value != null) {
                            viewModel.dashboardData.value.selectedCourseLocations.find {
                                it.id == locationId
                            }
                        } else {
                            viewModel.dashboardData.value.locations.find {
                                it.id == locationId
                            }
                        }
                        binding.buttonFilterLocation.apply {
                            text = location?.name ?: getString(R.string.hint_select_location)
                            updateFilterButtonState(this, location != null)
                        }
                        updateActiveFiltersVisibility()
                    }
                }

                // UI State beobachten
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is BaseDashboardViewModel.UiState.Loading -> {
                                binding.textEmpty.visibility = View.GONE
                                binding.progressBar.visibility = View.VISIBLE
                            }

                            else -> {
                                binding.progressBar.visibility = View.GONE
                                // Empty State wird in updateEmptyState() gesteuert
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(timeslots: List<Timeslot>) {
        binding.textEmpty.apply {
            // Empty State nur anzeigen wenn keine Timeslots UND nicht im Loading
            visibility = if (timeslots.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_timeslots_message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}