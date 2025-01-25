package biz.pock.coursebookingapp.ui.fragments.dashboard

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.databinding.FragmentTrainerDashboardBinding
import biz.pock.coursebookingapp.shared.model.TabInfo
import biz.pock.coursebookingapp.ui.adapters.dashboard.TrainerDashboardPagerAdapter
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.BaseDashboardViewModel
import biz.pock.coursebookingapp.ui.viewmodels.dashboard.TrainerDashboardViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TrainerDashboardFragment : BaseDashboardFragment<FragmentTrainerDashboardBinding>() {

    private val viewModel: TrainerDashboardViewModel by viewModels()
    private var adapter: TrainerDashboardPagerAdapter? = null
    private var verticalNavItems = mutableListOf<MaterialCardView>()

    // Alle Tab-Informationen an einer Stelle
    private val tabs = listOf(
        TabInfo(R.string.tab_courses, R.drawable.ic_school),
        TabInfo(R.string.tab_bookings, R.drawable.ic_receipt),
        TabInfo(R.string.tab_timeslots, R.drawable.ic_schedule)
    )

    override fun getViewBinding(): FragmentTrainerDashboardBinding =
        FragmentTrainerDashboardBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDashboard()
        observeViewModel()
    }

    override fun setupDashboard() {
        try {
            binding.viewPager.apply {
                offscreenPageLimit = 1
                setPageTransformer { page, position ->
                    page.alpha = if (position in -1f..1f) 1f else 0f
                }
                isUserInputEnabled = true
                adapter = TrainerDashboardPagerAdapter(this@TrainerDashboardFragment)

                // Tabwechsel überwachen
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        viewModel.onTabSelected(position)
                        updateSelectedTab(position)
                        updateActionBarTitle(position)
                    }
                })
            }

            // Orientierungs-spezifische Navigation
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Vertikale Navigation für Landscape
                setupVerticalNavigation()
                // TabLayout ausblenden
                binding.tabLayout?.visibility = View.GONE
            } else {
                // Horizontale Navigation für Portrait
                // TabLayout einblenden
                binding.tabLayout?.visibility = View.VISIBLE
                // TabLayout mit ViewPager verbinden
                binding.tabLayout?.let { tabLayout ->
                    TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
                        tabs.getOrNull(position)?.let { tabInfo ->
                            tab.text = getString(tabInfo.titleResId)
                            tab.setIcon(tabInfo.iconResId)
                            tab.contentDescription = getString(tabInfo.titleResId)
                        }
                    }.attach()
                }
            }

            // Initial den korrekten Tab setzen und UI-Updates beobachten
            viewLifecycleOwner.lifecycleScope.launch {
                // Tab-Änderungen beobachten
                launch {
                    viewModel.currentTab.collect { tab ->
                        if (binding.viewPager.currentItem != tab) {
                            binding.viewPager.setCurrentItem(tab, false)
                            updateSelectedTab(tab)
                            updateActionBarTitle(tab)
                        }
                    }
                }

                // UI-State beobachten
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
            }

            // Initiale Tab-Position setzen
            viewModel.onTabSelected(0)
            updateSelectedTab(0)
            updateActionBarTitle(0)

        } catch (e: Exception) {
            Timber.e(e, ">>> Error in setupDashboard")
        }
    }

    private fun setupVerticalNavigation() {
        binding.verticalNavigation?.removeAllViews()
        verticalNavItems.clear()

        tabs.forEachIndexed { index, tabInfo ->
            val itemView = layoutInflater.inflate(
                R.layout.layout_vertical_nav,
                binding.verticalNavigation,
                false
            ) as MaterialCardView

            // Häkchen entfernen, weils irgendwie störend ist
            itemView.checkedIcon = null

            itemView.apply {
                // Icon setzen
                findViewById<ImageView>(R.id.iconView).setImageResource(tabInfo.iconResId)
                // Text setzen
                findViewById<TextView>(R.id.titleView).setText(tabInfo.titleResId)

                // Click Listener
                setOnClickListener {
                    binding.viewPager.currentItem = index
                    updateSelectedTab(index)
                }
            }

            verticalNavItems.add(itemView)
            binding.verticalNavigation?.addView(itemView)
        }
    }

    private fun updateSelectedTab(position: Int) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            verticalNavItems.forEachIndexed { index, item ->
                item.isChecked = index == position
                item.setCardBackgroundColor(
                    if (index == position)
                        context?.getColor(R.color.secondary_container) ?: 0
                    else
                        context?.getColor(R.color.surface) ?: 0
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setupVerticalNavigation()
        }
    }


    private fun handleUiState(state: BaseDashboardViewModel.UiState) {
        // Loading Container Sichtbarkeit
        binding.loadingContainer.visibility = when (state) {
            is BaseDashboardViewModel.UiState.Loading -> View.VISIBLE
            else -> View.GONE
        }

        // ViewPager Interaktivität
        binding.viewPager.isUserInputEnabled = state !is BaseDashboardViewModel.UiState.Loading

        when (state) {
            is BaseDashboardViewModel.UiState.Empty -> {
                showEmptyState()
            }
            is BaseDashboardViewModel.UiState.Error -> {
                AlertUtils.showError(text = state.message)
            }
            is BaseDashboardViewModel.UiState.Success<*> -> {
                showContent()
            }
            BaseDashboardViewModel.UiState.Loading -> {
                // Loading wird oben gehandelt
            }
        }
    }

    private fun updateActionBarTitle(position: Int) {
        tabs.getOrNull(position)?.let { tabInfo ->
            val newTitle = getString(tabInfo.titleResId)
            (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = newTitle
        }
    }

    override fun loadData() {
        viewModel.handleAction(BaseDashboardViewModel.Action.Refresh)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // UI State beobachten
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                // Events beobachten
                launch {
                    viewModel.events.collect { event ->
                        event?.let {
                            handleEvent(it)
                            viewModel.clearEvent()
                        }
                    }
                }

                // Dashboard Daten beobachten
                launch {
                    viewModel.dashboardData.collect { data ->
                        Timber.d(
                            ">>> Dashboard data updated: " +
                                    "Courses=${data.courses.size}, " +
                                    "Bookings=${data.bookings.size}, " +
                                    "Timeslots=${data.timeslots.size}"
                        )
                    }
                }
            }
        }
    }

    private fun handleEvent(event: BaseDashboardViewModel.Event) {
        when (event) {
            is BaseDashboardViewModel.Event.ShowMessage -> {
                AlertUtils.showInfo(text = event.message)
            }
            is BaseDashboardViewModel.Event.ShowError -> {
                AlertUtils.showError(text = event.message)
            }
            is BaseDashboardViewModel.Event.NavigateBack -> {
                // Back Navigation implementieren falls notwendig
            }
        }
    }

    private fun showEmptyState() {
        Timber.d(">>> Showing empty state")
    }

    private fun showContent() {
        Timber.d(">>> Showing content")
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }
}