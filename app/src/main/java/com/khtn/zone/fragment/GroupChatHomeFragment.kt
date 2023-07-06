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
import com.khtn.zone.adapter.GroupChatHomeAdapter
import com.khtn.zone.database.data.GroupWithMessages
import com.khtn.zone.databinding.FragmentGroupChatHomeBinding
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.ScreenState
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.addRestorePolicy
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.viewmodel.GroupChatHomeViewModel
import com.khtn.zone.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class GroupChatHomeFragment: Fragment(), ItemClickListener {
    private lateinit var binding: FragmentGroupChatHomeBinding
    private val viewModel: GroupChatHomeViewModel by viewModels()
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private lateinit var activity: Activity
    private val groups = mutableListOf<GroupWithMessages>()
    private lateinit var profile: UserProfile

    @Inject
    lateinit var preference: SharedPreferencesManager

    private val homeChatAdapter by lazy {
        GroupChatHomeAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupChatHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        activity = requireActivity()
        binding.lifecycleOwner = viewLifecycleOwner
        profile = preference.getUserProfile()!!

        initView()
        observer()
    }

    private fun observer() {
        lifecycleScope.launch {
            viewModel.getGroupMessages().collect { groupWithmsgs ->
                updateList(groupWithmsgs)
            }
        }

        sharedViewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is ScreenState.IdleState) {
                CoroutineScope(Dispatchers.IO).launch {
                    updateList(viewModel.getGroupMessagesAsList())
                }
            }
        }

        sharedViewModel.lastQuery.observe(viewLifecycleOwner) {
            if (sharedViewModel.state.value is ScreenState.SearchState)
                homeChatAdapter.filter(it)
        }
    }

    private fun initView() {
        binding.recGroupChatHome.adapter = homeChatAdapter
        binding.recGroupChatHome.itemAnimator = null
        GroupChatHomeAdapter.itemClickListener = this
        binding.headerGroupChatHome.setRightImageUrl(profile.image)
        homeChatAdapter.addRestorePolicy()
    }

    private suspend fun updateList(list: List<GroupWithMessages>) {
        withContext(Dispatchers.Main) {
            if (list.isNotEmpty()) {
                val list1 = list.filter { it.messages.isEmpty() }
                    .sortedByDescending { it.group.createdAt }.toMutableList()
                val groupHasMsgsList = list.filter { it.messages.isNotEmpty() }
                    .sortedBy { it.messages.last().createdAt }

                for (a in groupHasMsgsList)
                    list1.add(0, a)

                homeChatAdapter.submitList(list1)
                GroupChatHomeAdapter.allList = list1
                groups.clear()
                groups.addAll(list1)
                if (sharedViewModel.state.value is ScreenState.SearchState)
                    homeChatAdapter.filter(sharedViewModel.lastQuery.value.toString())
            } else {
            }
            //binding.imageEmpty.show()
        }
    }

    override fun onItemClicked(v: View, position: Int) {
        sharedViewModel.setState(ScreenState.IdleState)
        val group = homeChatAdapter.currentList[position].group
        preference.setCurrentGroup(group.id)
        val action =
            GroupChatHomeFragmentDirections.actionGroupChatHomeFragmentToGroupChatFragment(group)
        findNavController().navigate(action)
    }

    override fun onItemLongClicked(v: View, position: Int) {

    }
}