package com.khtn.zone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.khtn.zone.R
import com.khtn.zone.databinding.FragmentSetupProfileBinding

class SetupProfileFragment : Fragment() {
    private lateinit var binding: FragmentSetupProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupProfileBinding.inflate(inflater)
        return binding.root
    }
}