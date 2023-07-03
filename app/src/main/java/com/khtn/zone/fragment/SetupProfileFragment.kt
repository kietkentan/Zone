package com.khtn.zone.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.canhub.cropper.CropImage
import com.google.firebase.firestore.CollectionReference
import com.khtn.zone.R
import com.khtn.zone.databinding.FragmentSetupProfileBinding
import com.khtn.zone.di.UserCollection
import com.khtn.zone.model.UserStatus
import com.khtn.zone.utils.*
import com.khtn.zone.utils.Utils.isPermissionOk
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

    @UserCollection
    @Inject
    lateinit var userCollection: CollectionReference

    private val setupProfileViewModel: SetupProfileViewModel by viewModels()

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
        binding.viewModel = setupProfileViewModel

        observer()
        clickView()
    }

    private fun validate() {
        val name = setupProfileViewModel.name.value
        if (name.isNullOrEmpty() || name.length < 2) {
            setupProfileViewModel.setError(getString(R.string.error_name_setup))
            return
        }
        if (setupProfileViewModel.profilePicPath.value.toString().isEmpty() || setupProfileViewModel.progressProPic.value!!) {
            setupProfileViewModel.setError(getString(R.string.error_profile_photo))
            return
        }
        setupProfileViewModel.uploadProfileImage()
    }

    private fun clickView() {
        binding.ivProfile.setOnClickListener {
            ImageUtils.askImageCameraPermission(this)
        }

        binding.floatingButtonNext.setOnClickListener{
            validate()
        }

        binding.edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                setupProfileViewModel.setName(s.toString())
            }
        })
    }

    private fun observer() {
        setupProfileViewModel.profileUpdateState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    setupProfileViewModel.setProgressSetup(true)
                }

                is UiState.Failure -> {
                    setupProfileViewModel.setProgressSetup(false)
                }

                is UiState.Success -> {
                    setupProfileViewModel.setProgressSetup(false)
                    if (findNavController().isValidDestination(R.id.setupProfileFragment)) {
                        findNavController().navigate(R.id.action_setupProfileFragment_to_singleChatHomeFragment)
                    }
                }
            }
        }

        setupProfileViewModel.checkUserNameState.observe(viewLifecycleOwner) {
            when (it) {
                is UiState.Loading -> {
                    setupProfileViewModel.setProgressSetup(true)
                }

                is UiState.Failure -> {
                    setupProfileViewModel.setProgressSetup(false)
                }

                is UiState.Success -> {
                    setupProfileViewModel.setProgressSetup(false)
                }
            }
        }

        setupProfileViewModel.progressSetup.observe(viewLifecycleOwner) { progress ->
            if (progress) {
                binding.layoutProgress.showView()
                this.view?.forEachChildView { it.isEnabled = false  }
            } else {
                binding.layoutProgress.hideView()
                this.view?.forEachChildView { it.isEnabled = true  }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> onCropResult(data)

            ImageUtils.TAKE_PHOTO, ImageUtils.FROM_GALLERY -> ImageUtils.cropImage(context, data, true)

            Utils.REQUEST_APP_SETTINGS -> {
                val isPermissionImageCamera = isPermissionOk(
                    context = requireContext(),
                    permissions = ImageUtils.IMAGE_CAMERA_PERMISSION
                )

                if (isPermissionImageCamera) ImageUtils.showCameraOptions(this)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ImageUtils.REQUEST_IMAGE_CAMERA_PERMISSION) {
            val isPermissionOk = isPermissionOk(*grantResults)

            if (isPermissionOk) ImageUtils.showCameraOptions(this)
            else toast(getString(R.string.permission_error))
        }
    }

    private fun onCropResult(data: Intent?) {
        try {
            val imagePath: Uri? = ImageUtils.getCroppedImage(data)
            imagePath?.let { setupProfileViewModel.setUriProfilePicture(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}