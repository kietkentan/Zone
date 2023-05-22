package com.khtn.zone.viewmodel

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

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepo: AuthRepo,
    private val preference: SharedPreferencesManager,
    private val firestore: FirebaseFirestore
): BaseViewModel() {
    val country = MutableLiveData<Country>()

    val mobile = MutableLiveData<String>()

    val userProfileGot = MutableLiveData<Boolean>()

    private val progress = MutableLiveData(false)

    private val verifyProgress = MutableLiveData(false)

    var canResend: Boolean = false

    val resendTxt = MutableLiveData<String>()

    val otpOne = MutableLiveData<String>()

    val otpTwo = MutableLiveData<String>()

    val otpThree = MutableLiveData<String>()

    val otpFour = MutableLiveData<String>()

    val otpFive = MutableLiveData<String>()

    val otpSix = MutableLiveData<String>()

    var verifyCode: String = ""

    private lateinit var timer: CountDownTimer

    init {
        "LogInViewModel init".printMeD()
    }

    fun setCountry(country: Country) {
        this.country.value = country
    }

    fun setMobile() {
        authRepo.clearOldAuth()
        saveMobile()
        authRepo.setMobile(country.value!!, mobile.value!!)
    }

    fun setProgress(show: Boolean) {
        progress.value = show
    }

    fun getProgress(): LiveData<Boolean> {
        return progress
    }

    fun resendClicked() {
        "Resend Clicked".printMeD()
        if (canResend) {
            setVProgress(true)
            setMobile()
        }
    }

    fun startTimer() {
        try {
            canResend = false
            timer = object : CountDownTimer(60000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    setTimerTxt(millisUntilFinished/1000L)
                }

                override fun onFinish() {
                    canResend = true
                    resendTxt.value = context.getString(R.string.txt_resend)
                }
            }
            timer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetTimer() {
        canResend = false
        resendTxt.value = ""
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
            resendTxt.value = resend
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEmptyText(){
        otpOne.value = ""
        otpTwo.value = ""
        otpThree.value = ""
        otpFour.value = ""
        otpFive.value = ""
        otpSix.value = ""
    }

    fun setVProgress(show: Boolean) {
        verifyProgress.value = show
    }

    fun getVProgress(): LiveData<Boolean> {
        return verifyProgress
    }

    fun getCredential(): LiveData<PhoneAuthCredential> {
        return authRepo.getCredential()
    }

    fun setCredential(credential: PhoneAuthCredential) {
        setVProgress(true)
        authRepo.setCredential(credential)
    }

    fun setVCodeNull(){
        verifyCode = authRepo.getVCode().value!!
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
                setVProgress(false)
                progress.value = false
                if (data.exists()) {
                    val appUser = data.toObject(UserProfile::class.java)
                    Timber.v("UserId ${appUser?.uId}")
                    preference.saveUserProfile(appUser!!)
                    // if device id is not same, send new_user_logged type notification to the token
                    checkLastDevice(appUser)
                }
                userProfileGot.value = true
            }.addOnFailureListener { e ->
                setVProgress(false)
                progress.value = false
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
        userProfileGot.value = false
        authRepo.clearOldAuth()
    }
}