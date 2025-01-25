package biz.pock.coursebookingapp.ui.viewmodels.dashboard

import biz.pock.coursebookingapp.data.model.Booking
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.Invoice
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface TimeslotViewModel {
    sealed class TimeslotAction : BaseDashboardViewModel.Action() {
        data class UpdateTimeEntries(
            val timeslotId: String,
            val timeEntries: List<TimeEntry>
        ) : TimeslotAction()

        data class CreateTimeEntry(
            val timeslotId: String,
            val timeEntry: TimeEntry
        ) : TimeslotAction()

        data class UpdateTimeEntry(
            val timeslotId: String,
            val timeEntryId: String,
            val timeEntry: TimeEntry
        ) : TimeslotAction()

        data class DeleteTimeEntry(
            val timeslotId: String,
            val timeEntryId: String
        ) : TimeslotAction()
    }

    val dashboardData: StateFlow<DashboardData>
    fun handleAction(action: BaseDashboardViewModel.Action)
    val uiState: StateFlow<BaseDashboardViewModel.UiState>
    val events: StateFlow<BaseDashboardViewModel.Event?>
}

// Gemeinsames Datenmodell für beide ViewModels
data class DashboardData(
    // Master Data (vollständige Listen)
    val allCourses: List<Course> = emptyList(),
    val allLocations: List<Location> = emptyList(),
    val allBookings: List<Booking> = emptyList(),
    val allTimeslots: List<Timeslot> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val allInvoices: List<Invoice> = emptyList(),

    // Gefilterte Ansichten
    val courses: List<Course> = emptyList(),
    val locations: List<Location> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val timeslots: List<Timeslot> = emptyList(),
    val users: List<User> = emptyList(),
    val invoices: List<Invoice> = emptyList(),

    // Spezielle Ansichten
    val selectedCourseLocations: List<Location> = emptyList()
)

