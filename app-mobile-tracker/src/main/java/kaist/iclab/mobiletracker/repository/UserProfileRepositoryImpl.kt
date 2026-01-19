package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of UserProfileRepository using in-memory StateFlow.
 * Caches user profile data loaded after login for quick access throughout the app.
 */
class UserProfileRepositoryImpl : UserProfileRepository {
    private val _profile = MutableStateFlow<ProfileData?>(null)
    override val profileFlow: StateFlow<ProfileData?> = _profile.asStateFlow()

    override fun saveProfile(profile: ProfileData) {
        _profile.value = profile
    }

    override fun clearProfile() {
        _profile.value = null
    }
}

