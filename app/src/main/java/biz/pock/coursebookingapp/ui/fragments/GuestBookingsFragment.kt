package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.ui.adapters.dashboard.BookingListAdapter
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GuestBookingsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GuestDashboardViewModel by viewModels()
    private val bookingAdapter = BookingListAdapter { _, _ -> } // Readonly für Gäste

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        // FAB ausblenden da Gäste keine Buchungen erstellen können
        binding.fabAdd.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bookingAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.guestBookings.collectLatest { bookings ->
                    bookingAdapter.submitList(bookings)
                    binding.textEmpty.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
                    binding.textEmpty.text = getString(R.string.empty_bookings_message)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}