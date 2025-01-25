package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.databinding.DialogTimeEntriesBinding
import biz.pock.coursebookingapp.ui.adapters.dashboard.TimeEntryAdapter
import biz.pock.coursebookingapp.ui.swiper.SwipeActionCallback
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimeEntriesDialog : DialogFragment() {

    private var _binding: DialogTimeEntriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var timeEntryAdapter: TimeEntryAdapter
    private var onTimeEntriesChange: ((List<TimeEntry>) -> Unit)? = null

    private lateinit var timeslot: Timeslot
    private var timeEntries: List<TimeEntry> = emptyList()

    private var onCreateEntry: ((String, TimeEntry) -> Unit)? = null
    private var onUpdateEntry: ((String, String, TimeEntry) -> Unit)? = null
    private var onDeleteEntry: ((String, String) -> Unit)? = null

    // Update die Setter für die Callbacks
    fun setOnCreateTimeEntry(listener: (String, TimeEntry) -> Unit) {
        onCreateEntry = listener
    }

    fun setOnUpdateTimeEntry(listener: (String, String, TimeEntry) -> Unit) {
        onUpdateEntry = listener
    }

    fun setOnDeleteTimeEntry(listener: (String, String) -> Unit) {
        onDeleteEntry = listener
    }


    companion object {
        private const val ARG_TIMESLOT = "arg_timeslot"
        private const val ARG_TIME_ENTRIES = "arg_time_entries"

        fun newInstance(
            timeslot: Timeslot,
            timeEntries: List<TimeEntry>
        ) = TimeEntriesDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TIMESLOT, timeslot)
                putParcelableArrayList(ARG_TIME_ENTRIES, ArrayList(timeEntries))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTimeEntriesBinding.inflate(layoutInflater)

        timeslot = arguments?.getParcelable(ARG_TIMESLOT)
            ?: throw IllegalArgumentException("Timeslot required")

        @Suppress("DEPRECATION")
        timeEntries = arguments?.getParcelableArrayList(ARG_TIME_ENTRIES)
            ?: emptyList()

        setupRecyclerView()
        setupFab()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.manage_time_entries, timeEntries.size))
            .setView(binding.root)
            .setPositiveButton(R.string.close, null)
            .create()
    }

    private fun setupRecyclerView() {
        timeEntryAdapter = TimeEntryAdapter { timeEntry, view ->
            showTimeEntryOptions(timeEntry, view)
        }

        binding.recyclerViewTimeEntries.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = timeEntryAdapter

            ItemTouchHelper(
                SwipeActionCallback(
                    context = requireContext(),
                    onDelete = { timeEntry, resetSwipeState ->
                        confirmDeleteTimeEntry(timeEntry, resetSwipeState)
                    },
                    onEdit = { timeEntry ->
                        showEditTimeEntryDialog(timeEntry)
                    },
                    getItem = { position -> timeEntryAdapter.currentList[position] }
                )
            ).attachToRecyclerView(this)
        }

        // Initial TimeEntries setzen
        updateTimeEntries(timeEntries)
    }

    private fun setupFab() {
        binding.fabAddTimeEntry.setOnClickListener {
            showEditTimeEntryDialog(null)
        }
    }

    private fun showTimeEntryOptions(timeEntry: TimeEntry, anchorView: View) {
        val popupMenu = PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.menu_time_entry_options, menu)
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
                    showEditTimeEntryDialog(timeEntry)
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteTimeEntry(timeEntry) {}
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showEditTimeEntryDialog(timeEntry: TimeEntry?) {
        TimeEntryDialog.newInstance(timeslot, timeEntry).apply {
            setOnSaveListener { savedTimeEntry ->
                val currentEntries = timeEntryAdapter.currentList.toMutableList()

                if (timeEntry == null) {
                    // API Call
                    createTimeEntry(savedTimeEntry)

                    currentEntries.add(savedTimeEntry)
                } else {
                    // Bestehenden Eintrag aktualisieren - API Call
                    updateTimeEntry(savedTimeEntry)
                    val index = currentEntries.indexOfFirst { it.id == savedTimeEntry.id }
                    if (index != -1) {
                        currentEntries[index] = savedTimeEntry
                    }
                }

                updateTimeEntries(currentEntries)
            }
        }.show(childFragmentManager, "time_entry_edit")
    }

    private fun confirmDeleteTimeEntry(timeEntry: TimeEntry, onCancel: () -> Unit = {}) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_time_entry_title)
            .setMessage(getString(R.string.dialog_delete_time_entry_message))
            .setPositiveButton(R.string.button_delete) { _, _ ->

                // API Call
                deleteTimeEntry(timeEntry)

                val currentEntries = timeEntryAdapter.currentList.toMutableList()
                currentEntries.remove(timeEntry)
                updateTimeEntries(currentEntries)
            }
            .setNegativeButton(R.string.button_cancel) { _, _ -> onCancel() }
            .setOnCancelListener { onCancel() }
            .show()
    }

    private fun updateTimeEntries(newEntries: List<TimeEntry>) {
        // Nach Datum und Zeit sortieren
        val sortedEntries = newEntries.sortedWith(compareBy(
            { it.date },
            { it.startTime }
        ))

        timeEntries = sortedEntries
        timeEntryAdapter.submitList(sortedEntries)
        dialog?.setTitle(getString(R.string.manage_time_entries, sortedEntries.size))
        updateEmptyState()
        onTimeEntriesChange?.invoke(sortedEntries)
    }

    private fun createTimeEntry(timeEntry: TimeEntry) {
        timeslot?.let { slot ->
            onCreateEntry?.invoke(slot.id, timeEntry)
        }
    }

    private fun updateTimeEntry(timeEntry: TimeEntry) {
        timeslot?.let { slot ->
            onUpdateEntry?.invoke(slot.id, timeEntry.id, timeEntry)
        }
    }

    private fun deleteTimeEntry(timeEntry: TimeEntry) {
        timeslot?.let { slot ->
            onDeleteEntry?.invoke(slot.id, timeEntry.id)
        }
    }

    private fun updateEmptyState() {
        binding.textEmpty.visibility = if (timeEntries.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}