package com.khtn.zone.fragment

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.khtn.zone.R
import com.khtn.zone.custom.dialog.DialogCountrySheet
import com.khtn.zone.custom.dialog.SheetCountriesListener
import com.khtn.zone.databinding.FragmentLoginBinding
import com.khtn.zone.model.Country
import com.khtn.zone.utils.*
import com.khtn.zone.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment: Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        initView()
        clickView()
    }

    private fun observer() {
        try {
            authViewModel.country.observe(viewLifecycleOwner) {
                binding.textInputPhone.noCode = it.noCode
            }

            authViewModel.getVerificationId().observe(viewLifecycleOwner) { vCode ->
                vCode?.let {
                    authViewModel.setProgress(false)
                    authViewModel.resetTimer()
                    authViewModel.setVCodeNull()
                    authViewModel.setEmptyText()
                    if (findNavController().isValidDestination(R.id.loginFragment))
                        findNavController().navigate(R.id.action_loginFragment_to_verifyFragment)
                }
            }

            authViewModel.getProgress().observe(viewLifecycleOwner) {
                binding.progressGetOtp.visibility = if (it) View.VISIBLE else View.GONE
                binding.btnGetOtp.visibility = if (it) View.GONE else View.VISIBLE
            }

            authViewModel.getTaskResult().observe(viewLifecycleOwner) { taskId ->
                if (taskId != null && authViewModel.getCredential().value?.smsCode.isNullOrEmpty())
                    authViewModel.fetchUser(taskId)
            }

            authViewModel.userProfileGot.observe(viewLifecycleOwner) { success ->
                /*if (success && authViewModel.getCredential().value?.smsCode.isNullOrEmpty()
                    && findNavController().isValidDestination(R.id.FLogIn)
                ) {
                    requireActivity().toastLong("Authenticated successfully using Instant verification")
                    findNavController().navigate(R.id.action_FLogIn_to_FProfile)
                }*/
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestOTP() {
        findNavController().navigate(R.id.action_loginFragment_to_verifyFragment)
    }

    private fun openDialogSelectNoCode() {
        val builder = DialogCountrySheet.newInstance(Bundle())
        builder.addListener(object: SheetCountriesListener {
            override fun selectedItem(country: Country) {
                authViewModel.setCountry(country)
                builder.dialog?.dismiss()
            }
        })
        builder.show(this.childFragmentManager, "")
    }

    private fun clickView() {
        binding.textInputPhone.onLeftClickListener {
            openDialogSelectNoCode()
        }

        binding.btnGetOtp.setOnClickListener {
            validate()
        }

        binding.textInputPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                authViewModel.mobile.value = s.toString()
            }
        })

        binding.textInputPhone.onKeyListener { _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                activity?.closeKeyBoard()
                binding.textInputPhone.editText.clearFocus()
                binding.tvErrorPhoneInput.visibility =
                    if (authViewModel.isValidPhoneNumber()) View.GONE else View.VISIBLE
                return@onKeyListener true
            }
            return@onKeyListener false
        }
    }

    private fun validate() {
        try {
            activity?.closeKeyBoard()
            val mobileNo = authViewModel.mobile.value?.trim()
            val country = authViewModel.country.value
            when {
                Validator.isMobileNumberEmpty(mobileNo) -> snack(requireActivity(), "Enter valid mobile number")
                country == null -> snack(requireActivity(), "Select a country")
                !Validator.isValidNo(country.code, mobileNo!!) -> snack(
                    requireActivity(),
                    "Enter valid mobile number"
                )
                Utils.isNoInternet(requireContext()) -> snackNet(requireActivity())
                else -> {
                    authViewModel.setMobile()
                    authViewModel.setProgress(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDefaultCountry() {
        try {
            var country = Utils.getDefaultCountry()
            val manager =
                requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as (TelephonyManager)?
            manager?.let {
                val countryCode = Utils.clearNull(manager.networkCountryIso)
                if (countryCode.isEmpty())
                    return
                val countries = Countries.getCountries()
                for (i in countries) {
                    if (i.code.equals(countryCode, true))
                        country = i
                }
                authViewModel.setCountry(country)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initView() {
        setDefaultCountry()
    }
}