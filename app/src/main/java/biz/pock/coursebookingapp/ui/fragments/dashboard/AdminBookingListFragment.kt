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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.shared.enums.BookingStatus
import biz.pock.coursebookingapp.ui.adapters.dashboard.BookingListAdapter
import biz.pock.coursebookingapp.ui.swiper.SwipeActionCallback
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.AdminDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminBookingListFragment : FilterableFragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private var selectedStatus: BookingStatus? = null

    override val viewModel: AdminDashboardViewModel by activityViewModels()

    private val bookingAdapter = BookingListAdapter { booking, view ->
        showBookingOptions(booking, view)
    }

    override fun applyCurrentFilters() {
        // Filter aus dem ViewModel anwenden
        viewModel.onTabSelected(0)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        viewModel.selectedBookingStatus.value?.let { status ->
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

    private fun setupStatusFilter() {
        binding.chipGroupFilter.apply {
            removeAllViews()

            addChip(null)
            BookingStatus.entries.forEach { status ->
                addChip(status)
            }

            // Status aus dem ViewModel wiederherstellen
            val currentStatus = viewModel.selectedBookingStatus.value?.let { statusStr ->
                BookingStatus.fromApiString(statusStr)
            }

            if (currentStatus != null) {
                val chipToSelect = (0 until childCount)
                    .map { getChildAt(it) as? Chip }
                    .firstOrNull { (it?.tag as? BookingStatus) == currentStatus }
                chipToSelect?.isChecked = true
            } else {
                findViewById<Chip>(R.id.chipAll)?.isChecked = true
            }

            setOnCheckedStateChangeListener { group, checkedIds ->
                val selectedChip = checkedIds.firstOrNull()
                    ?: return@setOnCheckedStateChangeListener
                val status = if (selectedChip == R.id.chipAll) null
                else group.findViewById<Chip>(selectedChip)?.tag as? BookingStatus

                filterBookings(status?.let { BookingStatus.toApiString(it) })
            }
        }
    }

    private fun ChipGroup.addChip(status: BookingStatus?) {
        val chip = Chip(context).apply {
            id = if (status == null) R.id.chipAll else View.generateViewId()
            text = if (status == null)
                context.getString(R.string.filter_status_all)
            else
                context.getString(status.resId)
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
            chipMinHeight = resources.getDimensionPixelSize(R.dimen.button_height_choice_chip).toFloat()
        }
        addView(chip)
    }

    private fun filterBookings(status: String?) {
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.FilterBookings(status)
        )
    }

    private fun showBookingOptions(booking: Booking, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_booking_options_admin, menu)
            MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())

            // Status-abhängige Menüoptionen je nach aktuellem Status
            with(menu) {
                // Bestätigen nur bei PENDING möglich
                findItem(R.id.action_confirm)?.isVisible = booking.status == Booking.STATUS_PENDING
                // Stornieren nur bei PENDING oder CONFIRMED möglich
                findItem(R.id.action_cancel)?.isVisible = booking.status in listOf(
                    Booking.STATUS_PENDING,
                    Booking.STATUS_CONFIRMED
                )
                // Löschen immer möglich
                findItem(R.id.action_delete)?.isVisible = true
            }

            // Delete und Cancel rot einfärben
            DropDownUtils.tintMenuItem(requireContext(), this, R.id.action_delete, R.color.error)
            DropDownUtils.tintMenuItem(requireContext(), this, R.id.action_cancel, R.color.error)
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_confirm -> {
                    updateBookingStatus(booking, Booking.STATUS_CONFIRMED)
                    true
                }

                R.id.action_cancel -> {
                    updateBookingStatus(booking, Booking.STATUS_CANCELED)
                    true
                }

                R.id.action_delete -> {
                    confirmDeleteBooking(booking) {}
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.UpdateBookingStatus(
                bookingId = booking.id,
                status = newStatus
            )
        )
    }

    private fun confirmDeleteBooking(booking: Booking, onCancel: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_booking_title)
            .setMessage(getString(R.string.dialog_delete_booking_message, booking.id))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.handleAction(AdminDashboardViewModel.AdminAction.DeleteBooking(booking.id))
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = bookingAdapter

            ItemTouchHelper(
                SwipeActionCallback(
                    context = requireContext(),
                    onDelete = { booking, resetSwipeState ->
                        confirmDeleteBooking(booking, resetSwipeState)
                    },
                    onEdit = { booking ->
                        showStatusUpdateDialog(booking)
                    },
                    getItem = { position -> bookingAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun showStatusUpdateDialog(booking: Booking) {
        val items = arrayOf(
            Booking.STATUS_PENDING,
            Booking.STATUS_CONFIRMED,
            Booking.STATUS_CANCELED
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_update_booking_status_title)
            .setItems(items.map { it.replaceFirstChar { char -> char.uppercase() } }
                .toTypedArray()) { _, which ->
                updateBookingStatus(booking, items[which])
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
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

        // UI State beobachten
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