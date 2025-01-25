package biz.pock.coursebookingapp.utils.validators

import android.content.Context
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.enums.SkillLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingFormValidator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emailValidator: EmailValidator
) {
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_name_required)
            )
            name.length < 2 -> ValidationResult.Invalid(
                context.getString(R.string.error_name_too_short)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateEmail(email: String): ValidationResult {
        return when(val result = emailValidator.validate(email)) {
            is EmailValidator.ValidationResult.Valid -> ValidationResult.Valid
            is EmailValidator.ValidationResult.Invalid -> ValidationResult.Invalid(
                context.getString(result.errorMessageId)
            )
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_phone_required)
            )
            phone.length < 8 -> ValidationResult.Invalid(
                context.getString(R.string.error_phone_too_short)
            )
            !phone.matches(Regex("^\\+?[0-9 ()-]+$")) -> ValidationResult.Invalid(
                context.getString(R.string.error_phone_invalid_format)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateAddress(address: String): ValidationResult {
        return when {
            address.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_address_required)
            )
            address.length < 5 -> ValidationResult.Invalid(
                context.getString(R.string.error_address_too_short)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateZip(zip: String): ValidationResult {
        return when {
            zip.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_zip_required)
            )
            !zip.matches(Regex("^[0-9]{4,5}$")) -> ValidationResult.Invalid(
                context.getString(R.string.error_zip_invalid)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateCity(city: String): ValidationResult {
        return when {
            city.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_city_required)
            )
            city.length < 2 -> ValidationResult.Invalid(
                context.getString(R.string.error_city_too_short)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateCountry(country: String): ValidationResult {
        return when {
            country.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_country_required)
            )
            country.length < 2 -> ValidationResult.Invalid(
                context.getString(R.string.error_country_too_short)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateBirthdate(birthdate: String): ValidationResult {
        return when {
            birthdate.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_birthdate_required)
            )
            !birthdate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) -> ValidationResult.Invalid(
                context.getString(R.string.error_birthdate_invalid_format)
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateSkillLevel(skillLevel: String): ValidationResult {
        return when {
            skillLevel.isBlank() -> ValidationResult.Invalid(
                context.getString(R.string.error_skill_level_required)
            )
            // Erst prüfen ob es ein lokalisierter Name ist
            SkillLevel.fromLocalizedName(context, skillLevel) != null -> ValidationResult.Valid
            // Dann prüfen ob es ein API String ist
            SkillLevel.fromApiString(skillLevel) != null -> ValidationResult.Valid
            // Wenn beides nicht zutrifft, dann ist es ungültig
            else -> ValidationResult.Invalid(
                context.getString(R.string.error_skill_level_invalid)
            )
        }
    }
}