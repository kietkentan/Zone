package com.khtn.zone.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImage
import com.khtn.zone.R
import com.khtn.zone.adapter.ContactAddGroupAdapter
import com.khtn.zone.adapter.ContactMemberAdapter
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.Group
import com.khtn.zone.databinding.FragmentCreateGroupChatBinding
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UiState
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.addRestorePolicy
import com.khtn.zone.utils.closeKeyBoard
import com.khtn.zone.utils.forEachChildView
import com.khtn.zone.utils.hideAnimation
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.isValidDestination
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showAnimation
import com.khtn.zone.utils.showSoftKeyboard
import com.khtn.zone.utils.showView
import com.khtn.zone.utils.toast
import com.khtn.zone.viewmodel.CreateGroupChatViewModel
import com.khtn.zone.viewmodel.ItemRemoveListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CreateGroupChatFragment : Fragment(), ItemRemoveListener {
    private lateinit var binding: FragmentCreateGroupChatBinding
    private val viewModel: CreateGroupChatViewModel by viewModels()
    private val contactList = ArrayList<ChatUser>()

    @Inject
    lateinit var preference: SharedPreferencesManager

    private var animSuccess = true
    private var heightNameGroup: Float = 0F
    private var lastQuery: String = ""

    private val adContact: ContactAddGroupAdapter by lazy {
        ContactAddGroupAdapter(requireContext(), viewModel)
    }

    private val adMember: ContactMemberAdapter by lazy {
        ContactMemberAdapter(requireContext(), viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateGroupChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.setListener(this)
        binding.viewmodel = viewModel

        observer()
        initView()
        clickView()
    }

    override fun onResume() {
        super.onResume()

/*        val chipList = AdChip.allAddedContacts
        val allUsers = ContactAddGroupAdapter.allContacts
        for (user in allUsers)
            user.isSelected = chipList.contains(user)
        viewModel.setContactList(allUsers)*/
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Utils.PERMISSION_REQ_CODE -> ImageUtils.onImagePerResult(this, *grantResults)
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> onCropResult(data)

            ImageUtils.TAKE_PHOTO, ImageUtils.FROM_GALLERY -> ImageUtils.cropImage(requireActivity(), data, true)

            Utils.REQUEST_APP_SETTINGS -> {
                val isPermissionImageCamera = Utils.isPermissionOk(
                    context = requireContext(),
                    permissions = ImageUtils.IMAGE_CAMERA_PERMISSION
                )

                if (isPermissionImageCamera) ImageUtils.showCameraOptions(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().closeKeyBoard()
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            imagePath?.let {
                viewModel.setImageUri(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observer() {
        viewModel.stateGroupCreate.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Success -> {
                    viewModel.setProgressCreate(false)
                    if (findNavController().isValidDestination(R.id.createGroupChatFragment)) {
                        val group = it.data as Group
                        preference.setCurrentGroup(group.id)
                        val action = CreateGroupChatFragmentDirections.actionCreateGroupChatFragmentToGroupChatFragment(group)
                        findNavController().navigate(action)
                    }
                }

                is UiState.Failure -> {
                    viewModel.setProgressCreate(false)
                }

                is UiState.Loading -> {
                    viewModel.setProgressCreate(true)
                }
            }
        }

        viewModel.getChatList().observe(viewLifecycleOwner) { contacts ->
            val allContacts = contacts.filter { it.locallySaved }
            if (allContacts.isNotEmpty()) {
                if (viewModel.isFirstCall.value == true) {
                    viewModel.setContactList(allContacts)
                    viewModel.setIsFirstCall(false)
                }
                Timber.v("allContacts ->${viewModel.getContactList()}")

                contactList.clear()
                viewModel.getContactList()?.let { contactList.addAll(it) }
                val pair: MutableList<Pair<ChatUser, Boolean>> = mutableListOf()
                for (contact in contactList)
                    pair.add(Pair(contact, false))

                ContactAddGroupAdapter.allContacts = pair
                viewModel.setFilterContact(pair)
                adContact.filter(lastQuery)
            }
        }

        viewModel.filterContacts.observe(viewLifecycleOwner) {
            adContact.submitList(it)
        }

        viewModel.itemChange.observe(viewLifecycleOwner) {
            adContact.onChangeList(it)
        }

        viewModel.memberList.observe(viewLifecycleOwner) { addedList ->
            /*AdChip.allAddedContacts = addedList
            adChip.submitList(addedList.toList())
            adChip.notifyDataSetChanged()
            if (addedList.isEmpty()) {
                binding.fab.hide()
            } else {
                binding.listChip.post {
                    binding.listChip.smoothScrollToPosition(addedList.lastIndex)
                }
            }*/
        }

        viewModel.queryState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Success -> {
                    val emptyList = it.data as ArrayList<*>
                    /*if (emptyList.isEmpty()) {
                        binding.viewEmpty.show()
                        binding.progress.hide()
                        binding.viewEmpty.playAnimation()
                    } else {
                        binding.viewHolder.show()
                        binding.progress.hide()
                    }*/
                }

                is UiState.Failure -> {
                    /*binding.viewHolder.hide()
                    binding.progress.hide()
                    binding.viewEmpty.playAnimation()*/
                }

                is UiState.Loading -> {
                    /*binding.viewEmpty.hide()
                    binding.viewHolder.hide()
                    binding.progress.show()*/
                }
            }
        }

        viewModel.memberList.observe(viewLifecycleOwner) {
            adMember.submitList(it)
            if (binding.layoutNameGroup.visibility == View.VISIBLE) {
                binding.toolbar.subtitle = String.format(getString(R.string.selected), viewModel.getMemberCount())
            }
        }

        viewModel.imageUrl.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                viewModel.createGroup()
            }
        }
    }

    private fun initView() {
        binding.recContractSuggest.adapter = adContact
        binding.recContactSelected.adapter = adMember
        adContact.addRestorePolicy()
        adMember.addRestorePolicy()
    }

    private fun clickView() {
        binding.edtEnterGroupName.setOnFocusChangeListener { _, isFocus ->
            if (isFocus) binding.ivCreateGroup.showView()
            else binding.ivCreateGroup.hideView()
        }

        binding.layoutNameGroup.apply {
            val vto: ViewTreeObserver = this.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (this@apply.measuredHeight > 0) {
                        vto.removeOnGlobalLayoutListener(this)
                        heightNameGroup = this@apply.measuredHeight.toFloat()
                    }
                }
            })
        }

        binding.recContractSuggest.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                binding.layoutNameGroup.apply {
                    if (dy > 0 && this@apply.visibility != View.VISIBLE) {
                        this@apply.showAnimation(heightNameGroup)
                        animSuccess = true
                        binding.toolbar.apply {
                            title = getString(R.string.create_new_group)
                            subtitle = String.format(getString(R.string.selected), viewModel.getMemberCount())
                        }
                    } else if (dy < 0 && animSuccess) {
                        animSuccess = false
                        this@apply.hideAnimation(heightNameGroup)
                        val name = viewModel.getNameGroup()

                        binding.toolbar.apply {
                            title = name.ifBlank { getString(R.string.create_new_group) }
                            subtitle =
                                if (name.isBlank()) getString(R.string.click_to_add_the_name)
                                else String.format(getString(R.string.selected), viewModel.getMemberCount())
                        }
                    }
                }
            }
        })

        binding.toolbar.setOnClickListener {
            if (binding.layoutNameGroup.visibility == View.GONE) {
                requireActivity().showSoftKeyboard(binding.edtEnterGroupName)
                binding.layoutNameGroup.showView()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivGroup.setOnClickListener {
            ImageUtils.askImageCameraPermission(this)
        }

        binding.ivGroupImage.setOnClickListener {
            ImageUtils.askImageCameraPermission(this)
        }

        binding.ivCreateGroup.setOnClickListener {
            validate()
        }

        binding.searchContact.onTextChangeListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                val newQuery = binding.searchContact.binding.edtTextInput.text.toString()
                if (lastQuery != newQuery) {
                    lastQuery = newQuery
                    adContact.filter(lastQuery)
                }
            }
        })
    }

    private fun validate() {
        val groupName = viewModel.groupName.value.toString().trim()
        if (groupName.isNotEmpty() &&
            viewModel.memberList.value?.size!! > 1
        ) {
            this.view?.forEachChildView { it.isEnabled = false }
            if (viewModel.imageUri.value == null || viewModel.imageUri.value.toString() == "")
                viewModel.createGroup()
            else viewModel.uploadProfileImage()
        }
        else toast(getString(R.string.invalid_group_name))
    }

    override fun itemRemove(user: ChatUser) {
        adContact.onRemove(user)
    }
}