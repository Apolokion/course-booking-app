package biz.pock.coursebookingapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.utils.validators.EmailValidator
import biz.pock.coursebookingapp.utils.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stringProvider: StringProvider,
    private val emailValidator: EmailValidator
) : ViewModel() {

    // Aktueller Zustand des Login-Prozesses
    // - Initial: Noch kein Login-Vorgang gestartet
    // - Loading: Der Login-Vorgang läuft, es wird zB auf die Server-Antwort gewartet
    // - Success: Der Login war erfolgreich
    // - Error: Ein Fehler ist aufgetreten, zB falsche Zugangsdaten oder Netzwerkprobleme
    sealed class LoginState {
        data object Initial : LoginState()
        data object Loading : LoginState()
        data class Success(val userRole: String) : LoginState() // Role hinzugefügt
        data class Error(val error: LoginError) : LoginState()
    }

    // Zustand der Benutzereingaben
    // - Initial: Keine Validierung durchgeführt
    // - Valid: Alle Eingaben sind gültig
    // - Invalid: Eine oder mehrere Eingaben sind ungültig
    sealed class InputState {
        data object Initial : InputState()
        data object Valid : InputState()
        data class Invalid(
            val emailError: Int? = null,  // Resource ID statt String
            val passwordError: Int? = null // Resource ID statt String
        ) : InputState()
    }

    // Zustand des Login Fehlers
    sealed class LoginError {
        data object InvalidCredentials : LoginError()
        data object NetworkError : LoginError()
        data class UnknownError(val message: String? = null) : LoginError()
    }

    // Intern verwalteter StateFlow für den Login-Zustand
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState = _loginState.asStateFlow()

    // Intern verwalteter StateFlow für den Eingabezustand
    private val _inputState = MutableStateFlow<InputState>(InputState.Initial)
    val inputState = _inputState.asStateFlow()

    // Validiert Email- und Passwort Eingabe
    // - Prüft, ob die E-Mail syntaktisch korrekt ist
    // - Prüft, ob das Passwort nicht leer ist
    //   Setzt anschließend den InputState abhängig davon,
    //   ob die Eingaben gültig sind oder nicht
    private fun validateInput(email: String, password: String): Boolean {
        val emailValidation = emailValidator.validate(email)
        val passwordErrorId = if (password.isBlank()) {
            R.string.error_password_required
        } else null

        val isValid = emailValidation is EmailValidator.ValidationResult.Valid
                && passwordErrorId == null

        _inputState.update {
            if (isValid) {
                InputState.Valid
            } else {
                InputState.Invalid(
                    emailError = when (emailValidation) {
                        is EmailValidator.ValidationResult.Invalid -> emailValidation.errorMessageId
                        else -> null
                    },
                    passwordError = passwordErrorId
                )
            }
        }

        return isValid
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                if (!validateInput(email, password)) {
                    return@launch
                }

                _loginState.value = LoginState.Loading

                val loginResponse = authRepository.login(email, password)

                loginResponse.user?.role?.let { role ->
                    _loginState.value = LoginState.Success(role)
                } ?: run {
                    _loginState.value = LoginState.Error(LoginError.InvalidCredentials)
                }

            } catch (e: Exception) {
                Timber.e(e, ">>> Login failed")
                val loginError = when {
                    e.message?.contains("401") == true -> LoginError.InvalidCredentials
                    e.message?.contains("network", ignoreCase = true) == true -> LoginError.NetworkError
                    else -> LoginError.UnknownError(e.message)
                }
                _loginState.value = LoginState.Error(loginError)
            }
        }
    }
}