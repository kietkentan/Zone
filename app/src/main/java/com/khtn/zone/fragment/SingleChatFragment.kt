package com.khtn.zone.fragment

import android.Manifest
import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.fragment.app.Fragment
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
import com.khtn.zone.adapter.SingleChatAdapter
import com.khtn.zone.custom.textField.CustomEditText
import com.khtn.zone.database.data.AudioMessage
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.ImageMessage
import com.khtn.zone.database.data.Message
import com.khtn.zone.database.data.TextMessage
import com.khtn.zone.databinding.FragmentSingleChatBinding
import com.khtn.zone.model.MyImage
import com.khtn.zone.model.Sticker
import com.khtn.zone.model.UserProfile
import com.khtn.zone.utils.ContactUtils
import com.khtn.zone.utils.ImageTypeConstants
import com.khtn.zone.utils.ImageUtils
import com.khtn.zone.utils.KeyBoardListener
import com.khtn.zone.utils.MessageTypeConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.UserUtils
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.Utils.isPermissionOk
import com.khtn.zone.utils.addRestorePolicy
import com.khtn.zone.utils.closeKeyBoard
import com.khtn.zone.utils.disable
import com.khtn.zone.utils.enabled
import com.khtn.zone.utils.event.EventAudioMessage
import com.khtn.zone.utils.hideAnimation
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
import com.khtn.zone.viewmodel.SingleChatViewModel
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
class SingleChatFragment : Fragment(), ItemClickListener, StickerListener,CustomEditText.KeyBoardInputCallbackListener {
    private val args by navArgs<SingleChatFragmentArgs>()

    @Inject
    lateinit var preference: SharedPreferencesManager

    private lateinit var binding: FragmentSingleChatBinding
    private val viewModel: SingleChatViewModel by viewModels()

    private lateinit var chatUser: ChatUser
    private lateinit var fromUser: UserProfile
    private lateinit var toUser: UserProfile
    private var messageList = mutableListOf<Message>()
    private lateinit var localUserId: String
    private var recorder: MediaRecorder? = null
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var gridLayoutManager2: GridLayoutManager
    private lateinit var chatUserId: String

    var isRecording = false // whether is recoding now or not
    var bottomShow = DEFAULT

    private var recordStart = 0L
    private var recordDuration = 0L
    private var lastAudioFile = ""
    private var msgPostponed = false
    private var addFriendHeight: Float = 0f
    private var animSuccess = true
    private var lastActionCallPermission: Int = DEFAULT

