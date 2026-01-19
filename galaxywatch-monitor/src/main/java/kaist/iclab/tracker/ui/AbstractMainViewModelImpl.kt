package kaist.iclab.tracker.ui

import android.util.Log
import androidx.lifecycle.viewModelScope
import kaist.iclab.tracker.controller.CollectorController
import kaist.iclab.tracker.database.DatabaseInterface
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class MainViewModelImpl(
    val database: DatabaseInterface,
    val collectorController: CollectorController
) : AbstractMainViewModel() {
    init {
        viewModelScope.launch {
            Log.d(TAG, "INITIALIZED")
            collectorController.isRunningFlow().collect {
                Log.d(TAG, "isRunningState: $it")
                _isRecordingState.value = it
            }
        }
    }

    var timer: Job? = null

    companion object {
        const val TAG = "RealMainViewModel"
    }

    override fun tag() {
        Log.d(TAG, "tag")
        database.insert(
            "TAG", mapOf(
                "timestamp" to System.currentTimeMillis(),
                "tag" to ""
            )
        )
    }

    override fun export() {
        Log.d(TAG, "export")
    }

    override fun delete() {
        Log.d(TAG, "delete")
    }

    override fun stop() {
        collectorController.stop()
        timer?.cancel()
        _lapsedTime.value = 0
        timer = null
    }

    override fun start() {
        collectorController.start()
        timer = viewModelScope.launch {
            sleep(1000)
            _lapsedTime.value += 1
        }
    }
}