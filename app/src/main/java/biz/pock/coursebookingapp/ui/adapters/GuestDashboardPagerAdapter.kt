package biz.pock.coursebookingapp.ui.adapters
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import biz.pock.coursebookingapp.ui.fragments.GuestCourseListFragment
import biz.pock.coursebookingapp.ui.fragments.GuestHelpFragment
import biz.pock.coursebookingapp.ui.fragments.GuestWelcomeFragment

class GuestDashboardPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GuestWelcomeFragment()
            1 -> GuestCourseListFragment()
            2 -> GuestHelpFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}