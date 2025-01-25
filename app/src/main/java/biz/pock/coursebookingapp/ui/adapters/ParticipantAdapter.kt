package biz.pock.coursebookingapp.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.databinding.ItemBookingParticipantBinding
import biz.pock.coursebookingapp.shared.enums.SkillLevel

class ParticipantAdapter(
    private val onEditClick: (BookingParticipant) -> Unit,
    private val onDeleteClick: (BookingParticipant) -> Unit,
    private val context: Context
) : ListAdapter<BookingParticipant, ParticipantAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    inner class ParticipantViewHolder(
        private val binding: ItemBookingParticipantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: BookingParticipant) {
            binding.apply {
                textName.text = "${participant.firstname} ${participant.lastname}"
                textEmail.text = participant.email
                textBirthdate.text = participant.birthdate
                // Skill Level als enum
                chipSkillLevel.text = SkillLevel.fromApiString(participant.skillLevel)
                    ?.let { root.context.getString(it.resId) }
                    ?: participant.skillLevel.replaceFirstChar { it.uppercase() }

                buttonEdit.setOnClickListener { onEditClick(participant) }
                buttonDelete.setOnClickListener { onDeleteClick(participant) }

                root.contentDescription = buildContentDescription(participant)
            }
        }

        private fun buildContentDescription(participant: BookingParticipant): String {
            return context.getString(
                R.string.participant_description,
                participant.firstname,
                participant.lastname,
                participant.email,
                participant.birthdate,
                participant.skillLevel
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        return ParticipantViewHolder(
            ItemBookingParticipantBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ParticipantDiffCallback : DiffUtil.ItemCallback<BookingParticipant>() {
        override fun areItemsTheSame(
            oldItem: BookingParticipant,
            newItem: BookingParticipant
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: BookingParticipant,
            newItem: BookingParticipant
        ): Boolean {
            return oldItem == newItem
        }
    }
}