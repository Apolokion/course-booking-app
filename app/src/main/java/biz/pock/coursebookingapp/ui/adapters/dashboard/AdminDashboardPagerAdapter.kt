package biz.pock.coursebookingapp.ui.adapters.dashboard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminBookingListFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminInvoiceListFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminLocationListFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminUserListFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminCoursesFragment
import biz.pock.coursebookingapp.ui.fragments.dashboard.AdminTimeslotsFragment

class AdminDashboardPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 6

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminBookingListFragment()
            1 -> AdminInvoiceListFragment()
            2 -> AdminCoursesFragment()
            3 -> AdminLocationListFragment()
            4 -> AdminTimeslotsFragment()
            5 -> AdminUserListFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}