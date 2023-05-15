package com.khtn.zone.adapter

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.adapter.FragmentViewHolder
import com.khtn.zone.fragment.OnBoardingFragment
import com.khtn.zone.model.OnBoardingItems

class OnBoardingAdapter(
    private val list : List<OnBoardingItems>,
    manager: FragmentManager,
    lifecycle : Lifecycle
) : FragmentStateAdapter(
    manager,
    lifecycle
) {
    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return OnBoardingFragment(list[position])
    }
}