package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Course
import biz.pock.coursebookingapp.databinding.ItemCourseBinding
import biz.pock.coursebookingapp.shared.enums.AgeGroup
import biz.pock.coursebookingapp.shared.enums.CourseStatus
import biz.pock.coursebookingapp.shared.enums.CourseType

class CourseListAdapter(
    private val onCourseClick: (Course, View) -> Unit
) : BaseListAdapter<Course, ItemCourseBinding>(CourseDiffCallback()) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemCourseBinding {
        return ItemCourseBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemCourseBinding, item: Course) {
        binding.apply {
            // Titel
            textTitle.text = item.title
            textTitle.contentDescription = root.context.getString(
                R.string.course_title_format,
                item.title
            )

            // Preis
            val priceText = root.context.getString(
                R.string.price_per_participant,
                item.pricePerParticipant
            )
            textPrice.text = priceText
            textPrice.contentDescription = root.context.getString(
                R.string.course_price_desc,
                item.pricePerParticipant
            )

            setupTypeChip(root.context, item)
            setupStatusChip(root.context, item)
            setupAgeGroupChip(root.context, item)

            // TODO: Anzahl der Teilnehmer ermitteln, wenn API das unterstützt
            // Aktuell wird die Teilnehmeranzahl in den Timeslots angegeben
            // eventuell könnten wir hier eine Gesamtmenge ermitteln und anzeigen
            textParticipants.visibility = View.GONE

            // Gesamtbeschreibung für das Item
            root.contentDescription = root.context.getString(
                R.string.course_details_format,
                item.title,
                textType.text,
                textStatus.text,
                priceText
            )

            root.setOnClickListener { view ->
                onCourseClick(item, view)
            }
        }
    }

    private fun ItemCourseBinding.setupTypeChip(context: Context, course: Course) {
        val type = CourseType.fromApiString(course.type)
        textType.text = type?.let { context.getString(it.resId) } ?: course.type
        textType.setTextColor(context.getColor(
            when (type) {
                CourseType.PUBLIC -> R.color.course_type_public
                CourseType.PRIVATE -> R.color.course_type_private
                null -> R.color.on_surface
            }
        ))
    }

    private fun ItemCourseBinding.setupStatusChip(context: Context, course: Course) {
        val status = CourseStatus.fromApiString(course.status)
        textStatus.text = status?.let { context.getString(it.resId) } ?: course.status
        textStatus.setTextColor(context.getColor(
            when (status) {
                CourseStatus.DRAFT -> R.color.status_draft
                CourseStatus.PUBLISHED -> R.color.status_confirmed
                CourseStatus.ARCHIVED -> R.color.status_canceled
                null -> R.color.on_surface
            }
        ))
    }

    private fun ItemCourseBinding.setupAgeGroupChip(context: Context, course: Course) {
        val ageGroup = AgeGroup.fromApiString(course.ageGroup ?: "mixed")
        textAgeGroup.text = ageGroup?.let { context.getString(it.resId) } ?: (course.ageGroup ?: "Mixed")
        textAgeGroup.setTextColor(context.getColor(R.color.on_surface))
    }

    private class CourseDiffCallback : DiffUtil.ItemCallback<Course>() {
        override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem == newItem
        }
    }
}