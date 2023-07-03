package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.databinding.ItemStickerReviewBinding
import com.khtn.zone.model.Sticker
import com.khtn.zone.utils.ImageUtils

class SingleSetStickerAdapter(val onItemClick: (item: Sticker) -> Unit): RecyclerView.Adapter<SingleSetStickerAdapter.MyViewHolder>() {
    companion object {
        var listSticker: List<Sticker> = listOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = ItemStickerReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return listSticker.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: List<Sticker>) {
        listSticker = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = listSticker[position]
        holder.bind(item)
    }

    inner class MyViewHolder(private val binding: ItemStickerReviewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Sticker) {
            ImageUtils.loadSingleSticker(
                binding.ivStickerReview,
                binding.progressStickerReview,
                item
            )

            binding.ivStickerReview.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}