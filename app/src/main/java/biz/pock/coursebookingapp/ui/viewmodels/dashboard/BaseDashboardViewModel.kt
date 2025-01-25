package biz.pock.coursebookingapp.ui.viewmodels.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseDashboardViewModel : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data object Empty : UiState()
        data class Error(val message: String) : UiState()
        data class Success<T>(val data: T) : UiState()
    }

    sealed class Event {
        data class ShowMessage(val message: String) : Event()
        data class ShowError(val message: String) : Event()
        data object NavigateBack : Event()
    }

    sealed class Action {
        data object Refresh : Action()
        data object Retry : Action()
    }

    // UI Status
    protected val mutableUiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = mutableUiState.asStateFlow()

    // Events
    protected val mutableEvents = MutableStateFlow<Event?>(null)
    val events = mutableEvents.asStateFlow()

    fun clearEvent() {
        mutableEvents.value = null
    }

    abstract fun handleAction(action: Action)
}