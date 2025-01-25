package biz.pock.coursebookingapp.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Course(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("price_per_participant")
    val pricePerParticipant: Double,
    val status: String,
    val type: String,
    @SerializedName("age_group")
    val ageGroup: String?,
    // @RawValue für komplexe Typen, die nicht direkt Parcelable sind
    val locations: @RawValue List<Location>? = null,
    val timeslots: @RawValue List<Timeslot>? = null
) : Parcelable

@Parcelize
data class Location(
    val id: String,
    val name: String,
    val address: String,
    val postcode: String,
    val city: String,
    val country: String
) : Parcelable

// Neue Klasse für Course Updates durch Trainer
@Parcelize
data class CourseUpdate(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("price_per_participant")
    val pricePerParticipant: Double,
    val type: String,
    @SerializedName("age_group")
    val ageGroup: String?,
    // Für die Standort-IDs
    val locations: List<String>? = null
) : Parcelable {
    companion object {
        fun fromCourse(course: Course): CourseUpdate {
            return CourseUpdate(
                id = course.id,
                title = course.title,
                description = course.description,
                pricePerParticipant = course.pricePerParticipant,
                type = course.type,
                ageGroup = course.ageGroup,
                locations = course.locations?.map { it.id }
            )
        }
    }
}

@Parcelize
data class Timeslot(
    val id: String = "",
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null,
    @SerializedName("start_time")
    val startTime: String? = null,  // Optional
    @SerializedName("end_time")
    val endTime: String? = null,    // Optional
    @SerializedName("max_capacity")
    val maxCapacity: Int = 0,
    @SerializedName("filled_participants")
    val filledParticipants: Int = 0,
    @SerializedName("available_participants")
    val availableParticipants: Int = maxCapacity,
    @SerializedName("course_id")
    val courseId: String = "",
    @SerializedName("location_id")
    val locationId: String = "",
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("time_entries")
    val timeEntries: List<TimeEntry>? = null,
    @SerializedName("location")
    val location: Location? = null,
    @SerializedName("course")
    val course: Course? = null
) : Parcelable

@Parcelize
data class TimeEntry(
    val id: String = "",
    val date: String? = null,
    @SerializedName("start_time")
    val startTime: String? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("timeslot_id")
    val timeslotId: String = "",
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    val timeslot: Timeslot? = null
) : Parcelable

data class TimeEntryUserUpdate(
    @SerializedName("user_ids")
    val userIds: List<String>
)

data class TimeEntryWithUsers(
    val id: String,
    val date: String,
    @SerializedName("start_time")
    val startTime: String?,
    @SerializedName("end_time")
    val endTime: String?,
    @SerializedName("timeslot_id")
    val timeslotId: String,
    val users: List<User>? = null
)

@Parcelize
data class CourseLocation(
    @SerializedName("location_id")
    val locationId: String
) : Parcelable

data class TimeslotUpdate(
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    @SerializedName("start_time")
    val startTime: String? = null,
    @SerializedName("end_time")
    val endTime: String? = null,
    @SerializedName("max_capacity")
    val maxCapacity: Int
) {
    init {
        require(maxCapacity > 0) { "max_capacity must be positive" }

        if (startTime != null) {
            requireNotNull(endTime) { "end_time required when start_time is set" }
        }
        if (endTime != null) {
            requireNotNull(startTime) { "start_time required when end_time is set" }
        }
    }
}