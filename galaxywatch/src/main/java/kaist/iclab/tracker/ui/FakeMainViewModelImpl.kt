package kaist.iclab.tracker.ui

class FakeMainViewModelImpl : AbstractMainViewModel() {
    companion object {
        const val TAG = "FakeMainViewModel"
    }

    override fun stop() {
        _isRunningState.value = false
    }

    override fun start() {
        _isRunningState.value = true
    }

    override val collectorList: List<String>
        get() = listOf("HR/IBI", "PPG", "ACC", "SkinTemp")


    override fun enable(name: String) {
        _collectorConfigState.value = _collectorConfigState.value.toMutableMap().apply {
            this[name] = true
        }
    }

    override fun disable(name: String) {
        _collectorConfigState.value = _collectorConfigState.value.toMutableMap().apply {
            this[name] = false
        }
    }
}