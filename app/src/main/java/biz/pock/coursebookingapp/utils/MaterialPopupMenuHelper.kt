package biz.pock.coursebookingapp.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.PopupWindow
import androidx.appcompat.widget.PopupMenu
import biz.pock.coursebookingapp.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import timber.log.Timber

// Popup Menü Design anpassen, um z.B. Icons im Menü anzeigen zu lassen
object MaterialPopupMenuHelper {
    fun stylePopupMenu(popupMenu: PopupMenu, context: Context) {
        try {
            // Zunächst benötigen wir Zugriff auf das private Feld mPopup in
            // der PopupMenu Klasse.
            val popupField = PopupMenu::class.java.getDeclaredField("mPopup")
            // privates Feld zugänglich machen
            popupField.isAccessible = true
            // Instanz des menuPopupHelper Menüs holen
            val menuPopupHelper = popupField.get(popupMenu)

            // Anzeige von den Icons im Menü erzwingen mit Reflection
            menuPopupHelper?.javaClass?.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                ?.invoke(menuPopupHelper, true)

            // Material3 Styling auf das Popup Menü anwenden
            val popupWindow = menuPopupHelper?.javaClass?.getDeclaredField("mPopup")
                ?.apply { isAccessible = true }
                ?.get(menuPopupHelper) as? PopupWindow

            popupWindow?.apply {
                setBackgroundDrawable(
                    MaterialShapeDrawable(
                        ShapeAppearanceModel.builder()
                            .setAllCornerSizes(context.resources.getDimension(R.dimen.popup_corner_radius))
                            .build()
                    ).apply {
                        // Hintergrundfarbe setzen
                        fillColor = ColorStateList.valueOf(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorSurface,
                                Color.WHITE
                            )
                        )
                        // Rahmenfarbe setzen
                        strokeColor = ColorStateList.valueOf(
                            MaterialColors.getColor(
                                context,
                                com.google.android.material.R.attr.colorOutline,
                                Color.GRAY
                            )
                        )
                        // Rahmenbreite setzen
                        strokeWidth = context.resources.getDimension(R.dimen.popup_stroke_width)
                        // Schatten Elevation setzen
                        elevation = context.resources.getDimension(R.dimen.popup_elevation)
                    }
                )
            }
        } catch (e: Exception) {
            // Fehler loggen, aber App weiterhin funktionsfähig halten
            Timber.e(e, ">>> Error styling popup menu")
        }
    }
}