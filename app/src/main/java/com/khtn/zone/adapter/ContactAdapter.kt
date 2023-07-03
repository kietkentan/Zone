package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.databinding.RowContactBinding
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.showView

class ContactAdapter(val onItemClick: (user: UserProfile, mode: Int) -> Unit): RecyclerView.Adapter<ContactAdapter.MyViewHolder>() {
    companion object {
        const val MODE_ITEM = 1
        const val MODE_CALL = 2
        const val MODE_VIDEO = 3

        var listContact: List<UserProfile> = listOf()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = RowContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return listContact.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = listContact[position]
        holder.bind(item)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateListUser(list: List<UserProfile>) {
        listContact = list
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: RowContactBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(userProfile: UserProfile) {
            binding.user = userProfile
            if (
                bindingAdapterPosition == 0 ||
                userProfile.userName[0].uppercase() != listContact[bindingAdapterPosition - 1].userName[0].uppercase()
            )
                binding.tvAlphabet.showView()
            else binding.tvAlphabet.hideView()

            binding.viewRoot.setOnClickListener {
                onItemClick(userProfile, MODE_ITEM)
            }

            binding.ivPhoneCall.setOnClickListener {
                onItemClick(userProfile, MODE_CALL)
            }

            binding.ivCallVideo.setOnClickListener {
                onItemClick(userProfile, MODE_VIDEO)
            }
        }
    }
}