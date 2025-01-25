package biz.pock.coursebookingapp.shared.enums

import android.content.Context
import androidx.annotation.StringRes
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

enum class Role(@StringRes override val resId: Int) : SpinnerItem {
    ADMIN(R.string.role_admin),
    TRAINER(R.string.role_trainer),
    GUEST(R.string.role_guest);

    companion object {
        // Wandelt den vom Server oder aus User stammenden String ("admin") in ein Role-Enum um
        fun fromUserRoleString(value: String): Role? {
            return when (value.lowercase()) {
                "admin" -> ADMIN
                "trainer" -> TRAINER
                "guest" -> GUEST
                else -> null
            }
        }

        // Wandelt den in der UI angezeigten, lokalisierten String (z.B. "Trainer") zurück in ein Role-Enum
        fun fromLocalizedName(context: Context, localizedName: String): Role? {
            return values().firstOrNull { roleEnum ->
                context.getString(roleEnum.resId).equals(localizedName, ignoreCase = true)
            }
        }

        // Wandelt ein Role-Enum wieder in den String um, den die API erwartet ("admin", "trainer", "guest")
        fun toUserRoleString(role: Role): String {
            return when(role) {
                ADMIN -> "admin"
                TRAINER -> "trainer"
                GUEST -> "guest"
            }
        }
    }
}