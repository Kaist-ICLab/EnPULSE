package kaist.iclab.tracker.ui

import android.util.Log

class FakeMainViewModelImpl() : AbstractMainViewModel() {
    companion object {
        const val TAG = "FakeMainViewModel"
    }

    override fun tag() {
        Log.d(TAG, "tag")
    }

    override fun export() {
        Log.d(TAG, "export")
    }

    override fun delete() {
        Log.d(TAG, "delete")
    }

    override fun stop() {
        _isRecordingState.value = false
    }

    override fun start() {
        _isRecordingState.value = true
    }
}