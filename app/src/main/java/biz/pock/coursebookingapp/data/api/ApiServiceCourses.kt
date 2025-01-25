package biz.pock.coursebookingapp.data.api

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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiServiceCourses {

    @GET("/api/v1/courses")
    suspend fun getCoursesList(
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("age_group") ageGroup: String? = null,
        @Query("with") with: String? = null
    ): Response<List<Course>>

    @GET("/api/v1/courses/{course}")
    suspend fun getCourseById(
        @Path("course") courseId: String
    ): Response<Course>

    @POST("/api/v1/courses")
    suspend fun storeCourse(
        @Body course: Course
    ): Response<Course>

    @PUT("/api/v1/courses/{course}")
    suspend fun updateCourse(
        @Path("course") courseId: String,
        @Body course: Course
    ): Response<Course>

    @PUT("/api/v1/courses/{course}")
    suspend fun updateCourseDetails(
        @Path("course") courseId: String,
        @Body courseUpdate: CourseUpdate
    ): Response<Course>

    @DELETE("/api/v1/courses/{course}")
    suspend fun deleteCourse(
        @Path("course") courseId: String
    ): Response<Unit>

    @GET("/api/v1/courses/{course}/locations")
    suspend fun getCourseLocations(
        @Path("course") courseId: String
    ): Response<List<Location>>

    @POST("/api/v1/courses/{course}/locations")
    suspend fun addCourseLocation(
        @Path("course") courseId: String,
        @Body courseLocation: CourseLocation
    ): Response<Location>

    @DELETE("/api/v1/courses/{course}/locations/{location}")
    suspend fun deleteCourseLocation(
        @Path("course") courseId: String,
        @Path("location") locationId: String
    ): Response<Unit>

    @GET("/api/v1/courses/{course}/locations/{location}/timeslots")
    suspend fun getCourseLocationTimeslots(
        @Path("course") courseId: String,
        @Path("location") locationId: String
    ): Response<List<Timeslot>>

    @POST("api/v1/courses/{courseId}/locations/{locationId}/timeslots")
    suspend fun storeCourseLocationTimeslot(
        @Path("courseId") courseId: String,
        @Path("locationId") locationId: String,
        @Body timeslot: Timeslot,
        @Query("user_ids") userIds: String? = null,
        @Query("start_time") startTime: String? = null,
        @Query("end_time") endTime: String? = null
    ): Response<Timeslot>

    @PUT("/api/v1/courses/{course}/locations/{location}/timeslots/{timeslot}")
    suspend fun updateCourseLocationTimeslot(
        @Path("course") courseId: String,
        @Path("location") locationId: String,
        @Path("timeslot") timeslotId: String,
        @Body update: TimeslotUpdate
    ): Response<Timeslot>

    @DELETE("/api/v1/courses/{course}/locations/{location}/timeslots/{timeslot}")
    suspend fun deleteCourseLocationTimeslot(
        @Path("course") courseId: String,
        @Path("location") locationId: String,
        @Path("timeslot") timeslotId: String
    ): Response<Unit>

    @PUT("/api/v1/courses/{course}/locations/{location}/timeslots/{timeslot}/time-entries")
    suspend fun updateTimeslotTimeEntries(
        @Path("course") courseId: String,
        @Path("location") locationId: String,
        @Path("timeslot") timeslotId: String,
        @Body timeEntries: List<TimeEntry>
    ): Response<Timeslot>

    @GET("/api/v1/timeslots/{timeslot}/time-entries")
    suspend fun getTimeslotTimeEntries(
        @Path("timeslot") timeslotId: String
    ): Response<List<TimeEntry>>

    @POST("/api/v1/timeslots/{timeslot}/time-entries")
    suspend fun createTimeEntry(
        @Path("timeslot") timeslotId: String,
        @Body timeEntry: TimeEntry
    ): Response<TimeEntry>

    @PUT("/api/v1/timeslots/{timeslot}/time-entries/{timeEntry}")
    suspend fun updateTimeEntry(
        @Path("timeslot") timeslotId: String,
        @Path("timeEntry") timeEntryId: String,
        @Body timeEntry: TimeEntry
    ): Response<TimeEntry>

    @DELETE("/api/v1/timeslots/{timeslot}/time-entries/{timeEntry}")
    suspend fun deleteTimeEntry(
        @Path("timeslot") timeslotId: String,
        @Path("timeEntry") timeEntryId: String
    ): Response<Unit>

    @POST("api/v1/time-entries/{timeEntryId}/users/sync")
    suspend fun syncTimeEntryUsers(
        @Path("timeEntryId") timeEntryId: String,
        @Body userIds: TimeEntryUserUpdate
    ): Response<Unit>

    @GET("api/v1/timeslots/{timeslotId}/time-entries")
    suspend fun getTimeslotTimeEntriesWithUsers(
        @Path("timeslotId") timeslotId: String,
        @Query("with") with: String = "users"
    ): Response<List<TimeEntryWithUsers>>

    @GET("api/v1/time-entries/{timeEntryId}/users")
    suspend fun getTimeEntryUsers(
        @Path("timeEntryId") timeEntryId: String
    ): Response<List<User>>


}