package biz.pock.coursebookingapp.utils

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.shared.interfaces.SpinnerItem

object DropDownUtils {
    // Für Enums
    fun <T> setupEnumDropdown(
        context: Context,
        autoCompleteTextView: AutoCompleteTextView,
        enumValues: Array<T>
    ) where T : Enum<T>, T : SpinnerItem {
        val options = enumValues.map { enumItem ->
            context.getString(enumItem.resId)
        }

        val adapter = ArrayAdapter(
            context,
            R.layout.dropdown_item,
            options
        )
        autoCompleteTextView.setAdapter(adapter)

        // Manuelle Eingabe verhindern
        autoCompleteTextView.isFocusable = false
        autoCompleteTextView.isFocusableInTouchMode = false
    }

     fun tintMenuItem(context: Context, popupMenu: PopupMenu, itemId: Int, colorRes: Int) {
        val item = popupMenu.menu.findItem(itemId) ?: return
        val color = context.getColor(colorRes)

        // Icon einfärben
        item.icon?.setTint(color)

        // Text einfärben, falls möglich
        val menu = popupMenu.menu
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
            val spannableTitle = SpannableString(item.title).apply {
                setSpan(ForegroundColorSpan(color), 0, length, 0)
            }
            item.title = spannableTitle
        }
    }


}