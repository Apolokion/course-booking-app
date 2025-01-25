package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.content.res.ColorStateList
import android.content.res.Configuration
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
import biz.pock.coursebookingapp.databinding.FragmentAdminTimeslotsBinding
import biz.pock.coursebookingapp.ui.adapters.dashboard.TimeslotListAdapter
import biz.pock.coursebookingapp.ui.dialogs.TimeslotEditDialog
import biz.pock.coursebookingapp.ui.dialogs.TrainerSelectionDialog
import biz.pock.coursebookingapp.ui.swiper.SwipeActionCallback
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.AdminDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
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
class AdminTimeslotsFragment : FilterableFragment() {

    private var _binding: FragmentAdminTimeslotsBinding? = null
    private val binding get() = _binding!!

    override val viewModel: AdminDashboardViewModel by activityViewModels()

    private val timeslotAdapter by lazy {
        TimeslotListAdapter(
            viewModel = viewModel,
            fragment = this,
            onTimeslotClick = { timeslot, view -> showTimeslotOptions(timeslot, view) }
        )
    }

    // Von FilterableFragment implementierte Methode, um bei einem
    // Fragment resume die voreingestellten Filter anzuwenden
    override fun applyCurrentFilters() {
        // Tab Index korrekt setzen für Pull-To-Refresh Action
        viewModel.onTabSelected(4)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)
        filterTimeslots()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminTimeslotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilter()
        observeViewModel()
        // Neue Methode für Filter-Beobachtung, da ansonsten der
        // Status der Buttons und der Filter Status für die Abfrage
        // nicht erhalten bleibt
        observeFilterStates()

        binding.fabAddTimeslot.setOnClickListener {
            showTimeslotDialog(null)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTimeslots.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = timeslotAdapter

            // Swipe-Funktionalität hinzufügen
            ItemTouchHelper(
                SwipeActionCallback(
                    context = requireContext(),
                    onDelete = { timeslot, resetSwipeState ->
                        confirmDeleteTimeslot(timeslot, resetSwipeState)
                    },
                    onEdit = { timeslot ->
                        showTimeslotDialog(timeslot)
                    },
                    getItem = { position -> timeslotAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun observeFilterStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Beobachte Course Filter
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

                // Beobachte Location Filter
                launch {
                    viewModel.selectedLocationId.collect { locationId ->
                        val location = if (viewModel.selectedCourseId.value != null) {
                            viewModel.dashboardData.value.selectedCourseLocations.find {
                                it.id == locationId
                            }
                        } else {
                            viewModel.dashboardData.value.allLocations.find {
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
            }
        }
    }

    private fun setupFilter() {
        binding.apply {
            buttonFilterCourse.setOnClickListener { view ->
                showCourseFilterMenu(view)
            }
            buttonFilterLocation.setOnClickListener { view ->
                showLocationFilterMenu(view)
            }
            buttonResetFilters.setOnClickListener {
                clearFilters()
            }
        }
    }

    private fun clearFilters() {
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.FilterTimeslots(null, null)
        )
    }

    private fun updateActiveFiltersVisibility() {
        binding.buttonResetFilters.visibility = if (
            viewModel.selectedCourseId.value != null ||
            viewModel.selectedLocationId.value != null
        ) View.VISIBLE else View.GONE
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
                        AdminDashboardViewModel.AdminAction.FilterTimeslots(
                            courseId = null,
                            locationId = null
                        )
                    )
                    true
                }

                else -> {
                    val selectedCourse = viewModel.dashboardData.value.allCourses
                        .find { it.title == menuItem.title }

                    selectedCourse?.let { course ->
                        viewModel.loadLocationsForCourse(course.id)

                        viewModel.handleAction(
                            AdminDashboardViewModel.AdminAction.FilterTimeslots(
                                courseId = course.id,
                                locationId = null
                            )
                        )
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
            viewModel.dashboardData.value.allLocations
        }

        popup.menu.add(getString(R.string.hint_select_location))
        availableLocations.forEach { location ->
            popup.menu.add(location.name)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title.toString()) {
                getString(R.string.hint_select_location) -> {
                    viewModel.handleAction(
                        AdminDashboardViewModel.AdminAction.FilterTimeslots(
                            courseId = viewModel.selectedCourseId.value,
                            locationId = null
                        )
                    )
                    true
                }

                else -> {
                    val location = availableLocations.find { it.name == menuItem.title }
                    location?.let {
                        viewModel.handleAction(
                            AdminDashboardViewModel.AdminAction.FilterTimeslots(
                                courseId = viewModel.selectedCourseId.value,
                                locationId = it.id
                            )
                        )
                        true
                    } ?: false
                }
            }
        }
        popup.show()
    }

    private fun filterTimeslots() {
        Timber.w(">>> TAB -> filterTimeslots ${viewModel.currentTab.value}}")
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.FilterTimeslots(
                courseId = viewModel.selectedCourseId.value,
                locationId = viewModel.selectedLocationId.value
            )
        )
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

    private fun showTimeslotOptions(timeslot: Timeslot, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_timeslot_options_admin, menu)
            MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())

            // Delete-Item einfärben
            DropDownUtils.tintMenuItem(requireContext(), this, R.id.action_delete, R.color.error)
            // Edit-Item einfärben
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_edit,
                R.color.on_background
            )
            // Manage Trainers-Item einfärben
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_manage_trainers,
                R.color.on_background
            )
        }

        popup.setOnMenuItemClickListener { menuItem ->
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
        popup.show()
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
        // Die courseId und locationId kommen jetzt aus den ausgewählten Filter-Werten
        val courseId = timeslot?.courseId ?: viewModel.selectedCourseId.value
        val locationId = timeslot?.locationId ?: viewModel.selectedLocationId.value

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
                        AdminDashboardViewModel.AdminAction.CreateTimeslot(savedTimeslot)
                    )
                } else {
                    viewModel.handleAction(
                        AdminDashboardViewModel.AdminAction.UpdateTimeslot(savedTimeslot)
                    )
                }
            }
        }.show(childFragmentManager, "timeslot_edit")
    }

    private fun confirmDeleteTimeslot(timeslot: Timeslot, resetSwipeState: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_timeslot_title)
            .setMessage(getString(R.string.dialog_delete_timeslot_message))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.handleAction(
                    AdminDashboardViewModel.AdminAction.DeleteTimeslot(timeslot.id)
                )
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> resetSwipeState() }
            .setOnCancelListener { resetSwipeState() }
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
                viewModel.dashboardData
                    .map { it.timeslots }
                    .collect { timeslots ->
                        timeslotAdapter.submitList(timeslots)
                        updateEmptyState(timeslots)
                    }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BaseDashboardViewModel.UiState.Loading -> {
                            binding.textEmpty.visibility = View.GONE
                        }

                        else -> {
                            // Empty State wird in updateEmptyState() gesteuert
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(timeslots: List<Timeslot>) {
        binding.textEmpty.apply {
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