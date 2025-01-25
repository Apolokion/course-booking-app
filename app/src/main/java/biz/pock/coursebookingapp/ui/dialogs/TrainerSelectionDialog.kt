package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.TimeEntryWithUsers
import biz.pock.coursebookingapp.data.model.User
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.data.repositories.UserRepository
import biz.pock.coursebookingapp.databinding.DialogTrainerSelectionBinding
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TrainerDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TrainerSelectionDialog : DialogFragment() {
    private var _binding: DialogTrainerSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var timeslotId: String
    private var selectedTrainerIds = mutableSetOf<String>()
    private var timeEntries = mutableListOf<TimeEntryWithUsers>()
    private var isUpdating = false // Flag für Update-Status

    @Inject
    lateinit var courseRepository: CourseRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var onTrainerChangeListener: ((Int) -> Unit)? = null

    private val viewModel: TrainerDashboardViewModel by activityViewModels()

    fun setOnTrainerChangeListener(listener: (Int) -> Unit) {
        onTrainerChangeListener = listener
    }

    companion object {
        private const val ARG_TIMESLOT_ID = "arg_timeslot_id"
        private const val ARG_SELECTED_TRAINERS = "arg_selected_trainers"

        fun newInstance(timeslotId: String, selectedTrainerIds: Set<String>) =
            TrainerSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIMESLOT_ID, timeslotId)
                    putStringArrayList(ARG_SELECTED_TRAINERS, ArrayList(selectedTrainerIds))
                }
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTrainerSelectionBinding.inflate(layoutInflater)

        timeslotId = arguments?.getString(ARG_TIMESLOT_ID)
            ?: throw IllegalArgumentException("Timeslot ID required")

        selectedTrainerIds = arguments?.getStringArrayList(ARG_SELECTED_TRAINERS)?.toMutableSet()
            ?: mutableSetOf()

        // Eigene User ID initial hinzufügen
        authRepository.getCurrentUserId()?.let { currentUserId ->
            selectedTrainerIds.add(currentUserId)
        }

        setupTrainerList()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.manage_trainers_with_count, selectedTrainerIds.size))
            .setView(binding.root)
            .setPositiveButton(R.string.close, null)
            .create()
    }

    private fun setupTrainerList() {
        lifecycleScope.launch {
            try {
                val trainers = userRepository.getUsers("trainer").first()
                val currentUserId = authRepository.getCurrentUserId()
                val currentUserRole = authRepository.getCurrentRole()

                // TimeEntries laden
                timeEntries = courseRepository.getTimeslotTimeEntriesWithUsers(timeslotId).toMutableList()

                // Parallel für alle TimeEntries die User laden
                val userJobs = timeEntries.map { timeEntry ->
                    async {
                        courseRepository.getTimeEntryUsers(timeEntry.id)
                    }
                }

                // Sammle alle User IDs
                selectedTrainerIds = mutableSetOf<String>().apply {
                    userJobs.awaitAll().forEach { users ->
                        users.forEach { user ->
                            add(user.id)
                        }
                    }
                    // Eigene User ID nur hinzufügen wenn es ein Trainer ist
                    if (currentUserRole == "trainer" && currentUserId != null) {
                        add(currentUserId)
                    }
                }

                // UI Setup
                binding.chipGroupTrainers.removeAllViews()
                trainers.forEach { trainer ->
                    val chip = Chip(requireContext()).apply {
                        text = "${trainer.firstname} ${trainer.lastname}"
                        isCheckable = true
                        // Chip ist für Admins immer enabled, für Trainer nur wenn es nicht die eigene ID ist
                        isEnabled = currentUserRole == "admin" || trainer.id != currentUserId
                        isChecked = selectedTrainerIds.contains(trainer.id)
                        tag = trainer.id

                        setOnCheckedChangeListener { _, isChecked ->
                            if (!isUpdating) {
                                handleTrainerSelection(trainer, isChecked)
                            }
                        }
                    }
                    binding.chipGroupTrainers.addView(chip)
                }

                dialog?.setTitle(
                    getString(
                        R.string.manage_trainers_with_count,
                        selectedTrainerIds.size
                    )
                )

            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading trainers: ${e.message}")
                AlertUtils.showError(
                    textRes = R.string.error_loading_trainers
                )
            }
        }
    }

    private fun handleTrainerSelection(trainer: User, isChecked: Boolean) {
        if (isUpdating) return

        lifecycleScope.launch {
            try {
                isUpdating = true

                if (isChecked) {
                    selectedTrainerIds.add(trainer.id)
                } else {
                    selectedTrainerIds.remove(trainer.id)
                }

                // Eigene ID immer dabei
                authRepository.getCurrentUserId()?.let { currentUserId ->
                    selectedTrainerIds.add(currentUserId)
                }

                // Parallel für alle TimeEntries die User synchronisieren
                val syncJobs = timeEntries.map { timeEntry ->
                    async {
                        courseRepository.syncTimeEntryUsers(
                            timeEntryId = timeEntry.id,
                            userIds = selectedTrainerIds.toList()
                        )
                    }
                }

                // Warten bis alle Sync-Jobs fertig sind
                syncJobs.awaitAll()

                // UI aktualisieren
                dialog?.setTitle(
                    getString(
                        R.string.manage_trainers_with_count,
                        selectedTrainerIds.size
                    )
                )
                onTrainerChangeListener?.invoke(selectedTrainerIds.size)

                // Verzögerter Refresh
                delay(500)
                viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

            } catch (e: Exception) {
                Timber.e(e, ">>> Error syncing trainers: ${e.message}")
                // Chip-Status zurücksetzen
                val chip = binding.chipGroupTrainers.findViewWithTag<Chip>(trainer.id)
                chip?.isChecked = !isChecked

                AlertUtils.showError(
                    textRes = if (isChecked)
                        R.string.error_adding_trainer
                    else
                        R.string.error_removing_trainer
                )
            } finally {
                isUpdating = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}