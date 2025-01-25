package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.core.widget.addTextChangedListener
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.databinding.DialogAddParticipantBinding
import biz.pock.coursebookingapp.shared.enums.SkillLevel
import biz.pock.coursebookingapp.utils.validators.BookingFormValidator
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ParticipantDialog : DialogFragment() {

    private var _binding: DialogAddParticipantBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var formValidator: BookingFormValidator

    private var onSave: ((BookingParticipant) -> Unit)? = null
    private var participant: BookingParticipant? = null

    private val skillLevels = arrayOf("beginner", "intermediate", "advanced")
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val ARG_PARTICIPANT = "arg_participant"

        fun newInstance(participant: BookingParticipant? = null) = ParticipantDialog().apply {
            arguments = Bundle().apply {
                participant?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        putParcelable(ARG_PARTICIPANT, it)
                    } else {
                        @Suppress("DEPRECATION")
                        putParcelable(ARG_PARTICIPANT, it)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddParticipantBinding.inflate(layoutInflater)

        // Für API 33 und höher
        participant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_PARTICIPANT, BookingParticipant::class.java)
        } else {
            // Für ältere API Versionen
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_PARTICIPANT)
        }

        setupViews()
        setupValidation()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (participant == null) R.string.add_participant else R.string.edit_participant)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
            .apply {
                setOnShowListener { dialog ->
                    getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        if (validateAndSave()) {
                            dialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun setupViews() {
        // Setup skill level dropdown
        val skillLevels = SkillLevel.entries.map {
            it.name to getString(it.resId)
        }.toMap()

        val skillLevelAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            skillLevels.values.toList()
        )
        binding.autoCompleteSkillLevel.setAdapter(skillLevelAdapter)

        // Setup date picker
        binding.editTextBirthdate.setOnClickListener {
            showDatePicker()
        }

        // Fill data if editing
        participant?.let { fillParticipantData(it, skillLevels) }
    }

    private fun fillParticipantData(participant: BookingParticipant, skillLevels: Map<String, String>) {
        binding.apply {
            editTextFirstname.setText(participant.firstname)
            editTextLastname.setText(participant.lastname)
            editTextEmail.setText(participant.email)

            // Convert date format for display
            try {
                val date = apiDateFormat.parse(participant.birthdate)
                editTextBirthdate.setText(date?.let { displayDateFormat.format(it) })
            } catch (e: Exception) {
                editTextBirthdate.setText(participant.birthdate)
            }

            // Find localized skill level text
            val skillLevel = SkillLevel.fromApiString(participant.skillLevel)
            skillLevel?.let {
                autoCompleteSkillLevel.setText(getString(it.resId), false)
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_birthdate)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            binding.editTextBirthdate.setText(displayDateFormat.format(date))
        }

        picker.show(parentFragmentManager, "date_picker")
    }

    private fun setupValidation() {
        binding.apply {
            editTextFirstname.addTextChangedListener {
                inputLayoutFirstname.error = null
            }
            editTextLastname.addTextChangedListener {
                inputLayoutLastname.error = null
            }
            editTextEmail.addTextChangedListener {
                inputLayoutEmail.error = null
            }
            editTextBirthdate.addTextChangedListener {
                inputLayoutBirthdate.error = null
            }
            autoCompleteSkillLevel.addTextChangedListener {
                inputLayoutSkillLevel.error = null
            }
        }
    }

    private fun validateAndSave(): Boolean {
        var isValid = true
        binding.apply {
            // Validate each field
            formValidator.validateName(editTextFirstname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutFirstname.error = result.errorMessage
                    isValid = false
                }
            }

            formValidator.validateName(editTextLastname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutLastname.error = result.errorMessage
                    isValid = false
                }
            }

            formValidator.validateEmail(editTextEmail.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutEmail.error = result.errorMessage
                    isValid = false
                }
            }

            // Convert display date to API format for validation
            val birthdate = try {
                displayDateFormat.parse(editTextBirthdate.text.toString())?.let {
                    apiDateFormat.format(it)
                } ?: editTextBirthdate.text.toString()
            } catch (e: Exception) {
                editTextBirthdate.text.toString()
            }

            formValidator.validateBirthdate(birthdate).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBirthdate.error = result.errorMessage
                    isValid = false
                }
            }

            formValidator.validateSkillLevel(
                autoCompleteSkillLevel.text.toString().lowercase()
            ).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutSkillLevel.error = result.errorMessage
                    isValid = false
                }
            }

            if (isValid) {
                val selectedSkillLevel = binding.autoCompleteSkillLevel.text.toString()
                val skillLevel = SkillLevel.fromLocalizedName(requireContext(), selectedSkillLevel)
                    ?.let { SkillLevel.toApiString(it) }
                    ?: run {
                        // Wenn keine Übereinstimmung gefunden wurde, setze Fehler
                        inputLayoutSkillLevel.error = getString(R.string.error_invalid_skill_level)
                        isValid = false
                        return false
                    }

                val participantData = BookingParticipant(
                    id = participant?.id,
                    firstname = editTextFirstname.text.toString(),
                    lastname = editTextLastname.text.toString(),
                    email = editTextEmail.text.toString(),
                    birthdate = birthdate,
                    skillLevel = skillLevel
                )
                onSave?.invoke(participantData)
            }
        }
        return isValid
    }

    fun setOnSaveListener(listener: (BookingParticipant) -> Unit) {
        onSave = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}