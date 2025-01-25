package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.shared.enums.BookingStatus
import biz.pock.coursebookingapp.ui.adapters.dashboard.BookingListAdapter
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TrainerDashboardViewModel
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrainerBookingsFragment : FilterableFragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override val viewModel: TrainerDashboardViewModel by activityViewModels()

    private var selectedStatus: BookingStatus? = null

    // Adapter mit Click-Handler nur für bestätigte Buchungen
    private val bookingAdapter = BookingListAdapter { booking, view ->
        if (booking.status == Booking.STATUS_CONFIRMED) {
            showBookingOptions(booking, view)
        }
    }

    override fun applyCurrentFilters() {
        // Erst den aktuellen Status merken
        val currentStatus = viewModel.selectedBookingStatus.value

        // Dann Tab setzen und Refresh ausführen
        viewModel.onTabSelected(1)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        // Dann den gemerkten Status wieder anwenden
        currentStatus?.let { status ->
            filterBookings(status)
        }
    }

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
        setupStatusFilter()
        observeViewModel()

        // FAB ausblenden
        binding.fabAdd.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            // Spaltenanzahl basierend auf der Bildschirmbreite berechnen
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = bookingAdapter
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // RecyclerView Layout neu setzen bei Orientierungsänderung
        setupRecyclerView()
    }

    private fun setupStatusFilter() {
        binding.chipGroupFilter.apply {
            removeAllViews()

            // Chips dynamisch hinzufügen
            addFilterChip(null)  // "All" Chip
            addFilterChip(BookingStatus.CONFIRMED)
            addFilterChip(BookingStatus.CANCELED)

            // Vorherige Auswahl wiederherstellen
            val currentStatus = viewModel.selectedBookingStatus.value?.let { statusStr ->
                BookingStatus.fromApiString(statusStr)
            }
            selectedStatus = currentStatus

            // Initial Selection
            val chipToSelect = when (currentStatus) {
                BookingStatus.CONFIRMED -> findViewById(BookingStatus.CONFIRMED.ordinal)
                BookingStatus.CANCELED -> findViewById<Chip>(BookingStatus.CANCELED.ordinal)
                null -> findViewById(R.id.chipAll)
                else -> findViewById(R.id.chipAll)
            }
            chipToSelect?.isChecked = true

            setOnCheckedStateChangeListener { group, checkedIds ->
                val selectedChip = checkedIds.firstOrNull()
                    ?: return@setOnCheckedStateChangeListener
                selectedStatus = when (selectedChip) {
                    R.id.chipAll -> null
                    else -> group.findViewById<Chip>(selectedChip)?.tag as? BookingStatus
                }
                filterBookings(selectedStatus?.let { BookingStatus.toApiString(it) })
            }
        }
    }

    private fun ChipGroup.addFilterChip(status: BookingStatus?) {
        val chip = Chip(context).apply {
            id = status?.ordinal ?: R.id.chipAll
            text = if (status == null)
                context.getString(R.string.filter_status_all)
            else
                context.getString(
                    when (status) {
                        BookingStatus.CONFIRMED -> R.string.booking_status_confirmed
                        BookingStatus.CANCELED -> R.string.booking_status_canceled
                        else -> R.string.filter_status_all
                    }
                )
            isCheckable = true
            tag = status

            // Einheitliche Höhe und Stil
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen.button_height_choice_chip)
            )
            height = resources.getDimensionPixelSize(R.dimen.button_height_choice_chip)
            minHeight = resources.getDimensionPixelSize(R.dimen.button_height_choice_chip)

            setPadding(
                resources.getDimensionPixelSize(R.dimen.chip_padding_horizontal),
                0,
                resources.getDimensionPixelSize(R.dimen.chip_padding_horizontal),
                0
            )

            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER

            setChipBackgroundColorResource(R.color.filter_button_background)
            setTextColor(context.getColor(R.color.on_surface))
            chipMinHeight =
                resources.getDimensionPixelSize(R.dimen.button_height_choice_chip).toFloat()
        }
        addView(chip)
    }

    private fun showBookingOptions(booking: Booking, anchorView: View) {
        // Nur für bestätigte Buchungen das Menü anzeigen
        if (booking.status == Booking.STATUS_CONFIRMED) {
            val popupMenu = PopupMenu(requireContext(), anchorView).apply {
                menuInflater.inflate(R.menu.menu_booking_options_trainer, menu)
                MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_cancel -> {
                        updateBookingStatus(booking, Booking.STATUS_CANCELED)
                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.UpdateBookingStatus(
                bookingId = booking.id,
                status = newStatus
            )
        )
    }

    private fun filterBookings(status: String?) {
        // Erst im ViewModel speichern
        viewModel.selectedBookingStatus.value = status

        // Dann den lokalen Status aktualisieren
        selectedStatus = status?.let { BookingStatus.fromApiString(it) }

        // Dann die Filter-Action ausführen
        viewModel.handleAction(
            TrainerDashboardViewModel.TrainerAction.FilterBookings(status)
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardData
                    .map { it.bookings }
                    .collect { bookings ->
                        bookingAdapter.submitList(bookings)
                        updateEmptyState(bookings)
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

    private fun updateEmptyState(bookings: List<Booking>) {
        binding.textEmpty.apply {
            visibility = if (bookings.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_bookings_message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}