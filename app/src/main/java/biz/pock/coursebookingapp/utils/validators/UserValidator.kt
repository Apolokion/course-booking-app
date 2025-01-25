package biz.pock.coursebookingapp.utils.validators

import biz.pock.coursebookingapp.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserValidator @Inject constructor(
    private val emailValidator: EmailValidator
) {
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(
            val firstnameError: Int? = null,
            val lastnameError: Int? = null,
            val emailError: Int? = null,
            val passwordError: Int? = null,
            val roleError: Int? = null
        ) : ValidationResult()
    }

    fun validate(
        firstname: String,
        lastname: String,
        email: String,
        password: String?,
        role: String,
        isNewUser: Boolean // für später benötigt, nicht löschen!
    ): ValidationResult {
        val firstnameError = if (firstname.isBlank()) {
            R.string.error_firstname_required
        } else null

        val lastnameError = if (lastname.isBlank()) {
            R.string.error_lastname_required
        } else null

        val emailValidation = emailValidator.validate(email)
        val emailError = when (emailValidation) {
            is EmailValidator.ValidationResult.Invalid -> emailValidation.errorMessageId
            else -> null
        }

        // Falls Passwort null oder leer ist: Fehler
        val passwordError = if (password.isNullOrBlank()) {
            R.string.error_password_required
        } else if (password.length < 8) {
            R.string.error_password_too_short
        } else null

        val roleError = if (role.isBlank()) {
            R.string.error_role_required
        } else null

        return if (firstnameError == null && lastnameError == null &&
            emailError == null && passwordError == null && roleError == null
        ) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                firstnameError = firstnameError,
                lastnameError = lastnameError,
                emailError = emailError,
                passwordError = passwordError,
                roleError = roleError
            )
        }
    }
}