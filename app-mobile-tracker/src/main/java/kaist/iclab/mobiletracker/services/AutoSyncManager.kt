package kaist.iclab.mobiletracker.services

import android.content.Context

/**
 * Thin, injectable wrapper around [AutoSyncService]'s start/stop lifecycle.
 *
 * Holds the application [Context] (injected via DI) so callers such as ViewModels can start and
 * stop the auto-sync service without keeping a Context reference of their own.
 */
class AutoSyncManager(
    private val context: Context
) {
    fun start() = AutoSyncService.start(context)

    fun stop() = AutoSyncService.stop(context)
}
