package com.khtn.zone.utils.listener

import android.view.View
import com.khtn.zone.model.Country

interface ItemCountriesClickListener {
    fun onItemClicked(v: View, country: Country)
}