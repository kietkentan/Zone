package com.khtn.zone.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.canhub.cropper.CropImage
import com.google.firebase.firestore.CollectionReference
import com.khtn.zone.databinding.FragmentSetupProfileBinding
import com.khtn.zone.model.UserStatus
import com.khtn.zone.utils.*
import com.khtn.zone.viewmodel.SetupProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@AndroidEntryPoint
class SetupProfileFragment: Fragment() {
    private lateinit var binding: FragmentSetupProfileBinding
    private lateinit var context: Activity

    @Inject
    lateinit var preference: SharedPreferencesManager

    @Inject
    lateinit var userCollection: CollectionReference

    private val profileViewModel: SetupProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context = requireActivity()
        UserUtils.updatePushToken(context, userCollection, true)
        EventBus.getDefault().post(UserStatus())
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = profileViewModel
        binding.ivProfile.setOnClickListener { ImageUtils.askPermission(this) }
        binding.floatingButtonNext.setOnClickListener{ validate() }
        observer()
    }

    private fun validate() {
        val name = profileViewModel.name.value
        if (!name.isNullOrEmpty() && name.length > 1 && !profileViewModel.progressProPic.value!!)
            profileViewModel.storeProfileData()
    }

    private fun observer() {
        profileViewModel.profileUpdateState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}

                is UiState.Failure -> {}

                is UiState.Success -> {}
            }
        }

        profileViewModel.checkUserNameState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {}

                is UiState.Failure -> {}

                is UiState.Success -> {}
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        "onActivityResult: ${data?.data}".printMeD()
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) onCropResult(data)
        else ImageUtils.cropImage(context, data, true)
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            imagePath?.let { profileViewModel.uploadProfileImage(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ImageUtils.onImagePerResult(this, *grantResults)
    }
}