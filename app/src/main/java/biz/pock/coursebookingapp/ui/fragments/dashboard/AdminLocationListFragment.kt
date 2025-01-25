package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.content.res.Configuration
import android.os.Bundle
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
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.ui.adapters.dashboard.LocationListAdapter
import biz.pock.coursebookingapp.ui.dialogs.LocationEditDialog
import biz.pock.coursebookingapp.ui.swiper.SwipeActionCallback
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.AdminDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AdminLocationListFragment : FilterableFragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override val viewModel: AdminDashboardViewModel by activityViewModels()

    private val locationAdapter = LocationListAdapter { location, view ->
        showLocationOptions(location, view)
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
        // Filter Card ausblenden da keine Filter benötigt
        binding.filterCard.visibility = View.GONE
        setupRecyclerView()
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            showEditLocationDialog(null)
        }
    }

    override fun applyCurrentFilters() {
        // Hier haben wir keine Filter, müssen aber vom FilterableFragment erben
        // um die globale Pull-To-Refresh Funktionalität zu gewährleisten
        viewModel.onTabSelected(3)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        Timber.d(">>> LocationListFragment refreshed ")
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = locationAdapter

            ItemTouchHelper(
                SwipeActionCallback(
                    context = requireContext(),
                    onDelete = { location, resetSwipeState: () -> Unit ->
                        confirmDeleteLocation(location, resetSwipeState)
                    },
                    onEdit = { location ->
                        showEditLocationDialog(location)
                    },
                    getItem = { position -> locationAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun showLocationOptions(location: Location, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_item_options, menu)
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
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditLocationDialog(location)
                    true
                }

                R.id.action_delete -> {
                    confirmDeleteLocation(location) {}
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }


    private fun confirmDeleteLocation(location: Location, onCancel: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_location_title)
            .setMessage(getString(R.string.dialog_delete_location_message, location.name))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.handleAction(AdminDashboardViewModel.AdminAction.DeleteLocation(location.id))
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardData
                    .map { it.locations }
                    .collect { locations ->
                        locationAdapter.submitList(locations)
                        updateEmptyState(locations)
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

    private fun showEditLocationDialog(location: Location?) {
        LocationEditDialog.newInstance(location).apply {
            setOnSaveListener { savedLocation ->
                val action = if (location == null) {
                    AdminDashboardViewModel.AdminAction.CreateLocation(savedLocation)
                } else {
                    AdminDashboardViewModel.AdminAction.UpdateLocation(savedLocation)
                }
                viewModel.handleAction(action)
            }
        }.show(childFragmentManager, "location_edit")
    }

    private fun updateEmptyState(locations: List<Location>) {
        binding.textEmpty.apply {
            visibility = if (locations.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_locations_message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}