package biz.pock.coursebookingapp.shared.enums

import android.content.Context
import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class CourseType(@StringRes override val resId: Int) : SpinnerItem {
    PRIVATE(R.string.course_type_private),
    PUBLIC(R.string.course_type_public);

    companion object {
        fun fromApiString(value: String): CourseType? {
            return when(value.lowercase()) {
                "private" -> PRIVATE
                "public" -> PUBLIC
                else -> null
            }
        }

        fun toApiString(type: CourseType): String {
            return when(type) {
                PRIVATE -> "private"
                PUBLIC -> "public"
            }
        }

        fun fromLocalizedName(context: Context, localizedName: String): CourseType? {
            return entries.firstOrNull { type ->
                context.getString(type.resId).equals(localizedName, ignoreCase = true)
            }
        }
    }
}