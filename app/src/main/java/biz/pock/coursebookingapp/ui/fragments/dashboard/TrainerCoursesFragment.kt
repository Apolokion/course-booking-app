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
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.CourseUpdate
import biz.pock.coursebookingapp.databinding.FragmentTrainerCoursesBinding
import biz.pock.coursebookingapp.shared.enums.AgeGroup
import biz.pock.coursebookingapp.shared.enums.CourseStatus
import biz.pock.coursebookingapp.shared.enums.CourseType
import biz.pock.coursebookingapp.ui.adapters.dashboard.CourseListAdapter
import biz.pock.coursebookingapp.ui.dialogs.CourseEditDialog
import biz.pock.coursebookingapp.ui.dialogs.LocationSelectionDialog
import biz.pock.coursebookingapp.ui.swiper.TrainerSwipeActionCallback
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TrainerDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TrainerCoursesFragment : FilterableFragment() {

    private var _binding: FragmentTrainerCoursesBinding? = null
    private val binding get() = _binding!!

    override val viewModel: TrainerDashboardViewModel by activityViewModels()

    private val courseAdapter = CourseListAdapter { course, view ->
        showCourseDetails(course, view)
    }

    // Filter States
    private var selectedType: CourseType? = null
    private var selectedStatus: CourseStatus? = null
    private var selectedAgeGroup: AgeGroup? = null

    override fun applyCurrentFilters() {

        viewModel.onTabSelected(0)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.FilterCourses(
                type = viewModel.selectedCourseType.value,
                status = viewModel.selectedCourseStatus.value,
                ageGroup = viewModel.selectedAgeGroup.value
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainerCoursesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilter()
        updateActiveFiltersVisibility()
        observeViewModel()

        // FAB ausblenden
        binding.fabAddCourse.visibility = View.GONE

        binding.layoutFilters.buttonResetFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCourses.apply {
            // Spaltenanzahl basierend auf der Bildschirmbreite berechnen
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = courseAdapter

            ItemTouchHelper(
                TrainerSwipeActionCallback(
                    context = requireContext(),
                    onEdit = { course ->
                        showEditCourseDialog(course)
                    },
                    getItem = { position -> courseAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // RecyclerView Layout neu setzen bei Orientierungsänderung
        setupRecyclerView()
    }

    private fun setupFilter() {
        binding.layoutFilters.apply {
            // Initial states aus ViewModel wiederherstellen
            val currentType = viewModel.selectedCourseType.value?.let { typeStr ->
                CourseType.fromApiString(typeStr)
            }
            val currentStatus = viewModel.selectedCourseStatus.value?.let { statusStr ->
                CourseStatus.fromApiString(statusStr)
            }
            val currentAgeGroup = viewModel.selectedAgeGroup.value?.let { groupStr ->
                AgeGroup.fromApiString(groupStr)
            }

            // Type Filter Button
            buttonFilterType.apply {
                text = if (currentType != null) {
                    updateFilterButtonState(this, true)
                    context.getString(currentType.resId)
                } else {
                    updateFilterButtonState(this, false)
                    getString(R.string.filter_type_all)
                }
                setOnClickListener { view -> showTypeFilterMenu(view) }
            }

            // Status Filter Button
            buttonFilterStatus.apply {
                text = if (currentStatus != null) {
                    updateFilterButtonState(this, true)
                    context.getString(currentStatus.resId)
                } else {
                    updateFilterButtonState(this, false)
                    getString(R.string.filter_status_all)
                }
                setOnClickListener { view -> showStatusFilterMenu(view) }
            }

            // Age Group Filter Button
            buttonFilterAgeGroup.apply {
                text = if (currentAgeGroup != null) {
                    updateFilterButtonState(this, true)
                    context.getString(currentAgeGroup.resId)
                } else {
                    updateFilterButtonState(this, false)
                    getString(R.string.filter_age_group_all)
                }
                setOnClickListener { view -> showAgeGroupFilterMenu(view) }
            }

            // Reset Filter Button
            buttonResetFilters.setOnClickListener {
                clearFilters()
            }
        }
        updateActiveFiltersVisibility()
    }

    private fun showTypeFilterMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_course_type_filter, popup.menu)
        MaterialPopupMenuHelper.stylePopupMenu(popup, requireContext())

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.filter_type_all -> {
                    selectedType = null
                    binding.layoutFilters.buttonFilterType.apply {
                        text = getString(R.string.filter_type_all)
                        updateFilterButtonState(this, false)
                    }
                    updateFilters()
                    true
                }

                R.id.filter_type_public -> {
                    selectedType = CourseType.PUBLIC
                    binding.layoutFilters.buttonFilterType.apply {
                        text = getString(R.string.filter_type_public)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                R.id.filter_type_private -> {
                    selectedType = CourseType.PRIVATE
                    binding.layoutFilters.buttonFilterType.apply {
                        text = getString(R.string.filter_type_private)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showStatusFilterMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        MaterialPopupMenuHelper.stylePopupMenu(popup, requireContext())

        // Status-Menü: Alle, Published und Archived (kein Draft)
        popup.menu.add(getString(R.string.filter_status_all))
        popup.menu.add(getString(R.string.filter_status_published))
        popup.menu.add(getString(R.string.filter_status_archived))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title.toString()) {
                getString(R.string.filter_status_all) -> {
                    selectedStatus = null
                    binding.layoutFilters.buttonFilterStatus.apply {
                        text = getString(R.string.filter_status_all)
                        updateFilterButtonState(this, false)
                    }
                    updateFilters()
                    true
                }

                getString(R.string.filter_status_published) -> {
                    selectedStatus = CourseStatus.PUBLISHED
                    binding.layoutFilters.buttonFilterStatus.apply {
                        text = getString(R.string.filter_status_published)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                getString(R.string.filter_status_archived) -> {
                    selectedStatus = CourseStatus.ARCHIVED
                    binding.layoutFilters.buttonFilterStatus.apply {
                        text = getString(R.string.filter_status_archived)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showAgeGroupFilterMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_course_age_group_filter, popup.menu)
        MaterialPopupMenuHelper.stylePopupMenu(popup, requireContext())

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.filter_age_group_all -> {
                    selectedAgeGroup = null
                    binding.layoutFilters.buttonFilterAgeGroup.apply {
                        text = getString(R.string.filter_age_group_all)
                        updateFilterButtonState(this, false)
                    }
                    updateFilters()
                    true
                }

                R.id.filter_age_group_mixed -> {
                    selectedAgeGroup = AgeGroup.MIXED
                    binding.layoutFilters.buttonFilterAgeGroup.apply {
                        text = getString(R.string.filter_age_group_mixed)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                R.id.filter_age_group_children -> {
                    selectedAgeGroup = AgeGroup.CHILDREN_AND_TEENS
                    binding.layoutFilters.buttonFilterAgeGroup.apply {
                        text = getString(R.string.filter_age_group_children)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                R.id.filter_age_group_adults -> {
                    selectedAgeGroup = AgeGroup.ADULTS
                    binding.layoutFilters.buttonFilterAgeGroup.apply {
                        text = getString(R.string.filter_age_group_adults)
                        updateFilterButtonState(this, true)
                    }
                    updateFilters()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showCourseDetails(course: Course, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_course_options_trainer, menu)
            MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())

            // Icon Farben anpassen
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_edit,
                R.color.on_surface
            )
            DropDownUtils.tintMenuItem(
                requireContext(),
                this,
                R.id.action_manage_locations,
                R.color.on_surface
            )
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditCourseDialog(course)
                    true
                }

                R.id.action_manage_locations -> {
                    showLocationSelectionDialog(course)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showEditCourseDialog(course: Course) {
        CourseEditDialog.newInstance(course).apply {
            setOnSaveListener { savedCourse ->
                val courseUpdate = CourseUpdate.fromCourse(savedCourse)
                viewModel.handleAction(
                    TrainerDashboardViewModel.TrainerAction.UpdateCourse(courseUpdate)
                )
            }
        }.show(childFragmentManager, "course_edit")
    }

    private fun showLocationSelectionDialog(course: Course) {
        lifecycleScope.launch {
            try {
                val currentLocations = viewModel.courseRepository.getCourseLocations(course.id)
                val currentLocationIds = currentLocations.map { it.id }.toSet()

                LocationSelectionDialog.newInstance(
                    courseId = course.id,
                    selectedLocationIds = currentLocationIds
                ).show(childFragmentManager, "location_selection")
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading course locations")
                AlertUtils.showError(textRes = R.string.error_loading_locations)
            }
        }
    }

    private fun clearFilters() {
        binding.layoutFilters.apply {
            buttonFilterType.apply {
                text = getString(R.string.filter_type_all)
                updateFilterButtonState(this, false)
            }
            buttonFilterStatus.apply {
                text = getString(R.string.filter_status_all)
                updateFilterButtonState(this, false)
            }
            buttonFilterAgeGroup.apply {
                text = getString(R.string.filter_age_group_all)
                updateFilterButtonState(this, false)
            }
        }

        viewModel.clearFilters()
        updateActiveFiltersVisibility()
    }

    private fun updateFilters() {
        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.FilterCourses(
                type = selectedType?.let { CourseType.toApiString(it) },
                status = selectedStatus?.let { CourseStatus.toApiString(it) },
                ageGroup = selectedAgeGroup?.let { AgeGroup.toApiString(it) }
            )
        )
        updateActiveFiltersVisibility()
    }

    private fun updateActiveFiltersVisibility() {
        binding.layoutFilters.buttonResetFilters.visibility = if (
            viewModel.selectedCourseType.value != null ||
            viewModel.selectedCourseStatus.value != null ||
            viewModel.selectedAgeGroup.value != null
        ) View.VISIBLE else View.GONE
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardData
                    .map { it.courses }
                    .collect { courses ->
                        courseAdapter.submitList(courses)
                        updateEmptyState(courses)
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

    private fun updateEmptyState(courses: List<Course>) {
        binding.textEmpty.apply {
            visibility = if (courses.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_courses_message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}