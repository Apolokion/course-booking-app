package biz.pock.coursebookingapp.utils.validators

import android.util.Patterns
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.utils.StringProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailValidator @Inject constructor(
    // vorbereit für spätere Nutzung, darf nicht entfernt werden!
    private val stringProvider: StringProvider
) {

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val errorMessageId: Int) : ValidationResult()
    }

    fun validate(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Invalid(R.string.error_email_required)
            !email.contains("@") -> ValidationResult.Invalid(R.string.error_email_invalid_format)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult.Invalid(R.string.error_email_invalid)
            !isValidEmailDomain(email) -> ValidationResult.Invalid(R.string.error_email_invalid_domain)
            else -> ValidationResult.Valid
        }
    }

    private fun isValidEmailDomain(email: String): Boolean {
        val domain = email.substringAfterLast('@', "")
        return when {
            domain.isBlank() -> false
            !domain.contains(".") -> false
            domain.endsWith(".") -> false
            domain.startsWith(".") -> false
            else -> true
        }
    }
}