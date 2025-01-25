package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import biz.pock.coursebookingapp.databinding.FragmentGuestBookingSuccessBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuestBookingSuccessFragment : Fragment() {

    private var _binding: FragmentGuestBookingSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestBookingSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }



    private fun setupButtons() {
        binding.buttonBookAnother.setOnClickListener {
            // Zum Course List Fragment navigieren
            findNavController().navigate(
                GuestBookingSuccessFragmentDirections
                    .actionGuestBookingSuccessFragmentToGuestDashboardFragment(1)
            )
        }

        binding.buttonBackToStart.setOnClickListener {
            // Zurück zum Welcome Fragment
            findNavController().navigate(
                GuestBookingSuccessFragmentDirections
                    .actionGuestBookingSuccessFragmentToGuestDashboardFragment()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}