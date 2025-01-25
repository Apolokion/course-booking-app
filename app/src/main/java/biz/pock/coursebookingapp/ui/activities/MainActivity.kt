package biz.pock.coursebookingapp.ui.activities

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.app.CourseBookingApp
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.databinding.ActivityMainBinding
import biz.pock.coursebookingapp.shared.GUEST_EMAIL
import biz.pock.coursebookingapp.shared.GUEST_PASSWORD
import biz.pock.coursebookingapp.shared.KEY_PENDING_THEME_CHANGE
import biz.pock.coursebookingapp.utils.MaterialPopupMenuHelper
import biz.pock.coursebookingapp.utils.ThemeLanguageUtils
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

// Die Activity wird mit @AndroidEntrypoint als Ziel für Dependency Injection
// mit Dagger Hilt markiert
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Binding für Zugriff auf UI-Elemente. Aktiviert wird ViewBinding in
    // der build.gradle (app) unter den BuildFeatures mit viewBinding = true
    // So erspart man sich die ständige Zuweisung mit findViewById ....
    private lateinit var binding: ActivityMainBinding
    private var pendingThemeChange = false

    // Für spezifischen Zurück-Button Callback den wir zB
    // im GuestTimeslotListFragment benötigen, um zur korrekten
    // Tab zu navigieren
    private var customNavigateUpCallback: (() -> Boolean)? = null

    @Inject
    lateinit var authRepository: AuthRepository

    // Global definierter NavController
    private lateinit var navController: NavController

    // Änderungen Vlad
    private lateinit var appBarLayout: AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Theme Change Status wiederherstellen
        pendingThemeChange =
            savedInstanceState?.getBoolean(KEY_PENDING_THEME_CHANGE, false) ?: false

        // ViewBinding initialisieren
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment und NavController initialisieren
        // Initialize the views here, after setContentView()
        appBarLayout = findViewById(R.id.appBarLayout)

        // NavController initialisieren
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Toolbar als ActionBar für die Activity setzen
        setSupportActionBar(binding.toolbarMain)

        // Auth Check und Navigation
        checkAuthAndNavigate()

        // App Titel und Back-Button handling
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.guestBookingsFillOutFormFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.book_course_preview)
                        setDisplayHomeAsUpEnabled(true)
                        setDisplayShowHomeEnabled(true)
                    }
                }
                R.id.guestDashboardFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.app_name)
                        setDisplayHomeAsUpEnabled(false)
                        setDisplayShowHomeEnabled(false)
                    }
                }
                R.id.guestTimeslotListFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.available_timeslots)
                        setDisplayHomeAsUpEnabled(true)
                        setDisplayShowHomeEnabled(true)
                    }
                }
                R.id.loginFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.login_title)
                        setDisplayHomeAsUpEnabled(true)
                        setDisplayShowHomeEnabled(true)
                    }
                }
                R.id.trainerDashboardFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.trainerdashboard_title)
                        setDisplayHomeAsUpEnabled(false)
                        setDisplayShowHomeEnabled(false)
                    }
                }
                R.id.adminDashboardFragment -> {
                    supportActionBar?.apply {
                        title = getString(R.string.admindashboard_title)
                        setDisplayHomeAsUpEnabled(false)
                        setDisplayShowHomeEnabled(false)
                    }
                }
                else -> {}
            }
        }

    }


    // Main Menü inflaten
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // Logout-Button nur anzeigen, wenn eingeloggt und nicht guest
        val logoutItem = menu.findItem(R.id.action_logout)
        logoutItem.isVisible = authRepository.isLoggedIn() &&
                authRepository.getCurrentRole() != "guest"

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val themeItem = menu.findItem(R.id.action_theme)
        val isDarkMode = ThemeLanguageUtils.isDarkModeEnabled(this)

        // Setze je nach Theme Mode das passende Icon für
        // Dark- oder Lightmode
        themeItem.setIcon(
            if (isDarkMode) R.drawable.ic_light_mode
            else R.drawable.ic_dark_mode
        )

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PENDING_THEME_CHANGE, pendingThemeChange)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                toggleTheme()
                true
            }

            R.id.action_language -> {
                showLanguageMenu(findViewById(R.id.action_language))
                true
            }

            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_logout_title)
            .setMessage(R.string.dialog_logout_message)
            .setPositiveButton(R.string.button_logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            authRepository.logout()

            delay(300)
            authRepository.login(GUEST_EMAIL, GUEST_PASSWORD)

            // Nach dem Logout zum Login navigieren
            navController.navigate(R.id.guestDashboardFragment)

            // Menü aktualisieren
            invalidateOptionsMenu()
        }
    }

    private fun checkAuthAndNavigate() {
        lifecycleScope.launch {
            try {
                if (!authRepository.isLoggedIn()) {
                    // Automatischer Guest-Login
                    authRepository.login(GUEST_EMAIL, GUEST_PASSWORD)
                }

                when (authRepository.getCurrentRole()) {
                    "admin" -> navController.navigate(R.id.adminDashboardFragment)
                    "trainer" -> navController.navigate(R.id.trainerDashboardFragment)
                    else -> {
                        // Standardmäßig immer das guestDashboard verwenden
                        navController.navigate(R.id.guestDashboardFragment)
                    }
                }

                // Menü aktualisieren, nun mit Verzögerung, damit
                // der Logout Button immer angezeigt wird
                delay(500)
                invalidateOptionsMenu()

            } catch (e: Exception) {
                Timber.e(e, ">>> Error during guest login")
                // Trotzdem zum GuestDashboard navigieren
                navController.navigate(R.id.guestDashboardFragment)
            }
        }
    }

    private fun toggleTheme() {
        val newMode = if (ThemeLanguageUtils.isDarkModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        ThemeLanguageUtils.saveDarkModePreference(newMode == AppCompatDelegate.MODE_NIGHT_YES, this)

        // Theme Change Status setzen
        pendingThemeChange = true
        ThemeLanguageUtils.setPendingThemeChange(this, true)

        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    // Popup Menü für die Sprachauswahl
    private fun showLanguageMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_language_options, popup.menu)

        MaterialPopupMenuHelper.stylePopupMenu(popup, this)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.language_de -> {
                    changeLanguage("de")
                    true
                }

                R.id.language_en -> {
                    changeLanguage("en")
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun changeLanguage(languageCode: String) {
        (application as CourseBookingApp).setLocale(languageCode)
        recreate()
    }

    // Ganz wichtig um den Base-Context für die Sprachauswahl anzuwenden
    // Ohne diesen Part funktioniert die Änderunge zur Laufzeit nicht.
    // Auch der Dark-Light Mode Switch muss hier gehandelt werden
    override fun attachBaseContext(newBase: Context) {
        // Dark Mode
        val isDarkMode = ThemeLanguageUtils.isDarkModeEnabled(newBase)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        val languageCode = ThemeLanguageUtils.getSavedLanguage(newBase)
        val locale = Locale(languageCode)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        // Neuen Context mit der oben angepassten Konfiguration erstellen
        val context = newBase.createConfigurationContext(config)
        // Neuen Context zuweisen
        super.attachBaseContext(context)
    }

    fun setCustomNavigateUpCallback(callback: (() -> Boolean)?) {
        customNavigateUpCallback = callback
    }

    override fun onSupportNavigateUp(): Boolean {
        // Wenn ein Custom Callback gesetzt ist, diesen ausführen
        customNavigateUpCallback?.let { callback ->
            return callback()
        }
        // Ansonsten Standard-Navigation
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}