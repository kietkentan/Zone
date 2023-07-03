package com.khtn.zone.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.adapter.ContactAdapter
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.databinding.FragmentContactBinding
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.ContactUtils
import com.khtn.zone.utils.ScreenState
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UiState
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.Utils.isPermissionOk
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.toast
import com.khtn.zone.viewmodel.ContactViewModel
import com.khtn.zone.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ContactFragment: Fragment() {
    @Inject
    lateinit var preference: SharedPreferencesManager

    private lateinit var binding: FragmentContactBinding
    private val sharedViewModel by activityViewModels<SharedViewModel>()
    private val viewModel: ContactViewModel by viewModels()

    private val contactAdapter: ContactAdapter by lazy {
        ContactAdapter(
            onItemClick = { user, mode ->
                "Click User: ${user.uId} || Mode: $mode".printMeD()
                when (mode) {
                    ContactAdapter.MODE_ITEM -> onItemClick(user)

                    ContactAdapter.MODE_CALL -> onCallClick(user)

                    ContactAdapter.MODE_VIDEO -> onVideoClick(user)
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ContactUtils.askContactPermission(
            context = this,
            actionIsPermission = { viewModel.getContacts() }
        )
        observer()

        val linearLayoutManager = LinearLayoutManager(context)
        binding.recContact.apply {
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            layoutManager = linearLayoutManager
            itemAnimator = null
            adapter = contactAdapter
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ContactUtils.REQUEST_CONTACT_PERMISSION) {
            val isPermissionOk = isPermissionOk(*grantResults)

            if (isPermissionOk) viewModel.getContacts()
            else {
                toast(getString(R.string.permission_error))
                findNavController().popBackStack()
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Utils.REQUEST_APP_SETTINGS) {
            val isPermissionContactOk = isPermissionOk(
                context = requireContext(),
                permissions = ContactUtils.CONTACT_PERMISSION
            )

            if (isPermissionContactOk) viewModel.getContacts()
            else findNavController().popBackStack()
        }
    }

    private fun observer() {
        viewModel.listContact.observe(viewLifecycleOwner) {
            if (it != null && it.isNotEmpty())
                viewModel.checkContact()
        }

        viewModel.listUser.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    contactAdapter.updateListUser(state.data)
                }

                is UiState.Failure -> {
                    "Failure".printMeD()
                }

                else -> {}
            }
        }
    }

    private fun onItemClick(user: UserProfile) {
        sharedViewModel.setState(ScreenState.IdleState)
        preference.setCurrentUser(user.uId!!)
        val action =
            ContactFragmentDirections.actionContactFragmentToSingleChatFragment(
                ChatUser(
                    id = user.uId!!,
                    localName = user.userName,
                    user = user,
                    locallySaved = true,
                    isSearchedUser = false
                )
            )
        findNavController().navigate(action)
    }

    private fun onVideoClick(user: UserProfile) {

    }

    private fun onCallClick(user: UserProfile) {

    }
}