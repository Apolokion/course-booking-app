package biz.pock.coursebookingapp.data.repositories

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {


    fun clearTempFiles(prefix: String, suffix: String) {
        context.cacheDir.listFiles { file ->
            file.name.startsWith(prefix) && file.name.endsWith(suffix)
        }?.forEach { it.delete() }
    }
}