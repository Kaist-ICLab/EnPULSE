package kaist.iclab.tracker.listener

import android.content.Context
import com.samsung.android.sdk.health.data.HealthDataService

class SamsungHealthDataInitializer(
    context: Context,
) {
    val store by lazy {
        HealthDataService.getStore(context)
    }
}