    private val chatAdapter: SingleChatAdapter by lazy {
        SingleChatAdapter(requireContext(), this)
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
        binding = FragmentSingleChatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewmodel = viewModel
        chatUser = args.chatUserProfile!!
        viewModel.setUnReadCountZero(chatUser)
        binding.viewChatHeader.btnAddFriend.apply {
            if (!chatUser.locallySaved && chatUser.isSearchedUser)
                showView()
            else hideView()
        }

        viewModel.canScroll(false)
        binding.viewChatBottomInput.edtMessage.setKeyBoardInputCallbackListener(this)

        observer()
        initView()
        clickView()

        lifecycleScope.launch {
            viewModel.getMessagesByChatUserId(chatUserId).collect { mMessageList ->
                if (mMessageList.isEmpty())
                    return@collect

                // update status when add new message
                if (messageList.size != mMessageList.size)
                    binding.recMessage.adapter?.notifyItemChanged(messageList.lastIndex)

                messageList = mMessageList as MutableList<Message>

                if (SingleChatAdapter.isPlaying()) {
                    msgPostponed = true
                    return@collect
                }

                SingleChatAdapter.messageList = messageList
                chatAdapter.submitList(mMessageList)
                chatAdapter.updatePositionClicked(mMessageList.lastIndex)

                // scroll to last items in recycler (recent messages)
                if (messageList.isNotEmpty()) {
                    if (viewModel.getCanScroll())
                        binding.recMessage.smoothScrollToPos(messageList.lastIndex)
                    else viewModel.canScroll(true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initResume()
    }

    override fun onDestroy() {
        activity?.closeKeyBoard()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        preference.clearCurrentUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        SingleChatAdapter.stopPlaying()
        EventBus.getDefault().unregister(this)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val isPermissionOk = isPermissionOk(*grantResults)

        if (!isPermissionOk) toast(getString(R.string.permission_error))
        else {
            when (requestCode) {
                REQUEST_AUDIO_PERMISSION -> startRecording()

                ContactUtils.REQUEST_CONTACT_PERMISSION -> openSaveIntent()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_ADD_CONTACT -> {
                if (resultCode == Activity.RESULT_OK) {
                    binding.viewChatHeader.btnAddFriend.hideView()
                    val contacts = UserUtils.fetchContacts(requireContext())
                    val savedName = contacts.firstOrNull { it.mobile == chatUser.user.mobile }

                    savedName?.let {
                        binding.viewChatHeader.tvLocalName.text = it.name
                        chatUser.localName = it.name
                        chatUser.locallySaved = true
                        viewModel.insertUser(chatUser)
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Timber.v("Cancelled Added Contact")
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> onCropResult(data)

            ImageUtils.TAKE_PHOTO, ImageUtils.FROM_GALLERY -> ImageUtils.cropImage(requireActivity(), data, true)

            Utils.REQUEST_APP_SETTINGS -> {
                "app setting: $lastActionCallPermission".printMeD()
                when (lastActionCallPermission) {
                    SETTING_RECORD -> {
                        val isPermissionOk = isPermissionOk(
                            context = requireContext(),
                            permissions = RECORD_AUDIO_PERMISSION
                        )
                        if (isPermissionOk) startRecording()
                    }

                    SETTING_CONTACT -> {
                        val isPermissionOk = isPermissionOk(
                            context = requireContext(),
                            permissions = ContactUtils.CONTACT_PERMISSION
                        )
                        if (isPermissionOk) openSaveIntent()
                    }
                }
            }
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
        binding.viewChatBottomInput.ivRecord.disable()
        onAudioEvent(EventAudioMessage(true))

        // name of the file where record will be stored
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
                "ChatFragment.startRecording${e.message}".printMeD()
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
        binding.viewChatBottomInput.ivRecord.enabled()

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

    private fun onAttachmentOptions(position: Int) {

    }

    private fun initResume() {
        viewModel.setSeenAllMessage()
        preference.setCurrentUser(chatUserId)
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
                if (dy < 0)
                    checkBottomLayout()

                if (!chatUser.locallySaved && chatUser.isSearchedUser) {
                    binding.viewChatHeader.btnAddFriend.apply {
                        if (dy > 0 && this@apply.visibility != View.VISIBLE) {
                            this@apply.showAnimation(addFriendHeight)
                            animSuccess = true
                        } else if (dy < 0 && animSuccess) {
                            animSuccess = false
                            this@apply.hideAnimation(addFriendHeight)
                        }
                    }
                }
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
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            })
    }

    private fun initView() {
        try {
            fromUser = preference.getUserProfile()!!
            toUser = chatUser.user
            chatUserId = toUser.uId!!
            viewModel.setChatUser(chatUser)
            localUserId = fromUser.uId!!
            linearLayoutManager = LinearLayoutManager(context)
            gridLayoutManager = GridLayoutManager(context, 4)
            gridLayoutManager2 = GridLayoutManager(context, 4)
            fromUser.listSticker.let {
                viewModel.getSetSticker(it)
                viewModel.getAllListSticker(it)
            }

            binding.recMessage.apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                linearLayoutManager.stackFromEnd = true
                layoutManager = linearLayoutManager
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
                itemAnimator = null
                adapter = chatAdapter
            }

            binding.viewAttachment.recAttachmentOption.apply {
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                layoutManager = gridLayoutManager2
                itemAnimator = null
                adapter = attachmentAdapter
            }

            binding.viewSticker.apply {
                updateListener(this@SingleChatFragment)
            }

            chatAdapter.addRestorePolicy()
            binding.viewChatHeader.chatUser = chatUser

            binding.viewChatBottomInput.edtMessage.addTextChangedListener(msgTxtChangeListener)

            binding.viewChatBottomInput.lottieSend.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    if (binding.viewChatBottomInput.edtMessage.value.isEmpty()) {
                        binding.viewChatBottomInput.ivRecord.showView()
                        binding.viewChatBottomInput.ivAddSomeFile.showView()
                        binding.viewChatBottomInput.lottieSend.hideView()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}

            })

            binding.lottieVoice.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    binding.lottieVoice.hideView()
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}

            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clickView() {
        binding.viewChatBottomInput.lottieSend.setOnClickListener {
            sendMessage()
        }

        binding.viewChatHeader.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.viewChatBottomInput.ivRecord.setOnClickListener {
            activity?.closeKeyBoard()
            lastActionCallPermission = SETTING_RECORD
            SingleChatAdapter.stopPlaying()
            if (Utils.checkPermission(
                    context = this,
                    Manifest.permission.RECORD_AUDIO,
                    reqCode = REQUEST_AUDIO_PERMISSION
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
                    chatUsers = ArrayList()
                }
                viewModel.uploadToCloud(msg, lastAudioFile)
            }
        }

        binding.viewChatHeader.btnAddFriend.setOnClickListener {
            lastActionCallPermission = SETTING_CONTACT
            ContactUtils.askContactPermission(
                context = this,
                actionIsPermission = { openSaveIntent() }
            )
        }

        binding.viewChatBottomInput.ivAddIcon.setOnClickListener {
            handleBottomShowing(option = STICKER)
        }

        binding.viewChatBottomInput.ivAddSomeFile.setOnClickListener {
            handleBottomShowing(option = ATTACHMENT)
        }

        if (!chatUser.locallySaved && chatUser.isSearchedUser) {
            binding.viewChatHeader.btnAddFriend.apply {
                val vto: ViewTreeObserver = this.viewTreeObserver
                vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (this@apply.measuredHeight > 0) {
                            vto.removeOnGlobalLayoutListener(this)
                            addFriendHeight = this@apply.measuredHeight.toFloat()
                        }
                    }
                })
            }
        }
    }

    private fun observer() {
        viewModel.chatUserOnlineStatus.observe(viewLifecycleOwner) {
            if (!chatUser.locallySaved && !chatUser.isSearchedUser)
                binding.viewChatHeader.tvLastSeen.text = getString(R.string.unknown)
            else
                Utils.setOnlineStatus(
                    context = requireContext(),
                    txtView = binding.viewChatHeader.tvLastSeen,
                    status = it,
                    uId = localUserId
                )
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

    private fun handleGetAllSticker() {
        if (!viewModel.listSetSticker.value.isNullOrEmpty() &&
            !viewModel.listAllSticker.value.isNullOrEmpty()) {
            viewModel.getAllSticker(viewModel.listAllSticker.value!!, viewModel.listSetSticker.value!!)
        }
    }

    private fun sendMessage() {
        val msg = binding.viewChatBottomInput.edtMessage.value
        if (msg.isEmpty())
            return

        binding.viewChatBottomInput.lottieSend.playAnimation()
        val message = createMessage().apply {
            textMessage = TextMessage(msg)
            chatUsers = ArrayList()
        }
        viewModel.sendMessage(message)
        binding.viewChatBottomInput.edtMessage.setText("")
    }

    private fun sendSticker(sticker: Sticker) {
        val message = createMessage().apply {
            type = MessageTypeConstants.IMAGE
            imageMessage = ImageMessage(
                uri = sticker.url,
                imageType = ImageTypeConstants.STICKER,
                sticker = sticker,
                isGiftSticker = sticker.type == ImageTypeConstants.GIF
            )
            chatUsers = ArrayList()
        }
        viewModel.sendMessage(message)
    }

    @Suppress("DEPRECATION")
    private fun openSaveIntent() {
        val contactIntent = Intent(ContactsContract.Intents.Insert.ACTION)
        contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
        contactIntent
            .putExtra(ContactsContract.Intents.Insert.NAME, chatUser.user.userName)
            .putExtra(ContactsContract.Intents.Insert.PHONE, chatUser.user.mobile?.country + " " + chatUser.user.mobile?.number.toString())
        startActivityForResult(contactIntent, REQUEST_ADD_CONTACT)
    }

    private fun createMessage(): Message {
        return Message(
            createdAt = System.currentTimeMillis(),
            from = preference.getUid().toString(),
            to = toUser.uId!!,
            senderName = fromUser.userName,
            senderImage = fromUser.image,
            chatUserId = chatUserId,
        )
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)

            if (imagePath != null) {
                val message = createMessage().apply {
                    type = MessageTypeConstants.IMAGE
                    imageMessage = ImageMessage(imagePath.toString())
                    chatUsers = ArrayList()
                }
                viewModel.uploadToCloud(message, imagePath.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Subscribe
    fun onAudioEvent(audioEvent: EventAudioMessage) {
        if (audioEvent.isPlaying) {
            // lock current orientation
            val currentOrientation = requireActivity().resources.configuration.orientation
            requireActivity().requestedOrientation = currentOrientation
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (msgPostponed) {
                // refresh list
                SingleChatAdapter.messageList = messageList
                chatAdapter.submitList(messageList)
                msgPostponed = false
            }
        }
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

            MessageTypeConstants.TEXT -> chatAdapter.updatePositionClicked(position)

            MessageTypeConstants.VIDEO -> {

            }

            MessageTypeConstants.FILE -> {

            }
        }
    }

    override fun onItemLongClicked(v: View, position: Int) {

    }

    override fun onStickerClicked(sticker: Sticker) {
        "StickerClicked".printMeD()
    }

    override fun onCommitContent(
        inputContentInfo: InputContentInfoCompat?,
        flags: Int,
        opts: Bundle?
    ) {
        val imageMsg = createMessage()
        val image = ImageMessage("${inputContentInfo?.contentUri}")
        image.imageType = if (image.uri.toString().endsWith(".png")) ImageTypeConstants.STICKER else ImageTypeConstants.GIF
        imageMsg.apply {
            type = MessageTypeConstants.IMAGE
            imageMessage = image
            chatUsers = ArrayList()
        }
        viewModel.uploadToCloud(imageMsg, image.toString())
    }

    private val msgTxtChangeListener = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.sendTyping(binding.viewChatBottomInput.edtMessage.trim())
            if(binding.viewChatBottomInput.lottieSend.isAnimating)
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

    override fun onStickerMessageClicked(sticker: Sticker) {

    }

    override fun onStickerSetClicked(sticker: Sticker) {
        sendSticker(sticker)
    }
}