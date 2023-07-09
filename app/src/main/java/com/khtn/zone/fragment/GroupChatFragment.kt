package com.khtn.zone.fragment

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImage
import com.khtn.zone.MyApplication
import com.khtn.zone.R
import com.khtn.zone.adapter.AttachmentAdapter
import com.khtn.zone.adapter.GroupChatAdapter
import com.khtn.zone.custom.textField.CustomEditText
import com.khtn.zone.database.dao.GroupDao
import com.khtn.zone.database.data.AudioMessage
import com.khtn.zone.database.data.Group
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.database.data.ImageMessage
import com.khtn.zone.database.data.TextMessage
import com.khtn.zone.databinding.FragmentGroupChatBinding
import com.khtn.zone.model.MyImage
import com.khtn.zone.model.Sticker
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.AttachmentOptions
import com.khtn.zone.utils.BindingAdapters
import com.khtn.zone.utils.ImageTypeConstants
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.KeyBoardListener
import com.khtn.zone.utils.MessageTypeConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.addRestorePolicy
import com.khtn.zone.utils.closeKeyBoard
import com.khtn.zone.utils.event.EventAudioMessage
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.utils.listener.StickerListener
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showAnimation
import com.khtn.zone.utils.showSoftKeyboard
import com.khtn.zone.utils.showView
import com.khtn.zone.utils.smoothScrollToPos
import com.khtn.zone.utils.toast
import com.khtn.zone.utils.trim
import com.khtn.zone.utils.value
import com.khtn.zone.viewmodel.GroupChatViewModel
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.io.IOException
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class GroupChatFragment: Fragment(), ItemClickListener, StickerListener, CustomEditText.KeyBoardInputCallbackListener {
    @Inject
    lateinit var groupDao: GroupDao

    @Inject
    lateinit var preference: SharedPreferencesManager

    private val viewModel: GroupChatViewModel by viewModels()
    private lateinit var binding: FragmentGroupChatBinding
    private val args by navArgs<GroupChatFragmentArgs>()
    lateinit var group: Group
    private var messageList = mutableListOf<GroupMessage>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var localUserId: String
    private lateinit var fromUser: UserProfile
    private var lastAudioFile = ""
    private var msgPostponed = false

    private var lastActionCallPermission: Int = DEFAULT
    var isRecording = false //whether is recoding now or not
    var bottomShow = DEFAULT

    private var recordStart = 0L
    private var recorder: MediaRecorder? = null
    private val REQ_AUDIO_PERMISSION = 29
    private var recordDuration = 0L

    private val adChat: GroupChatAdapter by lazy {
        GroupChatAdapter(requireContext(), this)
    }

    private val attachmentAdapter: AttachmentAdapter by lazy {
        AttachmentAdapter(
            onItemClick = { position ->
                onAttachmentOptions(position)
            }
        )
    }

    companion object{
        private const val REQUEST_ADD_CONTACT = 22
        private const val REQUEST_AUDIO_PERMISSION = 29
        private const val ATTACHMENT = 111
        private const val STICKER = 112
        private const val DEFAULT = -1

        private const val SETTING_CONTACT = 1001
        private const val SETTING_RECORD = 1002
        private const val SETTING_IMAGE_FROM_GALLERY = 1003

        private val RECORD_AUDIO_PERMISSION = arrayOf(Manifest.permission.RECORD_AUDIO)
        private val heightBottomLayout = MyApplication.getMaxHeight()*0.4F
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewmodel = viewModel
        group = args.group!!
        binding.group = group

        binding.viewChatBottomInput.edtMessage.setKeyBoardInputCallbackListener(this)
        UserUtils.setUnReadCountGroup(groupDao, group)

        initView()
        clickView()
        observer()

        lifecycleScope.launch {
            viewModel.getGroupMessages(group.id).collect { message ->
                if (message.isEmpty())
                    return@collect

                messageList = message as MutableList<GroupMessage>

                if (GroupChatAdapter.isPlaying()) {
                    msgPostponed = true
                    return@collect
                }

                GroupChatAdapter.messageList = messageList
                adChat.submitList(messageList)

                Timber.v("Message list ${messageList.last()}")

                //scroll to last items in recycler (recent messages)
                if (messageList.isNotEmpty()) {
                    if (viewModel.getCanScroll())  //scroll only if new message arrived
                        binding.recMessage.smoothScrollToPos(messageList.lastIndex)
                    else viewModel.canScroll(true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        GroupChatAdapter.stopPlaying()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        initResume()
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        preference.clearCurrentGroup()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().closeKeyBoard()
        stopRecording()
        GroupChatAdapter.stopPlaying()
        EventBus.getDefault().unregister(this)
    }

    private fun clickView() {
        binding.viewChatBottomInput.lottieSend.setOnClickListener {
            sendMessage()
        }

        binding.viewChatHeader.ivBack.setOnClickListener {
            findNavController().navigate(R.id.action_groupChatFragment_to_groupChatHomeFragment)
        }

        binding.viewChatBottomInput.ivRecord.setOnClickListener {
            GroupChatAdapter.stopPlaying()
            if (Utils.checkPermission(
                    context = this,
                    Manifest.permission.RECORD_AUDIO,
                    reqCode = REQ_AUDIO_PERMISSION
                )) {
                startRecording()
            }
        }

        binding.lottieVoice.setOnClickListener {
            if (isRecording) {
                stopRecording()

                val duration = (recordDuration / 1000).toInt()
                if (duration <= 1) {
                    toast(getString(R.string.nothing_recorded))
                    return@setOnClickListener
                }
                val msg = createMessage().apply {
                    type = MessageTypeConstants.AUDIO
                    audioMessage = AudioMessage(lastAudioFile, duration)
                }
                viewModel.uploadToCloud(msg, lastAudioFile)
            }
        }

        binding.viewChatBottomInput.ivAddIcon.setOnClickListener {
            handleBottomShowing(option = STICKER)
        }

        binding.viewChatBottomInput.ivAddSomeFile.setOnClickListener {
            handleBottomShowing(option = ATTACHMENT)
        }
    }

    private fun handleGetAllSticker() {
        if (!viewModel.listSetSticker.value.isNullOrEmpty() &&
            !viewModel.listAllSticker.value.isNullOrEmpty()) {
            viewModel.getAllSticker(viewModel.listAllSticker.value!!, viewModel.listSetSticker.value!!)
        }
    }

    private fun sendMessage() {
        val msg = binding.viewChatBottomInput.edtMessage.text?.trim().toString()
        if (msg.isEmpty())
            return
        binding.viewChatBottomInput.lottieSend.playAnimation()
        val messageData = TextMessage(msg)
        val message = createMessage()
        message.textMessage = messageData
        viewModel.sendMessage(message)
        binding.viewChatBottomInput.edtMessage.setText("")
    }

    private fun sendSticker(sticker: Sticker) {
        val message = createMessage().apply {
            type = MessageTypeConstants.IMAGE
            imageMessage = ImageMessage(
                uri = sticker.url,
                imageType = ImageTypeConstants.STICKER,
                sticker = sticker
            )
        }
        viewModel.sendMessage(message)
    }

    private fun createMessage(): GroupMessage {
        val toUsers = group.members?.map { it.id } as ArrayList
        val groupSize = group.members!!.size
        val statusList = ArrayList<Int>()
        val deliveryTimeList = ArrayList<Long>()
        for (index in 0 until groupSize) {
            statusList.add(0)
            deliveryTimeList.add(0L)
        }
        return GroupMessage(
            System.currentTimeMillis(), group.id, from = localUserId,
            to = toUsers, fromUser.userName, fromUser.image, statusList, deliveryTimeList,
            deliveryTimeList
        )
    }

    private fun onAttachmentOptions(position: Int) {
        when (position) {
            AttachmentOptions.IMAGE_VIDEO -> {
                lastActionCallPermission = SETTING_IMAGE_FROM_GALLERY
                ImageUtils.askImageCameraPermission(this)
            }

            AttachmentOptions.QUICK_MESSAGE -> {}

            AttachmentOptions.FILE -> {}

            else -> {}
        }
    }

    private fun observer() {
        viewModel.getChatUsers().observe(viewLifecycleOwner) { chatUsers ->
            GroupChatAdapter.chatUserList = chatUsers.toMutableList()
        }

        viewModel.typingUsers.observe(viewLifecycleOwner) { typingUser ->
            if (typingUser.isEmpty())
                BindingAdapters.setMemberNames(binding.viewChatHeader.tvMember, group)
            else binding.viewChatHeader.tvMember.text = typingUser
        }

        viewModel.listAllSticker.observe(viewLifecycleOwner) {
            handleGetAllSticker()
        }

        viewModel.listSetSticker.observe(viewLifecycleOwner) {
            handleGetAllSticker()
        }

        viewModel.allSticker.observe(viewLifecycleOwner) {
            "ListS: $it".printMeD()
            if (it.isNotEmpty()) {
                binding.viewSticker.updateListItem(it)
            }
        }
    }

    private fun initView() {
        try {
            fromUser = preference.getUserProfile()!!
            localUserId = fromUser.uId!!
            linearLayoutManager = LinearLayoutManager(context)
            gridLayoutManager = GridLayoutManager(context, 4)

            fromUser.listSticker.let {
                viewModel.getSetSticker(it)
                viewModel.getAllListSticker(it)
            }

            binding.recMessage.apply {
                linearLayoutManager.stackFromEnd = true
                layoutManager = linearLayoutManager
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
                itemAnimator = null
                adapter = adChat
            }

            binding.viewAttachment.recAttachmentOption.apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                layoutManager = gridLayoutManager
                itemAnimator = null
                adapter = attachmentAdapter
            }

            binding.viewSticker.apply {
                updateListener(this@GroupChatFragment)
            }

            adChat.addRestorePolicy()

            viewModel.setGroup(group)
            binding.viewChatBottomInput.edtMessage.addTextChangedListener(msgTxtChangeListener)
            binding.viewChatBottomInput.lottieSend.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    if (binding.viewChatBottomInput.edtMessage.value.isEmpty()) {
                        binding.viewChatBottomInput.ivRecord.showView()
                        binding.viewChatBottomInput.ivAddSomeFile.showView()
                        binding.viewChatBottomInput.lottieSend.hideView()
                    }
                }

                override fun onAnimationCancel(p0: Animator) {}
                override fun onAnimationRepeat(p0: Animator) {}
            })

            binding.lottieVoice.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}

                override fun onAnimationEnd(p0: Animator) {
                    binding.viewChatBottomInput.ivRecord.showView()
                    binding.lottieVoice.hideView()
                }

                override fun onAnimationCancel(p0: Animator) {}
                override fun onAnimationRepeat(p0: Animator) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val msgTxtChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.sendTyping(binding.viewChatBottomInput.edtMessage.trim())
            if (binding.viewChatBottomInput.lottieSend.isAnimating)
                return

            if(s.isNullOrBlank()) {
                binding.viewChatBottomInput.ivRecord.showView()
                binding.viewChatBottomInput.ivAddSomeFile.showView()
                binding.viewChatBottomInput.lottieSend.hideView()
            } else {
                binding.viewChatBottomInput.lottieSend.showView()
                binding.viewChatBottomInput.ivAddSomeFile.hideView()
                binding.viewChatBottomInput.ivRecord.hideView()
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    override fun onItemClicked(v: View, position: Int) {
        val message = messageList[position]

        when (message.type) {
            MessageTypeConstants.IMAGE -> {
                when (message.imageMessage!!.imageType) {
                    ImageTypeConstants.IMAGE -> {
                        binding.ivFullScreen.showView()
                        StfalconImageViewer.Builder(
                            context,
                            listOf(MyImage(messageList[position].imageMessage?.uri!!))
                        ) { imageView, myImage ->
                            ImageUtils.loadGalleryImage(myImage.url, imageView)
                        }
                            .withDismissListener { binding.ivFullScreen.visibility = View.GONE }
                            .show()
                    }

                    ImageTypeConstants.GIF -> {

                    }

                    ImageTypeConstants.STICKER -> {

                    }
                }
            }

            MessageTypeConstants.TEXT -> adChat.updatePositionClicked(position)

            MessageTypeConstants.VIDEO -> {

            }

            MessageTypeConstants.FILE -> {

            }
        }
    }

    override fun onItemLongClicked(v: View, position: Int) {

    }

    private fun initResume() {
        preference.setCurrentGroup(group.id)
        viewModel.setSeenAllMessage()
        viewModel.sendCachedTxtMessages()
        Utils.removeNotification(requireContext())

        KeyBoardListener(activity as AppCompatActivity) { isOpen ->
            if (isOpen) {
                closeAllBottomLayout()
            } else {
                when (bottomShow) {
                    ATTACHMENT -> showAttachment()

                    STICKER -> showSticker()

                    else -> {}
                }
            }
        }

        binding.recMessage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) checkBottomLayout()
            }
        })

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        bottomShow != DEFAULT -> closeAllBottomLayout()

                        isRecording -> stopRecording()

                        isEnabled -> {
                            isEnabled = false
                            findNavController().navigate(R.id.action_groupChatFragment_to_groupChatHomeFragment)
                        }
                    }
                }
            })
    }

    override fun onCommitContent(
        inputContentInfo: InputContentInfoCompat?,
        flags: Int,
        opts: Bundle?
    ) {
        val imageMsg = createMessage()
        val image = ImageMessage("${inputContentInfo?.contentUri}")
        image.imageType = if (image.uri.toString().endsWith(".png")) "sticker" else "gif"
        imageMsg.apply {
            type = "image"
            imageMessage = image
        }
        viewModel.uploadToCloud(imageMsg, image.toString())
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        "ReCode: $requestCode\nData: ${data?.data}".printMeD()
        when (requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> onCropResult(data)

            ImageUtils.TAKE_PHOTO, ImageUtils.FROM_GALLERY -> ImageUtils.cropImage(requireActivity(), data, true)

            Utils.REQUEST_APP_SETTINGS -> {
                when (lastActionCallPermission) {
                    SETTING_RECORD -> {
                        val isPermissionOk = Utils.isPermissionOk(
                            context = requireContext(),
                            permissions = RECORD_AUDIO_PERMISSION
                        )
                        if (isPermissionOk) startRecording()
                    }

                    SETTING_IMAGE_FROM_GALLERY -> {
                        val isPermissionOk = Utils.isPermissionOk(
                            context = requireContext(),
                            permissions = ImageUtils.IMAGE_CAMERA_PERMISSION
                        )
                        if (isPermissionOk) ImageUtils.showCameraOptions(this)
                    }
                }
            }
        }
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            val size = imagePath?.let { ImageUtils.getImageSize(requireActivity(), it) }
            "size: $size".printMeD()

            if (imagePath != null) {
                val message = createMessage()
                message.type = MessageTypeConstants.IMAGE
                message.imageMessage = ImageMessage(
                    uri = imagePath.toString(),
                    size = size
                )
                viewModel.uploadToCloud(message, imagePath.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val isPermissionOk = Utils.isPermissionOk(*grantResults)

        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (!isPermissionOk)
                toast(getString(R.string.permission_error))
            else startRecording()
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
        binding.lottieVoice.showView()
        binding.lottieVoice.playAnimation()
        binding.viewChatBottomInput.edtMessage.apply {
            isEnabled = false
            hint = getText(R.string.recording)
        }
        onAudioEvent(EventAudioMessage(true))

        //name of the file where record will be stored
        lastAudioFile =
            "${requireActivity().externalCacheDir?.absolutePath}/audiorecord${System.currentTimeMillis()}.mp3"
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setOutputFile(lastAudioFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
            } catch (e: IOException) {
                println("ChatFragment.startRecording${e.message}")
            }
            start()
            isRecording = true
            recordStart = Date().time
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.lottieVoice.pauseAnimation()
        }, 800)
    }


    private fun stopRecording() {
        onAudioEvent(EventAudioMessage(false))
        binding.viewChatBottomInput.edtMessage.apply {
            isEnabled = true
            hint = getText(R.string.write_message)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            binding.lottieVoice.resumeAnimation()
        }, 200)
        recorder?.apply {
            stop()
            release()
            recorder = null
        }
        isRecording = false
        recordDuration = Date().time - recordStart
    }

    private fun checkBottomLayout() {
        when (bottomShow) {
            ATTACHMENT -> {
                if (binding.viewAttachment.root.visibility == View.VISIBLE)
                    closeAttachment()
            }

            STICKER -> {
                if (binding.viewSticker.visibility == View.VISIBLE)
                    closeSticker()
            }
        }
        bottomShow = DEFAULT
    }

    private fun closeAllBottomLayout() {
        closeAttachment()
        closeSticker()
        bottomShow = DEFAULT
    }

    private fun handleBottomShowing(option: Int) {
        when (option) {
            bottomShow -> {
                when (bottomShow) {
                    ATTACHMENT -> closeAttachment()

                    STICKER -> closeSticker()
                }

                bottomShow = DEFAULT
                requireActivity().showSoftKeyboard(binding.viewChatBottomInput.edtMessage)
                return
            }

            ATTACHMENT -> {
                closeSticker()
                showAttachment()
            }

            STICKER -> {
                closeAttachment()
                showSticker()
            }
        }

        requireActivity().closeKeyBoard()
        bottomShow = option
    }

    private fun closeAttachment() {
        binding.viewAttachment.recAttachmentOption.hideView()
    }

    private fun showAttachment() {
        binding.viewAttachment.recAttachmentOption.showAnimation(heightBottomLayout)
    }

    private fun closeSticker() {
        binding.viewSticker.hideView()
    }

    private fun showSticker() {
        binding.viewSticker.showAnimation(heightBottomLayout)
    }

    @Subscribe
    fun onAudioEvent(audioEvent: EventAudioMessage) {
        if (audioEvent.isPlaying) {
            //lock current orientation
            val currentOrientation = requireActivity().resources.configuration.orientation
            requireActivity().requestedOrientation = currentOrientation
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (msgPostponed) {
                //refresh list
                GroupChatAdapter.messageList = messageList
                adChat.submitList(messageList)
                msgPostponed = false
            }
        }
    }

    override fun onStickerMessageClicked(sticker: Sticker) {

    }

    override fun onStickerSetClicked(sticker: Sticker) {
        sendSticker(sticker)
    }
}