package com.khtn.zone.utils

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseMethod
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import android.content.Context
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.chip.Chip
import com.khtn.zone.MyApplication
import com.khtn.zone.R
import com.khtn.zone.database.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object BindingAdapters {

    @BindingAdapter("main", "secondText")
    @JvmStatic
    fun setBoldString(
        view: TextView,
        maintext: String,
        sequence: String
    ) {
        view.text = getBoldText(maintext, sequence)
    }

    @JvmStatic
    fun getBoldText(
        text: String,
        name: String
    ): SpannableStringBuilder {
        val str = SpannableStringBuilder(text)
        val textPosition = text.indexOf(name)
        str.setSpan(
            android.text.style.StyleSpan(Typeface.BOLD),
            textPosition, textPosition + name.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return str
    }

    @BindingAdapter("imageUrl")
    @JvmStatic
    fun loadImage(
        view: ImageView,
        url: String?
    ) {
        if (url.isNullOrEmpty()) return
        else {
            ImageViewCompat.setImageTintList(view, null) // removing image tint
            view.setPadding(0)
        }
        ImageUtils.loadUserImage(view, url)
    }

    @BindingAdapter("lastMessage")
    @JvmStatic
    fun setLastMessage(txtView: TextView, msgList: List<Message>) {
        val lastMsg = msgList.last()
        txtView.text = getLastMsgTxt(txtView.context, lastMsg)
    }

    @InverseMethod("messageBsg")
    @JvmStatic
    fun messageBsg(msg: Message): String {
        return "sd"
    }

    @BindingAdapter("message", "messageList", "adapterPos")
    @JvmStatic
    fun messageBackground(
        txtView: TextView,
        msg: Message,
        list: ArrayList<Message>,
        adapterPos: Int
    ) {
        txtView.setBackgroundResource(R.drawable.shape_send_msg)
        if (list.size <= 2) {
            val message = list[adapterPos - 1]
            if (message.from == "me") {

            }
        }

    }

    @BindingAdapter("loadImage")
    @JvmStatic
    fun loadImage(imgView: ImageView, message: Message) {
        val url = message.imageMessage?.uri
        val imageType = message.imageMessage?.imageType
        ImageUtils.loadMsgImage(imgView, url!!, imageType!!)
    }

    @BindingAdapter("loadGroupMsgImage")
    @JvmStatic
    fun loadGrpMsgImage(
        imgView: ImageView,
        message: GroupMessage
    ) {
        val url = message.imageMessage?.uri
        val imageType = message.imageMessage?.imageType
        ImageUtils.loadMsgImage(imgView, url.toString(), imageType.toString())
    }

    fun getLastMsgTxt(context: Context, msg: Message): String {
        return when (msg.type) {
            MessageTypeConstants.TEXT -> msg.textMessage?.text.toString()

            MessageTypeConstants.AUDIO -> context.getString(R.string.new_audio)

            MessageTypeConstants.IMAGE -> msg.imageMessage?.imageType.toString().capitalize(Locale.getDefault())

            MessageTypeConstants.VIDEO -> context.getString(R.string.new_video)

            MessageTypeConstants.FILE -> context.getString(R.string.new_file)

            else -> {
                "Steotho Image"
            }
        }
    }

    fun getLastMsgTxt(context: Context, msg: GroupMessage): String {
        return when (msg.type) {
            MessageTypeConstants.TEXT -> msg.textMessage?.text.toString()

            MessageTypeConstants.AUDIO -> context.getString(R.string.new_audio)

            MessageTypeConstants.IMAGE -> msg.imageMessage?.imageType.toString().capitalize(Locale.getDefault())

            MessageTypeConstants.VIDEO -> context.getString(R.string.new_video)

            MessageTypeConstants.FILE -> context.getString(R.string.new_file)

            else -> {
                "Steotho image"
            }
        }
    }

    @BindingAdapter("messageSendTime")
    @JvmStatic
    fun setMessageTime(
        txtView: TextView,
        msgList: List<Message>
    ) {
        val lastMsg = msgList.last()
        val sentTime = lastMsg.createdAt
        txtView.text = Utils.getTime(sentTime, txtView.context)
    }

    @BindingAdapter("showMsgTime")
    @JvmStatic
    fun showMsgTime(
        txtView: TextView,
        lastMsg: Message
    ) {
        val sentTime = lastMsg.createdAt
        txtView.text = Utils.getTime(sentTime, txtView.context)
    }

    @BindingAdapter("showGrpMsgTime")
    @JvmStatic
    fun showGrpMsgTime(
        txtView: TextView,
        lastMsg: GroupMessage
    ) {
        val sentTime = lastMsg.createdAt
        txtView.text = Utils.getTime(sentTime, txtView.context)
    }

    @BindingAdapter("loadAsDrawable")
    @JvmStatic
    fun loadAsDrawable(
        chip: Chip,
        user: ChatUser
    ) {
        val url = user.user.image
        if (url.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val drawable = getBitmap(url)
                withContext(Dispatchers.Main) {
                    chip.chipIcon = drawable
                }
            }
        }
    }


    private suspend fun getBitmap(url: String): Drawable {
        val context = MyApplication.appContext
        val request = Glide.with(context)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .submit()
        val bitmap: Bitmap = withContext(Dispatchers.IO) {
            request.get()
        }
        return BitmapDrawable(context.resources, bitmap)
    }

    @BindingAdapter("messageStatus")
    @JvmStatic
    fun setState(
        txtStatus: TextView,
        status: Int
    ) {
        txtStatus.text = when (status) {
            MessageStatusConstants.SENDING -> txtStatus.context.getString(R.string.sending)

            MessageStatusConstants.SENT -> txtStatus.context.getString(R.string.sent)

            MessageStatusConstants.DELIVERED -> txtStatus.context.getString(R.string.delivered)

            MessageStatusConstants.SEEN -> txtStatus.context.getString(R.string.seen)

            else -> txtStatus.context.getString(R.string.failed)
        }
    }

    @BindingAdapter("groupMessageStatus")
    @JvmStatic
    fun groupMsgStatus(txtStatus: TextView, message: GroupMessage) {
        val statusList = message.status
        val myStatus = statusList.first()
        if (message.from == SharedPreferencesManager(context = txtStatus.context).retrieveStringByKey(SharedPrefConstants.UID))
            statusList.removeAt(0)
        val delivered = message.status.any { it == MessageStatusConstants.DELIVERED || it == MessageStatusConstants.SEEN }  // if anyone has seen the message
        val seen = message.status.all { it == MessageStatusConstants.SEEN }  // all members seen the messge

        txtStatus.text = when {
            myStatus == MessageStatusConstants.SENDING -> txtStatus.context.getString(R.string.sending)

            seen -> txtStatus.context.getString(R.string.seen)

            delivered -> txtStatus.context.getString(R.string.delivered)

            myStatus == MessageStatusConstants.SENT -> txtStatus.context.getString(R.string.sent)

            else -> txtStatus.context.getString(R.string.failed)
        }
    }

    @BindingAdapter("progressState")
    @JvmStatic
    fun setProgressState(
        view: ProgressBar,
        state: UiState<Any>?
    ) {
        state?.let {
            view.visibility = when (it) {
                UiState.Loading -> View.VISIBLE

                else -> View.GONE
            }
        }
    }

    @BindingAdapter("setUnReadCount")
    @JvmStatic
    fun setUnReadCount(
        txtView: TextView,
        count: Int
    ) {
        txtView.text = if (count < 10) count.toString() else "N+"
        txtView.visibility = if (count == 0) View.GONE else View.VISIBLE
    }

    @BindingAdapter("showSelected")
    @JvmStatic
    fun showSelected(
        view: LottieAnimationView,
        isSelected: Boolean
    ) {
        if (isSelected) {
            view.playAnimation()
            view.showView()
        } else
            view.hideView()
    }

    @BindingAdapter("showTxtView")
    @JvmStatic
    fun setChipIcon(
        txtView: TextView,
        user: ChatUser
    ) {
        if (user.user.image.isEmpty())
            txtView.text = user.localName.first().toString()
        else
            txtView.hideView()
    }

    @BindingAdapter("setMemberNames")
    @JvmStatic
    fun setMemberNames(
        txtView: TextView,
        group: Group
    ) {
        val members = group.members?.map { chatUser ->
            val savedName = chatUser.localName
            savedName.ifEmpty { "${chatUser.user.mobile?.country} ${chatUser.user.mobile?.number}" }
        }
        members?.let {
            txtView.text = TextUtils.join(", ", it)
        }
    }

    @BindingAdapter("setGroupName")
    @JvmStatic
    fun setGroupName(
        txtView: TextView,
        group: Group
    ) {
        txtView.text = Utils.getGroupName(group.id)
    }

    @BindingAdapter("groupLastMessage")
    @JvmStatic
    fun groupLastMessage(
        txtView: TextView,
        group: GroupWithMessages
    ) {
        val messages = group.messages
        if (messages.isEmpty()) {
            val createdBy = group.group.createdBy
            val msg = String.format("%s %s", txtView.context.getString(R.string.create_by), group.group.members?.first { it.id == createdBy }?.localName)
            txtView.text = msg
        } else {
            val message = messages.last()
            val localName = group.group.members?.first { it.id == message.from }?.localName
            val txtMsg = "$localName : ${getLastMsgTxt(txtView.context, message)}"
            txtView.text = txtMsg
        }
    }

    @BindingAdapter("setGroupMessageSendTime")
    @JvmStatic
    fun setGroupMessageSendTime(
        txtView: TextView,
        msgList: List<GroupMessage>
    ) {
        if (msgList.isNotEmpty()) {
            val lastMsg = msgList.last()
            val sentTime = lastMsg.createdAt
            txtView.text = Utils.getTime(sentTime, txtView.context)
        }
    }

    @BindingAdapter("setGroupUnReadCount")
    @JvmStatic
    fun setGroupUnReadCount(
        txtView: TextView,
        unReadCount: Int
    ) {
        if (unReadCount == 0)
            txtView.hideView()
        else {
            txtView.text = unReadCount.toString()
            txtView.showView()
        }
    }

    @BindingAdapter("chatUsers", "message")
    @JvmStatic
    fun setChatUserName(
        txtView: TextView,
        users: Array<ChatUser>,
        message: GroupMessage
    ) {
        val messageOwner = users.first { message.from == it.id }
        txtView.text = messageOwner.localName
    }

    @BindingAdapter("showUserIdIfNotLocalSaved", "currentMessage")
    @JvmStatic
    fun showUserIdIfNotLocalSaved(
        txtView: TextView,
        users: Array<ChatUser>, message: GroupMessage
    ) {
        val messageOwner = users.first { message.from == it.id }
        txtView.text = messageOwner.user.userName
        if (messageOwner.locallySaved)
            txtView.hideView()
        else
            txtView.showView()
    }

    @BindingAdapter("srcAttachment")
    @JvmStatic
    fun setResourceAttachment(
        imgView: ImageView,
        @DrawableRes resource: Int
    ) {
        imgView.setImageResource(resource)
    }
}