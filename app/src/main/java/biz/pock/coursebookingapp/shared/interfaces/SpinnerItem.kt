package biz.pock.coursebookingapp.shared.interfaces

import androidx.annotation.StringRes

interface SpinnerItem {
    @get:StringRes
    val resId: Int
}