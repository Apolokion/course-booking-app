package biz.pock.coursebookingapp.ui.adapters.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import biz.pock.coursebookingapp.R
import biz.pock.coursebookingapp.data.model.Location
import biz.pock.coursebookingapp.databinding.ItemLocationBinding

class LocationListAdapter(
    private val onLocationClick: (Location, View) -> Unit
) : BaseListAdapter<Location, ItemLocationBinding>(LocationDiffCallback()) {

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup): ItemLocationBinding {
        return ItemLocationBinding.inflate(inflater, parent, false)
    }

    override fun bind(binding: ItemLocationBinding, item: Location) {
        binding.apply {
            // Location Icon Accessibility
            imageLocation.contentDescription = root.context.getString(
                R.string.content_desc_location_icon
            )

            // Haupttexte mit Accessibility
            textName.text = item.name
            textName.contentDescription = root.context.getString(
                R.string.location_name_format,
                item.name
            )

            textAddress.text = item.address
            textAddress.contentDescription = root.context.getString(
                R.string.location_address_format,
                item.address
            )

            // Stadt-Info formatieren und Accessibility setzen
            val cityInfo = "${item.postcode} ${item.city}, ${item.country}"
            textCityInfo.text = cityInfo
            textCityInfo.contentDescription = root.context.getString(
                R.string.location_city_format,
                item.postcode,
                item.city,
                item.country
            )

            // Gesamtbeschreibung für das Item
            root.contentDescription = root.context.getString(
                R.string.location_details_format,
                item.name,
                item.address,
                cityInfo
            )

            // Click-Handler
            root.setOnClickListener { view ->
                onLocationClick(item, view)
            }

            // Icon Access
            imageLocation.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

            // Card Access
            root.apply {
                isClickable = true
                isFocusable = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        }
    }

    private class LocationDiffCallback : DiffUtil.ItemCallback<Location>() {
        override fun areItemsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Location, newItem: Location): Boolean {
            return oldItem == newItem
        }
    }
}