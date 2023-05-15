package com.khtn.zone.utils

object FireStoreCollection {
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
}

object ImageResourceSheetOptions {
    const val CAMERA = 0
    const val GALLERY = 1
    const val CANCEL = 2
}

object UserStatusConstants {
    const val ONLINE = "online"
    const val OFFLINE = "offline"
    const val TYPING = "typing"
    const val NON_TYPING = "non_typing"
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

object FirebaseCloudMessagingConstants {
    const val FCM_SERVER_KEY =
        "AAAArfAO8pI:APA91bHVkJHCJYYnxrW1XMkwQUEy6MN-841gr5iToXOxvdLB63RoV7pmlJFvVqwYDT4HJ0ylP5HihXZHg0IE1sUfXtZ3JIOo_gWm_512sVGwYLC9dVQ5B8i5Vt2X2TCrHO4XuMP-kWMh"
}

object WorkerConstants {
    const val MESSAGE_DATA = "message_data"
    const val MESSAGE_FILE_URI = "message_file_uri"
}

object Event {
    const val LOGIN_USER = "login_uer"
    const val VIEW_ON_BOARDING = "view_on_boarding"
}