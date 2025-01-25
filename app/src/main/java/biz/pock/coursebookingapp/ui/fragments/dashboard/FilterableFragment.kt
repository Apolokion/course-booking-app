package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class FilterableFragment : Fragment() {

    protected abstract val viewModel: BaseDashboardViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SwipeRefresh Setup
        view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)?.apply {
            setColorSchemeResources(
                R.color.primary,
                R.color.secondary
            )
            setOnRefreshListener {
                handleRefresh()
            }
        }
    }

    private fun handleRefresh() {
        lifecycleScope.launch {
            // Daten neu von API laden
            viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)

            // Filter anwenden
            applyCurrentFilters()

            // Nach 1 Sekunde SwipeRefresh ausblenden
            delay(1000)
            view?.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)?.isRefreshing = false
        }
    }

    abstract fun applyCurrentFilters()
}