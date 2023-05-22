package com.khtn.zone.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.PhoneAuthProvider
import com.khtn.zone.custom.view.OTPInputListener
import com.khtn.zone.databinding.FragmentVerifyBinding
import com.khtn.zone.utils.Utils
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.showSoftKeyboard
import com.khtn.zone.utils.snackNet
import com.khtn.zone.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyFragment : Fragment() {
    private lateinit var binding: FragmentVerifyBinding
    private val authViewModel: AuthViewModel by activityViewModels()

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
                }
            }

            authViewModel.resendTxt.observe(viewLifecycleOwner) {
                binding.tvResendOtp.text = it
            }

            authViewModel.getVerificationId().observe(viewLifecycleOwner) { veriCode ->
                veriCode?.let {
                    authViewModel.setVCodeNull()
                    authViewModel.startTimer()
                }
            }

            authViewModel.getTaskResult().observe(viewLifecycleOwner) { taskId ->
                taskId?.let { authViewModel.fetchUser(it) }
            }

            authViewModel.userProfileGot.observe(viewLifecycleOwner) { success ->
                /*if (success && findNavController().isValidDestination(R.id.FVerify))
                    findNavController().navigate(R.id.action_FVerify_to_FProfile)*/
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun validateOtp() {
        try {
            val otp = binding.otpInput.getOTPCode()
            when {
                otp.length < 6 -> binding.otpInput.showError()
                Utils.isNoInternet(requireContext()) -> {
                    snackNet(requireActivity())
                }
                else -> {
                    "VCode:: ${authViewModel.verifyCode}".printMeD()
                    "OTP:: $otp".printMeD()
                    val credential = PhoneAuthProvider.getCredential(authViewModel.verifyCode, otp)
                    authViewModel.setCredential(credential)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
/*
        progressView?.dismissIfShowing()
*/
        authViewModel.clearAll()
        super.onDestroy()
    }
}