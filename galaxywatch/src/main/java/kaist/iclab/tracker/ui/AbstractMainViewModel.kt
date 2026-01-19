package kaist.iclab.tracker.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AbstractMainViewModel : ViewModel() {
    protected val _isRunningState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunningState: StateFlow<Boolean>
        get() = _isRunningState.asStateFlow()

    abstract val collectorList: List<String>
    protected val _collectorConfigState: MutableStateFlow<Map<String, Boolean>> =
        MutableStateFlow(collectorList.associateWith { false })
    val collectorConfigState: StateFlow<Map<String, Boolean>>
        get() = _collectorConfigState.asStateFlow()

    abstract fun start()
    abstract fun stop()
    abstract fun enable(name: String)
    abstract fun disable(name: String)
}