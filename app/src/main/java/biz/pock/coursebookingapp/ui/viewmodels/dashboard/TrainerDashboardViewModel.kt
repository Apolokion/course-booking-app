package biz.pock.coursebookingapp.ui.viewmodels.dashboard

import androidx.lifecycle.viewModelScope
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.CourseUpdate
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.data.repositories.BookingRepository
import biz.pock.coursebookingapp.data.repositories.CourseRepository
import biz.pock.coursebookingapp.data.repositories.LocationRepository
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.ErrorHandler
import biz.pock.coursebookingapp.utils.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TrainerDashboardViewModel @Inject constructor(
    val courseRepository: CourseRepository,
    private val bookingRepository: BookingRepository,
    private val locationRepository: LocationRepository,
    private val errorHandler: ErrorHandler,
    private val stringProvider: StringProvider
) : BaseDashboardViewModel(), TimeslotViewModel {

    // ----------------------------------------------------------------------------------------
    //  TrainerAction
    // ----------------------------------------------------------------------------------------
    sealed class TrainerAction : Action() {
        // Booking-Aktionen
        data class FilterBookings(val status: String?) : TrainerAction()
        data class UpdateBookingStatus(val bookingId: String, val status: String) : TrainerAction()

        // Course-Aktionen
        data class FilterCourses(
            val type: String? = null,
            val status: String? = null,
            val ageGroup: String? = null
        ) : TrainerAction()

        data class CreateCourse(val course: Course) : TrainerAction()
        data class UpdateCourse(val course: CourseUpdate) : TrainerAction()
        data class DeleteCourse(val courseId: String) : TrainerAction()
        data class PublishCourse(val courseId: String) : TrainerAction()
        data class UnpublishCourse(val courseId: String) : TrainerAction()
        data class ArchiveCourse(val courseId: String) : TrainerAction()

        // Timeslot-Aktionen
        data class FilterTimeslots(val courseId: String?, val locationId: String?) : TrainerAction()
        data class CreateTimeslot(val timeslot: Timeslot) : TrainerAction()
        data class UpdateTimeslot(val timeslot: Timeslot) : TrainerAction()
        data class DeleteTimeslot(val timeslotId: String) : TrainerAction()
        data class UpdateTimeEntries(
            val timeslotId: String,
            val timeEntries: List<TimeEntry>
        ) : TrainerAction()

        // TimeEntry-Aktionen
        sealed class TimeEntryActions : TrainerAction() {
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
    //  DashboardData StateFlow & States
    // ----------------------------------------------------------------------------------------
    private val mutableDashboardData = MutableStateFlow(DashboardData())
    override val dashboardData: StateFlow<DashboardData> = mutableDashboardData.asStateFlow()

    // Filter States
    val selectedCourseType = MutableStateFlow<String?>(null)
    val selectedCourseStatus = MutableStateFlow<String?>(null)
    val selectedAgeGroup = MutableStateFlow<String?>(null)

    // Filter States für Timeslots
    val selectedCourseId = MutableStateFlow<String?>(null)
    val selectedLocationId = MutableStateFlow<String?>(null)

    // Filter States für Bookings
    val selectedBookingStatus = MutableStateFlow<String?>(null)

    // Aktueller Tab
    val currentTab = MutableStateFlow(0)

    // ----------------------------------------------------------------------------------------
    //  init & erste Datenladung
    // ----------------------------------------------------------------------------------------
    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            mutableUiState.value = UiState.Loading
            try {
                // Erst den aktuellen Tab laden
                val tab = currentTab.value
                loadCurrentTabData(tab)

                // Dann verzögert den Rest, aber nur wenn nicht im Timeslots Tab
                if (tab != 2) {
                    launch {
                        delay(500)
                        loadRemainingData(tab)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading dashboard data")
                val errorMsg = errorHandler.handleApiError(e)
                mutableUiState.value = UiState.Error(errorMsg)
                mutableEvents.value = Event.ShowError(errorMsg)
            }
        }
    }

    fun onTabSelected(position: Int) {
        currentTab.value = position
        loadDashboardData()
    }

    private suspend fun loadCurrentTabData(tab: Int) {
        //Timber.d(">>> TAB = $tab")
        when (tab) {
            0 -> {
                loadCourses()
                updateUiState()
            }

            1 -> {
                loadBookings()
                updateUiState()
            }

            2 -> {

                loadAllTimeslots()

                updateUiState()
            }
        }
    }

    private suspend fun loadRemainingData(excludeTab: Int) {
        try {
            when (excludeTab) {
                0 -> if (mutableDashboardData.value.bookings.isEmpty()) loadCourses()
                1 -> if (mutableDashboardData.value.invoices.isEmpty()) loadBookings()
                2 -> {
                    if (mutableDashboardData.value.courses.isEmpty()) loadCourses()
                    if (mutableDashboardData.value.allTimeslots.isEmpty()) loadAllTimeslots()
                    if (mutableDashboardData.value.locations.isEmpty()) loadLocations()
                }
            }

            // Restliche Daten nachladen
            loadOtherData(excludeTab)
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading remaining data")
        }
    }

    private suspend fun loadOtherData(excludeTab: Int) {
        viewModelScope.launch {
            if (excludeTab != 0) loadCoursesIfNeeded()
        }
        viewModelScope.launch {
            if (excludeTab != 1) loadBookingsIfNeeded()
        }
        viewModelScope.launch {
            if (excludeTab !in listOf(2, 4)) {
                loadCoursesIfNeeded()
                loadTimeslotsIfNeeded()
                loadLocationsIfNeeded()
            }
        }
    }

    private suspend fun loadBookingsIfNeeded() {
        if (mutableDashboardData.value.bookings.isEmpty()) {
            loadBookings()
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

    private fun isLoading(): Boolean {
        return viewModelScope.coroutineContext[Job]?.children?.any { it.isActive } == true
    }

    // ----------------------------------------------------------------------------------------
    //  Load-Funktionen (Bookings, Courses, Timeslots, Locations)
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

    private suspend fun loadCourses() {
        try {
            // Erste Anfrage mit Filtern aber ohne Locations
            val coursesWithoutLocations = courseRepository.getCourses(
                type = selectedCourseType.value,
                status = selectedCourseStatus.value,
                ageGroup = selectedAgeGroup.value,
                with = "timeslots,locations" // "locations" greift hier nicht
            ).first()

            Timber.d(">>> TRAINER DASHBOARD -> loadCourses -> $coursesWithoutLocations")

            // Für jeden Kurs einzeln die Locations nachladen
            val updatedCourses = mutableListOf<Course>()
            for (course in coursesWithoutLocations) {
                val courseLocations = courseRepository.getCourseLocations(course.id)
                val courseWithLocs = course.copy(locations = courseLocations)
                updatedCourses.add(courseWithLocs)
            }

            // Wenn allCourses leer ist, brauchen wir auch die ungefilterte Liste mit Locations
            if (mutableDashboardData.value.allCourses.isEmpty()) {
                // Separate Anfrage für alle Kurse ohne Filter
                val allCoursesWithoutLocations = courseRepository.getCourses(
                    type = null,
                    status = null,
                    ageGroup = null,
                    with = "timeslots,locations"
                ).first()

                // Auch für diese Kurse die Locations nachladen
                val allUpdatedCourses = mutableListOf<Course>()
                for (course in allCoursesWithoutLocations) {
                    val courseLocations = courseRepository.getCourseLocations(course.id)
                    val courseWithLocs = course.copy(locations = courseLocations)
                    allUpdatedCourses.add(courseWithLocs)
                }

                // Update DashboardData mit beiden Listen
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    courses = updatedCourses,
                    allCourses = allUpdatedCourses
                )
            } else {
                // Wenn allCourses schon existiert, nur die gefilterte Liste aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    courses = updatedCourses
                )
            }

            updateUiState()
            Timber.w(">>> COURSES LOADED (inkl. manuellem Location nachladen)")

        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading courses")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun loadAllTimeslots() {
        viewModelScope.launch {
            try {
                // Nutze die bereits geladenen Kurse
                val currentCourses = mutableDashboardData.value.allCourses
                var allTimeslots = mutableListOf<Timeslot>()

                // Für jeden Kurs und dessen Locations die Timeslots frisch von der API laden
                currentCourses.forEach { course ->
                    course.locations?.forEach { location ->
                        try {
                            val timeslots = courseRepository.getCourseLocationTimeslots(
                                courseId = course.id,
                                locationId = location.id
                            )
                            allTimeslots.addAll(timeslots)
                        } catch (e: Exception) {
                            Timber.e(e, ">>> Error loading timeslots for course ${course.id} and location ${location.id}")
                        }
                    }
                }

                // Beide Listen aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    allTimeslots = allTimeslots,
                    timeslots = filterTimeslotsWithCurrentCriteria(allTimeslots)
                )

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
                            updatedTimeslots.add(timeslot)
                        }
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, ">>> Error loading all timeslots: ${e.message}")
                mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
            }
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

                // Bookings
                is TrainerAction.FilterBookings -> loadBookings(action.status)
                is TrainerAction.UpdateBookingStatus -> updateBookingStatus(action)

                // Courses
                is TrainerAction.FilterCourses -> filterCourses(action)
                is TrainerAction.CreateCourse -> createCourse(action)
                is TrainerAction.UpdateCourse -> updateCourse(action)
                is TrainerAction.DeleteCourse -> deleteCourse(action)
                is TrainerAction.PublishCourse -> publishCourse(action)
                is TrainerAction.UnpublishCourse -> unpublishCourse(action)
                is TrainerAction.ArchiveCourse -> archiveCourse(action)

                // Timeslots
                is TrainerAction.FilterTimeslots -> filterTimeslots(
                    action.courseId,
                    action.locationId
                )

                is TrainerAction.CreateTimeslot -> createTimeslot(action)
                is TrainerAction.UpdateTimeslot -> updateTimeslot(action)
                is TrainerAction.DeleteTimeslot -> deleteTimeslot(action)
                is TrainerAction.UpdateTimeEntries -> updateTimeEntries(action)

                // TimeEntries
                is TimeslotViewModel.TimeslotAction.CreateTimeEntry -> {
                    createTimeEntry(
                        TrainerAction.TimeEntryActions.CreateTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntry = action.timeEntry
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.UpdateTimeEntry -> {
                    updateTimeEntry(
                        TrainerAction.TimeEntryActions.UpdateTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntryId = action.timeEntryId,
                            timeEntry = action.timeEntry
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.DeleteTimeEntry -> {
                    deleteTimeEntry(
                        TrainerAction.TimeEntryActions.DeleteTimeEntry(
                            timeslotId = action.timeslotId,
                            timeEntryId = action.timeEntryId
                        )
                    )
                }

                is TimeslotViewModel.TimeslotAction.UpdateTimeEntries -> {
                    updateTimeEntries(
                        TrainerAction.UpdateTimeEntries(
                            timeslotId = action.timeslotId,
                            timeEntries = action.timeEntries
                        )
                    )
                }

                else -> {
                    Timber.w(">>> Unhandled action type received: ${action::class.simpleName}")
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Filter-Funktionen
    // ----------------------------------------------------------------------------------------
    private fun filterCourses(action: TrainerAction.FilterCourses) {
        selectedCourseType.value = action.type
        selectedCourseStatus.value = action.status
        selectedAgeGroup.value = action.ageGroup

        try {
            val filteredCourses = mutableDashboardData.value.allCourses.filter { course ->
                (action.type == null || course.type == action.type) &&
                        (action.status == null || course.status == action.status) &&
                        (action.ageGroup == null || course.ageGroup == action.ageGroup)
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

    fun clearFilters() {
        when (getCurrentTab()) {
            0 -> clearCourseFilters()
            1 -> clearBookingFilters()
            2 -> clearTimeslotFilters()
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Course CRUD
    // ----------------------------------------------------------------------------------------
    private suspend fun createCourse(action: TrainerAction.CreateCourse) {
        try {
            courseRepository.createCourse(action.course)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_created_success)
            )
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateCourse(action: TrainerAction.UpdateCourse) {
        try {
            courseRepository.updateCourseDetails(action.course.id, action.course)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_updated_success)
            )
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteCourse(action: TrainerAction.DeleteCourse) {
        try {
            courseRepository.deleteCourse(action.courseId)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_deleted_success)
            )
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun publishCourse(action: TrainerAction.PublishCourse) {
        try {
            val course = courseRepository.getCourseById(action.courseId)
            val updatedCourse = course.copy(status = "published")
            courseRepository.updateCourse(action.courseId, updatedCourse)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_published_success)
            )
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private fun unpublishCourse(action: TrainerAction.UnpublishCourse) {
        try {
            AlertUtils.showInfo(textRes = R.string.error_not_supported)
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun archiveCourse(action: TrainerAction.ArchiveCourse) {
        try {
            val course = courseRepository.getCourseById(action.courseId)
            val updatedCourse = course.copy(status = "archived")
            courseRepository.updateCourse(action.courseId, updatedCourse)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.course_archived_success)
            )
            loadCourses()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Timeslot Funktionen
    // ----------------------------------------------------------------------------------------
    private fun filterTimeslots(courseId: String?, locationId: String?) {
        viewModelScope.launch {
            try {
                // Filter States aktualisieren
                selectedCourseId.value = courseId
                selectedLocationId.value = locationId

                // Prüfen ob wir überhaupt Daten haben
                if (mutableDashboardData.value.allTimeslots.isEmpty()) {
                    loadAllTimeslots()
                    return@launch
                }

                // Filter anwenden
                val filteredTimeslots = filterTimeslotsWithCurrentCriteria(
                    mutableDashboardData.value.allTimeslots
                )

                // State aktualisieren
                mutableDashboardData.value = mutableDashboardData.value.copy(
                    timeslots = filteredTimeslots
                )

                Timber.d(">>> Filtered timeslots: ${filteredTimeslots.size} (course=$courseId, location=$locationId)")
                updateUiState()

            } catch (e: Exception) {
                Timber.e(e, ">>> Error filtering timeslots")
                mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
            }
        }
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

    private suspend fun createTimeslot(action: TrainerAction.CreateTimeslot) {
        try {
            courseRepository.createCourseLocationTimeslot(
                courseId = action.timeslot.courseId,
                locationId = action.timeslot.locationId,
                timeslot = action.timeslot
            )
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.timeslot_created_success)
            )
            loadAllTimeslots()
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateTimeslot(action: TrainerAction.UpdateTimeslot) {
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
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun deleteTimeslot(action: TrainerAction.DeleteTimeslot) {
        try {
            val timeslot = mutableDashboardData.value.timeslots.find { it.id == action.timeslotId }
            if (timeslot != null) {
                courseRepository.deleteCourseLocationTimeslot(
                    courseId = timeslot.courseId,
                    locationId = timeslot.locationId,
                    timeslotId = timeslot.id
                )
                mutableEvents.value = Event.ShowMessage(
                    stringProvider.getString(R.string.timeslot_deleted_success)
                )
                loadAllTimeslots()
            }
        } catch (e: Exception) {
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateTimeEntries(action: TrainerAction.UpdateTimeEntries) {
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

    private suspend fun createTimeEntry(action: TrainerAction.TimeEntryActions.CreateTimeEntry) {
        try {
            courseRepository.createTimeEntry(
                action.timeslotId,
                action.timeEntry
            )
            loadAllTimeslots()
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.time_entries_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error creating time entry")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    private suspend fun updateTimeEntry(action: TrainerAction.TimeEntryActions.UpdateTimeEntry) {
        try {
            courseRepository.updateTimeEntry(
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

    private suspend fun deleteTimeEntry(action: TrainerAction.TimeEntryActions.DeleteTimeEntry) {
        try {
            courseRepository.deleteTimeEntry(
                action.timeslotId,
                action.timeEntryId
            )
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
    //  Booking Funktionen
    // ----------------------------------------------------------------------------------------
    private suspend fun updateBookingStatus(action: TrainerAction.UpdateBookingStatus) {
        try {
            val updatedBooking =
                bookingRepository.updateBookingStatus(action.bookingId, action.status)
            // Nach erfolgreicher Aktualisierung die Liste neu laden
            loadBookings(selectedBookingStatus.value)
            mutableEvents.value = Event.ShowMessage(
                stringProvider.getString(R.string.booking_status_updated_success)
            )
        } catch (e: Exception) {
            Timber.e(e, ">>> Error updating booking status")
            mutableEvents.value = Event.ShowError(errorHandler.handleApiError(e))
        }
    }

    // ----------------------------------------------------------------------------------------
    //  State Management & Helper Functions
    // ----------------------------------------------------------------------------------------
    private fun isEmptyData(data: DashboardData): Boolean {
        return when (getCurrentTab()) {
            0 -> data.courses.isEmpty()
            1 -> data.bookings.isEmpty()
            2 -> data.timeslots.isEmpty()
            else -> data.courses.isEmpty() && data.bookings.isEmpty() && data.timeslots.isEmpty()
        }
    }

    private fun updateUiState() {
        val currentData = mutableDashboardData.value
        mutableUiState.value = when {
            isEmptyData(currentData) && !isLoading() && getCurrentTab() == currentTab.value -> UiState.Empty
            else -> UiState.Success(currentData)
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Filter Reset & Clear Functions
    // ----------------------------------------------------------------------------------------

    fun clearBookingFilters() {
        selectedBookingStatus.value = null
        viewModelScope.launch {
            loadBookings()
        }
    }

    fun clearTimeslotFilters() {
        selectedCourseId.value = null
        selectedLocationId.value = null
        viewModelScope.launch {
            mutableDashboardData.value = mutableDashboardData.value.copy(
                timeslots = mutableDashboardData.value.allTimeslots
            )
            updateUiState()
        }
    }

    fun clearCourseFilters() {
        selectedCourseType.value = null
        selectedCourseStatus.value = null
        selectedAgeGroup.value = null
        viewModelScope.launch {
            loadCourses()
        }
    }

    // ----------------------------------------------------------------------------------------
    //  Tab Management
    // ----------------------------------------------------------------------------------------
    fun getCurrentTab(): Int = currentTab.value

}