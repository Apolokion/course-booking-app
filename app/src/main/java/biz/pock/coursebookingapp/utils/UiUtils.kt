package biz.pock.coursebookingapp.utils

import android.content.Context
import biz.pock.coursebookingapp.R
import kotlin.math.max

object UiUtils {
    fun calculateSpanCount(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val itemMinWidthDp =
            context.resources.getDimension(R.dimen.item_min_width) / displayMetrics.density
        return max(1, (screenWidthDp / itemMinWidthDp).toInt())
    }
}