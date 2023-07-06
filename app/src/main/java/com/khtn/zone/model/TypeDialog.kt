package com.khtn.zone.model

enum class TypeDialog(
    var title: String? = null,
    var titleColor: Int? = null,
    var message: String? = null,
    var messageColor: Int? = null,
    var firstBtnMessage: String? = null,
    var secondBtnMessage: String? = null,
    var firstaction: () -> Unit = {},
    var secondAction: () -> Unit = {}
) {
    DIALOG_2_BUTTON, DIALOG_1_BUTTON;
}