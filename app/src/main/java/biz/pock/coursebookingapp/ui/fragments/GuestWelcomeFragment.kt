package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.databinding.FragmentGuestWelcomeBinding

class GuestWelcomeFragment : Fragment() {

    private var _binding: FragmentGuestWelcomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.welcomeGuestButton.setOnClickListener {
            // Als Gast fortfahren - zum nächsten Tab (GuestCourseList) wechseln
            (parentFragment as? GuestDashboardFragment)?.navigateToTab(1)
        }

        binding.welcomeLoginButton.setOnClickListener {
            // Zum Login navigieren
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}