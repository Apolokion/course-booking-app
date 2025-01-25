package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import biz.pock.coursebookingapp.databinding.FragmentGuestHelpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuestHelpFragment : Fragment() {

    private var _binding: FragmentGuestHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}