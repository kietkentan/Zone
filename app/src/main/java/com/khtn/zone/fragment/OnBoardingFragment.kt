package com.khtn.zone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.khtn.zone.databinding.FragmentOnBoardingBinding
import com.khtn.zone.model.OnBoardingItems
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingFragment(private val items : OnBoardingItems) : Fragment() {
    private lateinit var binding : FragmentOnBoardingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnBoardingBinding.inflate(inflater)

        binding.ivIntro.setImageResource(items.image)
        binding.tvTitleIntro.text = getText(items.title)
        binding.tvTextIntro.text = getText(items.desc)

        return binding.root
    }
}