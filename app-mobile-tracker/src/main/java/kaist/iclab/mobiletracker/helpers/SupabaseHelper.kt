package kaist.iclab.mobiletracker.helpers

/**
 * Direct Supabase integration using the supabase-kt library
 * This is a general helper for Supabase operations.
 * For specific data operations, use services in the services package.
 */
import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kaist.iclab.mobiletracker.config.SupabaseConfigManager

class SupabaseHelper(context: Context) {
    private val settings = SharedPreferencesSettings(
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    )

    private val configManager = SupabaseConfigManager(context)

    var supabaseClient: SupabaseClient = buildClient()
        private set

    private fun buildClient(): SupabaseClient {
        val url = configManager.getUrl()
        val anonKey = configManager.getAnonKey()

        val finalUrl = if (url.isBlank() || url.contains("MISSING")) "" else url
        val finalAnonKey = if (anonKey.isBlank() || anonKey.contains("MISSING")) "" else anonKey

        return createSupabaseClient(
            supabaseUrl = finalUrl,
            supabaseKey = finalAnonKey
        ) {
            install(Postgrest)
            install(Realtime)
            install(Auth) {
                // Persist session across app restarts
                sessionManager = io.github.jan.supabase.auth.SettingsSessionManager(settings)
            }
            install(Functions)
        }
    }

    /**
     * Rebuilds the Supabase client using the latest configuration.
     * Useful when the user updates the backend connection details dynamically.
     */
    fun reinitialize() {
        supabaseClient = buildClient()
    }
}
