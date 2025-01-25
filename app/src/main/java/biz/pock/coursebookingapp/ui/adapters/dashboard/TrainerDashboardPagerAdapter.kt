package biz.pock.coursebookingapp.ui.adapters.dashboard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import biz.pock.coursebookingapp.ui.fragments.dashboard.TrainerBookingsFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.TrainerCoursesFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.TrainerTimeslotsFragment

class TrainerDashboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TrainerCoursesFragment()
            1 -> TrainerBookingsFragment()
            2 -> TrainerTimeslotsFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}