package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.User
import biz.pock.coursebookingapp.databinding.ItemUserBinding
import biz.pock.coursebookingapp.shared.enums.Role

class UserListAdapter(
    private val onUserClick: (User, View) -> Unit
) : BaseListAdapter<User, ItemUserBinding>(UserDiffCallback()) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemUserBinding {
        return ItemUserBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemUserBinding, item: User) {
        binding.apply {
            val fullName = "${item.firstname} ${item.lastname}"

            // Avatar Icon Accessibility
            imageAvatar.apply {
                contentDescription = root.context.getString(R.string.content_desc_user_avatar)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }

            // User Name mit Accessibility
            textName.text = fullName
            textName.contentDescription = root.context.getString(
                R.string.user_name_format,
                item.firstname,
                item.lastname
            )

            // Email mit Accessibility
            textEmail.text = item.email
            textEmail.contentDescription = root.context.getString(
                R.string.user_email_desc,
                item.email
            )

            // Rolle mit Accessibility
            val roleText = Role.fromUserRoleString(item.role)?.let {
                root.context.getString(it.resId)
            } ?: item.role
            textRole.text = roleText
            textRole.contentDescription = root.context.getString(
                R.string.user_role_desc,
                roleText
            )

            // Gesamte Item Description
            root.contentDescription = root.context.getString(
                R.string.user_details_format,
                item.firstname,
                item.lastname,
                roleText
            )

            // Card Accessibility und Click Handler
            root.apply {
                isClickable = true
                isFocusable = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                setOnClickListener { view ->
                    onUserClick(item, view)
                }
            }

            // Textfelder explizit als wichtig für Accessibility markieren
            textName.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textEmail.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            textRole.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}