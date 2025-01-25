package biz.pock.coursebookingapp.app

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import biz.pock.coursebookingapp.utils.ThemeLanguageUtils
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.Locale

// Diese Annotation sorgt dafür, dass Hilt in den Lebenszyklus der App
// integriert wird, wodurch die Dependencies schon beim Start der App
// bereitgestellt werden können
@HiltAndroidApp
class CourseBookingApp : Application() {

    // Instanz für Alert Dialog für die globale Referenz
    // auf die aktuelle Activity
    companion object {
        lateinit var instance: CourseBookingApp
            private set
    }

    // WeakReference wird hier verwendet, um Speicherlecks vorzubeugen
    // da das System die Activity dann trotzdem zerstören kann, was wir
    // zB beim Setzen des Dark-/Lightmode benötigen
    private var _currentActivity: WeakReference<FragmentActivity>? = null

    // Öffentlicher Getter
    val currentActivity: FragmentActivity?
        get() = _currentActivity?.get()

    override fun onCreate() {
        super.onCreate()
        instance = this

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Nur speichern, wenn es wirklich eine FragmentActivity ist
                if (activity is FragmentActivity) {
                    _currentActivity = WeakReference(activity)
                }
            }
            override fun onActivityStarted(activity: Activity) {
                if (activity is FragmentActivity) {
                    _currentActivity = WeakReference(activity)
                }
            }
            override fun onActivityResumed(activity: Activity) {
                if (activity is FragmentActivity) {
                    _currentActivity = WeakReference(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                // Wenn die gerade referenzierte Activity zerstört wird, auf null setzen
                if (_currentActivity?.get() == activity) {
                    _currentActivity = null
                }
            }
        })


        // Debug Tree erstellen
        Timber.plant(Timber.DebugTree())

        // ThreeTenABP initialisieren für erweiterte Zeit- und Datums-Funktionen
        AndroidThreeTen.init(this)

        // Initial Sprache setzen
        val languageCode = ThemeLanguageUtils.getSavedLanguage(this)
        setLocale(languageCode)
    }

    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // Kopie der aktuellen Systemkonfiguration erstellen
        val config = Configuration(resources.configuration)
        // Sprache ändern
        config.setLocale(locale)
        // Neuer Context mit geänderter Konfiguration
        createConfigurationContext(config)

        ThemeLanguageUtils.saveLanguage(languageCode, this)
    }

}