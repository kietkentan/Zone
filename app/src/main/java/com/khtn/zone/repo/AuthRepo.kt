package com.khtn.zone.repo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.khtn.zone.activity.MainActivity
import com.khtn.zone.utils.LogInFailedState
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import timber.log.Timber
import com.khtn.zone.R
import com.khtn.zone.model.Country
import com.khtn.zone.utils.printMeD
import com.khtn.zone.utils.toast
import com.khtn.zone.viewmodel.ErrorChange
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepo @Inject constructor(
    @ActivityRetainedScoped val actContext: MainActivity,
    @ApplicationContext val context: Context,
    private val auth: FirebaseAuth
): PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    private val verificationId: MutableLiveData<String> = MutableLiveData()
    private val credential: MutableLiveData<PhoneAuthCredential> = MutableLiveData()
    private val taskResult: MutableLiveData<Task<AuthResult>> = MutableLiveData()
    private val failedState: MutableLiveData<LogInFailedState> = MutableLiveData()
    private var errorChange: ErrorChange? = null

    fun setMobile(country: Country, mobile: String) {
        Timber.v("Mobile $mobile")
        val number = country.noCode + " " + mobile
        auth.setLanguageCode(country.code)
        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(actContext)
                .setCallbacks(this)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setListener(listener: ErrorChange) {
        errorChange = listener
    }

    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Timber.v("onVerificationCompleted:$credential")
        this.credential.value = credential
        Handler(Looper.getMainLooper()).postDelayed({
            signInWithPhoneAuthCredential(credential)
        }, 1000)
    }

    override fun onVerificationFailed(exp: FirebaseException) {
        failedState.value = LogInFailedState.Verification
        when (exp) {
            is FirebaseAuthInvalidCredentialsException ->
                context.toast(context.getString(R.string.invalid_request))
            else -> context.toast(exp.message.toString())
        }
    }

    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        Timber.v("onCodeSent:$verificationId")
        this.verificationId.value = verificationId
        context.toast(context.getString(R.string.otp_sent_succesfully))
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.v("signInWithCredential:success")
                    taskResult.value = task
                } else {
                    Timber.v("signInWithCredential:failure ${task.exception}")
                    if (task.exception is FirebaseAuthInvalidCredentialsException)
                        errorChange?.onErrorChange(context.getString(R.string.invalid_verification_code))
                    failedState.value = LogInFailedState.SignIn
                }
            }
    }

    fun setCredential(credential: PhoneAuthCredential) {
        signInWithPhoneAuthCredential(credential)
    }

    fun getVCode(): MutableLiveData<String> {
        return verificationId
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun setVCodeNull() {
        verificationId.value = null
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun clearOldAuth() {
        credential.value = null
        taskResult.value = null
    }

    fun getCredential(): LiveData<PhoneAuthCredential> {
        return credential
    }

    fun getTaskResult(): LiveData<Task<AuthResult>> {
        return taskResult
    }

    fun getFailed(): LiveData<LogInFailedState> {
        return failedState
    }
}