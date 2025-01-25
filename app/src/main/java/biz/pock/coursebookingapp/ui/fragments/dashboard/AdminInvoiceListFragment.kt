package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Invoice
import biz.pock.coursebookingapp.databinding.FragmentListBinding
import biz.pock.coursebookingapp.shared.enums.InvoiceStatus
import biz.pock.coursebookingapp.ui.adapters.dashboard.InvoiceListAdapter
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.AdminDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.StoragePermissionHelper
import biz.pock.coursebookingapp.utils.UiUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AdminInvoiceListFragment : FilterableFragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private var selectedStatus: InvoiceStatus? = null


    override val viewModel: AdminDashboardViewModel by activityViewModels()

    private var pendingInvoiceDownload: Invoice? = null

    private val invoiceAdapter = InvoiceListAdapter(
        onInvoiceClick = { invoice, view -> showInvoiceOptions(invoice, view) },
        onDownloadClick = { invoice -> checkAndRequestPermissions(invoice) },
        onExtendClick = { invoice -> showTokenExtensionDialog(invoice) },
        onViewClick = { invoice -> navigateToPdfViewer(invoice) }
    )

    private fun navigateToPdfViewer(invoice: Invoice) {
        // Die Navigation muss vom Parent-Fragment (AdminDashboardFragment) ausgehen
        val parentFragment = requireParentFragment()
        val navController = parentFragment.findNavController()

        // Bundle mit der invoiceId erstellen
        val bundle = Bundle().apply {
            putString("invoiceId", invoice.id)
        }

        // Direkt mit der Fragment ID und Bundle navigieren
        navController.navigate(
            R.id.invoiceViewerFragment,
            bundle
        )
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        handlePermissionResult()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        handlePermissionResult(allGranted)
    }

    override fun applyCurrentFilters() {
        // Status aus dem ViewModel wiederherstellen

        viewModel.onTabSelected(1)
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

        val currentStatus = viewModel.selectedInvoiceStatus.value?.let { statusStr ->
            InvoiceStatus.fromApiString(statusStr)
        }
        selectedStatus = currentStatus
        filterInvoices(currentStatus?.let { InvoiceStatus.toApiString(it) })
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
            val spanCount = UiUtils.calculateSpanCount(requireContext())
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = invoiceAdapter
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupRecyclerView()
    }

    private fun setupStatusFilter() {
        binding.chipGroupFilter.apply {
            removeAllViews()

            addChip(null)
            InvoiceStatus.entries.forEach { status ->
                addChip(status)
            }

            // Status aus dem ViewModel wiederherstellen
            val currentStatus = viewModel.selectedInvoiceStatus.value?.let { statusStr ->
                InvoiceStatus.fromApiString(statusStr)
            }

            if (currentStatus != null) {
                val chipToSelect = (0 until childCount)
                    .map { getChildAt(it) as? Chip }
                    .firstOrNull { (it?.tag as? InvoiceStatus) == currentStatus }
                chipToSelect?.isChecked = true
            } else {
                findViewById<Chip>(R.id.chipAll)?.isChecked = true
            }

            setOnCheckedStateChangeListener { group, checkedIds ->
                val selectedChip = checkedIds.firstOrNull()
                    ?: return@setOnCheckedStateChangeListener
                selectedStatus = if (selectedChip == R.id.chipAll) null
                else group.findViewById<Chip>(selectedChip)?.tag as? InvoiceStatus

                filterInvoices(selectedStatus?.let { InvoiceStatus.toApiString(it) })
            }
        }
    }

    private fun ChipGroup.addChip(status: InvoiceStatus?) {
        // Gleiche Implementierung wie bei Bookings, nur mit InvoiceStatus
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
            chipMinHeight =
                resources.getDimensionPixelSize(R.dimen.button_height_choice_chip).toFloat()
        }
        addView(chip)
    }

    private fun filterInvoices(status: String?) {
        viewModel.handleAction(
            AdminDashboardViewModel.AdminAction.FilterInvoices(status)
        )
    }

    private fun showInvoiceOptions(invoice: Invoice, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_invoice_options, menu)
            MaterialPopupMenuHelper.stylePopupMenu(this, requireContext())

            // Status-abhängige Menüoptionen je nach aktuellem Status
            with(menu) {
                // View ist nur möglich bei PAID oder PENDING + confirmed und reversal
                findItem(R.id.action_view)?.isVisible = when (invoice.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_REVERSAL,
                    Invoice.STATUS_PENDING -> true

                    else -> false
                }

                // Download ist nur möglich bei PAID, DRAFT oder PENDING + Confirmed und Reversal
                findItem(R.id.action_download)?.isVisible = when (invoice.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_REVERSAL,
                    Invoice.STATUS_PENDING,
                    Invoice.STATUS_DRAFT -> true

                    else -> false
                }

                // Extend Token nur bei PAID, DRAFT oder PENDING wenn Token existiert + Confirmed
                findItem(R.id.action_extend)?.isVisible = when (invoice.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_PENDING,
                    Invoice.STATUS_DRAFT -> invoice.downloadTokenExpiresAt != null

                    else -> false
                }

                // Cancel nur bei PAID, PENDING oder DRAFT möglich + Confirmed
                findItem(R.id.action_cancel)?.isVisible = when (invoice.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_PENDING,
                    Invoice.STATUS_DRAFT -> true

                    else -> false
                }
            }
        }

        // Menu Item Click Handling
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_view -> {
                    navigateToPdfViewer(invoice)
                    true
                }

                R.id.action_download -> {
                    checkAndRequestPermissions(invoice)
                    true
                }

                R.id.action_extend -> {
                    showTokenExtensionDialog(invoice)
                    true
                }

                R.id.action_cancel -> {
                    confirmCancelInvoice(invoice) {}
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun showTokenExtensionDialog(invoice: Invoice) {
        val initialDate = invoice.downloadTokenExpiresAt?.let { expiryDateString ->
            try {
                // Versuche das Ablaufdatum zu parsen
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                dateFormat.parse(expiryDateString)?.time
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing expiry date: $expiryDateString")
                null
            }
        } ?: run {
            // Fallback -> Eine Woche ab heute wenn kein oder ungültiges Datum
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 7)
            }.timeInMillis
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.dialog_extend_token_title)
            .setSelection(initialDate)
            // Mindestdatum auf heute setzen
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(MaterialDatePicker.todayInUtcMilliseconds())
                    .build()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(selection))

            viewModel.handleAction(
                AdminDashboardViewModel.AdminAction.ExtendInvoiceToken(
                    invoiceId = invoice.id,
                    newExpiryDate = date
                )
            )
        }

        datePicker.show(childFragmentManager, "date_picker")
    }

    private fun checkAndRequestPermissions(invoice: Invoice) {
        if (StoragePermissionHelper.hasPermissions(requireActivity())) {
            handleInvoiceDownload(invoice)
        } else {
            pendingInvoiceDownload = invoice
            if (StoragePermissionHelper.shouldShowPermissionRationale(requireActivity())) {
                showStoragePermissionDialog()
            } else {
                requestPermissions()
            }
        }
    }

    private fun showStoragePermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_permission_dialog_title)
            .setMessage(R.string.storage_permission_dialog_message)
            .setPositiveButton(R.string.ok) { _, _ -> requestPermissions() }
            .setNegativeButton(R.string.cancel) { _, _ ->
                AlertUtils.showError(textRes = R.string.storage_permission_denied)
            }
            .show()
    }

    private fun requestPermissions() {
        Timber.d(">>> Requesting permissions")
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${requireActivity().packageName}")
                }
                storagePermissionLauncher.launch(intent)
            }

            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun handlePermissionResult(
        granted: Boolean = StoragePermissionHelper.hasPermissions(
            requireActivity()
        )
    ) {
        Timber.d(">>> Handling permission result: granted=$granted")
        if (granted) {
            pendingInvoiceDownload?.let { startDownload(it) }
        } else {
            AlertUtils.showError(textRes = R.string.storage_permission_required)
        }
        pendingInvoiceDownload = null
    }

    private fun handleInvoiceDownload(invoice: Invoice) {
        pendingInvoiceDownload = invoice
        viewModel.handleAction(AdminDashboardViewModel.AdminAction.DownloadInvoice(invoice.id))
    }

    private fun startDownload(invoice: Invoice) {
        Timber.d(">>> Starting download for invoice: ${invoice.id}")
        viewModel.handleAction(AdminDashboardViewModel.AdminAction.DownloadInvoice(invoice.id))
    }

    private fun confirmCancelInvoice(invoice: Invoice, onCancel: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_cancel_invoice_title)
            .setMessage(getString(R.string.dialog_cancel_invoice_message, invoice.number))
            .setPositiveButton(R.string.button_cancel_invoice) { _, _ ->
                viewModel.handleAction(AdminDashboardViewModel.AdminAction.CancelInvoice(invoice.id))
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Gefilterte Rechnungen beobachten
                viewModel.filteredInvoices.collect { invoices ->
                    invoiceAdapter.submitList(invoices)
                    updateEmptyState(invoices)
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

    private fun updateEmptyState(invoices: List<Invoice>) {
        binding.textEmpty.apply {
            visibility = if (invoices.isEmpty() &&
                viewModel.uiState.value !is BaseDashboardViewModel.UiState.Loading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = getString(R.string.empty_invoices_message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}