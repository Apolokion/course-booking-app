package biz.pock.coursebookingapp.data.repositories

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.model.Invoice
import biz.pock.coursebookingapp.utils.AlertUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val apiClient: ApiClient,
    @ApplicationContext private val context: Context
) {

    fun getInvoices(): Flow<List<Invoice>> = flow {
        val response = apiClient.apiServiceInvoices.getInvoices()
        if (response.isSuccessful) {
            emit(response.body() ?: emptyList())
        } else {
            throw HttpException(response)
        }
    }

    suspend fun downloadInvoice(invoiceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!isExternalStorageWritable()) {
                    Timber.e(">>> External storage not writable")
                    AlertUtils.showError(textRes = R.string.error_download_directory_access)
                    return@withContext false
                }

                val response = apiClient.apiServiceInvoices.downloadInvoice(invoiceId)
                if (!response.isSuccessful) {
                    Timber.e(">>> Download request failed with code: ${response.code()}")
                    AlertUtils.showError(textRes = R.string.error_download_failed)
                    return@withContext false
                }

                val filename = response.headers()["Content-Disposition"]?.let { header ->
                    getFileNameFromContentDisposition(header)
                } ?: "invoice_$invoiceId.pdf"

                Timber.d(">>> Saving file as: $filename")

                try {
                    response.body()?.let { body ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+ verwendet MediaStore
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                                put(MediaStore.Downloads.IS_PENDING, 1)
                            }

                            val uri = context.contentResolver.insert(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                contentValues
                            ) ?: throw IOException("Failed to create MediaStore entry")

                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                body.byteStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            context.contentResolver.update(uri, contentValues, null, null)
                        } else {
                            // Vor Android 10: Direct File Access mit MediaScanner
                            @Suppress("DEPRECATION")
                            val downloadDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val destinationFile = File(downloadDir, filename)

                            destinationFile.outputStream().use { fileOut ->
                                body.byteStream().use { bodyIn ->
                                    bodyIn.copyTo(fileOut)
                                }
                            }

                            // MediaScanner über die neue API informieren
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(destinationFile.absolutePath),
                                arrayOf("application/pdf")
                            ) { path, uri ->
                                Timber.d(">>> Scan completed: Path=$path, Uri=$uri")
                            }
                        }

                        AlertUtils.showSuccess(
                            textRes = R.string.success_downlad_invoice,
                            duration = 1000
                        )
                        return@withContext true
                    } ?: run {
                        Timber.e(">>> Response body is null")
                        AlertUtils.showError(textRes = R.string.error_download_failed)
                        return@withContext false
                    }
                } catch (e: Exception) {
                    Timber.e(e, ">>> Error saving file: ${e.message}")
                    AlertUtils.showError(textRes = R.string.error_download_failed)
                    return@withContext false
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error in download process: ${e.message}")
                AlertUtils.showError(textRes = R.string.action_download_invoice_error)
                return@withContext false
            }
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun getFileNameFromContentDisposition(contentDisposition: String?): String? {
        return contentDisposition?.let {
            val pattern = Pattern.compile("filename\\s*=\\s*\"?([^\"]+)")
            val matcher = pattern.matcher(it)
            if (matcher.find()) {
                matcher.group(1)
            } else null
        }
    }


    suspend fun extendInvoiceToken(invoiceId: String, expiryDate: String): Invoice {
        val response = apiClient.apiServiceInvoices.extendDownloadToken(
            invoiceId,
            mapOf("download_token_expires_at" to expiryDate)
        )
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun cancelInvoice(invoiceId: String): Invoice {
        val response = apiClient.apiServiceInvoices.cancelInvoice(invoiceId)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun downloadInvoicePdf(invoiceId: String): Uri {
        return withContext(Dispatchers.IO) {
            val response = apiClient.apiServiceInvoices.downloadInvoice(invoiceId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val responseBody = response.body() ?: throw Exception("Empty PDF response")

            // Temporäre Datei erstellen
            val file = File(context.cacheDir, "invoice_$invoiceId.pdf")
            file.outputStream().use { outputStream ->
                responseBody.byteStream().copyTo(outputStream)
            }

            // URI für die temporäre Datei erstellen
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
    }
}