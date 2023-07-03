package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.databinding.ViewSingleSetStickerBinding
import com.khtn.zone.model.SetSticker
import com.khtn.zone.model.Sticker
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.setMargin
import com.khtn.zone.utils.showView

class AllSetStickerAdapter(
    val context: Context,
    val onItemClick: (item: Sticker) -> Unit
): RecyclerView.Adapter<AllSetStickerAdapter.MyViewHolder>() {
    companion object {
        var mapSticker: Map<SetSticker, List<Sticker>> = mapOf()
        var listKey: Set<SetSticker> = setOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = ViewSingleSetStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mapSticker.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(map: Map<SetSticker, List<Sticker>>, set: Set<SetSticker>) {
        mapSticker = map
        listKey = set
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = mapSticker[listKey.elementAt(position)]
        holder.bind(context, item, listKey.elementAt(position))
    }

    inner class MyViewHolder(private val binding: ViewSingleSetStickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: List<Sticker>?, set: SetSticker) {
            val gridLayoutManager = GridLayoutManager(context, 4)
            val stickerAdapter: SingleSetStickerAdapter by lazy {
                SingleSetStickerAdapter(
                    onItemClick = { item ->
                        onItemClick.invoke(item)
                    }
                )
            }

            binding.tvNameSetSticker.text = set.name

            binding.recSetSticker.apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                layoutManager = gridLayoutManager
                itemAnimator = null
                adapter = stickerAdapter
            }

            if (item.isNullOrEmpty())
                binding.root.hideView()
            else {
                binding.root.showView()
                stickerAdapter.updateList(item)
            }

            if (bindingAdapterPosition == 0) {
                binding.root.setMargin(top = context.resources.getDimension(R.dimen.dp35).toInt())
            }
        }
    }
}