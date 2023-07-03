package com.khtn.zone.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.databinding.ItemAttachmentBinding
import com.khtn.zone.model.Attachment
import com.khtn.zone.utils.printMeD

class AttachmentAdapter(
    val onItemClick: (position: Int) -> Unit
): RecyclerView.Adapter<AttachmentAdapter.MyViewHolder>() {
    private val listAttachment = Attachment.getData()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = ItemAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return listAttachment.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = listAttachment[position]
        holder.bind(item)
    }

    inner class MyViewHolder(private val binding: ItemAttachmentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Attachment) {
            binding.attachment = item
            binding.attachmentItem.setOnClickListener {
                onItemClick(item.pos)
            }
        }
    }
}