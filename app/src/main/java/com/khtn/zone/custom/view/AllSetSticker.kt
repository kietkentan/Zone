package com.khtn.zone.custom.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.adapter.AllSetStickerAdapter
import com.khtn.zone.adapter.SetStickerAdapter
import com.khtn.zone.databinding.ViewAllSetStickerBinding
import com.khtn.zone.model.SetSticker
import com.khtn.zone.model.Sticker
import com.khtn.zone.utils.hideAnimation
import com.khtn.zone.utils.listener.StickerListener
import com.khtn.zone.utils.showAnimation

class AllSetSticker: ConstraintLayout {
    val binding = ViewAllSetStickerBinding.inflate(LayoutInflater.from(context), this, true)

    private val linearLayoutToolbar = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    private val linearLayoutAllList = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    private lateinit var stickerListener: StickerListener
    private var mapAllSticker: Map<SetSticker, List<Sticker>> = mapOf()
    private var setAllSetSticker: Set<SetSticker> = setOf()
    private val heightSetReview = context.resources.getDimension(R.dimen.dp35)
    private var currentPage = 0
    private var isHide = false

    private val allSetStickerAdapter: AllSetStickerAdapter by lazy {
        AllSetStickerAdapter(
            context = context,
            onItemClick = { item ->
                stickerListener.onStickerSetClicked(item)
            }
        )
    }

    private val setStickerAdapter: SetStickerAdapter by lazy {
        SetStickerAdapter(
            onItemClick = {}
        )
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    private fun initView() {
        binding.recAllSetReview.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            layoutManager = linearLayoutToolbar
            itemAnimator = null
            adapter = setStickerAdapter
        }

        binding.recAllSetSticker.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            layoutManager = linearLayoutAllList
            itemAnimator = null
            adapter = allSetStickerAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    changePosition()

                    binding.recAllSetReview.apply {
                        if (dy < 0 && this.visibility == View.GONE) {
                            isHide = false
                            this.showAnimation(heightSetReview)
                        } else if (dy > 0 && !isHide) {
                            this.hideAnimation(heightSetReview)
                            isHide = true
                        }
                    }
                }
            })
        }
    }

    private fun changePosition() {
        val pagePosition = (binding.recAllSetSticker.layoutManager as LinearLayoutManager?)!!
            .findFirstCompletelyVisibleItemPosition()
        if (pagePosition in setAllSetSticker.indices) {
            if (currentPage != pagePosition) {
                currentPage = pagePosition
                setStickerAdapter.updatePositionSelected(pagePosition)
            }
        }
    }

    fun updateListItem(map: Map<SetSticker, List<Sticker>>) {
        mapAllSticker = map
        setAllSetSticker = map.keys
        allSetStickerAdapter.updateList(mapAllSticker, setAllSetSticker)
        setStickerAdapter.updateList(setAllSetSticker)
    }

    fun updateListener(listener: StickerListener) {
        stickerListener = listener
    }
}