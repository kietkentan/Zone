package com.khtn.zone.custom.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.khtn.zone.R
import com.khtn.zone.databinding.AlertDialogBinding
import com.khtn.zone.model.TypeDialog
import com.khtn.zone.utils.hideView
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showView

class AlertDialog(context: Context): Dialog(context, R.style.Theme_Dialog_Small) {
    private val binding: AlertDialogBinding = AlertDialogBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(binding.root)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    fun showDialog(typeDialog: TypeDialog) {
        when (typeDialog) {
            TypeDialog.DIALOG_1_BUTTON -> openDialogOneButton(typeDialog)

            TypeDialog.DIALOG_2_BUTTON -> openDialog2Button(typeDialog)
        }
    }

    private fun openDialogOneButton(typeDialog: TypeDialog) {
        typeDialog.title?.let {
            binding.tvTitleError.showView()
            binding.tvTitleError.text = it
        } ?: binding.tvTitleError.hideView()
        typeDialog.titleColor?.let { binding.tvTitleError.setTextColor(it) }

        typeDialog.message?.let {
            binding.tvSmallError.showView()
            binding.tvSmallError.text = it
        } ?: binding.tvSmallError.hideView()
        typeDialog.messageColor?.let { binding.tvSmallError.setTextColor(it) }

        binding.tvButton1.text = typeDialog.firstBtnMessage
        binding.tvButton2.hideView()
        binding.tvButton1.setOnClickListener {
            try {
                typeDialog.firstaction.invoke()
            } catch (e: Exception) {
                "dialog error ${e.message.toString()}".printMeD()
            }
            dismiss()
        }

        show()
    }

    private fun openDialog2Button(typeDialog: TypeDialog) {
        typeDialog.title?.let {
            binding.tvTitleError.showView()
            binding.tvTitleError.text = it
        } ?: binding.tvTitleError.hideView()
        typeDialog.titleColor?.let { binding.tvTitleError.setTextColor(it) }

        typeDialog.message?.let {
            binding.tvSmallError.showView()
            binding.tvSmallError.text = it
        } ?: binding.tvSmallError.hideView()
        typeDialog.messageColor?.let { binding.tvSmallError.setTextColor(it) }

        binding.tvButton1.text = typeDialog.firstBtnMessage
        binding.tvButton2.text = typeDialog.secondBtnMessage

        binding.tvButton1.setOnClickListener {
            try {
                typeDialog.firstaction.invoke()
            } catch (e: Exception) {
                "dialog error ${e.message.toString()}".printMeD()
            }
            dismiss()
        }

        binding.tvButton2.setOnClickListener {
            try {
                typeDialog.secondAction.invoke()
            } catch (e: Exception) {
                "dialog error ${e.message.toString()}".printMeD()
            }
            dismiss()
        }

        show()
    }
}
