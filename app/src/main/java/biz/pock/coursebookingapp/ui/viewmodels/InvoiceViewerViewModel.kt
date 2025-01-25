package biz.pock.coursebookingapp.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.pock.coursebookingapp.data.repositories.FileRepository
import biz.pock.coursebookingapp.data.repositories.InvoiceRepository
import biz.pock.coursebookingapp.utils.ErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class InvoiceViewerViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val fileRepository: FileRepository,
    private val errorHandler: ErrorHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceViewerState>(InvoiceViewerState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class InvoiceViewerState {
        data object Loading : InvoiceViewerState()
        data class Success(val pdfUri: Uri) : InvoiceViewerState()
        data class Error(val message: String) : InvoiceViewerState()
    }

    fun loadInvoicePdf(invoiceId: String) {
        viewModelScope.launch {
            _uiState.value = InvoiceViewerState.Loading
            try {
                val uri = invoiceRepository.downloadInvoicePdf(invoiceId)
                _uiState.value = InvoiceViewerState.Success(uri)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading PDF")
                _uiState.value = InvoiceViewerState.Error(errorHandler.handleApiError(e))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fileRepository.clearTempFiles("temp_pdf_", ".pdf")
    }
}