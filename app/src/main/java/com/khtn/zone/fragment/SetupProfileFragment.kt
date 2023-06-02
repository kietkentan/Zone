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
        if (setupProfileViewModel.profilePicUrl.value.isNullOrEmpty() || setupProfileViewModel.progressProPic.value!!) {
            setupProfileViewModel.setError(getString(R.string.error_profile_photo))
            return
        }
        setupProfileViewModel.storeProfileData()
    }

    private fun clickView() {
        binding.ivProfile.setOnClickListener { ImageUtils.askPermission(this) }
        binding.floatingButtonNext.setOnClickListener{ validate() }
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

                is UiState.Success -> {setupProfileViewModel.setProgressSetup(false)}
            }
        }

        setupProfileViewModel.progressSetup.observe(viewLifecycleOwner) {
            if (it) {
                binding.layoutProgress.show()
                this.view?.forEachChildView { it -> it.isEnabled = false  }
            } else {
                binding.layoutProgress.hide()
                this.view?.forEachChildView { it -> it.isEnabled = true  }
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
            imagePath?.let { setupProfileViewModel.uploadProfileImage(it) }
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