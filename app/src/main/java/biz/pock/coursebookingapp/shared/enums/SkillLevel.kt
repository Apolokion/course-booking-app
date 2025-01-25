package biz.pock.coursebookingapp.shared.enums

import android.content.Context
import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class SkillLevel(@StringRes override val resId: Int) : SpinnerItem {
    BEGINNER(R.string.skill_level_beginner),
    INTERMEDIATE(R.string.skill_level_intermediate);
    // Ist in der API dokumentiert unter den enums, geht aber nicht
    //ADVANCED(R.string.skill_level_advanced);

    companion object {
        fun fromApiString(value: String): SkillLevel? {
            return when(value.lowercase()) {
                "beginner" -> BEGINNER
                "intermediate" -> INTERMEDIATE
                "anfänger" -> BEGINNER
                "fortgeschritten" -> INTERMEDIATE
                // Ist in der API dokumentiert, aber geht naürlich nicht
                //"advanced" -> ADVANCED
                //"profi" -> ADVANCED
                else -> null
            }
        }

        fun toApiString(skillLevel: SkillLevel): String {
            return when(skillLevel) {
                BEGINNER -> "beginner"
                INTERMEDIATE -> "intermediate"
                // Ist in der API dokumentiert, geht aber nicht
                //ADVANCED -> "advanced"
            }
        }

        fun fromLocalizedName(context: Context, localizedName: String): SkillLevel? {
            return entries.firstOrNull { level ->
                context.getString(level.resId).equals(localizedName, ignoreCase = true)
            }
        }
    }
}