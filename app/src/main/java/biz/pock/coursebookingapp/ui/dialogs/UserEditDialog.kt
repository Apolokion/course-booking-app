package biz.pock.coursebookingapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.User
import biz.pock.coursebookingapp.databinding.DialogUserEditBinding
import biz.pock.coursebookingapp.shared.enums.Role
import biz.pock.coursebookingapp.utils.DropDownUtils
import biz.pock.coursebookingapp.utils.validators.UserValidator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UserEditDialog : DialogFragment() {

    private var _binding: DialogUserEditBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userValidator: UserValidator

    private var user: User? = null
    private var onSave: ((User) -> Unit)? = null

    companion object {
        private const val ARG_USER = "arg_user"

        fun newInstance(user: User? = null) = UserEditDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_USER, user)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUserEditBinding.inflate(layoutInflater)
        user = arguments?.getParcelable(ARG_USER)

        setupViews()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (user == null) R.string.create_user else R.string.edit_user)
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

    private fun validateAndSave(): Boolean {
        val firstname = binding.editTextFirstname.text.toString().trim()
        val lastname = binding.editTextLastname.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val selectedRoleText = binding.dropdownRole.text.toString()

        val selectedRoleEnum = Role.fromLocalizedName(requireContext(), selectedRoleText)
        val userRoleString = selectedRoleEnum?.let { Role.toUserRoleString(it) } ?: ""

        val validationResult = userValidator.validate(
            firstname = firstname,
            lastname = lastname,
            email = email,
            password = password, // Hier immer required
            role = userRoleString,
            isNewUser = (user == null)
        )

        // Fehler zurücksetzen
        binding.textInputFirstname.error = null
        binding.textInputLastname.error = null
        binding.textInputEmail.error = null
        binding.textInputPassword.error = null
        binding.textInputRole.error = null

        return when (validationResult) {
            is UserValidator.ValidationResult.Valid -> {
                val userToSave = User(
                    id = user?.id ?: "",
                    firstname = firstname,
                    lastname = lastname,
                    email = email,
                    role = userRoleString,
                    emailVerifiedAt = user?.emailVerifiedAt,
                    createdAt = user?.createdAt ?: "",
                    updatedAt = user?.updatedAt ?: "",
                    password = password // Muss immer gesetzt werden
                )
                onSave?.invoke(userToSave)
                true
            }

            is UserValidator.ValidationResult.Invalid -> {
                validationResult.firstnameError?.let {
                    binding.textInputFirstname.error = getString(it)
                }
                validationResult.lastnameError?.let {
                    binding.textInputLastname.error = getString(it)
                }
                validationResult.emailError?.let {
                    binding.textInputEmail.error = getString(it)
                }
                validationResult.passwordError?.let {
                    binding.textInputPassword.error = getString(it)
                }
                validationResult.roleError?.let {
                    binding.textInputRole.error = getString(it)
                }
                false
            }
        }
    }

    private fun setupViews() {
        // Role-Dropdown mit Enum
        DropDownUtils.setupEnumDropdown(
            requireContext(),
            binding.dropdownRole,
            Role.entries.toTypedArray()
        )

        user?.let { u ->
            binding.apply {
                editTextFirstname.setText(u.firstname)
                editTextLastname.setText(u.lastname)
                editTextEmail.setText(u.email)
                // Ist eigentlich hinfällig, weil die API das Passwort
                // von einem gespeicherten User nicht zurückgibt
                // aber vielleicht ändert sich die API ja irgendwann mal
                editTextPassword.setText(u.password)

                Timber.d(">>> user: $u")

                val userRoleEnum = Role.fromUserRoleString(u.role)
                userRoleEnum?.let { roleEnum ->
                    dropdownRole.setText(getString(roleEnum.resId), false)
                }

            }
        }
    }

    fun setOnSaveListener(listener: (User) -> Unit) {
        onSave = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}