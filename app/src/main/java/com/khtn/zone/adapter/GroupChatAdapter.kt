package com.khtn.zone.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.khtn.zone.R
import com.khtn.zone.database.data.ChatUser
import com.khtn.zone.database.data.GroupMessage
import com.khtn.zone.databinding.RowGroupAudioReceiveBinding
import com.khtn.zone.databinding.RowGroupAudioSentBinding
import com.khtn.zone.databinding.RowGroupImageReceiveBinding
import com.khtn.zone.databinding.RowGroupImageSentBinding
import com.khtn.zone.databinding.RowGroupStickerReceiveBinding
import com.khtn.zone.databinding.RowGroupStickerSentBinding
import com.khtn.zone.databinding.RowReceiveGroupMessageBinding
import com.khtn.zone.databinding.RowSentGroupMessageBinding
import com.khtn.zone.utils.ImageTypeConstants
import com.khtn.zone.utils.MessageTypeConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.event.EventAudioMessage
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.setMargin
import com.khtn.zone.utils.showAnimation
import com.khtn.zone.utils.showView
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException

class GroupChatAdapter(
    private val context: Context,
    private val msgClickListener: ItemClickListener
): ListAdapter<GroupMessage, RecyclerView.ViewHolder>(DiffCallbackGroupMessages()) {
    private val preference = SharedPreferencesManager(context)

    companion object {
        private const val TYPE_TEXT_SENT = 0
        private const val TYPE_TEXT_RECEIVED = 1
        private const val TYPE_IMAGE_SENT = 2
        private const val TYPE_IMAGE_RECEIVE = 3
        private const val TYPE_STICKER_SENT = 4
        private const val TYPE_STICKER_RECEIVE = 5
        private const val TYPE_AUDIO_SENT = 6
        private const val TYPE_AUDIO_RECEIVE = 7
        @SuppressLint("StaticFieldLeak")
        private var lastPlayedHolder: RowGroupAudioSentBinding? = null
        @SuppressLint("StaticFieldLeak")
        private var lastReceivedPlayedHolder: RowGroupAudioReceiveBinding? = null
        private var lastPlayedAudioId: Long = -1
        private var lastPositionMessageClicked: Int = -1
        private var player = MediaPlayer()
        private var handler: Handler? = null
        lateinit var messageList: MutableList<GroupMessage>
        lateinit var chatUserList: MutableList<ChatUser>

        fun stopPlaying() {
            if (player.isPlaying) {
                lastReceivedPlayedHolder?.progressBar?.abandon()
                lastPlayedHolder?.progressBar?.abandon()
                lastReceivedPlayedHolder?.imgPlay?.setImageResource(R.drawable.ic_action_play)
                lastPlayedHolder?.imgPlay?.setImageResource(R.drawable.ic_action_play)
                player.apply {
                    stop()
                    reset()
                    EventBus.getDefault().post(EventAudioMessage(false))
                }
            }
        }

        fun isPlaying() = player.isPlaying
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TEXT_SENT -> {
                val binding = RowSentGroupMessageBinding.inflate(layoutInflater, parent, false)
                TxtSentVHolder(binding)
            }

            TYPE_TEXT_RECEIVED -> {
                val binding = RowReceiveGroupMessageBinding.inflate(layoutInflater, parent, false)
                TxtReceiveVHolder(binding)
            }

            TYPE_IMAGE_SENT -> {
                val binding = RowGroupImageSentBinding.inflate(layoutInflater, parent, false)
                ImageSentVHolder(binding)
            }

            TYPE_IMAGE_RECEIVE -> {
                val binding = RowGroupImageReceiveBinding.inflate(layoutInflater, parent, false)
                ImageReceiveVHolder(binding)
            }

            TYPE_STICKER_SENT -> {
                val binding = RowGroupStickerSentBinding.inflate(layoutInflater, parent, false)
                StickerSentVHolder(binding)
            }

            TYPE_STICKER_RECEIVE -> {
                val binding = RowGroupStickerReceiveBinding.inflate(layoutInflater, parent, false)
                StickerReceiveVHolder(binding)
            }

            TYPE_AUDIO_SENT -> {
                val binding = RowGroupAudioSentBinding.inflate(layoutInflater, parent, false)
                AudioSentVHolder(binding)
            }

            else -> {
                val binding = RowGroupAudioReceiveBinding.inflate(layoutInflater, parent, false)
                AudioReceiveVHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TxtSentVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is TxtReceiveVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is ImageSentVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is ImageReceiveVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is StickerSentVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is StickerReceiveVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is AudioSentVHolder ->
                holder.bind(context, getItem(position))

            is AudioReceiveVHolder ->
                holder.bind(context, getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val fromMe = message.from == preference.getUid()

        if (fromMe && message.type == MessageTypeConstants.TEXT)
            return TYPE_TEXT_SENT
        else if (!fromMe && message.type == MessageTypeConstants.TEXT)
            return TYPE_TEXT_RECEIVED
        else if (fromMe && message.type == MessageTypeConstants.IMAGE && message.imageMessage?.imageType == MessageTypeConstants.IMAGE)
            return TYPE_IMAGE_SENT
        else if (!fromMe && message.type == MessageTypeConstants.IMAGE && message.imageMessage?.imageType == MessageTypeConstants.IMAGE)
            return TYPE_IMAGE_RECEIVE
        else if (fromMe && message.type == MessageTypeConstants.IMAGE && (message.imageMessage?.imageType == ImageTypeConstants.STICKER
                    || message.imageMessage?.imageType == ImageTypeConstants.GIF)
        )
            return TYPE_STICKER_SENT
        else if (!fromMe && message.type == MessageTypeConstants.IMAGE && (message.imageMessage?.imageType == ImageTypeConstants.STICKER
                    || message.imageMessage?.imageType == ImageTypeConstants.GIF)
        )
            return TYPE_STICKER_RECEIVE
        else if (fromMe && message.type == MessageTypeConstants.AUDIO)
            return TYPE_AUDIO_SENT
        else if (!fromMe && message.type == MessageTypeConstants.AUDIO)
            return TYPE_AUDIO_RECEIVE
        return super.getItemViewType(position)
    }

    fun updatePositionClicked(position: Int) {
        val currentPosition = lastPositionMessageClicked
        lastPositionMessageClicked = position

        if (currentPosition in 0..messageList.lastIndex)
            notifyItemChanged(currentPosition)
        notifyItemChanged(lastPositionMessageClicked)
    }

    class TxtSentVHolder(val binding: RowSentGroupMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition >= 0 && bindingAdapterPosition + 1 < messageList.size) {
                val message = messageList[bindingAdapterPosition + 1]

                if (item.from != message.from ||
                    item.type != message.type ||
                    item.createdAt - message.createdAt > 300000
                ) {
                    binding.layoutMsg.setBackgroundResource(R.drawable.bg_item_chat_sent_ripple_8dp)
                    binding.txtMsgTime.showView()
                }
            }

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.layoutMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            //binding.executePendingBindings()
        }
    }

    class TxtReceiveVHolder(val binding: RowReceiveGroupMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.chatUsers = chatUserList.toTypedArray()

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition >= 0 && bindingAdapterPosition + 1 < messageList.size) {
                val message = messageList[bindingAdapterPosition + 1]

                if (item.from != message.from ||
                    item.type != message.type ||
                    item.createdAt - message.createdAt > 300000
                ) {
                    binding.txtMsg.setBackgroundResource(R.drawable.bg_item_chat_receive_ripple_8dp)
                    binding.txtMsgTime.showView()
                }
            }

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.layoutMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            binding.executePendingBindings()
        }
    }

    class ImageSentVHolder(val binding: RowGroupImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.executePendingBindings()
        }
    }

    class ImageReceiveVHolder(val binding: RowGroupImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.chatUsers = chatUserList.toTypedArray()
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.executePendingBindings()
        }
    }

    class StickerSentVHolder(val binding: RowGroupStickerSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            binding.executePendingBindings()
        }
    }

    class StickerReceiveVHolder(val binding: RowGroupStickerReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.chatUsers = chatUserList.toTypedArray()

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            binding.executePendingBindings()
        }
    }

    class AudioReceiveVHolder(val binding: RowGroupAudioReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: GroupMessage) {
            binding.message = item
            binding.chatUsers = chatUserList.toTypedArray()

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.progressBar.setStoriesCountDebug(1, 0)
            binding.progressBar.setAllStoryDuration(item.audioMessage?.duration!!.toLong() * 1000)
            binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
            binding.imgPlay.setOnClickListener {
                startPlaying(
                    context = context,
                    item = item,
                    currentHolder = binding
                )
            }
            binding
            binding.executePendingBindings()
        }

        private fun startPlaying(
            context: Context,
            item: GroupMessage,
            currentHolder: RowGroupAudioReceiveBinding
        ) {
            if (player.isPlaying) {
                stopPlaying()
                binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                if (lastPlayedAudioId == item.createdAt)
                    return

            }
            player = MediaPlayer()
            lastReceivedPlayedHolder = currentHolder
            lastPlayedAudioId = item.createdAt
            currentHolder.progressBuffer.showView()
            currentHolder.imgPlay.hideView()
            player.apply {
                try {
                    setDataSource(context, Uri.parse(item.audioMessage?.uri))
                    prepareAsync()
                    setOnPreparedListener {
                        currentHolder.progressBuffer.hideView()
                        currentHolder.imgPlay.showView()
                        if (item.createdAt == lastPlayedAudioId) {
                            Timber.v("Started..")
                            start()
                            initTime(item)
                            currentHolder.imgPlay.setImageResource(R.drawable.ic_action_stop)
                            currentHolder.progressBar.startStories()
                            EventBus.getDefault().post(EventAudioMessage(true))
                        }
                    }
                    setOnCompletionListener {
                        currentHolder.progressBar.abandon()
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_play)
                        EventBus.getDefault().post(EventAudioMessage(false))
                    }
                } catch (e: IOException) {
                    "ChatFragment.startPlaying:prepare failed".printMeD()
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun initTime(item: GroupMessage) {
            if (handler == null)
                handler = Handler()
            handler!!.postDelayed(object : Runnable{
                override fun run() {
                    if (player.isPlaying && item.createdAt == lastPlayedAudioId) {
                        try {
                            binding.tvTimeAudio.text =
                                Utils.getTimeAudio(item.audioMessage?.duration!! - 1 - player.currentPosition/1000.toLong())
                            handler!!.postDelayed(this, 1000)
                        } catch (e: Exception) {
                            binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                        }
                    } else {
                        binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                        handler!!.removeCallbacks(this)
                    }
                }
            },0)
        }
    }

    class AudioSentVHolder(val binding: RowGroupAudioSentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            item: GroupMessage,
        ) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            if (bindingAdapterPosition > 0) {
                val message = messageList[bindingAdapterPosition - 1]

                if (item.from != message.from)
                    binding.layout.setMargin(top = context.resources.getDimension(R.dimen.dp5).toInt())
            }

            binding.progressBar.setStoriesCountDebug(1, 0)
            binding.progressBar.setAllStoryDuration(item.audioMessage?.duration!!.toLong() * 1000)
            binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
            binding.layoutMsgAudio.setOnClickListener {
                startPlaying(
                    context = context,
                    item = item,
                    currentHolder = binding
                )
            }
            binding.executePendingBindings()
        }

        private fun startPlaying(
            context: Context,
            item: GroupMessage,
            currentHolder: RowGroupAudioSentBinding
        ) {
            if (player.isPlaying) {
                binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                stopPlaying()
                if (lastPlayedAudioId == item.createdAt)
                    return
            }
            player = MediaPlayer()
            lastPlayedHolder = currentHolder
            lastPlayedAudioId = item.createdAt
            currentHolder.progressBuffer.showView()
            currentHolder.imgPlay.hideView()
            player.apply {
                try {
                    setDataSource(context, Uri.parse(item.audioMessage?.uri))
                    prepareAsync()
                    setOnPreparedListener {
                        currentHolder.imgPlay.showView()
                        currentHolder.progressBuffer.hideView()
                        if (item.createdAt == lastPlayedAudioId) {
                            Timber.v("Started..")
                            start()
                            initTime(item)
                            currentHolder.imgPlay.setImageResource(R.drawable.ic_action_stop)
                            currentHolder.progressBar.startStories()
                            EventBus.getDefault().post(EventAudioMessage(true))
                        }
                    }
                    setOnCompletionListener {
                        currentHolder.progressBar.abandon()
                        binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                        currentHolder.imgPlay.setImageResource(R.drawable.ic_action_play)
                        EventBus.getDefault().post(EventAudioMessage(false))
                    }
                } catch (e: IOException) {
                    "ChatFragment.startPlaying:prepare failed".printMeD()
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun initTime(item: GroupMessage) {
            if (handler == null)
                handler = Handler()
            handler!!.postDelayed(object : Runnable{
                override fun run() {
                    if (player.isPlaying && item.createdAt == lastPlayedAudioId) {
                        try {
                            binding.tvTimeAudio.text =
                                Utils.getTimeAudio(item.audioMessage?.duration!! - 1 - player.currentPosition/1000.toLong())
                            handler!!.postDelayed(this, 1000)
                        } catch (e: Exception) {
                            binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                        }
                    } else {
                        binding.tvTimeAudio.text = Utils.getTimeAudio(item.audioMessage?.duration!!.toLong())
                        handler!!.removeCallbacks(this)
                    }
                }
            },0)
        }
    }
}

class DiffCallbackGroupMessages : DiffUtil.ItemCallback<GroupMessage>() {
    override fun areItemsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
        return oldItem.createdAt == newItem.createdAt
    }

    override fun areContentsTheSame(oldItem: GroupMessage, newItem: GroupMessage): Boolean {
        return oldItem == newItem
    }
}