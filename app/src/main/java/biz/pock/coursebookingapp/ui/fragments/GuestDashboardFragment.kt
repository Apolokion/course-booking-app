package biz.pock.coursebookingapp.ui.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.databinding.FragmentGuestDashboardBinding
import biz.pock.coursebookingapp.shared.GUEST_EMAIL
import biz.pock.coursebookingapp.shared.GUEST_PASSWORD
import biz.pock.coursebookingapp.shared.model.TabInfo
import biz.pock.coursebookingapp.ui.adapters.GuestDashboardPagerAdapter
import biz.pock.coursebookingapp.ui.viewmodels.GuestDashboardViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GuestDashboardFragment : Fragment() {

    private var _binding: FragmentGuestDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GuestDashboardViewModel by viewModels()
    private var adapter: GuestDashboardPagerAdapter? = null
    private val args: GuestDashboardFragmentArgs by navArgs()
    private var verticalNavItems = mutableListOf<MaterialCardView>()

    @Inject
    lateinit var authRepository: AuthRepository

    // Alle Tab-Informationen an einer Stelle
    private val tabs = listOf(
        TabInfo(R.string.guest_dashboard_welcome, R.drawable.ic_welcome),
        TabInfo(R.string.guest_dashboard_booking, R.drawable.ic_course_booking),
        TabInfo(R.string.guest_dashboard_help, R.drawable.ic_help)
    )


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.w(">>> GUEST-STATUS onViewCreated")
        checkGuestStatus()
        setupDashboard()
        observeViewModel()

        checkTabIndexFromArgs()
    }

    override fun onResume() {
        super.onResume()

        Timber.w(">>> GUEST-STATUS onResume")
        checkGuestStatus()

        checkTabIndexFromArgs()

    }

    private fun checkTabIndexFromArgs() {
        Timber.w(">>> checkTabIndexFromArgs called")
        val tabIndex = args.tabId
        tabIndex?.let { tabPosition ->
            navigateToTab(tabPosition)
        }
    }

    private fun checkGuestStatus() {
        // Als Gast einloggen, wenn aktuell kein Auth
        // Status vorhanden ist, weil wir sonst keine
        // Kurse anzeigen können, die wir ja dann buchen wollen
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            if (!authRepository.isLoggedIn()) {
                // Automatischer Guest-Login
                try {
                    authRepository.login(GUEST_EMAIL, GUEST_PASSWORD)
                    delay(300)
                } catch (e: Exception) {
                    Timber.e(e, ">>> Guest-Login failed")
                }
            }
        }
    }

    private fun setupDashboard() {
        binding.viewPager.apply {
            adapter = GuestDashboardPagerAdapter(this@GuestDashboardFragment)
            offscreenPageLimit = 2
            isUserInputEnabled = true
        }

        // Orientierungs-spezifische Navigation
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Vertikale Navigation für Landscape
            setupVerticalNavigation()
            // TabLayout ausblenden
            binding.appBarLayout?.visibility = View.GONE
        } else {
            // Horizontale Navigation für Portrait
            binding.appBarLayout?.visibility = View.VISIBLE
            // TabLayout einblenden und Tabs setzen
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tabs.getOrNull(position)?.let { tabInfo ->
                    tab.setIcon(tabInfo.iconResId)
                    tab.contentDescription = getString(tabInfo.titleResId)
                }
            }.attach()
        }

        // Tab-Wechsel überwachen
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateSelectedTab(position)
            }
        })
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
            binding.appBarLayout?.visibility = View.GONE
        } else {
            binding.appBarLayout?.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingContainer.visibility = when (state) {
                        is GuestDashboardViewModel.UiState.Loading -> View.VISIBLE
                        else -> View.GONE
                    }
                }
            }
        }
    }

    fun navigateToTab(position: Int) {
        _binding?.let {
            binding.viewPager.post {
                binding.viewPager.setCurrentItem(position, true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.w(">>> DESTROYED")
        adapter = null
    }
}