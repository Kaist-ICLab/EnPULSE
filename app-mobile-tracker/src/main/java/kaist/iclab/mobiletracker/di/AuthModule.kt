package kaist.iclab.mobiletracker.di

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
import org.koin.dsl.module

val authModule = module {
    // SupabaseHelper - singleton instance shared across all services
    single {
        SupabaseHelper(context = androidContext())
    }

    // AuthRepository - bind interface to implementation
    single<AuthRepository> {
        AuthRepositoryImpl(context = androidContext())
    }

    single<Authentication> {
        SupabaseAuth(
            supabaseHelper = get<SupabaseHelper>(),
            syncTimestampService = get<SyncTimestampService>()
        )
    }

    viewModel {
        AuthViewModel(
            authentication = get<Authentication>(),
            authRepository = get<AuthRepository>(),
            userProfileRepository = get<UserProfileRepository>(),
            campaignSensorRepository = get(),
            savedStateHandle = get()
        )
    }
}
