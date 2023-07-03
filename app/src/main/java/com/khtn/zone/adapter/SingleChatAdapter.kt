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
import com.khtn.zone.database.data.Message
import com.khtn.zone.databinding.RowAudioReceiveBinding
import com.khtn.zone.databinding.RowAudioSentBinding
import com.khtn.zone.databinding.RowImageReceiveBinding
import com.khtn.zone.databinding.RowImageSentBinding
import com.khtn.zone.databinding.RowReceiveMessageBinding
import com.khtn.zone.databinding.RowSentMessageBinding
import com.khtn.zone.databinding.RowStickerReceiveBinding
import com.khtn.zone.databinding.RowStickerSentBinding
import com.khtn.zone.utils.ImageTypeConstants
import com.khtn.zone.utils.MessageTypeConstants
import com.khtn.zone.utils.SharedPreferencesManager
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.event.EventAudioMessage
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.listener.ItemClickListener
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showAnimation
import com.khtn.zone.utils.showView
import kotlinx.coroutines.Runnable
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException

class SingleChatAdapter(
    private val context: Context,
    private val msgClickListener: ItemClickListener
): ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallbackMessages()) {
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
        private var lastPlayedHolder: RowAudioSentBinding? = null
        @SuppressLint("StaticFieldLeak")
        private var lastReceivedPlayedHolder: RowAudioReceiveBinding? = null
        private var lastPlayedAudioId: Long = -1
        private var lastPositionMessageClicked: Int = -1
        private var player = MediaPlayer()
        private var handler: Handler? = null
        lateinit var messageList: MutableList<Message>

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
                val binding = RowSentMessageBinding.inflate(layoutInflater, parent, false)
                TxtSentVHolder(binding)
            }

            TYPE_TEXT_RECEIVED -> {
                val binding = RowReceiveMessageBinding.inflate(layoutInflater, parent, false)
                TxtReceiveVHolder(binding)
            }

            TYPE_IMAGE_SENT -> {
                val binding = RowImageSentBinding.inflate(layoutInflater, parent, false)
                ImageSentVHolder(binding)
            }

            TYPE_IMAGE_RECEIVE -> {
                val binding = RowImageReceiveBinding.inflate(layoutInflater, parent, false)
                ImageReceiveVHolder(binding)
            }

            TYPE_STICKER_SENT -> {
                val binding = RowStickerSentBinding.inflate(layoutInflater, parent, false)
                StickerSentVHolder(binding)
            }

            TYPE_STICKER_RECEIVE -> {
                val binding = RowStickerReceiveBinding.inflate(layoutInflater, parent, false)
                StickerReceiveVHolder(binding)
            }

            TYPE_AUDIO_SENT -> {
                val binding = RowAudioSentBinding.inflate(layoutInflater, parent, false)
                AudioSentVHolder(binding)
            }

            else -> {
                val binding = RowAudioReceiveBinding.inflate(layoutInflater, parent, false)
                AudioReceiveVHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TxtSentVHolder ->
                holder.bind(context, getItem(position), msgClickListener)

            is TxtReceiveVHolder ->
                holder.bind(context, getItem(position))

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

    class TxtSentVHolder(val binding: RowSentMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.messageList = messageList as ArrayList<Message>

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

            binding.layoutMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            binding.executePendingBindings()
        }
    }

    class TxtReceiveVHolder(val binding: RowReceiveMessageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message) {
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
                    binding.txtMsg.setBackgroundResource(R.drawable.bg_item_chat_receive_ripple_8dp)
                    binding.txtMsgTime.showView()
                }
            }

            binding.executePendingBindings()
        }
    }

    class ImageSentVHolder(val binding: RowImageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            binding.executePendingBindings()
        }
    }

    class ImageReceiveVHolder(val binding: RowImageReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message, msgClickListener: ItemClickListener) {
            binding.message = item
            binding.imageMsg.setOnClickListener {
                msgClickListener.onItemClicked(it, bindingAdapterPosition)
            }

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            binding.executePendingBindings()
        }
    }

    class StickerSentVHolder(val binding: RowStickerSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message, msgClickListener: ItemClickListener) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            binding.imageMsg.setOnClickListener {
                msgClickListener.onStickerClicked(item.imageMessage!!.sticker!!)
            }

            binding.executePendingBindings()
        }
    }

    class StickerReceiveVHolder(val binding: RowStickerReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message, msgClickListener: ItemClickListener) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

            binding.imageMsg.setOnClickListener {
                msgClickListener.onStickerClicked(item.imageMessage!!.sticker!!)
            }

            binding.executePendingBindings()
        }
    }

    class AudioReceiveVHolder(val binding: RowAudioReceiveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(context: Context, item: Message) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

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
            item: Message,
            currentHolder: RowAudioReceiveBinding
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
        private fun initTime(item: Message) {
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

    class AudioSentVHolder(val binding: RowAudioSentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(
            context: Context,
            item: Message,
        ) {
            binding.message = item

            if (bindingAdapterPosition == lastPositionMessageClicked)
                binding.txtMsgStatus.showAnimation(context.resources.getDimension(R.dimen.dp13))
            else if (bindingAdapterPosition != lastPositionMessageClicked && bindingAdapterPosition == messageList.lastIndex)
                binding.txtMsgStatus.visibility = View.INVISIBLE
            else binding.txtMsgStatus.hideView()

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
            item: Message,
            currentHolder: RowAudioSentBinding
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
        private fun initTime(item: Message) {
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

class DiffCallbackMessages : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.createdAt == newItem.createdAt
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }
}