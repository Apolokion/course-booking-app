package biz.pock.coursebookingapp.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.utils.AlertUtils
import timber.log.Timber

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d(">>> DownloadReceiver.onReceive called")

        if (context == null) {
            Timber.e(">>> Context is null in DownloadReceiver")
            return
        }

        if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            Timber.e(">>> Wrong action received: ${intent?.action}")
            return
        }

        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) {
            Timber.e(">>> Invalid download ID received")
            return
        }

        Timber.d(">>> Processing download ID: $id")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)

        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) {
                Timber.e(">>> No download found for ID: $id")
                return
            }

            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (columnIndex == -1) {
                Timber.e(">>> Status column not found in cursor")
                return
            }

            val status = cursor.getInt(columnIndex)
            Timber.d(">>> Download status for ID $id: $status")

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Timber.d(">>> Download successful")
                    AlertUtils.showInfo(
                        titleRes = R.string.alertdialog_info,
                        textRes = R.string.success_downlad_invoice
                    )

                }
                DownloadManager.STATUS_FAILED -> {
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                    Timber.e(">>> Download failed with reason: $reason")

                    AlertUtils.showInfo(
                        titleRes = R.string.alertdialog_error,
                        textRes = R.string.error_download_failed
                    )

                }
                else -> Timber.d(">>> Download status: $status")
            }
        }
    }
}