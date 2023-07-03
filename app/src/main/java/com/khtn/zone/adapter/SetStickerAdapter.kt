package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.databinding.ItemStickerSetBinding
import com.khtn.zone.model.SetSticker
import com.khtn.zone.utils.ImageUtils

class SetStickerAdapter(val onItemClick: (item: SetSticker) -> Unit): RecyclerView.Adapter<SetStickerAdapter.MyViewHolder>() {
    companion object {
        var setSetSticker: Set<SetSticker> = setOf()
        var positionSelected = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = ItemStickerSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return setSetSticker.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(set: Set<SetSticker>) {
        setSetSticker = set
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePositionSelected(position: Int) {
        positionSelected = position
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = setSetSticker.elementAt(position)
        holder.bind(item)
    }

    inner class MyViewHolder(private val binding: ItemStickerSetBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SetSticker) {
            ImageUtils.loadSetSticker(
                binding.ivStickerSet,
                binding.progressStickerSet,
                item
            )

            binding.ivStickerSet.setOnClickListener {
                onItemClick(item)
            }

            binding.layoutStickerSet.setBackgroundResource(
                if (positionSelected == bindingAdapterPosition) R.color.color_background
                else R.color.white
            )
        }
    }
}