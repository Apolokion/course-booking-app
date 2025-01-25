package biz.pock.coursebookingapp.utils

import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.app.CourseBookingApp
import com.tapadoo.alerter.Alerter

object AlertUtils {

    /**
     // AlertDialog in verschiedenen Stilen
     // @param titleRes (optional): Standard ist R.string.alertdialog _success _info, _warning oder _error
     // @param textRes (optional): Falls != 0, wird dieser Resource-Text angezeigt
     // @param text (optional): Falls nicht null, wird dieser String angezeigt (hat Vorrang vor textRes)
     // @param duration Anzeigedauer in Millisekunden
     */
    fun showSuccess(
        @StringRes titleRes: Int = R.string.alertdialog_success,
        @StringRes textRes: Int = 0,
        text: String? = null,
        duration: Long = 3000
    ) {
        showAlert(
            titleRes = titleRes,
            textRes = textRes,
            textString = text,
            backgroundColorRes = R.color.successBackground,
            iconRes = R.drawable.ic_success,
            iconTintRes = R.color.successIconTint,
            duration = duration
        )
    }

    fun showInfo(
        @StringRes titleRes: Int = R.string.alertdialog_info,
        @StringRes textRes: Int = 0,
        text: String? = null,
        duration: Long = 3000
    ) {
        showAlert(
            titleRes = titleRes,
            textRes = textRes,
            textString = text,
            backgroundColorRes = R.color.infoBackground,
            iconRes = R.drawable.ic_info,
            iconTintRes = R.color.infoIconTint,
            duration = duration
        )
    }

    fun showWarning(
        @StringRes titleRes: Int = R.string.alertdialog_warning,
        @StringRes textRes: Int = 0,
        text: String? = null,
        duration: Long = 3000
    ) {
        showAlert(
            titleRes = titleRes,
            textRes = textRes,
            textString = text,
            backgroundColorRes = R.color.warningBackground,
            iconRes = R.drawable.ic_warning,
            iconTintRes = R.color.warningIconTint,
            duration = duration
        )
    }

    fun showError(
        @StringRes titleRes: Int = R.string.alertdialog_error,
        @StringRes textRes: Int = 0,
        text: String? = null,
        duration: Long = 3000
    ) {
        showAlert(
            titleRes = titleRes,
            textRes = textRes,
            textString = text,
            backgroundColorRes = R.color.errorBackground,
            iconRes = R.drawable.ic_error,
            iconTintRes = R.color.errorIconTint,
            duration = duration
        )
    }

    /**
     // Postet die 'Alerter'-Dialoge in den UI-Thread
     // @param titleRes Resource-ID des Titels
     // @param textRes Resource-ID des Textes (falls != 0, wird dieser verwendet, sofern textString == null)
     // @param textString Falls nicht null, hat das Vorrang vor textRes
     */
    private fun showAlert(
        @StringRes titleRes: Int,
        @StringRes textRes: Int,
        textString: String?,
        backgroundColorRes: Int,
        iconRes: Int,
        iconTintRes: Int,
        duration: Long
    ) {
        val activity = CourseBookingApp.instance.currentActivity ?: return

        // Prüfen, ob wir im Main Thread sind
        if (Looper.myLooper() == Looper.getMainLooper()) {
            createAlerter(
                activity, titleRes, textRes, textString,
                backgroundColorRes, iconRes, iconTintRes, duration
            )
        } else {
            Handler(Looper.getMainLooper()).post {
                createAlerter(
                    activity, titleRes, textRes, textString,
                    backgroundColorRes, iconRes, iconTintRes, duration
                )
            }
        }
    }

    // Dialog erstellen und anzeigen
    private fun createAlerter(
        activity: FragmentActivity,
        @StringRes titleRes: Int,
        @StringRes textRes: Int,
        textString: String?,
        backgroundColorRes: Int,
        iconRes: Int,
        iconTintRes: Int,
        duration: Long
    ) {
        // Ermitteln des finalen Textes
        val finalText = textString ?: if (textRes != 0) {
            activity.getString(textRes)
        } else {
            // Fallback, falls gar nichts übergeben wurde
            activity.getString(R.string.default_empty_message)
        }

        Alerter.create(activity).apply {
            // Titel und Text
            setTitle(activity.getString(titleRes))
            setText(finalText)

            // Layout-Stile
            setTitleAppearance(R.style.AlerterTitleStyle)
            setTextAppearance(R.style.AlerterTextStyle)

            // Hintergrundfarbe und Icon
            setBackgroundColorRes(backgroundColorRes)
            setIcon(iconRes)
            setIconColorFilter(ContextCompat.getColor(activity, iconTintRes))

            // Anzeigedauer
            setDuration(duration)

            show()
        }
    }
}