package com.khtn.zone.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.databinding.ItemMemberAddingBinding
import com.khtn.zone.utils.printMeD
import com.khtn.zone.viewmodel.CreateGroupChatViewModel

class ContactMemberAdapter(
    val context: Context,
    val viewModel: CreateGroupChatViewModel
): ListAdapter<ChatUser, RecyclerView.ViewHolder>(DiffCallbackMemberAddGroup()) {
    companion object {
        var memberContact = ArrayList<ChatUser>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemMemberAddingBinding.inflate(layoutInflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as MyViewHolder
        holder.bind(context, viewModel, getItem(position))
    }

    override fun submitList(list: List<ChatUser>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    class MyViewHolder(private val binding: ItemMemberAddingBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, viewModel: CreateGroupChatViewModel, item: ChatUser) {
            binding.user = item
            binding.setClickListener {
                viewModel.listMemberRemove(item)
            }
            binding.executePendingBindings()
        }
    }
}

class DiffCallbackMemberAddGroup: DiffUtil.ItemCallback<ChatUser>() {
    override fun areItemsTheSame(
        oldItem: ChatUser,
        newItem: ChatUser
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ChatUser,
        newItem: ChatUser
    ): Boolean {
        return oldItem.isSelected == newItem.isSelected &&
                oldItem.locallySaved == newItem.locallySaved &&
                oldItem.unRead == newItem.unRead &&
                oldItem.localName == newItem.localName
    }
}