package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseDashboardFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    abstract fun getViewBinding(): VB
    abstract fun setupDashboard()
    abstract fun loadData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d(">>> BaseDashboardFragment: onCreateView")
        _binding = getViewBinding()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}