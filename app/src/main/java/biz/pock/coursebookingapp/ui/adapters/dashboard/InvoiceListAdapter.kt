package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Invoice
import biz.pock.coursebookingapp.databinding.ItemInvoiceBinding
import biz.pock.coursebookingapp.shared.enums.InvoiceStatus
import java.text.SimpleDateFormat
import java.util.Locale
import timber.log.Timber

class InvoiceListAdapter(
    private val onInvoiceClick: (Invoice, View) -> Unit,
    private val onDownloadClick: (Invoice) -> Unit,
    private val onExtendClick: (Invoice) -> Unit,
    private val onViewClick: (Invoice) -> Unit
) : BaseListAdapter<Invoice, ItemInvoiceBinding>(InvoiceDiffCallback()) {

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemInvoiceBinding {
        return ItemInvoiceBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemInvoiceBinding, item: Invoice) {
        binding.apply {
            // Rechnungsnummer
            val invoiceNumber = root.context.getString(R.string.invoice_number, item.number)
            textInvoiceNumber.text = invoiceNumber
            textInvoiceNumber.contentDescription = root.context.getString(
                R.string.invoice_number_desc,
                item.number
            )

            // Betrag formatiert
            val amountText = root.context.getString(R.string.invoice_amount, item.amount)
            textAmount.text = amountText

            // Status mit Enum konvertieren und entsprechend stylen
            val status = InvoiceStatus.fromApiString(item.status)
            val statusText = status?.let {
                root.context.getString(it.resId)
            } ?: item.status.replaceFirstChar { it.uppercase() }
            textStatus.text = statusText

            // Status-Chip Styling und Accessibility
            textStatus.setBackgroundResource(R.drawable.bg_status_chip)
            textStatus.setTextColor(getStatusColor(root.context, status))

            // Ablaufdatum des Download-Tokens mit Accessibility
            textTokenExpiry.apply {
                if (item.downloadTokenExpiresAt != null) {
                    try {
                        val date = apiDateFormat.parse(item.downloadTokenExpiresAt)
                        if (date != null) {
                            visibility = View.VISIBLE
                            val displayDate = displayDateFormat.format(date)
                            text = root.context.getString(R.string.invoice_expires, displayDate)
                            contentDescription = root.context.getString(
                                R.string.invoice_expiry_desc,
                                displayDate
                            )
                        } else {
                            visibility = View.GONE
                        }
                    } catch (e: Exception) {
                        Timber.e(e, ">>> Error parsing date: ${item.downloadTokenExpiresAt}")
                        visibility = View.GONE
                    }
                } else {
                    visibility = View.GONE
                }
            }

            // Download Button Visibility und Status
            buttonDownload.apply {
                visibility = when (item.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_REVERSAL,
                    Invoice.STATUS_PENDING -> View.VISIBLE
                    else -> View.GONE
                }
                setOnClickListener { onDownloadClick(item) }
            }

            // Extend Button Visibility und Status
            buttonExtend.apply {
                visibility = when {
                    (item.status == Invoice.STATUS_PAID ||
                            item.status == Invoice.STATUS_DRAFT ||
                            item.status == Invoice.STATUS_CONFIRMED ||
                            item.status == Invoice.STATUS_PENDING) &&
                            item.downloadTokenExpiresAt != null -> View.VISIBLE
                    else -> View.GONE
                }
                setOnClickListener { onExtendClick(item) }
            }

            buttonView.apply {
                visibility = when (item.status) {
                    Invoice.STATUS_PAID,
                    Invoice.STATUS_CONFIRMED,
                    Invoice.STATUS_REVERSAL,
                    Invoice.STATUS_PENDING -> View.VISIBLE
                    else -> View.GONE
                }
                setOnClickListener { onViewClick(item) }
            }

            // Gesamtbeschreibung für das Item
            root.contentDescription = root.context.getString(
                R.string.invoice_details_format,
                item.number,
                amountText,
                statusText
            )

            // Klick auf gesamte Card
            root.setOnClickListener { view ->
                onInvoiceClick(item, view)
            }
        }
    }

    private fun getStatusColor(context: Context, status: InvoiceStatus?): Int {
        return when (status) {
            InvoiceStatus.DRAFT -> context.getColor(R.color.status_draft)
            InvoiceStatus.PENDING -> context.getColor(R.color.status_pending)
            InvoiceStatus.PAID -> context.getColor(R.color.status_paid)
            InvoiceStatus.CONFIRMED -> context.getColor(R.color.status_confirmed)
            InvoiceStatus.CANCELED,
            InvoiceStatus.CANCELED_WITH_REVERSAL,
            InvoiceStatus.REVERSAL -> context.getColor(R.color.status_canceled)
            null -> context.getColor(R.color.on_surface)
        }
    }

    private class InvoiceDiffCallback : DiffUtil.ItemCallback<Invoice>() {
        override fun areItemsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Invoice, newItem: Invoice): Boolean {
            return oldItem == newItem
        }
    }
}