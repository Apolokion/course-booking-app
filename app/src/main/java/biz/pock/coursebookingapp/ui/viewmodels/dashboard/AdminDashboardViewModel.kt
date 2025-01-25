package biz.pock.coursebookingapp.ui.viewmodels.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.CourseLocation
import biz.pock.coursebookingapp.data.model.CourseUpdate
import biz.pock.coursebookingapp.data.model.Invoice
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.data.model.User
import biz.pock.coursebookingapp.data.repositories.BookingRepository
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.data.repositories.InvoiceRepository
import biz.pock.coursebookingapp.data.repositories.LocationRepository
import biz.pock.coursebookingapp.data.repositories.UserRepository
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.ErrorHandler
import biz.pock.coursebookingapp.utils.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    // ----------------------------------------------------------------------------------------
    //  Constructor & Dependencies
    // ----------------------------------------------------------------------------------------
    private val userRepository: UserRepository,
    val courseRepository: CourseRepository,
    private val locationRepository: LocationRepository,
    private val bookingRepository: BookingRepository,
    private val invoiceRepository: InvoiceRepository,
    private val errorHandler: ErrorHandler,
    private val stringProvider: StringProvider,
    private val savedStateHandle: SavedStateHandle
) : BaseDashboardViewModel(), TimeslotViewModel {

    // ----------------------------------------------------------------------------------------
    //  States & Filter States
    // ----------------------------------------------------------------------------------------

    val selectedCourseId = MutableStateFlow<String?>(null)
    val selectedLocationId = MutableStateFlow<String?>(null)

    // Filter States für Kurse
    val selectedCourseType = MutableStateFlow<String?>(null)
    val selectedCourseStatus = MutableStateFlow<String?>(null)
    val selectedAgeGroup = MutableStateFlow<String?>(null)

    // States für weitere Filter
    val selectedBookingStatus = MutableStateFlow<String?>(null)

    // Aktueller Filter der Rechnungen für die Anzeige
    // (Weil die API dies aktuell noch nicht unterstützt.)
    val selectedInvoiceStatus = MutableStateFlow<String?>(null)
    val selectedUserRole = MutableStateFlow<String?>(null)

    // State für den aktuellen Tab hinzufügen
    // Der currentTab State muss als savedStateHandle gespeichert werden
    private val _currentTab = MutableStateFlow(savedStateHandle.get<Int>("current_tab") ?: 0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // ----------------------------------------------------------------------------------------
    //  AdminAction
    // ----------------------------------------------------------------------------------------

    sealed class AdminAction : Action() {
        // User Actions
        data object LoadUsers : AdminAction()
        data class FilterUsers(val role: String?) : AdminAction()
        data class CreateUser(val user: User) : AdminAction()
        data class UpdateUser(val user: User) : AdminAction()
        data class DeleteUser(val userId: String) : AdminAction()

        // Location Actions
        data class CreateLocation(val location: Location) : AdminAction()
        data class UpdateLocation(val location: Location) : AdminAction()
        data class DeleteLocation(val locationId: String) : AdminAction()

        // Booking Actions
        data object LoadBookings : AdminAction()
        data class FilterBookings(val status: String?) : AdminAction()
        data class UpdateBookingStatus(val bookingId: String, val status: String) : AdminAction()
        data class DeleteBooking(val bookingId: String) : AdminAction()

        // Invoice Actions
        data object LoadInvoices : AdminAction()
        data class FilterInvoices(val status: String?) : AdminAction()
        data class DownloadInvoice(val invoiceId: String) : AdminAction()
        data class ExtendInvoiceToken(val invoiceId: String, val newExpiryDate: String) :
            AdminAction()

        data class CancelInvoice(val invoiceId: String) : AdminAction()

        // Course Actions
        data object LoadCourses : AdminAction()
        data class FilterCourses(
            val type: String? = null,
            val status: String? = null,
            val ageGroup: String? = null
        ) : AdminAction()

        data class CreateCourse(val course: Course) : AdminAction()
        data class UpdateCourse(val course: Course) : AdminAction()
        data class DeleteCourse(val courseId: String) : AdminAction()
        data class PublishCourse(val courseId: String) : AdminAction()
        data class UnpublishCourse(val courseId: String) : AdminAction()
        data class ArchiveCourse(val courseId: String) : AdminAction()

        // Course Location Actions
        data class AddCourseLocation(
            val courseId: String,
            val locationId: String
        ) : AdminAction()

        data class RemoveCourseLocation(
            val courseId: String,
            val locationId: String
        ) : AdminAction()

        // Timeslot Actions
        data class CreateTimeslot(val timeslot: Timeslot) : AdminAction()
        data class UpdateTimeslot(val timeslot: Timeslot) : AdminAction()
        data class DeleteTimeslot(val timeslotId: String) : AdminAction()
        data class FilterTimeslots(
            val courseId: String?,
            val locationId: String?,
            val dateRange: Pair<Long, Long>? = null
        ) : AdminAction()

        data class UpdateTimeEntries(
            val timeslotId: String,
            val timeEntries: List<TimeEntry>
        ) : AdminAction()

        // ----------------------------------------------------------------------------------------
        //  TimeEntries
        // ----------------------------------------------------------------------------------------
        sealed class TimeEntryActions : AdminAction() {
            data class CreateTimeEntry(
                val timeslotId: String,
                val timeEntry: TimeEntry
            ) : TimeEntryActions()

            data class UpdateTimeEntry(
                val timeslotId: String,
                val timeEntryId: String,
                val timeEntry: TimeEntry
            ) : TimeEntryActions()

            data class DeleteTimeEntry(
                val timeslotId: String,
                val timeEntryId: String
            ) : TimeEntryActions()
        }
    }

    // ----------------------------------------------------------------------------------------
    //  DashboardData StateFlow
    // ----------------------------------------------------------------------------------------

    private val mutableDashboardData = MutableStateFlow(DashboardData())
    override val dashboardData: StateFlow<DashboardData> = mutableDashboardData.asStateFlow()

    // ----------------------------------------------------------------------------------------
    //  init & erste Datenladung
    // ----------------------------------------------------------------------------------------

    init {
        Timber.d(">>> AdminDashboardViewModel: init")
        loadDashboardData()
    }

    // ----------------------------------------------------------------------------------------
    //  Beispiel kombinierter Flow (lokaler Filter)
    // ----------------------------------------------------------------------------------------

    // Die gefilterten Rechnungen (lokal, da API-Filtern noch nicht unterstützt)
    val filteredInvoices = combine(
        mutableDashboardData,
        selectedInvoiceStatus
    ) { data, filter ->
        if (filter == null) {
            // Zeige alle wenn kein Filter
            data.invoices
        } else {
            // Filter auf die gefilterte Liste anwenden, Master-Daten bleiben erhalten
            data.allInvoices.filter { it.status == filter }
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Haupt-Ladefunktion für DashboardData
    // ----------------------------------------------------------------------------------------

    private fun loadDashboardData() {
        viewModelScope.launch {
            mutableUiState.value = UiState.Loading

            Timber.w(">>> TAB loadDasboardData ${currentTab.value}")

            try {
                // Erst den aktuellen Tab laden
                val tab = currentTab.value
                loadCurrentTabData(tab)

                // Dann verzögert den Rest
                launch {
                    delay(500)
                    loadRemainingData(tab)
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading dashboard data")
                val errorMsg = errorHandler.handleApiError(e)
                mutableUiState.value = UiState.Error(errorMsg)
                mutableEvents.value = Event.ShowError(errorMsg)
            }
        }
    }


    // Funktion zum Tab-Wechsel
    fun onTabSelected(tab: Int) {
        _currentTab.value = tab
        savedStateHandle["current_tab"] = tab
    }

    private suspend fun loadCurrentTabData(tab: Int) {
        Timber.w(">>> TAB loadCurrentTabData $tab vs ${_currentTab.value}")
        when (tab) {
            0 -> {
                loadBookings()
                updateUiState()
            }

            1 -> {
                loadInvoices()
                updateUiState()
            }

            2 -> {
                loadCourses()
                loadAllTimeslots()
                updateUiState()
            }

            3 -> {
                loadLocations()
                updateUiState()
            }

            4 -> {
                loadCourses()
                loadAllTimeslots()
                updateUiState()
            }

            5 -> {
                loadUsers()
                updateUiState()
            }
        }
    }

    private suspend fun loadRemainingData(excludeTab: Int) {
        try {
            when (excludeTab) {
                0 -> if (mutableDashboardData.value.bookings.isEmpty()) loadBookings()
                1 -> if (mutableDashboardData.value.invoices.isEmpty()) loadInvoices()
                2, 4 -> {
                    Timber.d(">>> TAB loadRemainingData")
                    if (mutableDashboardData.value.courses.isEmpty()) loadCourses()
                    if (mutableDashboardData.value.allTimeslots.isEmpty()) loadAllTimeslots()
                }

                3 -> if (mutableDashboardData.value.locations.isEmpty()) loadLocations()
                5 -> if (mutableDashboardData.value.users.isEmpty()) loadUsers()
            }

            // Restliche Daten nachladen
            loadOtherData(excludeTab)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading remaining data")
        }
    }

    private suspend fun loadOtherData(excludeTab: Int) {
        viewModelScope.launch {
            if (excludeTab != 0) loadBookingsIfNeeded()
        }
        viewModelScope.launch {
            if (excludeTab != 1) loadInvoicesIfNeeded()
        }
        viewModelScope.launch {
            if (excludeTab !in listOf(2, 4)) {
                loadCoursesIfNeeded()
                loadTimeslotsIfNeeded()
            }
        }
        viewModelScope.launch {
            if (excludeTab != 3) loadLocationsIfNeeded()
        }
        viewModelScope.launch {
            if (excludeTab != 5) loadUsersIfNeeded()
        }
    }

    private suspend fun loadBookingsIfNeeded() {
        if (mutableDashboardData.value.bookings.isEmpty()) {
            loadBookings()
        }
    }

    private suspend fun loadInvoicesIfNeeded() {
        if (mutableDashboardData.value.invoices.isEmpty()) {
            loadInvoices()
        }
    }

    private suspend fun loadCoursesIfNeeded() {
        if (mutableDashboardData.value.courses.isEmpty()) {
            loadCourses()
        }
    }

    private suspend fun loadTimeslotsIfNeeded() {
        if (mutableDashboardData.value.allTimeslots.isEmpty()) {
            loadAllTimeslots()
        }
    }

    private suspend fun loadLocationsIfNeeded() {
        if (mutableDashboardData.value.locations.isEmpty()) {
            loadLocations()
        }
    }

    private suspend fun loadUsersIfNeeded() {
        if (mutableDashboardData.value.users.isEmpty()) {
            loadUsers()
        }
    }

    // Hilfsfunktion um zu prüfen ob noch geladen wird
    private fun isLoading(): Boolean {
        return viewModelScope.coroutineContext[Job]?.children?.any { it.isActive } == true
    }

    // ----------------------------------------------------------------------------------------
    //  Ladefunktionen (Bookings, Invoices, Courses, Timeslots, Locations, Users)
    // ----------------------------------------------------------------------------------------

    private suspend fun loadBookings(status: String? = null) {
        try {
            bookingRepository.getBookings(status).collect { bookings ->
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    bookings = bookings,
                    allBookings = if (mutableDashboardData.value.allBookings.isEmpty()) bookings
                    else mutableDashboardData.value.allBookings
                )
                // Nach dem Laden direkt den Filter anwenden falls gesetzt
                if (selectedBookingStatus.value != null) {
                    filterBookings(selectedBookingStatus.value)
                }
                updateUiState()
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading bookings")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun loadInvoices() {
        try {
            invoiceRepository.getInvoices().collect { invoices ->
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    invoices = invoices,
                    allInvoices = if (mutableDashboardData.value.allInvoices.isEmpty()) invoices
                    else mutableDashboardData.value.allInvoices
                )
                updateUiState()
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading invoices")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun loadCourses() {
        try {
            courseRepository.getCourses(
                type = null, //selectedCourseType.value,
                status = null, //selectedCourseStatus.value,
                ageGroup = null, //selectedAgeGroup.value,
                with = "timeslots,locations"
            ).collect { allCourses ->
                // Lokale Filterung auf die geladenen Kurse anwenden
                val filteredCourses = allCourses.filter { course ->
                    (selectedCourseType.value == null || course.type == selectedCourseType.value) &&
                            (selectedCourseStatus.value == null || course.status == selectedCourseStatus.value) &&
                            (selectedAgeGroup.value == null || course.ageGroup == selectedAgeGroup.value)
                }

                // Dashboard Data aktualisieren mit beiden Listen
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    // Ungefilterte Liste für spätere Filterungen
                    allCourses = allCourses,
                    // Gefilterte Liste für die Anzeige
                    courses = filteredCourses
                )
                updateUiState()
            }

            Timber.w(">>> COURSES LOADED")

        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading courses")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun loadAllTimeslots() {
        try {
            // 1. Erst alle Timeslots ohne TimeEntries laden und anzeigen
            val currentCourses = mutableDashboardData.value.allCourses
            var allTimeslots = mutableListOf<Timeslot>()

            currentCourses.forEach { course ->
                course.timeslots?.let { timeslots ->
                    allTimeslots.addAll(timeslots)
                }
            }

            // Sofort die Timeslots ohne TimeEntries anzeigen
            mutableDashboardData.value = mutableDashboardData.value.copy(
                allTimeslots = allTimeslots,
                timeslots = filterTimeslotsWithCurrentCriteria(allTimeslots)
            )
            updateUiState()

            // TimeEntries im Hintergrund laden
            viewModelScope.launch {
                val updatedTimeslots = mutableListOf<Timeslot>()

                allTimeslots.forEach { timeslot ->
                    try {
                        val timeEntries = courseRepository.getTimeslotTimeEntries(timeslot.id)
                        val updatedTimeslot = timeslot.copy(timeEntries = timeEntries)
                        updatedTimeslots.add(updatedTimeslot)

                        //  Progressives Update der Liste
                        mutableDashboardData.value = mutableDashboardData.value.copy(
                            allTimeslots = updatedTimeslots + allTimeslots.drop(updatedTimeslots.size),
                            timeslots = filterTimeslotsWithCurrentCriteria(
                                updatedTimeslots + allTimeslots.drop(updatedTimeslots.size)
                            )
                        )
                        updateUiState()

                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error loading time entries for timeslot ${timeslot.id}")
                        // Timeslot trotzdem hinzufügen, auch wenn Fehler in TimeEntries
                        updatedTimeslots.add(timeslot)
                    }
                }
            }

        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading all timeslots: ${e.message}")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun loadLocations() {
        try {
            locationRepository.getLocations().collect { locations ->
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    locations = locations,
                    allLocations = if (mutableDashboardData.value.allLocations.isEmpty()) locations
                    else mutableDashboardData.value.allLocations
                )
                updateUiState()
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading locations")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun loadUsers() {
        try {
            userRepository.getUsers().collect { allUsers ->
                // Dashboard Data aktualisieren mit beiden Listen
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    // Ungefilterte Liste für spätere Filterungen
                    allUsers = allUsers,
                    // Gefilterte Liste für die Anzeige, basierend auf selectedUserRole
                    users = filterUsersWithCurrentCriteria(allUsers)
                )
                updateUiState()
            }

            Timber.d(">>> USERS LOADED (mit lokaler Filterung)")

        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading users")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun filterUsersWithCurrentCriteria(users: List<User>): List<User> {
        return users.filter { user ->
            selectedUserRole.value == null || user.role == selectedUserRole.value
        }
    }

    fun loadLocationsForCourse(courseId: String) {
        viewModelScope.launch {
            try {
                mutableUiState.value = UiState.Loading
                val courseLocations = courseRepository.getCourseLocations(courseId)

                mutableDashboardData.value = mutableDashboardData.value.copy(
                    selectedCourseLocations = courseLocations
                )

                mutableUiState.value = UiState.Success(mutableDashboardData.value)
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading course locations")
                mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
            }
        }
    }


    // ----------------------------------------------------------------------------------------
    //  HandleAction
    // ----------------------------------------------------------------------------------------

    override fun handleAction(action: Action) {
        viewModelScope.launch {
            when (action) {
                is Action.Refresh -> loadDashboardData()
                is Action.Retry -> loadDashboardData()

                // User Actions
                is AdminAction.LoadUsers -> loadUsers()
                is AdminAction.FilterUsers -> filterUsers(action.role)
                is AdminAction.CreateUser -> createUser(action.user)
                is AdminAction.UpdateUser -> updateUser(action.user)
                is AdminAction.DeleteUser -> deleteUser(action.userId)

                // Location Actions
                is AdminAction.CreateLocation -> createLocation(action.location)
                is AdminAction.UpdateLocation -> updateLocation(action.location)
                is AdminAction.DeleteLocation -> deleteLocation(action.locationId)

                // Booking Actions
                is AdminAction.LoadBookings -> loadBookings()
                is AdminAction.FilterBookings -> filterBookings(action.status)
                is AdminAction.UpdateBookingStatus -> updateBookingStatus(action)
                is AdminAction.DeleteBooking -> deleteBooking(action)

                // Invoice Actions
                is AdminAction.LoadInvoices -> loadInvoices()
                is AdminAction.FilterInvoices -> filterInvoices(action.status)
                is AdminAction.DownloadInvoice -> downloadInvoice(action)
                is AdminAction.ExtendInvoiceToken -> extendInvoiceToken(action)
                is AdminAction.CancelInvoice -> cancelInvoice(action)

                // Course Actions
                is AdminAction.LoadCourses -> loadCourses()
                is AdminAction.FilterCourses -> filterCourses(
                    action.type,
                    action.status,
                    action.ageGroup
                )

                is AdminAction.CreateCourse -> createCourse(action.course)
                is AdminAction.UpdateCourse -> updateCourse(action.course)
                is AdminAction.DeleteCourse -> deleteCourse(action.courseId)
                is AdminAction.PublishCourse -> publishCourse(action.courseId)
                is AdminAction.UnpublishCourse -> unpublishCourse(action.courseId)
                is AdminAction.ArchiveCourse -> archiveCourse(action.courseId)

                // Course Location Actions
                is AdminAction.AddCourseLocation -> addCourseLocation(action)
                is AdminAction.RemoveCourseLocation -> removeCourseLocation(action)

                // Timeslot Actions
                is AdminAction.CreateTimeslot -> createTimeslot(action)
                is AdminAction.UpdateTimeslot -> updateTimeslot(action)
                is AdminAction.DeleteTimeslot -> deleteTimeslot(action)
                is AdminAction.FilterTimeslots -> filterTimeslots(action)
                is AdminAction.UpdateTimeEntries -> updateTimeEntries(action)

                is TimeslotViewModel.TimeslotAction.UpdateTimeEntries -> {
                    // Konvertiere die TimeslotAction in eine AdminAction
                    updateTimeEntries(
                        AdminAction.UpdateTimeEntries(
                            timeslotId = action.timeslotId,
                            timeEntries = action.timeEntries
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.CreateTimeEntry -> {
                    createTimeEntry(
                        AdminAction.TimeEntryActions.CreateTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntry = action.timeEntry
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.UpdateTimeEntry -> {
                    updateTimeEntry(
                        AdminAction.TimeEntryActions.UpdateTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntryId = action.timeEntryId,
                            timeEntry = action.timeEntry
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.DeleteTimeEntry -> {
                    deleteTimeEntry(
                        AdminAction.TimeEntryActions.DeleteTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntryId = action.timeEntryId
                        )
                    )
                }

                // Time Entries Actions
                is AdminAction.TimeEntryActions.CreateTimeEntry -> createTimeEntry(action)
                is AdminAction.TimeEntryActions.UpdateTimeEntry -> updateTimeEntry(action)
                is AdminAction.TimeEntryActions.DeleteTimeEntry -> deleteTimeEntry(action)

                else -> {
                    Timber.w(">>> Unhandled action type received: ${action::class.simpleName}")
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Filter-Funktionen
    // ----------------------------------------------------------------------------------------

    private fun filterCourses(type: String?, status: String?, ageGroup: String?) {
        selectedCourseType.value = type
        selectedCourseStatus.value = status
        selectedAgeGroup.value = ageGroup

        try {
            val filteredCourses = mutableDashboardData.value.allCourses.filter { course ->
                (type == null || course.type == type) &&
                        (status == null || course.status == status) &&
                        (ageGroup == null || course.ageGroup == ageGroup)
            }

            mutableDashboardData.value = mutableDashboardData.value.copy(
                courses = filteredCourses
            )
            updateUiState()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error filtering courses")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun filterBookings(status: String?) {
        try {
            // Status im ViewModel speichern
            selectedBookingStatus.value = status

            val filteredBookings = if (status == null) {
                mutableDashboardData.value.allBookings
            } else {
                mutableDashboardData.value.allBookings.filter { it.status == status }
            }
            mutableDashboardData.value = mutableDashboardData.value.copy(
                bookings = filteredBookings
            )
            updateUiState()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error filtering bookings")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun filterInvoices(status: String?) {
        try {
            // State aktualisieren
            selectedInvoiceStatus.value = status

            // Liste mit neuen Kriterien filtern
            val filteredInvoices =
                filterInvoicesWithCurrentCriteria(mutableDashboardData.value.allInvoices)

            // Gefilterte Liste setzen
            mutableDashboardData.value = mutableDashboardData.value.copy(
                invoices = filteredInvoices
            )
            updateUiState()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error filtering invoices")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun filterUsers(role: String?) {
        try {
            selectedUserRole.value = role

            val filteredUsers = if (role == null) {
                mutableDashboardData.value.allUsers
            } else {
                mutableDashboardData.value.allUsers.filter { it.role == role }
            }

            mutableDashboardData.value = mutableDashboardData.value.copy(
                users = filteredUsers
            )
            updateUiState()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error filtering users")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    fun clearFilters() {
        selectedCourseType.value = null
        selectedCourseStatus.value = null
        selectedAgeGroup.value = null
        viewModelScope.launch {
            filterCourses(null, null, null)
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Course CRUD
    // ----------------------------------------------------------------------------------------

    private suspend fun createCourse(course: Course) {
        try {
            // API Call zum Erstellen des Kurses
            val createdCourse = courseRepository.createCourse(course)

            // Bestehende Master-Liste kopieren und neuen Kurs hinzufügen
            val currentAllCourses = mutableDashboardData.value.allCourses.toMutableList()
            currentAllCourses.add(createdCourse)

            // Gefilterte Liste mit aktuellen Filtern aktualisieren
            val filteredCourses = filterCoursesWithCurrentCriteria(currentAllCourses)

            // Dashboard Data aktualisieren
            mutableDashboardData.value = mutableDashboardData.value.copy(
                allCourses = currentAllCourses,
                courses = filteredCourses
            )

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_created_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateCourse(course: Course) {
        try {
            // API Call zum Aktualisieren des Kurses


            // Wegen der fehlerhaften API muss das CourseUpdate Objekt herangezogen
            // werden, weil der status nicht übergeben werden darf, denn eigentlich
            // sollte es dieser Call sein:
            // val updatedCourse = courseRepository.updateCourse(course.id, course)
            val courseUpdate = CourseUpdate(
                id = course.id,
                title = course.title,
                description = course.description,
                pricePerParticipant = course.pricePerParticipant,
                type = course.type,
                ageGroup = course.ageGroup,
                locations = course.locations?.map { it.toString() }
            )


            val updatedCourse = courseRepository.updateCourseDetails(course.id, courseUpdate)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_updated_success)
            )


            // Bestehende Master-Liste kopieren
            val currentAllCourses = mutableDashboardData.value.allCourses.toMutableList()

            // Kurs in der Master-Liste aktualisieren
            val index = currentAllCourses.indexOfFirst { it.id == course.id }
            if (index != -1) {
                currentAllCourses[index] = updatedCourse

                // Gefilterte Liste mit aktuellen Filtern aktualisieren
                val filteredCourses = filterCoursesWithCurrentCriteria(currentAllCourses)

                // Dashboard Data aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allCourses = currentAllCourses,
                    courses = filteredCourses
                )
            }

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteCourse(courseId: String) {
        try {
            // API Call zum Löschen des Kurses
            courseRepository.deleteCourse(courseId)

            // Bestehende Master-Liste kopieren
            val currentAllCourses = mutableDashboardData.value.allCourses.toMutableList()

            // Kurs aus der Master-Liste entfernen
            currentAllCourses.removeAll { it.id == courseId }

            // Gefilterte Liste mit aktuellen Filtern aktualisieren
            val filteredCourses = filterCoursesWithCurrentCriteria(currentAllCourses)

            // Dashboard Data aktualisieren
            mutableDashboardData.value = mutableDashboardData.value.copy(
                allCourses = currentAllCourses,
                courses = filteredCourses
            )

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_deleted_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error deleting course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // Neue Hilfsfunktion zum Filtern basierend auf aktuellen Kriterien
    private fun filterCoursesWithCurrentCriteria(courses: List<Course>): List<Course> {
        return courses.filter { course ->
            (selectedCourseType.value == null || course.type == selectedCourseType.value) &&
                    (selectedCourseStatus.value == null || course.status == selectedCourseStatus.value) &&
                    (selectedAgeGroup.value == null || course.ageGroup == selectedAgeGroup.value)
        }
    }

    private suspend fun publishCourse(courseId: String) {
        try {
            val course = courseRepository.getCourseById(courseId)
            val updatedCourse = course.copy(status = "published")
            courseRepository.updateCourse(courseId, updatedCourse)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_published_success)
            )
            loadCourses()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error publishing course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun unpublishCourse(courseId: String) {
        try {
            // Wird von der API aktuell noch nicht unterstützt. Kann
            // aktiviert werden, sobald dies der Fall ist. Bis dahin
            // wird einfach eine Fehlermeldung ausgegeben

            AlertUtils.showInfo(textRes = R.string.error_not_supported)
            /*
            val course = courseRepository.getCourseById(courseId)
            val updatedCourse = course.copy(status = "draft")
            courseRepository.updateCourse(courseId, updatedCourse)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_unpublished_success)
            )
            loadCourses()
             */
        } catch (e: Exception) {
            Timber.e(e, ">>> Error unpublishing course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun archiveCourse(courseId: String) {
        try {
            val course = courseRepository.getCourseById(courseId)
            val updatedCourse = course.copy(status = "archived")
            courseRepository.updateCourse(courseId, updatedCourse)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_archived_success)
            )
            loadCourses()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error archiving course")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Timeslot CRUD
    // ----------------------------------------------------------------------------------------

    private suspend fun createTimeslot(action: AdminAction.CreateTimeslot) {
        try {
            // API Call zum Erstellen des Timeslots
            val createdTimeslot = courseRepository.createCourseLocationTimeslot(
                courseId = action.timeslot.courseId,
                locationId = action.timeslot.locationId,
                timeslot = action.timeslot
            )

            // TimeEntries für den neuen Timeslot nachladen
            val timeEntries = courseRepository.getTimeslotTimeEntries(createdTimeslot.id)
            val timeslotWithEntries = createdTimeslot.copy(timeEntries = timeEntries)

            // 1. Timeslot zu allTimeslots hinzufügen
            val currentAllTimeslots = mutableDashboardData.value.allTimeslots.toMutableList()
            currentAllTimeslots.add(timeslotWithEntries)

            // Gefilterte Timeslots Liste aktualisieren
            val filteredTimeslots = filterTimeslotsWithCurrentCriteria(currentAllTimeslots)

            // Timeslot zum zugehörigen Course hinzufügen
            val currentCourses = mutableDashboardData.value.allCourses.map { course ->
                if (course.id == action.timeslot.courseId) {
                    // Neues Course Objekt mit dem neuen Timeslot erstellen
                    course.copy(
                        timeslots = (course.timeslots ?: emptyList()) + timeslotWithEntries
                    )
                } else {
                    course
                }
            }

            // Dashboard Data mit allen Änderungen aktualisieren
            mutableDashboardData.value = mutableDashboardData.value.copy(
                allTimeslots = currentAllTimeslots,
                timeslots = filteredTimeslots,
                allCourses = currentCourses,
                courses = currentCourses.filter { course ->
                    (selectedCourseType.value == null || course.type == selectedCourseType.value) &&
                            (selectedCourseStatus.value == null || course.status == selectedCourseStatus.value) &&
                            (selectedAgeGroup.value == null || course.ageGroup == selectedAgeGroup.value)
                }
            )

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.timeslot_created_success)
            )

        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateTimeslot(action: AdminAction.UpdateTimeslot) {
        try {
            // Hier holen wir den aktualisierten Timeslot von der API
            val updatedTimeslot = courseRepository.updateCourseLocationTimeslot(
                courseId = action.timeslot.courseId,
                locationId = action.timeslot.locationId,
                timeslotId = action.timeslot.id,
                timeslot = action.timeslot
            )

            // TimeEntries für den aktualisierten Timeslot laden
            val timeEntries = courseRepository.getTimeslotTimeEntries(updatedTimeslot.id)
            val timeslotWithEntries = updatedTimeslot.copy(timeEntries = timeEntries)

            // Aktualisiere die lokalen Listen
            val currentAllTimeslots = mutableDashboardData.value.allTimeslots.toMutableList()
            val index = currentAllTimeslots.indexOfFirst { it.id == updatedTimeslot.id }
            if (index != -1) {
                currentAllTimeslots[index] = timeslotWithEntries

                // Gefilterte Liste aktualisieren
                val filteredTimeslots = filterTimeslotsWithCurrentCriteria(currentAllTimeslots)

                // Update des DashboardData
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allTimeslots = currentAllTimeslots,
                    timeslots = filteredTimeslots
                )
            }

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.timeslot_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating timeslot")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteTimeslot(action: AdminAction.DeleteTimeslot) {
        try {
            // Den zu löschenden Timeslot aus der aktuellen Liste finden
            val timeslot =
                mutableDashboardData.value.allTimeslots.find { it.id == action.timeslotId }
                    ?: return

            // API Call zum Löschen
            courseRepository.deleteCourseLocationTimeslot(
                timeslot.courseId,
                timeslot.locationId,
                timeslot.id
            )

            // Timeslot aus den allTimeslots/timeslots Listen entfernen
            val currentAllTimeslots = mutableDashboardData.value.allTimeslots.toMutableList()
            currentAllTimeslots.removeAll { it.id == action.timeslotId }

            // Gefilterte Timeslots Liste aktualisieren
            val filteredTimeslots = filterTimeslotsWithCurrentCriteria(currentAllTimeslots)

            // Timeslot aus dem zugehörigen Course entfernen
            val currentCourses = mutableDashboardData.value.allCourses.map { course ->
                if (course.id == timeslot.courseId) {
                    // Neues Course Objekt erstellen mit aktualisierten Timeslots
                    course.copy(
                        timeslots = course.timeslots?.filterNot { it.id == action.timeslotId }
                    )
                } else {
                    course
                }
            }

            // Dashboard Data mit allen Änderungen aktualisieren
            mutableDashboardData.value = mutableDashboardData.value.copy(
                allTimeslots = currentAllTimeslots,
                timeslots = filteredTimeslots,
                allCourses = currentCourses,
                // Auch die gefilterte Courses-Liste aktualisieren
                courses = currentCourses.filter { course ->
                    // Hier die aktuelle Filter-Logik anwenden
                    (selectedCourseType.value == null || course.type == selectedCourseType.value) &&
                            (selectedCourseStatus.value == null || course.status == selectedCourseStatus.value) &&
                            (selectedAgeGroup.value == null || course.ageGroup == selectedAgeGroup.value)
                }
            )

            // Erfolgsmeldung anzeigen
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.timeslot_deleted_success)
            )

        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  TimeEntries
    // ----------------------------------------------------------------------------------------

    private suspend fun updateTimeEntries(action: AdminAction.UpdateTimeEntries) {
        try {
            val timeslot = mutableDashboardData.value.allTimeslots
                .find { it.id == action.timeslotId }
                ?: throw Exception("Timeslot not found")

            val updatedTimeslot = courseRepository.updateTimeslotTimeEntries(
                courseId = timeslot.courseId,
                locationId = timeslot.locationId,
                timeslotId = timeslot.id,
                timeEntries = action.timeEntries
            )

            // Aktualisiere die lokale Liste
            val currentTimeslots = mutableDashboardData.value.allTimeslots.toMutableList()
            val index = currentTimeslots.indexOfFirst { it.id == timeslot.id }
            if (index != -1) {
                currentTimeslots[index] = updatedTimeslot
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allTimeslots = currentTimeslots,
                    timeslots = filterTimeslotsWithCurrentCriteria(currentTimeslots)
                )
            }

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.time_entries_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating time entries")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun createTimeEntry(action: AdminAction.TimeEntryActions.CreateTimeEntry) {
        try {
            val createdEntry = courseRepository.createTimeEntry(
                action.timeslotId,
                action.timeEntry
            )

            // Aktualisiere die Timeslot-Liste neu laden um neue Einträge zu sehen

            loadCourses()
            loadAllTimeslots()
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.time_entries_updated_success)
            )

        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating time entry")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateTimeEntry(action: AdminAction.TimeEntryActions.UpdateTimeEntry) {
        try {
            val updatedEntry = courseRepository.updateTimeEntry(
                action.timeslotId,
                action.timeEntryId,
                action.timeEntry
            )
            loadAllTimeslots()
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.time_entries_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating time entry")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteTimeEntry(action: AdminAction.TimeEntryActions.DeleteTimeEntry) {
        try {
            courseRepository.deleteTimeEntry(
                action.timeslotId,
                action.timeEntryId
            )

            loadCourses()
            loadAllTimeslots()
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.time_entries_updated_success)
            )

        } catch (e: Exception) {
            Timber.e(e, ">>> Error deleting time entry")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Timeslot-Filter
    // ----------------------------------------------------------------------------------------

    private fun filterTimeslots(action: AdminAction.FilterTimeslots) {
        Timber.w(">>> TAB -> filterTimeslots ${currentTab.value}")
        setTimeslotFilters(action.courseId, action.locationId)
        val filteredTimeslots =
            filterTimeslotsWithCurrentCriteria(mutableDashboardData.value.allTimeslots)
        mutableDashboardData.value = mutableDashboardData.value.copy(
            timeslots = filteredTimeslots
        )
    }

    private fun filterTimeslotsWithCurrentCriteria(timeslots: List<Timeslot>): List<Timeslot> {
        return timeslots.filter { slot ->
            when {
                selectedCourseId.value != null && selectedLocationId.value != null ->
                    (slot.courseId == selectedCourseId.value && slot.locationId == selectedLocationId.value)

                selectedCourseId.value != null ->
                    (slot.courseId == selectedCourseId.value)

                selectedLocationId.value != null ->
                    (slot.locationId == selectedLocationId.value)

                else -> true
            }
        }
    }

    fun setTimeslotFilters(courseId: String?, locationId: String?) {
        selectedCourseId.value = courseId
        selectedLocationId.value = locationId
    }

    // ----------------------------------------------------------------------------------------
    //  CourseLocation (Kopplung von Kurs & Ort)
    // ----------------------------------------------------------------------------------------

    private suspend fun addCourseLocation(action: AdminAction.AddCourseLocation) {
        try {
            val courseLocation = CourseLocation(locationId = action.locationId)
            courseRepository.addCourseLocation(action.courseId, courseLocation)
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun removeCourseLocation(action: AdminAction.RemoveCourseLocation) {
        try {
            courseRepository.removeCourseLocation(action.courseId, action.locationId)
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  User-Methoden
    // ----------------------------------------------------------------------------------------

    private suspend fun createUser(user: User) {
        try {
            userRepository.createUser(user)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.user_created_success))
            loadUsers()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleCreateUser")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateUser(user: User) {
        try {
            userRepository.updateUser(user.id, user)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.user_updated_success))
            loadUsers()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleUpdateUser")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteUser(userId: String) {
        try {
            userRepository.deleteUser(userId)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.user_deleted_success))
            loadUsers()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleDeleteUser")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Location-Methoden
    // ----------------------------------------------------------------------------------------

    private suspend fun createLocation(location: Location) {
        try {
            locationRepository.createLocation(location)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.location_created_success))
            loadLocations()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleCreateLocation")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateLocation(location: Location) {
        try {
            locationRepository.updateLocation(location.id, location)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.location_updated_success))
            loadLocations()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleUpdateLocation")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteLocation(locationId: String) {
        try {
            locationRepository.deleteLocation(locationId)
            mutableEvents.value =
                Event.ShowMessage(stringProvider.getString(R.string.location_deleted_success))
            loadLocations()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleDeleteLocation")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Booking-Methoden
    // ----------------------------------------------------------------------------------------

    private suspend fun updateBookingStatus(action: AdminAction.UpdateBookingStatus) {
        try {
            bookingRepository.updateBookingStatus(action.bookingId, action.status)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.booking_status_updated_success)
            )
            loadBookings()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleUpdateBookingStatus")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteBooking(action: AdminAction.DeleteBooking) {
        try {
            bookingRepository.deleteBooking(action.bookingId)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.booking_deleted_success)
            )
            loadBookings()
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleDeleteBooking")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Invoice-Methoden
    // ----------------------------------------------------------------------------------------

    private suspend fun downloadInvoice(action: AdminAction.DownloadInvoice) {
        try {
            invoiceRepository.downloadInvoice(action.invoiceId)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.invoice_download_started)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleDownloadInvoice")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun extendInvoiceToken(action: AdminAction.ExtendInvoiceToken) {
        try {
            // API Call zum Verlängern des Tokens
            val updatedInvoice =
                invoiceRepository.extendInvoiceToken(action.invoiceId, action.newExpiryDate)

            // Bestehende Master-Liste kopieren
            val currentAllInvoices = mutableDashboardData.value.allInvoices.toMutableList()

            // Rechnung in der Master-Liste aktualisieren
            val index = currentAllInvoices.indexOfFirst { it.id == action.invoiceId }
            if (index != -1) {
                currentAllInvoices[index] = updatedInvoice

                // Gefilterte Liste mit aktuellem Filter aktualisieren
                val filteredInvoices = filterInvoicesWithCurrentCriteria(currentAllInvoices)

                // Dashboard Data aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allInvoices = currentAllInvoices,
                    invoices = filteredInvoices
                )
            }

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.invoice_token_extended)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleExtendInvoiceToken")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun cancelInvoice(action: AdminAction.CancelInvoice) {
        try {
            // API Call zum Stornieren der Rechnung
            val canceledInvoice = invoiceRepository.cancelInvoice(action.invoiceId)

            // Bestehende Master-Liste kopieren
            val currentAllInvoices = mutableDashboardData.value.allInvoices.toMutableList()

            // Rechnung in der Master-Liste aktualisieren
            val index = currentAllInvoices.indexOfFirst { it.id == action.invoiceId }
            if (index != -1) {
                currentAllInvoices[index] = canceledInvoice

                // Gefilterte Liste mit aktuellem Filter aktualisieren
                val filteredInvoices = filterInvoicesWithCurrentCriteria(currentAllInvoices)

                // Dashboard Data aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allInvoices = currentAllInvoices,
                    invoices = filteredInvoices
                )
            }

            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.invoice_cancelled_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error in handleCancelInvoice")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // Hilfsfunktion zum Filtern der Rechnungen
    private fun filterInvoicesWithCurrentCriteria(invoices: List<Invoice>): List<Invoice> {
        return invoices.filter { invoice ->
            selectedInvoiceStatus.value?.let { status ->
                invoice.status == status
            } ?: true
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Sonstige Funktionen
    // ----------------------------------------------------------------------------------------

    // Aktualisiert den UI-Status je nach Datenlage
    private fun updateUiState() {
        val currentData = mutableDashboardData.value
        mutableUiState.value = when {
            // Nur Empty State zeigen wenn ALLE Daten geladen wurden und leer sind
            currentData.users.isEmpty() &&
                    currentData.locations.isEmpty() &&
                    currentData.bookings.isEmpty() &&
                    currentData.invoices.isEmpty() &&
                    !isLoading() -> UiState.Empty

            else -> UiState.Success(currentData)
        }
    }

}