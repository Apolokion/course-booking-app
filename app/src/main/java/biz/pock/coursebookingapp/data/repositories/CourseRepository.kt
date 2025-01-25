package biz.pock.coursebookingapp.data.repositories

import biz.pock.coursebookingapp.data.api.ApiClient
import biz.pock.coursebookingapp.data.auth.TokenManager
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.data.model.CourseLocation
import biz.pock.coursebookingapp.data.model.CourseUpdate
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.data.model.TimeEntry
import biz.pock.coursebookingapp.data.model.TimeEntryUserUpdate
import biz.pock.coursebookingapp.data.model.TimeEntryWithUsers
import biz.pock.coursebookingapp.data.model.Timeslot
import biz.pock.coursebookingapp.data.model.TimeslotUpdate
import biz.pock.coursebookingapp.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager
) {
    fun getCourses(
        type: String? = null,
        status: String? = null,
        ageGroup: String? = null,
        with: String? = null
    ): Flow<List<Course>> = flow {
        val response = apiClient.apiServiceCourses.getCoursesList(type, status, ageGroup, with)
        if (response.isSuccessful) {
            emit(response.body() ?: emptyList())
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun getCourseById(courseId: String): Course {
        val response = apiClient.apiServiceCourses.getCourseById(courseId)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun createCourse(course: Course): Course {
        val response = apiClient.apiServiceCourses.storeCourse(course)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun updateCourse(courseId: String, course: Course): Course {
        val response = apiClient.apiServiceCourses.updateCourse(courseId, course)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    // Update für Trainer: Komplett ohne Status-Feld
    suspend fun updateCourseDetails(courseId: String, course: CourseUpdate): Course {
        //val courseUpdate = CourseUpdate.fromCourse(course)

        val response = apiClient.apiServiceCourses.updateCourseDetails(courseId, course)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun getCourseLocations(courseId: String): List<Location> {
        val response = apiClient.apiServiceCourses.getCourseLocations(courseId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw HttpException(response)
        }
    }

    suspend fun deleteCourse(courseId: String) {
        val response = apiClient.apiServiceCourses.deleteCourse(courseId)
        if (!response.isSuccessful) {
            throw Exception(response.errorBody()?.string())
        }
    }

    suspend fun getCourseLocationTimeslots(
        courseId: String,
        locationId: String
    ): List<Timeslot> {
        val response = apiClient.apiServiceCourses.getCourseLocationTimeslots(courseId, locationId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw HttpException(response)
        }
    }

    suspend fun createCourseLocationTimeslot(
        courseId: String,
        locationId: String,
        timeslot: Timeslot
    ): Timeslot {
        // Aktuelle Trainer ID als initial user_ids setzen
        val currentUserId = tokenManager.getUserId()
        val userIds = currentUserId?.let { it }

        val response = apiClient.apiServiceCourses.storeCourseLocationTimeslot(
            courseId = courseId,
            locationId = locationId,
            timeslot = timeslot,
            userIds = userIds,
            startTime = timeslot.startTime,
            endTime = timeslot.endTime
        )

        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun updateCourseLocationTimeslot(
        courseId: String,
        locationId: String,
        timeslotId: String,
        timeslot: Timeslot
    ): Timeslot {
        val update = TimeslotUpdate(
            startDate = timeslot.startDate ?: throw IllegalArgumentException("start_date required"),
            endDate = timeslot.endDate ?: throw IllegalArgumentException("end_date required"),
            startTime = timeslot.startTime,
            endTime = timeslot.endTime,
            maxCapacity = timeslot.maxCapacity
        )

        val response = apiClient.apiServiceCourses.updateCourseLocationTimeslot(
            courseId, locationId, timeslotId, update
        )
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun deleteCourseLocationTimeslot(
        courseId: String,
        locationId: String,
        timeslotId: String
    ) {
        val response = apiClient.apiServiceCourses.deleteCourseLocationTimeslot(
            courseId, locationId, timeslotId
        )
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    suspend fun updateTimeslotTimeEntries(
        courseId: String,
        locationId: String,
        timeslotId: String,
        timeEntries: List<TimeEntry>
    ): Timeslot {
        val response = apiClient.apiServiceCourses.updateTimeslotTimeEntries(
            courseId,
            locationId,
            timeslotId,
            timeEntries
        )
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun getTimeslotTimeEntries(timeslotId: String): List<TimeEntry> {
        val response = apiClient.apiServiceCourses.getTimeslotTimeEntries(timeslotId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            throw HttpException(response)
        }
    }

    suspend fun addCourseLocation(courseId: String, courseLocation: CourseLocation): Location {
        val response = apiClient.apiServiceCourses.addCourseLocation(courseId, courseLocation)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun removeCourseLocation(courseId: String, locationId: String) {
        val response = apiClient.apiServiceCourses.deleteCourseLocation(courseId, locationId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    suspend fun createTimeEntry(
        timeslotId: String,
        timeEntry: TimeEntry
    ): TimeEntry {
        val response = apiClient.apiServiceCourses.createTimeEntry(timeslotId, timeEntry)
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun updateTimeEntry(
        timeslotId: String,
        timeEntryId: String,
        timeEntry: TimeEntry
    ): TimeEntry {
        val response = apiClient.apiServiceCourses.updateTimeEntry(
            timeslotId,
            timeEntryId,
            timeEntry
        )
        if (response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            throw HttpException(response)
        }
    }

    suspend fun deleteTimeEntry(
        timeslotId: String,
        timeEntryId: String
    ) {
        val response = apiClient.apiServiceCourses.deleteTimeEntry(timeslotId, timeEntryId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    suspend fun getTimeEntryUsers(timeEntryId: String): List<User> {
        try {
            val response = apiClient.apiServiceCourses.getTimeEntryUsers(timeEntryId)
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string()
                throw Exception(errorBody ?: "Error loading time entry users")
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading users for time entry $timeEntryId")
            throw e
        }
    }

    suspend fun getTimeslotTimeEntriesWithUsers(timeslotId: String): List<TimeEntryWithUsers> {
        try {
            val response = apiClient.apiServiceCourses.getTimeslotTimeEntriesWithUsers(timeslotId)
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            } else {
                val errorBody = response.errorBody()?.string()
                throw Exception(errorBody ?: "Error loading time entries with users")
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error loading time entries with users for timeslot $timeslotId")
            throw e
        }
    }

    suspend fun syncTimeEntryUsers(timeEntryId: String, userIds: List<String>) {
        try {
            val response = apiClient.apiServiceCourses.syncTimeEntryUsers(
                timeEntryId = timeEntryId,
                userIds = TimeEntryUserUpdate(userIds)
            )

            // Status 201 ist OK, auch mit leerem Body
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                throw Exception(errorBody ?: "Error syncing users for time entry")
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Error syncing users for time entry $timeEntryId: ${e.message}")
            throw e
        }
    }
}