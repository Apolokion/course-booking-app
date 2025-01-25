package biz.pock.coursebookingapp.shared.enums

import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class CourseStatus(@StringRes override val resId: Int) : SpinnerItem {
    DRAFT(R.string.course_status_draft),
    PUBLISHED(R.string.course_status_published),
    ARCHIVED(R.string.course_status_archived);

    companion object {
        fun fromApiString(value: String): CourseStatus? {
            return when(value.lowercase()) {
                "draft" -> DRAFT
                "published" -> PUBLISHED
                "archived" -> ARCHIVED
                else -> null
            }
        }

        fun toApiString(status: CourseStatus): String {
            return when(status) {
                DRAFT -> "draft"
                PUBLISHED -> "published"
                ARCHIVED -> "archived"
            }
        }
    }
}