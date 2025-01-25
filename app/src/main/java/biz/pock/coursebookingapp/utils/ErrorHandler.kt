package biz.pock.coursebookingapp.utils

import android.content.Context
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.enums.ApiErrorMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stringProvider: StringProvider
) {
    data class ApiErrorResponse(
        val message: String? = null,
        val errors: Map<String, List<String>>? = null
    )

    fun handleApiError(throwable: Throwable): String {
        val errorMessage = when (throwable) {
            is HttpException -> handleHttpError(throwable)
            is SocketTimeoutException -> context.getString(R.string.error_timeout)
            is UnknownHostException -> context.getString(R.string.error_connection)
            else -> {
                // Versuche den Fehlerstring zu parsen, falls es sich um einen JSON-String handelt
                throwable.message?.let { errorMsg ->
                    try {
                        parseErrorString(errorMsg)
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error parsing message string: $errorMsg")
                        translateApiError(errorMsg) ?: context.getString(R.string.error_unknown)
                    }
                } ?: context.getString(R.string.error_unknown)
            }
        }

        // Logging
        Timber.e(throwable, ">>> API Error: $errorMessage")

        // Alert anzeigen
        showAlert(errorMessage)

        return errorMessage
    }

    private fun parseErrorString(jsonString: String): String {
        return try {
            Timber.d(">>> Parsing error string: $jsonString")

            // Versuche zuerst als ApiErrorResponse zu parsen
            val errorResponse = Gson().fromJson(jsonString, ApiErrorResponse::class.java)

            if (errorResponse != null) {
                buildErrorMessage(errorResponse)
            } else {
                // Wenn das nicht klappt, versuche als einfachen String zu behandeln
                translateApiError(jsonString) ?: jsonString
            }
        } catch (e: JsonSyntaxException) {
            // Wenn es kein gültiges JSON ist, versuche trotzdem die Nachricht zu übersetzen
            Timber.e(e, ">>> Error parsing JSON string: $jsonString")
            translateApiError(jsonString) ?: jsonString
        }
    }

    private fun handleHttpError(exception: HttpException): String {
        return when (exception.code()) {
            401 -> context.getString(R.string.error_unauthorized)
            403 -> context.getString(R.string.error_forbidden)
            404 -> context.getString(R.string.error_not_found)
            405 -> context.getString(R.string.error_not_supported)
            422 -> parseValidationError(exception)
            in 500..599 -> context.getString(R.string.error_server)
            else -> parseErrorBody(exception)
        }
    }

    private fun parseValidationError(exception: HttpException): String {
        return try {
            val errorResponse = parseErrorResponse(exception)
            buildErrorMessage(errorResponse)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error parsing validation error")
            context.getString(R.string.error_validation)
        }
    }

    private fun parseErrorBody(exception: HttpException): String {
        return try {
            val errorResponse = parseErrorResponse(exception)
            buildErrorMessage(errorResponse)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error parsing error body")
            context.getString(R.string.error_unknown)
        }
    }

    private fun parseErrorResponse(exception: HttpException): ApiErrorResponse {
        val errorBody = exception.response()?.errorBody()?.string()

        return try {
            Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                ?: ApiErrorResponse(message = errorBody)
        } catch (e: JsonSyntaxException) {
            Timber.e(e, ">>> Error parsing JSON: $errorBody")
            ApiErrorResponse(message = errorBody)
        }
    }

    private fun buildErrorMessage(errorResponse: ApiErrorResponse): String {
        val messageBuilder = StringBuilder()

        // Hauptnachricht übersetzen wenn vorhanden
        errorResponse.message?.let { message ->
            messageBuilder.append(translateApiError(message))
        }

        // Einzelne Fehler hinzufügen wenn vorhanden
        errorResponse.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
            if (messageBuilder.isNotEmpty()) {
                messageBuilder.append("\n\n")
            }
            messageBuilder.append(context.getString(R.string.error_list_header))
            messageBuilder.append("\n")

            errors.forEach { (field, messages) ->
                messages.forEach { message ->
                    messageBuilder.append("• ")
                        .append(formatFieldName(field))
                        .append(": ")
                        .append(translateApiError(message))
                        .append("\n")
                }
            }
        }

        return messageBuilder.toString().trim()
    }

    private fun translateApiError(message: String?): String? {
        if (message == null) return null

        // Prüfe ob der String selbst ein JSON ist
        return try {
            val errorResponse = Gson().fromJson(message, ApiErrorResponse::class.java)
            if (errorResponse != null) {
                buildErrorMessage(errorResponse)
            } else {
                ApiErrorMessage.findMatchingError(message)?.let {
                    context.getString(it.resId)
                } ?: message
            }
        } catch (e: JsonSyntaxException) {
            // Wenn es kein JSON ist, versuche die direkte Übersetzung
            ApiErrorMessage.findMatchingError(message)?.let {
                context.getString(it.resId)
            } ?: message
        }
    }

    private fun formatFieldName(field: String): String {
        return try {
            val resourceId = context.resources.getIdentifier(
                "field_${field.lowercase()}",
                "string",
                context.packageName
            )
            if (resourceId != 0) context.getString(resourceId)
            else field.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error formatting field name: $field")
            field
        }
    }

    private fun showAlert(message: String, errorCode: Int = 0) {

        when (errorCode) {
            405 -> {
                AlertUtils.showInfo(
                    titleRes = R.string.alertdialog_info,
                    text = message,
                    duration = 3000
                )
            }
            else -> {
                AlertUtils.showError(
                    titleRes = R.string.alertdialog_error,
                    text = message,
                    duration = 3000
                )
            }
        }

    }
}