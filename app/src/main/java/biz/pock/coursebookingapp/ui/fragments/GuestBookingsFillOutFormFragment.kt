package biz.pock.coursebookingapp.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.BookingInvoiceContact
import biz.pock.coursebookingapp.data.model.BookingParticipant
import biz.pock.coursebookingapp.data.model.CreateBookingRequest
import biz.pock.coursebookingapp.data.model.GuestBookingDetails
import biz.pock.coursebookingapp.data.repositories.BookingRepository
import biz.pock.coursebookingapp.databinding.FragmentGuestBookingsFillOutFormBinding
import biz.pock.coursebookingapp.ui.activities.MainActivity
import biz.pock.coursebookingapp.ui.adapters.ParticipantAdapter
import biz.pock.coursebookingapp.ui.dialogs.ParticipantDialog
import biz.pock.coursebookingapp.utils.AlertUtils
import biz.pock.coursebookingapp.utils.validators.BookingFormValidator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GuestBookingsFillOutFormFragment : Fragment() {

    private var _binding: FragmentGuestBookingsFillOutFormBinding? = null
    private val binding get() = _binding!!
    private val args: GuestBookingsFillOutFormFragmentArgs by navArgs()
    private var allBillingFieldsValid = false

    private lateinit var participantAdapter: ParticipantAdapter

    @Inject
    lateinit var formValidator: BookingFormValidator

    @Inject
    lateinit var bookingRepository: BookingRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuestBookingsFillOutFormBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()

        setupActionBar()

        // Custom Navigation für Up Button setzen, damit die Kursliste
        // beim zurück navigieren ausgewählt wird
        (requireActivity() as MainActivity).setCustomNavigateUpCallback {
            findNavController().navigate(
                GuestTimeslotListFragmentDirections
                    .actionGuestTimeslotListFragmentToGuestDashboardFragment(1)
            )
            true
        }

    }

    private fun setupActionBar() {
        // Cast auf AppCompatActivity ist sicher, da wir MainActivity verwenden
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }


    private fun setupRecyclerView() {
        participantAdapter = ParticipantAdapter(
            onEditClick = { participant -> editParticipant(participant) },
            onDeleteClick = { participant -> deleteParticipant(participant) },
            requireContext()
        )
    }

    private fun setupUI() {
        // Buchungsinfos
        setupBookingInfo()
        // Teilnehmerliste
        setupParticipantsList()
        // Validierung
        setupBillingFieldValidation()
        // Button zum Übernehmen der Rechnungsdaten
        setupAddBillingAsParticipantButton()
        // Buchen Button
        setupBookButton()
    }

    private fun setupBookingInfo() {
        val timeslot = args.timeslot
        val course = args.course

        binding.apply {
            val location = course.locations?.find { it.id == timeslot.locationId }

            textCourseTitle.text = course?.title
            textLocation.text = location?.name ?: getString(R.string.unknown_location)
            textDateRange.text = "${timeslot.startDate} - ${timeslot.endDate}"
            textPrice.text =
                getString(R.string.price_per_participant, course?.pricePerParticipant ?: 0.0)
        }
    }

    private fun setupParticipantsList() {
        binding.apply {
            recyclerViewParticipants.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = participantAdapter
            }

            buttonAddParticipant.setOnClickListener {
                showAddParticipantDialog()
            }
        }
    }

    private fun setupBillingFieldValidation() {
        binding.apply {
            val fields = listOf(
                editTextBillingFirstname to inputLayoutBillingFirstname,
                editTextBillingLastname to inputLayoutBillingLastname,
                editTextBillingEmail to inputLayoutBillingEmail,
                editTextBillingPhone to inputLayoutBillingPhone,
                editTextBillingAddress to inputLayoutBillingAddress,
                editTextBillingZip to inputLayoutBillingZip,
                editTextBillingCity to inputLayoutBillingCity,
                editTextBillingCountry to inputLayoutBillingCountry
            )

            fields.forEach { (editText, inputLayout) ->
                editText.addTextChangedListener {
                    inputLayout.error = null
                    validateBillingFields()
                }
            }
        }
    }

    private fun validateBillingFields(): Boolean {
        var isValid = true
        binding.apply {
            // Vorname validieren
            formValidator.validateName(editTextBillingFirstname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingFirstname.error = result.errorMessage
                    isValid = false
                }
            }

            // Nachname validieren
            formValidator.validateName(editTextBillingLastname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingLastname.error = result.errorMessage
                    isValid = false
                }
            }

            // Email validieren
            formValidator.validateEmail(editTextBillingEmail.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingEmail.error = result.errorMessage
                    isValid = false
                }
            }

            // Telefon validieren
            formValidator.validatePhone(editTextBillingPhone.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingPhone.error = result.errorMessage
                    isValid = false
                }
            }

            // Adresse validieren
            formValidator.validateAddress(editTextBillingAddress.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingAddress.error = result.errorMessage
                    isValid = false
                }
            }

            // PLZ validieren
            formValidator.validateZip(editTextBillingZip.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingZip.error = result.errorMessage
                    isValid = false
                }
            }

            // Stadt validieren
            formValidator.validateCity(editTextBillingCity.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingCity.error = result.errorMessage
                    isValid = false
                }
            }

            // Land validieren
            formValidator.validateCountry(editTextBillingCountry.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingCountry.error = result.errorMessage
                    isValid = false
                }
            }
        }

        // Status aktualisieren und Button-Sichtbarkeit anpassen
        allBillingFieldsValid = isValid
        updateAddBillingAsParticipantButtonVisibility()
        return isValid
    }

    private fun setupAddBillingAsParticipantButton() {
        binding.buttonAddBillingAsParticipant.setOnClickListener {
            val participant = BookingParticipant(
                firstname = binding.editTextBillingFirstname.text.toString(),
                lastname = binding.editTextBillingLastname.text.toString(),
                email = binding.editTextBillingEmail.text.toString(),
                birthdate = "", // Wird im Dialog gesetzt
                skillLevel = "" // Wird im Dialog gesetzt
            )
            showAddParticipantDialog(participant)
        }
        updateAddBillingAsParticipantButtonVisibility()
    }

    private fun setupBookButton() {
        binding.buttonBook.setOnClickListener {
            if (validateForm()) {
                createBooking()
            }
        }
    }

    private fun updateAddBillingAsParticipantButtonVisibility() {
        binding.buttonAddBillingAsParticipant.visibility = if (
            allBillingFieldsValid &&
            participantAdapter.currentList.isEmpty()
        ) View.VISIBLE else View.GONE
    }

    private fun validateForm(): Boolean {
        var isValid = true
        binding.apply {
            // Vorname validieren
            formValidator.validateName(editTextBillingFirstname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingFirstname.error = result.errorMessage
                    isValid = false
                }
            }

            // Nachname validieren
            formValidator.validateName(editTextBillingLastname.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingLastname.error = result.errorMessage
                    isValid = false
                }
            }

            // Email validieren
            formValidator.validateEmail(editTextBillingEmail.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingEmail.error = result.errorMessage
                    isValid = false
                }
            }

            // Telefon validieren
            formValidator.validatePhone(editTextBillingPhone.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingPhone.error = result.errorMessage
                    isValid = false
                }
            }

            // Adresse validieren
            formValidator.validateAddress(editTextBillingAddress.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingAddress.error = result.errorMessage
                    isValid = false
                }
            }

            // PLZ validieren
            formValidator.validateZip(editTextBillingZip.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingZip.error = result.errorMessage
                    isValid = false
                }
            }

            // Stadt validieren
            formValidator.validateCity(editTextBillingCity.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingCity.error = result.errorMessage
                    isValid = false
                }
            }

            // Land validieren
            formValidator.validateCountry(editTextBillingCountry.text.toString()).let { result ->
                if (result is BookingFormValidator.ValidationResult.Invalid) {
                    inputLayoutBillingCountry.error = result.errorMessage
                    isValid = false
                }
            }

            // Prüfen ob mindestens ein Teilnehmer existiert
            if (participantAdapter.currentList.isEmpty()) {
                AlertUtils.showError(text = getString(R.string.error_no_participants))
                isValid = false
            }
        }
        return isValid
    }


    private fun deleteParticipant(participant: BookingParticipant) {
        val currentList = participantAdapter.currentList.toMutableList()
        currentList.remove(participant)
        participantAdapter.submitList(currentList)
        updateAddBillingAsParticipantButtonVisibility()
    }

    private fun getBillingContactData(): BookingInvoiceContact {
        return BookingInvoiceContact(
            firstname = binding.editTextBillingFirstname.text.toString(),
            lastname = binding.editTextBillingLastname.text.toString(),
            email = binding.editTextBillingEmail.text.toString(),
            phone = binding.editTextBillingPhone.text.toString(),
            address = binding.editTextBillingAddress.text.toString(),
            zip = binding.editTextBillingZip.text.toString(),
            city = binding.editTextBillingCity.text.toString(),
            country = binding.editTextBillingCountry.text.toString()
        )
    }

    private fun createFirstParticipantFromBillingContact(contact: BookingInvoiceContact): BookingParticipant {
        return BookingParticipant(
            firstname = contact.firstname,
            lastname = contact.lastname,
            email = contact.email,
            birthdate = "", // Will be set in dialog
            skillLevel = "" // Will be set in dialog
        )
    }

    private fun createBooking() {
        val billingContact = getBillingContactData()
        val participants = participantAdapter.currentList

        if (participants.isEmpty()) {
            showAddParticipantDialog(createFirstParticipantFromBillingContact(billingContact))
            return
        }

        // Create comment for booking
        val comment = "Booking for ${billingContact.firstname} ${billingContact.lastname}, " +
                "${billingContact.email}, ${billingContact.phone}, ${billingContact.address}"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Create booking
                val createBookingRequest = CreateBookingRequest(
                    timeslotId = args.timeslot.id,
                    courseId = args.timeslot.courseId,
                    comment = comment
                )

                val bookingResponse = bookingRepository.createBooking(createBookingRequest)

                // 2. Add invoice contact
                val invoiceContact = bookingRepository.createBookingInvoiceContact(
                    bookingId = bookingResponse.booking.id,
                    invoiceContact = billingContact,
                    bookingToken = bookingResponse.guest.token
                )

                // 3. Add participants
                val participantsList = mutableListOf<BookingParticipant>()
                participants.forEach { participant ->
                    val createdParticipant = bookingRepository.createBookingParticipant(
                        bookingId = bookingResponse.booking.id,
                        participant = participant,
                        bookingToken = bookingResponse.guest.token
                    )
                    participantsList.add(createdParticipant)
                }


                // 4. Erstelle GuestBookingDetails für die Success-Ansicht
                val bookingDetails = GuestBookingDetails(
                    id = bookingResponse.booking.id,
                    amount = bookingResponse.booking.amount,
                    comment = bookingResponse.booking.comment,
                    status = bookingResponse.booking.status,
                    courseId = bookingResponse.booking.courseId,
                    timeslotId = bookingResponse.booking.timeslotId,
                    invoiceContactId = bookingResponse.booking.invoiceContactId,
                    invoiceId = bookingResponse.booking.invoiceId,
                    createdAt = bookingResponse.booking.createdAt,
                    updatedAt = bookingResponse.booking.updatedAt,
                    course = args.course,  // Wir haben den vollen Course aus den Args
                    timeslot = args.timeslot,  // Wir haben den vollen Timeslot aus den Args
                    participants = participantsList,
                    invoiceContact = invoiceContact,
                    isExpanded = false  // Initial ausgeklappt anzeigen
                )

                // 5. Navigate to success screen
                Timber.w(">>> BOOKING DETAILS: $bookingDetails")
                findNavController().navigate(
                    GuestBookingsFillOutFormFragmentDirections
                        .actionGuestBookingsFillOutFormFragmentToGuestBookingSuccessFragment(
                            bookingDetails = bookingDetails
                        )
                )

                AlertUtils.showSuccess(text = getString(R.string.booking_successful))

            } catch (e: Exception) {
                Timber.e(e, "Error creating booking")
                AlertUtils.showError(text = e.message ?: getString(R.string.error_booking_failed))
            }
        }
    }

    private fun showAddParticipantDialog(participant: BookingParticipant? = null) {
        ParticipantDialog.newInstance(participant).apply {
            setOnSaveListener { savedParticipant ->
                val currentList = participantAdapter.currentList.toMutableList()

                if (participant != null) {
                    // Falls wir tatsächlich einen "bestehenden" Teilnehmer editieren wollen,
                    // etwas unsauber, aber erfüllt den Zweck auf die Schnelle
                    val index = currentList.indexOfFirst { existingParticipant ->
                        existingParticipant.firstname == participant.firstname &&
                                existingParticipant.lastname == participant.lastname &&
                                existingParticipant.email == participant.email &&
                                existingParticipant.birthdate == participant.birthdate &&
                                existingParticipant.skillLevel == participant.skillLevel
                    }

                    if (index != -1) {
                        // Teilnehmer gefunden, also updaten
                        currentList[index] = savedParticipant
                    } else {
                        // Teilnehmer mit diesen Daten existierte nicht in der Liste,
                        // daher als neuen hinzufügen
                        currentList.add(savedParticipant)
                    }
                } else {
                    // War null, also komplett neuer Teilnehmer
                    currentList.add(savedParticipant)
                }

                // Neue Liste dem Adapter übergeben
                participantAdapter.submitList(currentList)
                updateAddBillingAsParticipantButtonVisibility()
            }
        }.show(childFragmentManager, "participant_dialog")
    }

    private fun editParticipant(participant: BookingParticipant) {
        showAddParticipantDialog(participant)
    }


    override fun onDestroyView() {
        // Custom Navigation zurücksetzen
        (requireActivity() as MainActivity).setCustomNavigateUpCallback(null)

        super.onDestroyView()
        _binding = null
    }
}