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
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.User
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.shared.enums.Role
import biz.pock.coursebookingapp.ui.adapters.dashboard.UserListAdapter
import biz.pock.coursebookingapp.ui.dialogs.UserEditDialog
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
class AdminUserListFragment : FilterableFragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private var selectedRole: Role? = null

    override val viewModel: AdminDashboardViewModel by activityViewModels()

    private val userAdapter = UserListAdapter { user, view ->
        showUserOptions(user, view)
    }

    override fun applyCurrentFilters() {
        viewModel.onTabSelected(5)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)
        // Diese Methode wird beim Fragment resume aufgerufen
        // Hier müssen wir den aktuellen Filter aus dem ViewModel holen und anwenden
        val currentRole = viewModel.selectedUserRole.value?.let { roleStr ->
            Role.fromUserRoleString(roleStr)
        }
        selectedRole = currentRole
        filterUsers(currentRole)
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
        setupRoleFilter()
        observeViewModel()
        setupFab()

        // Initial load wenn nötig
        if (viewModel.dashboardData.value.users.isEmpty()) {
            viewModel.handleAction(AdminDashboardViewModel.AdminAction.LoadUsers)
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showEditUserDialog(null) }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = userAdapter

            ItemTouchHelper(
                SwipeActionCallback(
                    context = requireContext(),
                    onDelete = { user, resetSwipeState: () -> Unit ->
                        confirmDeleteUser(user, resetSwipeState)
                    },
                    onEdit = { user ->
                        showEditUserDialog(user)
                    },
                    getItem = { position -> userAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun setupRoleFilter() {
        binding.chipGroupFilter.apply {
            removeAllViews()

            addChip(null)
            Role.entries.forEach { role ->
                addChip(role)
            }

            // Status vom ViewModel wiederherstellen
            val currentRole = viewModel.selectedUserRole.value?.let { roleStr ->
                Role.fromUserRoleString(roleStr)
            }

            // Vorherige Auswahl wiederherstellen wenn vorhanden
            if (currentRole != null) {
                val chipToSelect = (0 until childCount)
                    .map { getChildAt(it) as? Chip }
                    .firstOrNull { (it?.tag as? Role) == currentRole }
                chipToSelect?.isChecked = true
            } else {
                findViewById<Chip>(R.id.chipAll)?.isChecked = true
            }

            setOnCheckedStateChangeListener { group, checkedIds ->
                val selectedChip =
                    checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
                val role = if (selectedChip == R.id.chipAll) null
                else group.findViewById<Chip>(selectedChip)?.tag as? Role

                filterUsers(role)
            }
        }
    }

    private fun ChipGroup.addChip(role: Role?) {
        // Gleiche Implementierung wie bei den anderen, nur mit Role
        val chip = Chip(context).apply {
            id = if (role == null) R.id.chipAll else View.generateViewId()
            text = if (role == null)
                context.getString(R.string.filter_role_all)
            else
                context.getString(role.resId)
            isCheckable = true
            tag = role

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

    private fun filterUsers(role: Role?) {
        selectedRole = role
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.FilterUsers(
                role?.let { Role.toUserRoleString(it) }
            )
        )
    }

    private fun confirmDeleteUser(user: User, onCancel: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_user_title)
            .setMessage(getString(R.string.dialog_delete_user_message, user.email))
            .setPositiveButton(R.string.button_delete) { _, _ ->
                viewModel.handleAction(AdminDashboardViewModel.AdminAction.DeleteUser(user.id))
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Beobachte die gefilterte User-Liste
                viewModel.dashboardData
                    // Hier verwenden wir die gefilterte Liste
                    .map { it.users }
                    .collect { users ->
                        userAdapter.submitList(users)
                        updateEmptyState(users)
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

    private fun updateEmptyState(users: List<User>) {
        binding.textEmpty.apply {
            visibility = if (users.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_users_message)
        }
    }

    private fun showUserOptions(user: User, anchorView: View) {
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
                    showEditUserDialog(user)
                    true
                }

                R.id.action_delete -> {
                    confirmDeleteUser(user) {}
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }


    private fun showEditUserDialog(user: User?) {
        UserEditDialog.newInstance(user).apply {
            setOnSaveListener { savedUser ->
                val action = if (user == null) {
                    AdminDashboardViewModel.AdminAction.CreateUser(savedUser)
                } else {
                    AdminDashboardViewModel.AdminAction.UpdateUser(savedUser)
                }
                viewModel.handleAction(action)
            }
        }.show(childFragmentManager, "user_edit")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}