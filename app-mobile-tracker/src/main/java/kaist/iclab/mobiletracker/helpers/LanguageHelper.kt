package kaist.iclab.mobiletracker.helpers

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

/**
 * Helper class for managing app language/locale.
 * Handles saving and applying language preferences.
 */
class LanguageHelper(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_LANGUAGE = "selected_language"
        private const val LANGUAGE_ENGLISH = "en"
        private const val LANGUAGE_KOREAN = "ko"
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Save selected language preference
     */
    fun saveLanguage(language: String) {
        sharedPreferences.edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }
    
    /**
     * Get saved language preference
     * @return Language code (en or ko), defaults to device language or "en"
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE, null) ?: run {
            // If no saved preference, use device language if it's Korean, otherwise English
            val deviceLang = Locale.getDefault().language
            if (deviceLang == "ko") LANGUAGE_KOREAN else LANGUAGE_ENGLISH
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
            LANGUAGE_KOREAN -> "한국어"
            LANGUAGE_ENGLISH -> "English"
            else -> "English"
        }
    }
    
    /**
     * Check if current language is Korean
     */
    fun isKorean(): Boolean {
        return getLanguage() == LANGUAGE_KOREAN
    }
    
    /**
     * Toggle between English and Korean
     */
    fun toggleLanguage(): String {
        val currentLang = getLanguage()
        val newLang = if (currentLang == LANGUAGE_KOREAN) {
            LANGUAGE_ENGLISH
        } else {
            LANGUAGE_KOREAN
        }
        saveLanguage(newLang)
        return newLang
    }
}

