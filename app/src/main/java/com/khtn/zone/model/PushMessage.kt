package com.khtn.zone.model

import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class PushMessage(
    var type: String? = null,
    var to: String? = null,
    var title: String? = null,
    var message: String? = null,
    var message_body: String? = null
)