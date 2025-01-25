package biz.pock.coursebookingapp.shared.enums

import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R

enum class ApiErrorMessage(val pattern: String, @StringRes val resId: Int) {
    // Authentifizierung
    INVALID_CREDENTIALS("credentials are incorrect", R.string.error_invalid_credentials),
    EMAIL_TAKEN("email has already been taken", R.string.error_email_taken),

    // Validierung
    REQUIRED_FIELD("field is required", R.string.error_field_required),
    INVALID_FORMAT("invalid format", R.string.error_invalid_format),

    // Berechtigungen
    FORBIDDEN_ACTION("Forbidden action", R.string.error_forbidden_action),

    // Allgemeine Fehler
    NETWORK_ERROR("network", R.string.error_network),
    SERVER_ERROR("server error", R.string.error_server),
    NOT_FOUND("not found", R.string.error_not_found),

    // Neue Fehler
    TIMESLOT_HAS_BOOKINGS("Timeslot has bookings and cannot be updated", R.string.error_timeslot_has_bookings),
    TIMESLOT_CAPACITY("Timeslot has bookings and max_capacity can only be increased", R.string.error_timeslot_capacity),
    INVALID_STATUS("The selected status is invalid", R.string.error_invalid_status),
    ARCHIVED_COURSE("Archived courses cannot be updated", R.string.error_archived_course),
    METHOD_NOT_SUPPORTED("method is not supported", R.string.error_method_not_supported),
    PHONE_LENGTH("phone field must be at least 8 characters", R.string.error_phone_length),
    INVALID_SKILL_LEVEL("The selected skill level is invalid", R.string.error_invalid_skill_level);

    companion object {
        fun findMatchingError(message: String): ApiErrorMessage? {
            return entries.firstOrNull { error ->
                message.lowercase().contains(error.pattern.lowercase())
            }
        }
    }
}