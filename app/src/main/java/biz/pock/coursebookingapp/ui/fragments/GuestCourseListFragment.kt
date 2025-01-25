package biz.pock.coursebookingapp.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.databinding.FragmentGuestCourseListBinding
import biz.pock.coursebookingapp.ui.adapters.GuestCourseAdapter
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.UiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GuestCourseListFragment : Fragment() {

    private var _binding: FragmentGuestCourseListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GuestDashboardViewModel by viewModels({ requireParentFragment() })

    private lateinit var courseAdapter: GuestCourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestCourseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        courseAdapter = GuestCourseAdapter(
            onCourseClick = { course ->
                navigateToTimeslotList(course)
            },
            onLocationClick = { course, location ->
                navigateToTimeslotList(course, location)
            },
            context = requireContext()
        )

        binding.recyclerViewCourses.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = courseAdapter
            setHasFixedSize(true)
        }
    }

    // Bei Konfigurationsänderungen triggern
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.primary, R.color.secondary)
            setOnRefreshListener {
                viewModel.refreshData()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefresh.isRefreshing =
                            state is GuestDashboardViewModel.UiState.Loading

                        when (state) {
                            is GuestDashboardViewModel.UiState.Error -> {
                                AlertUtils.showError(text = state.message)
                            }

                            else -> {} // Andere States werden durch die UI selbst gehandelt
                        }
                    }
                }

                launch {
                    viewModel.guestCourses.collect { courses ->
                        updateCoursesList(courses)
                    }
                }
            }
        }
    }

    private fun updateCoursesList(courses: List<Course>) {
        courseAdapter.submitList(courses)

        binding.emptyView.visibility = if (courses.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun navigateToTimeslotList(course: Course, preSelectedLocation: Location? = null) {
        val action = GuestDashboardFragmentDirections
            .actionGuestDashboardFragmentToGuestTimeslotListFragment(
                course = course,
                preSelectedLocationId = preSelectedLocation?.id
            )
        requireParentFragment().findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}