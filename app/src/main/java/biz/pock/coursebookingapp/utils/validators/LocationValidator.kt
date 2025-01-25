package biz.pock.coursebookingapp.utils.validators

import biz.pock.coursebookingapp.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationValidator @Inject constructor() {
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val nameError: Int? = null) : ValidationResult()
    }

    fun validate(name: String): ValidationResult {
        val nameError = if (name.isBlank()) {
            R.string.error_location_name_required
        } else null

        return if (nameError == null) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(nameError = nameError)
        }
    }
}