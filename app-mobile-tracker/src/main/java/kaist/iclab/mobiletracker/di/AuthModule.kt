package kaist.iclab.mobiletracker.di

import android.app.Activity
import kaist.iclab.mobiletracker.auth.SupabaseAuth
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.AuthRepository
import kaist.iclab.mobiletracker.repository.AuthRepositoryImpl
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.viewmodels.auth.AuthViewModel
import kaist.iclab.tracker.auth.Authentication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val authModule = module {
    // SupabaseHelper - singleton instance shared across all services
    single {
        SupabaseHelper()
    }

    // AuthRepository - bind interface to implementation
    single<AuthRepository> {
        AuthRepositoryImpl(context = androidContext())
    }

    // SupabaseAuth - factory for creating with Activity and server client ID
    factory { (activity: Activity, serverClientId: String) ->
        SupabaseAuth(
            context = activity,
            clientId = serverClientId,
            supabaseHelper = get<SupabaseHelper>(),
            syncTimestampService = get<SyncTimestampService>()
        ) as Authentication
    }

    // AuthViewModel - factory that creates SupabaseAuth internally
    viewModel { (activity: Activity, serverClientId: String) ->
        val authentication: Authentication =
            get(parameters = { parametersOf(activity, serverClientId) })
        AuthViewModel(
            authentication = authentication,
            authRepository = get<AuthRepository>(),
            userProfileRepository = get<UserProfileRepository>(),
            campaignSensorRepository = get(),
            savedStateHandle = get()
        )
    }
}

