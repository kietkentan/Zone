package com.khtn.zone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.PhoneAuthProvider
import com.khtn.zone.R
import com.khtn.zone.custom.view.OTPInputListener
import com.khtn.zone.databinding.FragmentVerifyBinding
import com.khtn.zone.utils.*
import com.khtn.zone.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyFragment : Fragment() {
    private lateinit var binding: FragmentVerifyBinding
    private val authViewModel: AuthViewModel by activityViewModels()

    private val OTP_LENGHT = 6

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVerifyBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        initView()
        clickView()
    }

    private fun clickView() {
        binding.tvResendOtp.setOnClickListener { authViewModel.resendClicked() }
        binding.otpInput.addListener(object : OTPInputListener {
            override fun onSuccess() {
                validateOtp()
            }
        })
    }

    private fun initView() {
        binding.viewModel = authViewModel
        activity?.showSoftKeyboard(binding.otpInput.listEdtOtp[0])
        if (authViewModel.resendTxt.value.isNullOrEmpty())
            authViewModel.startTimer()
    }

    private fun observer() {
        try {
            authViewModel.getCredential().observe(viewLifecycleOwner) { credential ->
                credential?.let {
                    val otp = credential.smsCode
                    otp?.let { binding.otpInput.setOTPCode(it) }
                    authViewModel.setVerifyProgress(show = true)
                }
            }

            authViewModel.resendTxt.observe(viewLifecycleOwner) {
                binding.tvResendOtp.text = it
            }

            authViewModel.getVerificationId().observe(viewLifecycleOwner) { verifyCode ->
                verifyCode?.let {
                    authViewModel.setVerifyProgress(show = false)
                    authViewModel.setVerifyCodeNull()
                    authViewModel.startTimer()
                }
            }

            authViewModel.verifyProgress.observe(viewLifecycleOwner) {
                if (it) {
                    binding.progressVerify.showView()
                    binding.layoutResendOtp.hideView()
                } else {
                    binding.progressVerify.hideView()
                    binding.layoutResendOtp.showView()
                }
            }

            authViewModel.getFailed().observe(viewLifecycleOwner) {
                authViewModel.setVerifyProgress(show = false)
            }

            authViewModel.getTaskResult().observe(viewLifecycleOwner) { taskId ->
                taskId?.let { authViewModel.fetchUser(it) }
            }

            authViewModel.userProfileGot.observe(viewLifecycleOwner) { success ->
                if (success && findNavController().isValidDestination(R.id.verifyFragment))
                    findNavController().navigate(R.id.action_verifyFragment_to_setupProfileFragment)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun validateOtp() {
        try {
            val otp = binding.otpInput.getOTPCode()
            when {
                otp.length < OTP_LENGHT -> authViewModel.setErrorOTP(getString(R.string.invalid_verification_code))
                Utils.isNoInternet(requireContext()) -> { snackNet(requireActivity()) }
                else -> {
                    val credential = PhoneAuthProvider.getCredential(authViewModel.verifyCode.value.toString(), otp)
                    authViewModel.setCredential(credential)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        authViewModel.clearAll()
        super.onDestroy()
    }
}