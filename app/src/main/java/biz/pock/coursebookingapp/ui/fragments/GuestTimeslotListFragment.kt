package biz.pock.coursebookingapp.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.FragmentGuestTimeslotListBinding
import biz.pock.coursebookingapp.ui.activities.MainActivity
import biz.pock.coursebookingapp.ui.adapters.GuestTimeslotAdapter
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.UiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GuestTimeslotListFragment : Fragment() {

    private var _binding: FragmentGuestTimeslotListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GuestDashboardViewModel by activityViewModels()
    private val args: GuestTimeslotListFragmentArgs by navArgs()

    private lateinit var timeslotAdapter: GuestTimeslotAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestTimeslotListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupLocationFilter()
        setupActionBar()

        // SwipeRefresh Setup
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.secondary
        )

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCourseTimeslots(
                courseId = args.course.id,
                preSelectedLocationId = args.preSelectedLocationId
            )
        }

        // Initial Aufruf zum Laden der Timeslots mit preselektierter Location
        viewModel.loadCourseTimeslots(
            courseId = args.course.id,
            preSelectedLocationId = args.preSelectedLocationId
        )

        // Custom Navigation für Up Button setzen, damit die Kursliste
        // beim zurück navigieren ausgewählt wird
        (requireActivity() as MainActivity).setCustomNavigateUpCallback {
            findNavController().navigate(
                GuestTimeslotListFragmentDirections
                    .actionGuestTimeslotListFragmentToGuestDashboardFragment(1)
            )
            true
        }

    }

    private fun setupActionBar() {
        // Cast auf AppCompatActivity ist sicher, da wir MainActivity verwenden
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        timeslotAdapter = GuestTimeslotAdapter(
            onTimeslotBook = { timeslot ->
                handleTimeslotBooking(timeslot)
            },
            context = requireContext(),
            viewModel = viewModel
        )

        binding.recyclerViewTimeslots.apply {
            // Grid Layout Manager basierend auf der Bildschirmbreite
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = timeslotAdapter
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    fun applyCurrentFilters() {
        // Aktuelle Filter wiederherstellen bei Resume
        viewModel.loadCourseTimeslots(
            courseId = args.course.id,
            preSelectedLocationId = args.preSelectedLocationId
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        binding.swipeRefresh.isRefreshing =
                            uiState is GuestDashboardViewModel.UiState.Loading
                        when (uiState) {
                            is GuestDashboardViewModel.UiState.Loading -> {
                                binding.textEmpty.visibility = View.GONE
                            }

                            is GuestDashboardViewModel.UiState.Error -> {
                                Timber.e(">>> Error loading timeslot data: ${uiState.message}")
                                AlertUtils.showError(text = uiState.message)
                            }

                            else -> {} // Andere States werden durch die UI selbst gehandelt
                        }
                    }
                }
                launch {
                    viewModel.availableLocations.collect { locations ->
                        setupLocationDropdown(locations)
                    }
                }
                launch {
                    viewModel.filteredTimeslots.collect { timeslots ->
                        timeslotAdapter.submitList(timeslots)
                        binding.textEmpty.visibility =
                            if (timeslots.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun setupLocationFilter() {
        binding.locationFilter.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                // "Alle Standorte" wurde gewählt
                viewModel.filterTimeslotsByLocation(null)
            } else {
                // Location wurde gewählt, -1 wegen "Alle Standorte" am Anfang
                val location = viewModel.availableLocations.value[position - 1]
                viewModel.filterTimeslotsByLocation(location.id)
            }
        }
    }

    private fun setupLocationDropdown(locations: List<Location>) {
        // "Alle Standorte" als erste Option hinzufügen
        val locationNames = mutableListOf(getString(R.string.hint_select_location))
        locationNames.addAll(locations.map { it.name })

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            locationNames
        )
        binding.locationFilter.setAdapter(adapter)

        // Vorselektierte Location setzen falls vorhanden
        args.preSelectedLocationId?.let { locationId ->
            val preselectedLocation = locations.find { it.id == locationId }
            preselectedLocation?.let { location ->
                val index = locations.indexOf(location) + 1 // +1 wegen "Alle Standorte"
                binding.locationFilter.setText(location.name, false)
                binding.locationFilter.listSelection = index
            }
        } ?: binding.locationFilter.setText(locationNames[0], false)
    }

    private fun handleTimeslotBooking(timeslot: Timeslot) {
        try {

            findNavController().navigate(
                GuestTimeslotListFragmentDirections
                    .actionGuestTimeslotListFragmentToGuestBookingsFillOutFormFragment(
                        timeslot = timeslot,
                        course = args.course
                    )
            )

        } catch (e: Exception) {
            Timber.e(e, ">>> Error navigating to booking form")
        }
    }

    override fun onDestroyView() {
        // Custom Navigation zurücksetzen
        (requireActivity() as MainActivity).setCustomNavigateUpCallback(null)

        super.onDestroyView()
        _binding = null
    }
}