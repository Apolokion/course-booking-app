package biz.pock.coursebookingapp.shared.enums

import android.content.Context
import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class AgeGroup(@StringRes override val resId: Int) : SpinnerItem {
    MIXED(R.string.age_group_mixed),
    CHILDREN_AND_TEENS(R.string.age_group_children_and_teens),
    ADULTS(R.string.age_group_adults);

    companion object {
        fun fromApiString(value: String): AgeGroup? {
            return when(value.lowercase()) {
                "mixed" -> MIXED
                "children_and_teens" -> CHILDREN_AND_TEENS
                "adults" -> ADULTS
                else -> null
            }
        }

        fun toApiString(ageGroup: AgeGroup): String {
            return when(ageGroup) {
                MIXED -> "mixed"
                CHILDREN_AND_TEENS -> "children_and_teens"
                ADULTS -> "adults"
            }
        }

        fun fromLocalizedName(context: Context, localizedName: String): AgeGroup? {
            return entries.firstOrNull { group ->
                context.getString(group.resId).equals(localizedName, ignoreCase = true)
            }
        }
    }
}