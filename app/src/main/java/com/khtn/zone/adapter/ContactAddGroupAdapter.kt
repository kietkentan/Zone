package com.khtn.zone.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.databinding.ItemContactAddGroupBinding
import com.khtn.zone.utils.printMeD
import com.khtn.zone.viewmodel.CreateGroupChatViewModel
import java.util.Locale

class ContactAddGroupAdapter(
    val context: Context,
    val viewModel: CreateGroupChatViewModel
): ListAdapter<Pair<ChatUser, Boolean>, RecyclerView.ViewHolder>(DiffCallbackContactAddGroup()) {
    companion object {
        var allContacts: MutableList<Pair<ChatUser, Boolean>> = mutableListOf()
    }

    class MyViewHolder(private val binding: ItemContactAddGroupBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, viewModel: CreateGroupChatViewModel, item: Pair<ChatUser, Boolean>) {
            binding.user = item.first
            if (binding.cbContactCreateGroup.isChecked != item.second)
                binding.cbContactCreateGroup.isChecked = item.second
            binding.setClickListener {
                val check = binding.cbContactCreateGroup.isChecked
                binding.cbContactCreateGroup.isChecked = !check
            }
            binding.viewmodel = viewModel
            binding.executePendingBindings()
        }
    }

    fun filter(query: String) {
        val list = ArrayList<Pair<ChatUser, Boolean>>()
        if (query.isEmpty())
            list.addAll(allContacts)
        else {
            val queryList = allContacts.filter {
                it.first.localName.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            }
            list.addAll(queryList)
        }
        submitList(list as MutableList<Pair<ChatUser, Boolean>>)
    }

    fun onRemove(user: ChatUser) {
        val pair = Pair(user, false)
        "OnReMove: $user".printMeD()
        for (i in 0..allContacts.size) {
            if (user == allContacts[i].first) {
                val list = currentList.toMutableList()
                val pos = list.indexOf((Pair(user, true)))
                "Position: $pos".printMeD()
                if (pos != -1) {
                    list.removeAt(pos)
                    list.add(pos, pair)
                    submitList(list)
                }

                allContacts.removeAt(i)
                allContacts.add(i, pair)
                return
            }
        }
    }

    fun onChangeList(item: Pair<ChatUser, Boolean>) {
        for (i in 0..allContacts.size) {
            if (allContacts[i].first == item.first) {
                allContacts.removeAt(i)
                allContacts.add(i, item)
                break
            }
        }

        val list = currentList.toMutableList()
        for (i in 0..list.size) {
            if (list[i].first == item.first) {
                list.removeAt(i)
                list.add(i, item)

                submitList(list)
                break
            }
        }
    }

    override fun submitList(list: List<Pair<ChatUser, Boolean>>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactAddGroupBinding.inflate(layoutInflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as MyViewHolder
        holder.bind(context, viewModel, getItem(position))
    }
}

class DiffCallbackContactAddGroup: DiffUtil.ItemCallback<Pair<ChatUser, Boolean>>() {
    override fun areItemsTheSame(
        oldItem: Pair<ChatUser, Boolean>,
        newItem: Pair<ChatUser, Boolean>
    ): Boolean {
        return oldItem.first.id == newItem.first.id && oldItem.second == newItem.second
    }

    override fun areContentsTheSame(
        oldItem: Pair<ChatUser, Boolean>,
        newItem: Pair<ChatUser, Boolean>
    ): Boolean {
        return oldItem.first.isSelected == newItem.first.isSelected &&
                oldItem.first.locallySaved == newItem.first.locallySaved &&
                oldItem.first.unRead == newItem.first.unRead &&
                oldItem.first.localName == newItem.first.localName &&
                oldItem.second == newItem.second
    }
}