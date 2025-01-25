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
import androidx.recyclerview.widget.GridLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.databinding.FragmentGuestBookingsOverviewBinding
import biz.pock.coursebookingapp.ui.adapters.GuestBookingAdapter
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.UiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GuestBookingsOverviewFragment : Fragment() {

    private var _binding: FragmentGuestBookingsOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GuestDashboardViewModel by viewModels({ requireParentFragment() })
    private lateinit var bookingsAdapter: GuestBookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestBookingsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        loadBookings()
    }

    private fun setupRecyclerView() {
        bookingsAdapter = GuestBookingAdapter(
            context = requireContext(),
            onEditBillingContact = { bookingDetails ->
                // TODO: Eventuell edit billing contact implementieren
                Timber.d(">>> Edit billing contact for booking ${bookingDetails.id}")
            }
        )

        binding.bookingsRecyclerView.apply {
            // Grid Layout Manager basierend auf der Bildschirmbreite
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            adapter = bookingsAdapter
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.primary, R.color.secondary)
            setOnRefreshListener {
                loadBookings()
            }
        }
    }

    private fun loadBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadGuestBookingDetails()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // UI State beobachten
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefresh.isRefreshing =
                            state is GuestDashboardViewModel.UiState.Loading

                        when (state) {
                            is GuestDashboardViewModel.UiState.Error -> {
                                AlertUtils.showError(text = state.message)
                            }

                            is GuestDashboardViewModel.UiState.Empty -> {
                                binding.emptyStateTextView.visibility = View.VISIBLE
                            }

                            else -> {
                                binding.emptyStateTextView.visibility = View.GONE
                            }
                        }
                    }
                }

                // Buchungsdetails beobachten
                launch {
                    viewModel.guestBookingDetails.collect { bookings ->
                        bookingsAdapter.submitList(bookings)
                        binding.emptyStateTextView.visibility =
                            if (bookings.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}