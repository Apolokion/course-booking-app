package biz.pock.coursebookingapp.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

object StoragePermissionHelper {

    fun hasPermissions(activity: Activity): Boolean {
        Timber.d(">>> Checking if permissions are granted for SDK: ${Build.VERSION.SDK_INT}")
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val hasPermission = android.os.Environment.isExternalStorageManager()
                Timber.d(">>> MANAGE_EXTERNAL_STORAGE granted: $hasPermission")
                hasPermission
            }

            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                val hasPermission = hasReadStoragePermission(activity)
                Timber.d(">>> READ_EXTERNAL_STORAGE granted: $hasPermission")
                hasPermission
            }

            else -> {
                val hasPermissions = hasStoragePermissions(activity)
                Timber.d(">>> Storage permissions granted: $hasPermissions")
                hasPermissions
            }
        }
    }

    private fun hasReadStoragePermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasStoragePermissions(activity: Activity): Boolean {
        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        return when {
            // Nicht verfügbar für MANAGE_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> false
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
            }
        }
    }
}