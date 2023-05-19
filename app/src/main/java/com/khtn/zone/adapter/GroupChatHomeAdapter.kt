package com.khtn.zone.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.databinding.RowGroupChatBinding
import com.khtn.zone.database.data.GroupWithMessages
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.listener.ItemClickListener
import java.util.*
import javax.inject.Inject

class GroupChatHomeAdapter(private val context: Context) :
    ListAdapter<GroupWithMessages, RecyclerView.ViewHolder>(DiffCallbackGroupChats()) {

    @Inject
    lateinit var preference: SharedPreferencesManager

    companion object {
        lateinit var allList: MutableList<GroupWithMessages>
        lateinit var itemClickListener: ItemClickListener
    }

    fun filter(query: String) {
        try {
            val list = mutableListOf<GroupWithMessages>()
            if (query.isEmpty())
                list.addAll(allList)
            else {
                for (group in allList) {
                    if (group.group.id.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))
                    ) {
                        list.add(group)
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
        val binding = RowGroupChatBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val viewHolder = holder as ViewHolder
        viewHolder.bind(getItem(position))
    }

    class ViewHolder(private val binding: RowGroupChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupWithMessages) {
            binding.groupChat = item
            binding.viewRoot.setOnClickListener { v ->
                itemClickListener.onItemClicked(v, bindingAdapterPosition)
            }
            binding.executePendingBindings()
        }
    }

}

class DiffCallbackGroupChats: DiffUtil.ItemCallback<GroupWithMessages>() {
    override fun areItemsTheSame(oldItem: GroupWithMessages, newItem: GroupWithMessages): Boolean {
        return oldItem.group.id == oldItem.group.id
    }

    override fun areContentsTheSame(
        oldItem: GroupWithMessages,
        newItem: GroupWithMessages
    ): Boolean {
        return oldItem.messages == newItem.messages && oldItem.group == newItem.group
    }
}
