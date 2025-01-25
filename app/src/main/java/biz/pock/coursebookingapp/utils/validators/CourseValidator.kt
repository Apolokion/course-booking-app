package biz.pock.coursebookingapp.utils.validators

import biz.pock.coursebookingapp.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseValidator @Inject constructor() {
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(
            val titleError: Int? = null,
            val priceError: Int? = null,
            val typeError: Int? = null
        ) : ValidationResult()
    }

    fun validate(
        title: String,
        price: String,
        type: String
    ): ValidationResult {
        val titleError = if (title.isBlank()) {
            R.string.error_title_required
        } else null

        val priceError = when {
            price.isBlank() -> R.string.error_price_required
            price.toDoubleOrNull() == null -> R.string.error_price_invalid
            price.toDouble() <= 0 -> R.string.error_price_must_be_positive
            else -> null
        }

        val typeError = if (type.isBlank()) {
            R.string.error_type_required
        } else null

        return if (titleError == null && priceError == null && typeError == null) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                titleError = titleError,
                priceError = priceError,
                typeError = typeError
            )
        }
    }
}