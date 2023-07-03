package com.khtn.zone.utils

object FireStoreCollection {
    const val GROUP_MESSAGE = "group_messages"
    const val GROUP = "groups"
    const val TO = "to"
    const val TYPING_STATUS = "typing_status"
    const val CHAT_USER = "chatUsers"
    const val LAST_SEEN = "last_seen"
    const val M_CHAT_USER = "chatuser"
    const val MESSAGE = "messages"
    const val USER = "users"
    const val MOBILE_NUMBER = "mobile.number"
    const val STATUS = "status"
    const val STICKER = "sticker"
    const val STICKER_ITEM = "sticker_item"
    const val LIST_STICKER = "listSticker"
    const val ID = "id"
}

object SharedPrefConstants {
    const val LOCAL_SHARED_PREF = "com.khtn.zone"
    const val LANGUAGE = "lang"
    const val UID = "user_id"
    const val LOGIN = "login"
    const val USER = "user"
    const val MOBILE = "mobile"
    const val TOKEN = "token"
    const val ONLINE_USER = "online_user"
    const val ONLINE_GROUP = "online_group"
    const val LOGIN_TIME = "login_time"
    const val LAST_LOGGED_DEVICE_SAME = "last_logged_device_same"
    const val KEY_LAST_QUERIED_LIST = "last_queried_list"
}

object FirebaseStorageConstants {
    const val ROOT_DIRECTORY = "app"
    const val USER = "user"
}

object ImageResourceSheetOptions {
    const val CAMERA = 0
    const val GALLERY = 1
    const val CANCEL = 2
}

object AttachmentOptions {
    const val IMAGE_VIDEO = 0
    const val FILE = 1
    const val QUICK_MESSAGE = 2
}

object UserStatusConstants {
    const val ONLINE = "online"
    const val OFFLINE = "offline"
    const val TYPING = "typing"
    const val NON_TYPING = "non_typing"
    const val NOT_TYPING = "not_typing"
}

object ActionConstants {
    const val ACTION_LOGGED_IN_ANOTHER_DEVICE = "action_logged_in_another_device"
    const val ACTION_NEW_MESSAGE = "action_new_message"
    const val ACTION_GROUP_NEW_MESSAGE = "action_group_new_message"
    const val ACTION_MARK_AS_READ = "action_mark_as_read"
    const val ACTION_REPLY = "action_reply"
}

object DataConstants {
    const val CHAT_USER_DB_NAME="chat_user_db"
    const val CHAT_USER_DATA = "chat_user_data"
    const val GROUP_DATA = "group_data"
    const val CHAT_DATA = "chat_data"
}

object MessageTypeConstants {
    const val TEXT = "text"
    const val AUDIO = "audio"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val FILE = "file"
}

object MessageStatusConstants {
    const val SENDING = 0
    const val SENT = 1
    const val DELIVERED = 2
    const val SEEN = 3
    const val FAILED = 4
}

object ImageTypeConstants {
    const val GIF = "gif"
    const val STICKER = "sticker"
    const val IMAGE = "image"
}

object FirebaseCloudMessagingConstants {
    const val FCM_SERVER_KEY =
        "AAAArfAO8pI:APA91bHVkJHCJYYnxrW1XMkwQUEy6MN-841gr5iToXOxvdLB63RoV7pmlJFvVqwYDT4HJ0ylP5HihXZHg0IE1sUfXtZ3JIOo_gWm_512sVGwYLC9dVQ5B8i5Vt2X2TCrHO4XuMP-kWMh"
}

object WorkerConstants {
    const val MESSAGE_DATA = "message_data"
    const val MESSAGE_FILE_URI = "message_file_uri"
}

object Event {
    const val LOGIN_USER = "login_user"
}