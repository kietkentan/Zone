package com.khtn.zone.fragment

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.adapter.SingleChatHomeAdapter
import com.khtn.zone.core.ChatHandler
import com.khtn.zone.core.ChatUserProfileListener
import com.khtn.zone.core.GroupChatHandler
import com.khtn.zone.database.dao.ChatUserDao
import com.khtn.zone.database.dao.MessageDao
import com.khtn.zone.database.data.ChatUserWithMessages
import com.khtn.zone.databinding.FragmentSingleChatHomeBinding
import com.khtn.zone.model.Sticker
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.ScreenState
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.isValidDestination
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.toast
import com.khtn.zone.viewmodel.SharedViewModel
import com.khtn.zone.viewmodel.SingleChatHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    @Inject
    lateinit var chatHandler: ChatHandler

    @Inject
    lateinit var groupChatHandler: GroupChatHandler

    @Inject
    lateinit var chatUsersListener: ChatUserProfileListener

    private var chatList = mutableListOf<ChatUserWithMessages>()
    private lateinit var activity: Activity
    private lateinit var profile: UserProfile

    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private val viewModel: SingleChatHomeViewModel by viewModels()

    private val homeChatAdapter: SingleChatHomeAdapter by lazy {
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

        activity = requireActivity()
        binding.lifecycleOwner = viewLifecycleOwner
        chatHandler.initHandler()
        groupChatHandler.initHandler()
        chatUsersListener.initListener()
        profile = preference.getUserProfile()!!

        initView()
        observer()
    }

    private fun initView() {
        binding.recSingleChatHome.itemAnimator = null
        binding.recSingleChatHome.adapter = homeChatAdapter
        binding.recSingleChatHome.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.headerSingleChatHome.setRightImageUrl(profile.image)
        SingleChatHomeAdapter.itemClickListener = this
    }

    private fun observer() {
        lifecycleScope.launch {
            viewModel.getChatUsers().collect { list ->
                updateList(list)
            }
        }

        sharedViewModel.state.observe(viewLifecycleOwner) { state ->
            "$state".printMeD()
            if (state is ScreenState.IdleState) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateList(viewModel.getChatUsersAsList())
                }
            }
        }
        sharedViewModel.lastQuery.observe(viewLifecycleOwner) {
            if (sharedViewModel.state.value is ScreenState.SearchState)
                homeChatAdapter.filter(it)
        }
    }

    private suspend fun updateList(list: List<ChatUserWithMessages>) {
        withContext(Dispatchers.Main) {
            val filteredList = list.filter { it.messages.isNotEmpty() }
            if (filteredList.isNotEmpty()) {
                //binding.imageEmpty.gone()
                chatList = filteredList as MutableList<ChatUserWithMessages>
                // sort by recent message
                chatList = filteredList.sortedByDescending { it.messages.last().createdAt }
                    .toMutableList()
                SingleChatHomeAdapter.allChatList = chatList
                homeChatAdapter.submitList(chatList)
                if (sharedViewModel.state.value is ScreenState.SearchState)
                    homeChatAdapter.filter(sharedViewModel.lastQuery.value.toString())
            } else {
            }
                //binding.imageEmpty.show()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Utils.isPermissionOk(*grantResults)){
            if (findNavController().isValidDestination(R.id.singleChatHomeFragment)) {
                //findNavController().navigate(R.id.action_FSingleChatHome_to_FContacts)
            }
        }
        else
            activity.toast("Permission is needed!")
    }

    override fun onItemClicked(v: View, position: Int) {
        sharedViewModel.setState(ScreenState.IdleState)
        val chatUser = homeChatAdapter.currentList[position]
        preference.setCurrentUser(chatUser.user.id)
        val action = SingleChatHomeFragmentDirections.actionSingleChatHomeFragmentToSingleChatFragment(chatUser.user)
        "Chat User: ${chatUser.user}".printMeD()
        findNavController().navigate(action)
    }

    override fun onItemLongClicked(v: View, position: Int) {

    }

    override fun onStickerClicked(sticker: Sticker) {

    }
}