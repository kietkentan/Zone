package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.custom.dialog.SheetCountriesListener
import com.khtn.zone.databinding.RowCountryBinding
import com.khtn.zone.model.Country
import com.khtn.zone.utils.Countries
import java.util.*
import kotlin.collections.ArrayList

class CountriesAdapter: RecyclerView.Adapter<CountriesAdapter.UserViewModel>() {
    lateinit var countries: ArrayList<Country>
    private lateinit var  allCountries: ArrayList<Country>

    @SuppressLint("LogNotTimber")
    fun setData() {
        this.countries = Countries.getCountries() as ArrayList<Country>
        allCountries = ArrayList()
        allCountries.addAll(countries)
    }

    companion object {
        var itemClickListener: SheetCountriesListener? = null
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        try {
            countries.clear()
            if (query.isEmpty())
                countries.addAll(allCountries)
            else {
                for (country in allCountries) {
                    if (country.name.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))
                    )
                        countries.add(country)
                }
            }
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewModel {
        val binding: RowCountryBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.row_country, parent, false
        )
        return UserViewModel(binding)
    }

    override fun onBindViewHolder(holder: UserViewModel, position: Int) {
        holder.bind(countries[position])
    }

    class UserViewModel(val binding: RowCountryBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Country) {
            binding.country = item
            binding.viewRoot.setOnClickListener {
                itemClickListener?.selectedItem(item)
            }
        }
    }

    override fun getItemCount() = countries.size
}