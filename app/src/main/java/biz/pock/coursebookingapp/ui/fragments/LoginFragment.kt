package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.repositories.AuthRepository
import biz.pock.coursebookingapp.databinding.FragmentLoginBinding
import biz.pock.coursebookingapp.ui.viewmodels.LoginViewModel
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.ErrorHandler
import biz.pock.coursebookingapp.utils.StringProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    // Binding Objekt für die FragmentLayout Elemente
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Zugriff auf das zugehörige ViewModel für den Login-Prozess
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var errorHandler: ErrorHandler

    @Inject
    lateinit var stringProvider: StringProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Layout-Inflation mit ViewBinding
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ActionBar Konfiguration
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)  // Back Button anzeigen
            setDisplayShowHomeEnabled(true)  // Home Icon anzeigen
        }

        // Prüfen ob bereits eingeloggt
        if (authRepository.isLoggedIn()) {
            when (authRepository.getCurrentRole()) {
                "admin" -> findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToAdminDashboardFragment()
                )

                "trainer" -> findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToTrainerDashboardFragment()
                )

                else -> {
                    // Bei guest keine Weiterleitung
                    Timber.d(">>> User is logged in as guest, staying on login screen")
                    setupUI()
                    observeViewModel()
                }
            }

            return
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            lifecycleScope.launch {
                Timber.w(">>> ROLE == ${authRepository.getCurrentRole()}")
                if (authRepository.getCurrentRole() == "guest") {
                    // Wenn als Gast eingeloggt, dann muss zuerst
                    // ausgeloggt werden
                    authRepository.logout()
                    delay(300)
                }

                viewModel.login(email, password)
            }
        }
    }


    private fun observeViewModel() {
        // Beobachtet die LiveData- bzw. StateFlows des ViewModels und aktualisiert die UI entsprechend.
        // Hier wird zwischen Login-Zuständen (Laden, Erfolg, Fehler) und Eingabe-Zuständen (gültig, ungültig) unterschieden.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Login Status Beobachtung starten
                launch {
                    viewModel.loginState.collect { state ->
                        when (state) {
                            // Initialer Zustand: Noch kein Login-Vorgang gestartet.
                            LoginViewModel.LoginState.Initial -> Unit

                            // Während des Login-Vorgangs -> Zeige Ladeindikator
                            // und deaktiviere Login-Button
                            LoginViewModel.LoginState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.buttonLogin.isEnabled = false
                            }

                            // Bei erfolgreichem Login -> Ladeindikator ausblenden,
                            // Button aktivieren und weiter navigieren
                            is LoginViewModel.LoginState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.buttonLogin.isEnabled = true
                                // Menü aktualisieren bevor wir navigieren
                                requireActivity().invalidateOptionsMenu()
                                // Navigiert zum entsprechenden Portal
                                navigateToUserPortal(state.userRole)
                            }

                            // Bei Fehler -> Ladeindikator ausblenden und Button aktivieren
                            is LoginViewModel.LoginState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.buttonLogin.isEnabled = true
                                AlertUtils.showError(
                                    textRes = R.string.error_login_failed,
                                    duration = 3000)
                            }
                        }
                    }
                }

                // Beobachtet den Zustand der Eingaben (E-Mail / Passwort)
                launch {
                    viewModel.inputState.collect { state ->
                        when (state) {
                            // Initialer Zustand: Noch keine Validierung durchgeführt
                            LoginViewModel.InputState.Initial -> Unit

                            // Eingaben sind gültig, keine Fehlermeldungen anzeigen
                            LoginViewModel.InputState.Valid -> {
                                binding.textInputLayoutEmail.error = null
                                binding.textInputLayoutPassword.error = null
                            }

                            // Eingaben sind ungültig, entsprechende Fehlermeldungen anzeigen
                            is LoginViewModel.InputState.Invalid -> {
                                // Die Fehlermeldungen werden hier aus dem Context geholt,
                                // um z. B. zur Laufzeit die Sprache wechseln zu können.
                                binding.textInputLayoutEmail.error = state.emailError?.let {
                                    getString(it)
                                }
                                binding.textInputLayoutPassword.error = state.passwordError?.let {
                                    getString(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun navigateToUserPortal(userRole: String) {
        try {
            when (userRole) {
                "admin" -> findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToAdminDashboardFragment()
                )

                "trainer" -> findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToTrainerDashboardFragment()
                )

                "guest" -> findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToGuestDashboardFragment()
                )

                else -> {
                    Timber.e(">>> Unknown role: $userRole")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, ">>> Navigation failed")
        }
    }


    // Beim Verlassen des Fragments muss das _binding Objekt freigegeben werden,
    // damit kein Speicherleck entsteht
    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
        _binding = null
    }
}