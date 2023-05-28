package com.khtn.zone.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import com.khtn.zone.R
import com.khtn.zone.TYPE_LOGGED_IN
import com.khtn.zone.base.BaseViewModel
import com.khtn.zone.model.Country
import com.khtn.zone.model.ModelMobile
import com.khtn.zone.model.UserProfile
import com.khtn.zone.repo.AuthRepo
import com.khtn.zone.utils.*
import com.khtn.zone.utils.Validator.isValidNo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

interface ErrorChange {
    fun onErrorChange(error: String)
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepo: AuthRepo,
    private val preference: SharedPreferencesManager,
    private val firestore: FirebaseFirestore
): BaseViewModel() {
    init {
        "LogInViewModel init".printMeD()
        authRepo.setListener(object : ErrorChange {
            override fun onErrorChange(error: String) {
                _errorOTP.value = error
            }
        })
    }

    private val _country = MutableLiveData<Country>()
    val country: LiveData<Country>
        get() = _country

    private val _mobile = MutableLiveData<String>()
    val mobile: LiveData<String>
        get() = _mobile

    private val _userProfileGot = MutableLiveData<Boolean>()
    val userProfileGot: LiveData<Boolean>
        get() = _userProfileGot

    private val _progress = MutableLiveData(false)
    val progress: LiveData<Boolean>
        get() = _progress

    private val _verifyProgress = MutableLiveData(false)
    val verifyProgress: LiveData<Boolean>
        get() = _verifyProgress

    private val _canResend = MutableLiveData(false)
    val canResend: LiveData<Boolean>
        get() = _canResend

    private val _resendTxt = MutableLiveData<String>()
    val resendTxt: LiveData<String>
        get() = _resendTxt

    private val _otpCode = MutableLiveData<String>()
    val otpCode: LiveData<String>
        get() = _otpCode

    private val _verifyCode = MutableLiveData<String>()
    val verifyCode: LiveData<String>
        get() = _verifyCode

    private val _errorPhone = MutableLiveData<String>()
    val errorPhone: LiveData<String>
        get() = _errorPhone

    private val _errorOTP = MutableLiveData<String>()
    val errorOTP: LiveData<String>
        get() = _errorOTP

    private lateinit var timer: CountDownTimer

    fun setCountry(country: Country) {
        this._country.value = country
    }

    fun setMobile() {
        authRepo.clearOldAuth()
        saveMobile()
        authRepo.setMobile(_country.value!!, _mobile.value!!)
    }

    fun setProgress(show: Boolean) {
        _progress.value = show
    }

    fun resendClicked() {
        "Resend Clicked".printMeD()
        if (_canResend.value == true) {
            setVerifyProgress(true)
            setEmptyText()
            setMobile()
        }
    }

    fun startTimer() {
        try {
            _canResend.value = false
            timer = object : CountDownTimer(60000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    setTimerTxt(millisUntilFinished/1000L)
                }

                override fun onFinish() {
                    _canResend.value = true
                    _resendTxt.value = context.getString(R.string.txt_resend)
                }
            }
            timer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun resetTimer() {
        _canResend.value = false
        _resendTxt.value = ""
        if (this::timer.isInitialized)
        timer.cancel()
    }

    private fun setTimerTxt(seconds: Long) {
        try {
            val s = seconds % 60
            val m = seconds / 60 % 60
            if (s == 0L && m == 0L) return
            val resend: String =
                context.getString(R.string.txt_resend) + " ${context.getString(R.string.txt_in)} " +
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            m,
                            s
                        )
            _resendTxt.value = resend
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEmptyErrorPhone() {
        _errorPhone.value = ""
    }

    fun setEmptyErrorOTP() {
        _errorOTP.value = ""
    }

    fun setEmptyText() {
        _otpCode.value = ""
    }

    fun setErrorPhone(error: String) {
        _errorPhone.value = error
    }

    fun setErrorOTP(error: String) {
        _errorOTP.value = error
    }

    fun setVerifyProgress(show: Boolean) {
        _verifyProgress.value = show
    }

    fun getCredential(): LiveData<PhoneAuthCredential> {
        return authRepo.getCredential()
    }

    fun setCredential(credential: PhoneAuthCredential) {
        setVerifyProgress(true)
        authRepo.setCredential(credential)
    }

    fun setVerifyCodeNull(){
        _verifyCode.value = authRepo.getVCode().value
        authRepo.setVCodeNull()
    }

    fun getVerificationId(): MutableLiveData<String> {
        return authRepo.getVCode()
    }

    fun getTaskResult(): LiveData<Task<AuthResult>> {
        return authRepo.getTaskResult()
    }

    fun getFailed(): LiveData<LogInFailedState> {
        return authRepo.getFailed()
    }

    private fun saveMobile() =
       preference.saveMobile(ModelMobile(country.value!!.noCode, mobile.value!!))

    fun saveMobile(mobile: String) {
        _mobile.value = mobile
    }

    fun fetchUser(taskId: Task<AuthResult>) {
        val user = taskId.result?.user
        Timber.v("FetchUser:: ${user?.uid}")
        val noteRef = firestore.document("${FireStoreCollection.USER}/" + user?.uid)
        noteRef.get()
            .addOnSuccessListener { data ->
                Timber.v("Uss:: ${preference.getUid()}")
                preference.setUid(user?.uid.toString())
                Timber.v("Uss11:: ${preference.getUid()}")
                preference.setLogin()
                preference.setLogInTime()
                setVerifyProgress(false)
                _progress.value = false
                if (data.exists()) {
                    val appUser = data.toObject(UserProfile::class.java)
                    Timber.v("UserId ${appUser?.uId}")
                    preference.saveUserProfile(appUser!!)
                    // if device id is not same, send new_user_logged type notification to the token
                    checkLastDevice(appUser)
                }
                _userProfileGot.value = true
            }.addOnFailureListener { e ->
                setVerifyProgress(false)
                _progress.value = false
                context.toast(e.message.toString())
            }
    }

    private fun checkLastDevice(appUser: UserProfile?) {
        try {
            if (appUser != null){
                val localDevice = UserUtils.getDeviceId(context)
                val deviceDetails = appUser.deviceDetails
                val sameDevice = deviceDetails?.device_id.equals(localDevice)
                if (!sameDevice)
                    UserUtils.sendPush(context, TYPE_LOGGED_IN,"", appUser.token, appUser.uId!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isValidPhoneNumber(): Boolean {
        return if (country.value?.code.isNullOrEmpty() || mobile.value.isNullOrEmpty()) true
        else isValidNo(country.value!!.code, mobile.value!!)
    }

    fun clearAll(){
        _userProfileGot.value = false
        authRepo.clearOldAuth()
    }
}