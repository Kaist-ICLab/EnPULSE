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

class SupabaseHelper(private val context: Context) {
    private val settings = SharedPreferencesSettings(
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    )
    
    private val configManager = SupabaseConfigManager(context)

    var supabaseClient: SupabaseClient = buildClient()
        private set

    private fun buildClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = configManager.getUrl(),
            supabaseKey = configManager.getAnonKey()
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
