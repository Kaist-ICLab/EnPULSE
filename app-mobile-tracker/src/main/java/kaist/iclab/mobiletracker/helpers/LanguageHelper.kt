package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import kaist.iclab.mobiletracker.Constants
import java.util.Locale

/**
 * Helper class for managing app language/locale.
 * Handles saving and applying language preferences.
 */
class LanguageHelper(context: Context) {


    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.Prefs.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save selected language preference
     */
    fun saveLanguage(languageCode: String) {
        sharedPreferences.edit {
            putString(Constants.Prefs.KEY_LANGUAGE, languageCode)
        }
    }

    /**
     * Get saved language preference
     * @return Language code (en or ko), defaults to device language or "en"
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(Constants.Prefs.KEY_LANGUAGE, null) ?: run {
            // If no saved preference, use device language if it's Korean, otherwise English
            val deviceLang = Locale.getDefault().language
            if (deviceLang == "ko") Constants.Language.KOREAN else Constants.Language.ENGLISH
        }
    }

    /**
     * Create a Locale from language code
     */
    private fun createLocale(language: String): Locale {
        return Locale.Builder().setLanguage(language).build()
    }

    /**
     * Apply language to the context and return new context with updated locale
     */
    fun applyLanguage(context: Context): Context {
        val language = getLanguage()
        val locale = createLocale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Apply language to base context for attachBaseContext
     * Used in Application and Activity classes
     */
    fun attachBaseContextWithLanguage(base: Context): Context {
        val language = getLanguage()
        val locale = createLocale(language)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)

        return base.createConfigurationContext(config)
    }

    /**
     * Get language display name
     */
    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            Constants.Language.KOREAN -> "한국어"
            Constants.Language.ENGLISH -> "English"
            else -> "English"
        }
    }

    /**
     * Check if current language is Korean
     */
    fun isKorean(): Boolean {
        return getLanguage() == Constants.Language.KOREAN
    }

    /**
     * Toggle between English and Korean
     */
    fun toggleLanguage(): String {
        val currentLang = getLanguage()
        val newLang = if (currentLang == Constants.Language.KOREAN) {
            Constants.Language.ENGLISH
        } else {
            Constants.Language.KOREAN
        }
        saveLanguage(newLang)
        return newLang
    }
}

