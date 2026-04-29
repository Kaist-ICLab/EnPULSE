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
import kaist.iclab.mobiletracker.config.AppConfig

class SupabaseHelper(context: Context) {
    private val settings = SharedPreferencesSettings(
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    )

    val supabaseClient: SupabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.SUPABASE_URL,
        supabaseKey = AppConfig.SUPABASE_ANON_KEY
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
