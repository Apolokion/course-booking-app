package biz.pock.coursebookingapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.data.model.BookingInvoiceContact
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.GuestBookingDetails
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.data.repositories.BookingRepository
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.shared.GUEST_EMAIL
import biz.pock.coursebookingapp.shared.GUEST_PASSWORD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GuestDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    val courseRepository: CourseRepository
) : ViewModel() {

    // UI States
    sealed class UiState {
        data object Loading : UiState()
        data object Empty : UiState()
        data class Error(val message: String) : UiState()
        data class Success<T>(val data: T) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Buchungen für Gäste
    private val _guestBookings = MutableStateFlow<List<Booking>>(emptyList())
    val guestBookings: StateFlow<List<Booking>> = _guestBookings.asStateFlow()

    // Timeslots für den ausgewählten Kurs
    private val _courseTimeslots = MutableStateFlow<List<Timeslot>>(emptyList())
    // Temporär deaktiviert, wird vielleicht später wieder benötigt
    //val courseTimeslots: StateFlow<List<Timeslot>> = _courseTimeslots.asStateFlow()

    // Verfügbare Locations für den ausgewählten Kurs
    private val _availableLocations = MutableStateFlow<List<Location>>(emptyList())
    val availableLocations: StateFlow<List<Location>> = _availableLocations.asStateFlow()

    // States für die Timeslot Ansicht
    private val _selectedLocationId = MutableStateFlow<String?>(null)
    // Temporär deaktiviert, wird vielleicht später wieder benötigt
    //val selectedLocationId = _selectedLocationId.asStateFlow()

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    // Temporär deaktiviert, wird vielleicht später wieder benötigt
    //val selectedCourseId = _selectedCourseId.asStateFlow()

    // Filtered states
    private val _filteredTimeslots = MutableStateFlow<List<Timeslot>>(emptyList())
    val filteredTimeslots = _filteredTimeslots.asStateFlow()

    // Buchungsdetails für Gast
    private val _guestBookingDetails = MutableStateFlow<List<GuestBookingDetails>>(emptyList())
    val guestBookingDetails: StateFlow<List<GuestBookingDetails>> = _guestBookingDetails.asStateFlow()


    // States für die Kurse
    private val _guestCourses = MutableStateFlow<List<Course>>(emptyList())
    val guestCourses = _guestCourses.asStateFlow()

    init {
        loadGuestData()
    }

    private fun loadGuestData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Auf Nummer sicher gehen, dass der Gast eingeloggt ist
                // speziell für den ersten Start der App
                ensureGuestLogin()

                courseRepository.getCourses(
                    status = "published",
                    with = "timeslots,locations"
                ).collectLatest { allCourses ->

                    Timber.d(">>> ALL COURSES: ${allCourses.size}, courses: ${allCourses.map { it.title }}")

                    // Filter Kurse ohne zukünftige Timeslots
                    val availableCourses = allCourses.filter { course ->
                        hasUpcomingTimeslots(course)
                    }

                    Timber.d(">>> FILTERED COURSES: ${availableCourses.size}, courses: ${availableCourses.map { it.title }}")
                    _guestCourses.value = availableCourses

                }

                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading guest data")
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun ensureGuestLogin() {
        if (!authRepository.isLoggedIn()) {
            try {
                val response = authRepository.login(GUEST_EMAIL, GUEST_PASSWORD)
                Timber.d(">>> ensureGuestLogin Response: $response")
                if (!authRepository.isLoggedIn()) {
                    throw Exception("Login failed")
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Guest login failed")
                throw e
            }
        }
    }

    suspend fun loadGuestBookingDetails() {
        try {
            _uiState.value = UiState.Loading

            // Basis-Buchungen laden
            val bookings = bookingRepository.getGuestBookings()

            // Details für jede Buchung laden
            val detailedBookings = bookings.map { booking ->
                GuestBookingDetails.fromBooking(
                    booking = booking,
                    participants = loadParticipants(booking.id),
                    invoiceContact = loadInvoiceContact(booking.id),
                    timeslot = loadTimeslotDetails(booking.timeslotId)
                )
            }

            _guestBookingDetails.value = detailedBookings
            _uiState.value = UiState.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading guest booking details")
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }

    private fun hasUpcomingTimeslots(course: Course): Boolean {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return course.timeslots?.any { timeslot ->
            try {
                val endDate = LocalDate.parse(timeslot.endDate, dateFormatter)
                endDate >= today
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing date for timeslot ${timeslot.id}")
                false
            }
        } ?: false
    }


    fun loadCourseTimeslots(courseId: String, preSelectedLocationId: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Locations für den Kurs laden
                val locations = courseRepository.getCourseLocations(courseId)
                _availableLocations.value = locations

                // Timeslots für alle Locations laden
                val allTimeslots = mutableListOf<Timeslot>()
                locations.forEach { location ->
                    val timeslots = courseRepository.getCourseLocationTimeslots(courseId, location.id)
                    allTimeslots.addAll(timeslots)
                }

                // Nur zukünftige Timeslots filtern
                val futureTimeslots = filterFutureTimeslots(allTimeslots)
                _courseTimeslots.value = futureTimeslots

                // TimeEntries für jeden Timeslot im Hintergrund laden
                futureTimeslots.forEach { timeslot ->
                    launch {
                        loadTimeslotTimeEntries(timeslot.id)
                    }
                }

                // Initial filtern wenn Location vorausgewählt
                if (preSelectedLocationId != null) {
                    _selectedLocationId.value = preSelectedLocationId
                    _filteredTimeslots.value = filterTimeslotsByLocationId(preSelectedLocationId, futureTimeslots)
                } else {
                    _filteredTimeslots.value = futureTimeslots
                }

                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading course timeslots")
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun filterTimeslotsByLocationId(locationId: String?, timeslots: List<Timeslot>): List<Timeslot> {
        return if (locationId == null) {
            timeslots
        } else {
            timeslots.filter { it.locationId == locationId }
        }
    }

    fun filterTimeslotsByLocation(locationId: String?) {
        if (locationId == null) {
            // Alle Timeslots anzeigen
            _filteredTimeslots.value = _courseTimeslots.value
        } else {
            // Nach Location filtern
            _filteredTimeslots.value = _courseTimeslots.value.filter {
                it.locationId == locationId
            }
        }
    }

    suspend fun loadTimeslotTimeEntries(timeslotId: String) {
        try {
            val timeEntries = courseRepository.getTimeslotTimeEntries(timeslotId)
            // Timeslot mit TimeEntries aktualisieren
            val updatedTimeslots = _courseTimeslots.value.map { timeslot ->
                if (timeslot.id == timeslotId) {
                    timeslot.copy(timeEntries = timeEntries)
                } else {
                    timeslot
                }
            }
            _courseTimeslots.value = updatedTimeslots
            _filteredTimeslots.value = filterTimeslotsByLocationId(_selectedLocationId.value, updatedTimeslots)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading time entries for timeslot $timeslotId")
        }
    }

    private fun filterFutureTimeslots(timeslots: List<Timeslot>): List<Timeslot> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return timeslots.filter { timeslot ->
            try {
                val endDate = LocalDate.parse(timeslot.endDate, formatter)
                endDate >= today
            } catch (e: Exception) {
                Timber.e(e, ">>> Error parsing date for timeslot ${timeslot.id}")
                false
            }
        }
    }

    fun refreshData() {
        loadGuestData()
    }

    private suspend fun loadParticipants(bookingId: String): List<BookingParticipant>? {
        return try {
            // Nutze vorhandenen Token aus BookingTokenStorage
            bookingRepository.getBookingParticipants(bookingId)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading participants for booking $bookingId")
            null
        }
    }

    private suspend fun loadInvoiceContact(bookingId: String): BookingInvoiceContact? {
        return try {
            // Erst die Buchungsdetails holen um die invoice_contact_id zu bekommen
            val bookingDetails = bookingRepository.getBookingDetails(
                bookingId = bookingId,
                includeDetails = false // Basis-Details reichen
            )

            // Mit der ID dann den Kontakt laden
            bookingDetails.invoiceContactId?.let { contactId ->
                bookingRepository.getBookingInvoiceContact(
                    bookingId = bookingId,
                    contactId = contactId
                )
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading invoice contact for booking $bookingId")
            null
        }
    }

    private suspend fun loadTimeslotDetails(timeslotId: String): Timeslot? {
        return try {
            // Direkt über das CourseRepository den Timeslot laden
            val timeEntries = courseRepository.getTimeslotTimeEntries(timeslotId)
            if (timeEntries.isNotEmpty()) {
                timeEntries.first().timeslot
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading timeslot details for $timeslotId")
            null
        }
    }

}