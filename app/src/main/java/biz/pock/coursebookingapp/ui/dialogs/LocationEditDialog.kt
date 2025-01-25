package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.databinding.DialogLocationEditBinding
import biz.pock.coursebookingapp.utils.validators.LocationValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationEditDialog : DialogFragment() {

    private var _binding: DialogLocationEditBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var locationValidator: LocationValidator

    private var location: Location? = null
    private var onSave: ((Location) -> Unit)? = null

    companion object {
        private const val ARG_LOCATION = "arg_location"

        fun newInstance(location: Location? = null) = LocationEditDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_LOCATION, location)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLocationEditBinding.inflate(layoutInflater)
        location = arguments?.getParcelable(ARG_LOCATION)

        setupViews()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (location == null) R.string.create_location else R.string.edit_location)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateAndSave()) {
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    private fun setupViews() {
        // Vorhandene Daten einfügen, falls wir bearbeiten
        location?.let { loc ->
            binding.apply {
                editTextName.setText(loc.name)
                editTextAddress.setText(loc.address)
                editTextPostcode.setText(loc.postcode)
                editTextCity.setText(loc.city)
                editTextCountry.setText(loc.country)
            }
        }
    }

    private fun validateAndSave(): Boolean {
        val name = binding.editTextName.text.toString()

        // Fehler zurücksetzen
        binding.textInputName.error = null

        val validationResult = locationValidator.validate(name = name)

        return when (validationResult) {
            is LocationValidator.ValidationResult.Valid -> {
                val locationToSave = Location(
                    id = location?.id ?: "",
                    name = name,
                    address = binding.editTextAddress.text.toString(),
                    postcode = binding.editTextPostcode.text.toString(),
                    city = binding.editTextCity.text.toString(),
                    country = binding.editTextCountry.text.toString()
                )
                onSave?.invoke(locationToSave)
                true
            }
            is LocationValidator.ValidationResult.Invalid -> {
                binding.textInputName.error = validationResult.nameError?.let {
                    getString(it)
                }
                false
            }
        }
    }

    fun setOnSaveListener(listener: (Location) -> Unit) {
        onSave = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}