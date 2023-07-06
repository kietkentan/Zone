package com.khtn.zone.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.khtn.zone.R
import com.khtn.zone.adapter.OnBoardingAdapter
import com.khtn.zone.databinding.ActivityOnBoardingBinding
import com.khtn.zone.model.OnBoardingItems
import com.khtn.zone.utils.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "OnBoardingActivity"

@AndroidEntryPoint
class OnBoardingActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityOnBoardingBinding
    private val list: List<OnBoardingItems>

    @Inject
    lateinit var eventGA: EventGAImp

    private val START_PAGE = 0
    private val END_PAGE: Int
    private var previousState =  ViewPager2.SCROLL_STATE_IDLE
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadLanguageConfig()
        this.transparentStatusBar(isLightBackground = true)

        binding.viewPagerOnboard.adapter = OnBoardingAdapter(list, supportFragmentManager, lifecycle)
        binding.circleIndicator.setViewPager(binding.viewPagerOnboard)

        binding.viewPagerOnboard.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        initView()
        actionView()
    }

    init {
        this.title = TAG
        list = OnBoardingItems.getData()
        END_PAGE = list.size - 1
    }

    private fun initView() {
        val str = intent.getStringExtra("state_app") ?: ""
        val pos = intent.getIntExtra("position_app", 0)

        currentPage = if (pos > START_PAGE) END_PAGE else if (str == "NORMAL") END_PAGE else START_PAGE
        setPageSelect(currentPage)
        checkLanguage()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun actionView() {
        for (i in START_PAGE..END_PAGE) {
            binding.circleIndicator[i].setOnClickListener {
                setPageSelect(i)
            }
        }

        binding.btnOnboardingLogin.setOnClickListener(this)
        binding.tvOnboardingSettingVietnamese.setOnClickListener(this)
        binding.tvOnboardingSettingEnglish.setOnClickListener(this)

        binding.viewPagerOnboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
            }

            @Suppress("KotlinConstantConditions")
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                if ((currentPage >= END_PAGE)// end of list. these checks can be
                    // used individually to detect end or start of pages
                    && previousState == ViewPager2.SCROLL_STATE_DRAGGING // from    DRAGGING
                    && state == ViewPager2.SCROLL_STATE_IDLE) {          // to      IDLE
                    //overscroll performed. work here
                    setPageSelect(START_PAGE)
                }
                if ((currentPage <= START_PAGE)// end of list. these checks can be
                    // used individually to detect end or start of pages
                    && previousState == ViewPager2.SCROLL_STATE_DRAGGING // from    DRAGGING
                    && state == ViewPager2.SCROLL_STATE_IDLE) {          // to      IDLE
                    //overscroll performed. work here
                    setPageSelect(END_PAGE)
                }
                previousState = state
            }
        })
    }

    private fun setPageSelect(position: Int) {
        binding.viewPagerOnboard.setCurrentItem(position, true)
    }

    private fun onLogin() {
        eventGA.eventAuth("login_user" /* custom event GA */, "item")
        startActivity(Intent(this@OnBoardingActivity, MainActivity::class.java))
        finish()
    }

    private fun loadLanguageConfig() {
        LocaleHelper.loadLanguageConfig(this)
    }

    private fun checkLanguage() {
        val lang = LocaleHelper.getLanguage(this@OnBoardingActivity)
        if (lang == SupportLanguage.VIETNAM.name) {
            binding.tvOnboardingSettingVietnamese.setTextColor(ContextCompat.getColor(baseContext, R.color.green_220))
            binding.tvOnboardingSettingEnglish.setTextColor(ContextCompat.getColor(baseContext, R.color.grey_160))
            binding.viewOnboardingVietnamese.visibility = View.VISIBLE
            binding.viewOnboardingEnglish.visibility = View.GONE
            binding.tvOnboardingSettingVietnamese.disable()
            binding.tvOnboardingSettingEnglish.enabled()
        } else {
            binding.tvOnboardingSettingVietnamese.setTextColor(ContextCompat.getColor(baseContext, R.color.grey_160))
            binding.tvOnboardingSettingEnglish.setTextColor(ContextCompat.getColor(baseContext, R.color.green_220))
            binding.viewOnboardingVietnamese.visibility = View.GONE
            binding.viewOnboardingEnglish.visibility = View.VISIBLE
            binding.tvOnboardingSettingVietnamese.enabled()
            binding.viewOnboardingEnglish.disable()
        }
    }

    private fun changeLanguage(lang: String) {
        LocaleHelper.setLocale(this, lang)
        finish()
        startActivity(intent.putExtra("position_app", currentPage + 1))
        overridePendingTransition(0, 0)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_onboarding_login -> onLogin()
            R.id.tv_onboarding_setting_vietnamese -> changeLanguage(SupportLanguage.VIETNAM.name)
            R.id.tv_onboarding_setting_english -> changeLanguage(SupportLanguage.ENGLISH.name)
        }
    }
}
