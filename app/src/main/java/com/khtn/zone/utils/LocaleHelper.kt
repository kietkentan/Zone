@file:Suppress("DEPRECATION")

package com.khtn.zone.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*
import javax.inject.Inject

object LocaleHelper {
    fun onAttach(context: Context): Context {
        return setLocale(context, getPersistedData(context))
    }

    fun getLanguage(context: Context?): String {
        return getPersistedData(context!!)
    }

    fun setLocale(
        context: Context,
        language: String
    ): Context {
        persistLanguage(context, language)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            updateResources(context, language)
        else
            updateResourcesLegacy(context, language)
    }

    private fun persistLanguage(
        context: Context,
        language: String
    ) {
        SharedPreferencesManager(context = context).saveStringByKey(SharedPrefConstants.LANGUAGE, language)
    }

    private fun getPersistedData(context: Context): String {
        return SharedPreferencesManager(context = context).retrieveStringByKey(SharedPrefConstants.LANGUAGE)
            ?: return SupportLanguage.VIETNAM.name
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(
        context: Context,
        language: String
    ): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(
        context: Context,
        language: String
    ): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration: Configuration = context.resources.configuration
        configuration.locale = locale
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        return context
    }

    fun loadLanguageConfig(context: Context) {
        if (getPersistedData(context) == SupportLanguage.ENGLISH.name) {
            setLocale(context, SupportLanguage.ENGLISH.name)
        } else {
            setLocale(context, SupportLanguage.VIETNAM.name)
        }
    }
}