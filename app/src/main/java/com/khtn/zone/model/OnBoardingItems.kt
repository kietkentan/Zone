package com.khtn.zone.model

import com.khtn.zone.R

class OnBoardingItems(
    val image : Int,
    val title : Int,
    val desc : Int
) {
    companion object {
        fun getData() : List<OnBoardingItems> {
            return listOf(
                OnBoardingItems(R.drawable.ic_intro_review_1, R.string.intro_title_1, R.string.intro_desc_1),
                OnBoardingItems(R.drawable.ic_intro_review_2, R.string.intro_title_2, R.string.intro_desc_2),
                OnBoardingItems(R.drawable.ic_intro_review_3, R.string.intro_title_3, R.string.intro_desc_3),
                OnBoardingItems(R.drawable.ic_intro_review_4, R.string.intro_title_4, R.string.intro_desc_4)
            )
        }
    }
}