package com.khtn.zone.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.databinding.RowChatBinding
import com.khtn.zone.database.data.ChatUserWithMessages
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showView
import java.util.*
import javax.inject.Inject

class SingleChatHomeAdapter(private val context: Context): ListAdapter<ChatUserWithMessages, RecyclerView.ViewHolder>(DiffCallbackChats()) {

    @Inject
    lateinit var preference: SharedPreferencesManager

    companion object {
        lateinit var allChatList: MutableList<ChatUserWithMessages>
        lateinit var itemClickListener: ItemClickListener
    }

    fun filter(query: String) {
        try {
            val list= mutableListOf<ChatUserWithMessages>()
            if (query.isEmpty())
                list.addAll(allChatList)
            else {
                for (contact in allChatList) {
                    if (contact.user.localName.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))) {
                        list.add(contact)
                    }
                }
            }
            submitList(null)
            submitList(list)
        } catch (e: Exception) {
            e.stackTrace
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RowChatBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val viewHolder=holder as ViewHolder
        viewHolder.bind(getItem(position))
    }

    class ViewHolder(private val binding: RowChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatUserWithMessages) {
            binding.chatUser = item

            if (bindingAdapterPosition != allChatList.size - 1)
                binding.viewBottomChatHome.showView()
            else binding.viewBottomChatHome.hideView()

            binding.viewRoot.setOnClickListener { v ->
                itemClickListener.onItemClicked(v, bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

}

class DiffCallbackChats : DiffUtil.ItemCallback<ChatUserWithMessages>() {
    override fun areItemsTheSame(oldItem: ChatUserWithMessages, newItem: ChatUserWithMessages): Boolean {
        return oldItem.user.id == newItem.user.id
    }

    override fun areContentsTheSame(oldItem: ChatUserWithMessages, newItem: ChatUserWithMessages): Boolean {
        return oldItem.messages == newItem.messages && oldItem.user == newItem.user
    }
}