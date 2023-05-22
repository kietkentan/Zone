package com.khtn.zone.custom.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.khtn.zone.adapter.CountriesAdapter
import com.khtn.zone.databinding.DialogCountrySheetBinding
import com.khtn.zone.model.Country
import com.khtn.zone.utils.setTransparentBackground
import dagger.hilt.android.AndroidEntryPoint

interface SheetCountriesListener {
    fun selectedItem(country: Country)
}

@AndroidEntryPoint
class DialogCountrySheet: BottomSheetDialogFragment() {
    private lateinit var binding: DialogCountrySheetBinding
    private lateinit var listener: SheetCountriesListener

    private lateinit var countriesAdapter: CountriesAdapter

    companion object{
        fun newInstance(bundle: Bundle): DialogCountrySheet {
            val fragment = DialogCountrySheet()
            fragment.arguments = bundle
            return fragment
        }
    }

    fun addListener(listener: SheetCountriesListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCountrySheetBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setTransparentBackground()

        initView()
        initClick()
    }

    private fun initClick() {
        binding.searchInput.onTextChangeListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                countriesAdapter.filter(s.toString())
            }
        })
    }

    private fun initView() {
        CountriesAdapter.itemClickListener = listener
        countriesAdapter = CountriesAdapter()
        countriesAdapter.setData()
        binding.recCountry.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.recCountry.adapter = countriesAdapter
    }
}