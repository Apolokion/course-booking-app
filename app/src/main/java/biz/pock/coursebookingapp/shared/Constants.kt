package biz.pock.coursebookingapp.shared

const val BASE_URL = "https://dev.api.smashpro.proplex.at"
const val SHARED_PREFS = "course_booking_prefs"

const val KEY_APP_LANGUAGE = "app_language_key"
const val KEY_DARK_MODE = "dark_mode_key"
const val KEY_PENDING_THEME_CHANGE = "pending_theme_change_key"

// Guest Login Konstanten für den default login
// als Gast, weil die API ja nicht mal public Kurse auflisten
// lässt, um diese buchen zu können.
// Das sollte wohl ein API Key handeln, den wir nicht haben
const val GUEST_EMAIL = "guest@pock.biz"
const val GUEST_PASSWORD = "password"