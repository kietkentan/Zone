package com.khtn.zone.custom.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.khtn.zone.databinding.DialogImageResourceSheetBinding
import com.khtn.zone.utils.ImageResourceSheetOptions
import com.khtn.zone.utils.setTransparentBackground

interface SheetListener {
    fun selectedItem(index: Int)
}

class DialogImageResourceSheet: BottomSheetDialogFragment() {
    private lateinit var binding: DialogImageResourceSheetBinding
    private lateinit var listener: SheetListener

    companion object{
        fun newInstance(bundle: Bundle): DialogImageResourceSheet {
            val fragment = DialogImageResourceSheet()
            fragment.arguments = bundle
            return fragment
        }
    }

    fun addListener(listener: SheetListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogImageResourceSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setTransparentBackground()

        binding.tvCamera.setOnClickListener {
            listener.selectedItem(index = ImageResourceSheetOptions.CAMERA)
            dismiss()
        }

        binding.tvGallery.setOnClickListener {
            listener.selectedItem(index = ImageResourceSheetOptions.GALLERY)
            dismiss()
        }

        binding.tvCancel.setOnClickListener {
            listener.selectedItem(index = ImageResourceSheetOptions.CANCEL)
            dismiss()
        }
    }
}