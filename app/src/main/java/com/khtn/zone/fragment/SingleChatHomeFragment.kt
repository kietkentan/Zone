package com.khtn.zone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.khtn.zone.adapter.SingleChatHomeAdapter
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.databinding.FragmentSingleChatHomeBinding
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.listener.ItemClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SingleChatHomeFragment : Fragment(), ItemClickListener {
    private lateinit var binding: FragmentSingleChatHomeBinding

    @Inject
    lateinit var preference: SharedPreferencesManager

    @Inject
    lateinit var chatUserDao: ChatUserDao

    @Inject
    lateinit var messageDao: MessageDao

    private val adChat: SingleChatHomeAdapter by lazy {
        SingleChatHomeAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSingleChatHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onItemClicked(v: View, position: Int) {
        //sharedViewModel.setState(ScreenState.IdleState)
        val chatUser = adChat.currentList[position]
        preference.setCurrentUser(chatUser.user.id)
        val action = SingleChatHomeFragmentDirections.actionSingleChatHomeFragmentToSingleChatFragment(chatUser.user)
        findNavController().navigate(action)
    }
}