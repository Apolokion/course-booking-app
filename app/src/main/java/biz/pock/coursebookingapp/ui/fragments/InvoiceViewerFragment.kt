package biz.pock.coursebookingapp.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.databinding.FragmentInvoiceViewerBinding
import biz.pock.coursebookingapp.ui.viewmodels.InvoiceViewerViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import com.rajat.pdfviewer.PdfRendererView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@AndroidEntryPoint
class InvoiceViewerFragment : Fragment() {

    private var _binding: FragmentInvoiceViewerBinding? = null
    private val binding get() = _binding!!

    private val args: InvoiceViewerFragmentArgs by navArgs()
    private val viewModel: InvoiceViewerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar Setup für Back-Navigation
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.invoice_viewer_title)
        }

        // Menü Provider für BackButton
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Hier könnten wir ein Menü inflaten, wenn wir eins brauchen
                // vielleicht kommt später noch eines dazu, sonst hätten wir das
                // auch in der MainActivity handeln können
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        findNavController().navigateUp()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupPdfViewer()
        observeViewModel()
        viewModel.loadInvoicePdf(args.invoiceId)
    }

    private fun setupPdfViewer() {
        binding.pdfView.statusListener = object : PdfRendererView.StatusCallBack {
            override fun onPdfLoadStart() {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {
                // Fortschrittsanzeige vielleicht für später, aktuell
                // in der Konsole ausgeben. Wird aber bei der Dateigröße
                // wohl eher sinnfrei sein
                Timber.d(">>> Loading PDF: $progress%")
            }

            override fun onPdfLoadSuccess(absolutePath: String) {
                binding.progressBar.visibility = View.GONE
            }

            override fun onError(error: Throwable) {
                binding.progressBar.visibility = View.GONE
                Timber.e(error, ">>> Error displaying PDF")
                AlertUtils.showError(textRes = R.string.error_displaying_invoice)
            }

            override fun onPageChanged(currentPage: Int, totalPage: Int) {
                Timber.d(">>> PDF Page $currentPage of $totalPage")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleState(state)
                }
            }
        }
    }

    private fun handleState(state: InvoiceViewerViewModel.InvoiceViewerState) {
        when (state) {
            is InvoiceViewerViewModel.InvoiceViewerState.Success -> {
                binding.progressBar.visibility = View.GONE
                showPdf(state.pdfUri)
            }
            is InvoiceViewerViewModel.InvoiceViewerState.Error -> {
                binding.progressBar.visibility = View.GONE
                AlertUtils.showError(textRes = R.string.error_loading_invoice)
            }
            InvoiceViewerViewModel.InvoiceViewerState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }
        }
    }

    private fun showPdf(uri: Uri) {
        try {
            val file = uriToFile(uri)
            binding.pdfView.initWithFile(file)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error initializing PDF")
            AlertUtils.showError(textRes = R.string.error_displaying_invoice)
        }
    }

    private fun uriToFile(uri: Uri): File {
        val context = requireContext()
        val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file
    }

    override fun onDestroyView() {
        // Toolbar zurücksetzen wenn wir das Fragment verlassen
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        binding.pdfView.closePdfRender()
        super.onDestroyView()
        _binding = null

        context?.cacheDir?.listFiles { file ->
            file.name.startsWith("temp_pdf_") && file.extension == "pdf"
        }?.forEach { file ->
            file.delete()
        }
    }
}