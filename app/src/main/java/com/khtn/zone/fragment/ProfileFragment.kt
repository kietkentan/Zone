package com.khtn.zone.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.khtn.zone.R
import com.khtn.zone.activity.MainActivity
import com.khtn.zone.databinding.FragmentProfileBinding
import com.khtn.zone.utils.LocaleHelper
import com.khtn.zone.utils.SupportLanguage
import com.khtn.zone.utils.printMeD
import com.khtn.zone.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        observer()
        initView()
        clickView()
    }

    private fun observer() {
        initLanguage()
        initDarkMode()
    }

    private fun clickView() {
        binding.btnChangeLanguage.setOnClickListener {
            "change".printMeD()
            changeLanguage()
        }

        binding.tvLogout.setOnClickListener {
            (requireActivity() as MainActivity).showLogoutAlert()
        }
    }

    private fun initView() {
        binding.headerSingleChatHome.setRightIcon(
            res = R.drawable.ic_search,
            pading = resources.getDimension(R.dimen.dp5).toInt()
        )
    }

    private fun initLanguage() {
        viewModel.setIsEnglish(LocaleHelper.getLanguage(requireContext()) == SupportLanguage.ENGLISH.name)
    }

    private fun initDarkMode() {
        viewModel.setIsDarkMode(false)
    }

    private fun changeLanguage() {
        viewModel.changeLanguage()
        LocaleHelper.setLocale(
            requireContext(),
            if (viewModel.isEnglish.value == true) SupportLanguage.ENGLISH.name else SupportLanguage.VIETNAM.name
        )
        reloadView()
    }

    private fun reloadView() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}