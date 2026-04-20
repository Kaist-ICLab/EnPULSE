package kaist.iclab.wearabletracker.ema

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the single-question microEMA survey on the watch.
 *
 * Lifecycle:
 * 1. [startSurvey] → loads config, records triggerTime, starts expiry countdown
 * 2. User answers → [answerQuestion] → records answer + responseTime, stops timer
 * 3. Auto-submits → [submit] → persists response, signals completion
 *
 * Alternative endings:
 * - User dismisses (swipe back) → [dismiss] → logs DISMISSED
 * - Timer expires → [onExpired] → logs EXPIRED
 */
class MicroEmaViewModel(
    private val repository: MicroEmaRepository,
    private val responseManager: MicroEmaResponseManager
) : ViewModel() {

    companion object {
        private const val TAG = "MicroEmaVM"
        private const val COUNTDOWN_TICK_MS = 1000L
    }

    // --- State ---

    private val _surveyConfig = MutableStateFlow<WatchSurveyConfig?>(null)
    val surveyConfig: StateFlow<WatchSurveyConfig?> = _surveyConfig.asStateFlow()

    /** The single question to display (first question from the config). */
    private val _question = MutableStateFlow<WatchQuestion?>(null)
    val question: StateFlow<WatchQuestion?> = _question.asStateFlow()

    /** Whether the survey session has ended (answered, expired, or dismissed). */
    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    /** The final status of this session. */
    private val _finalStatus = MutableStateFlow<ResponseStatus?>(null)
    val finalStatus: StateFlow<ResponseStatus?> = _finalStatus.asStateFlow()

    /** Remaining time in milliseconds before auto-expiry. Null if no expiry configured. */
    private val _remainingTimeMs = MutableStateFlow<Long?>(null)
    val remainingTimeMs: StateFlow<Long?> = _remainingTimeMs.asStateFlow()

    // --- Session metadata ---
    private var triggerTime: Long = 0L
    private var surveyStartTime: Long = 0L
    private var answer: String? = null
    private var responseTime: Long? = null

    private var countdownJob: Job? = null

    // --- Actions ---

    /**
     * Load the survey config and start the session.
     * Call this once when the survey activity is created.
     */
    fun startSurvey() {
        if (_surveyConfig.value != null) return // already started

        triggerTime = System.currentTimeMillis()

        val config = repository.loadSurveyConfig()
        if (config == null) {
            Log.e(TAG, "Failed to load survey config — finishing immediately")
            _isComplete.value = true
            return
        }

        _surveyConfig.value = config

        val selectedQuestion = config.questions.randomOrNull()
        if (selectedQuestion == null) {
            Log.e(TAG, "Survey has no questions — finishing immediately")
            _isComplete.value = true
            return
        }

        _question.value = selectedQuestion
        surveyStartTime = System.currentTimeMillis()

        // Start countdown timer if configured
        config.expireAfterMs?.let { expiryMs ->
            _remainingTimeMs.value = expiryMs
            startCountdown(expiryMs)
        }

        Log.d(TAG, "Survey started: ${config.title}, question: ${selectedQuestion.text}")
    }

    /**
     * Record the user's answer and complete the survey.
     */
    fun answerQuestion(selectedAnswer: String) {
        if (_isComplete.value) return // prevent double-submit

        answer = selectedAnswer
        responseTime = System.currentTimeMillis()
        _finalStatus.value = ResponseStatus.ANSWERED

        stopCountdown()
        submit()
    }

    /**
     * User dismissed the survey (e.g., swiped back).
     */
    fun dismiss() {
        if (_isComplete.value) return

        _finalStatus.value = ResponseStatus.DISMISSED
        stopCountdown()
        submitWithStatus(ResponseStatus.DISMISSED)
    }

    /**
     * Called when the expiry countdown reaches zero.
     */
    private fun onExpired() {
        if (_isComplete.value) return

        _finalStatus.value = ResponseStatus.EXPIRED
        submitWithStatus(ResponseStatus.EXPIRED)
    }

    /**
     * Persist the answered response and signal completion.
     */
    private fun submit() {
        submitWithStatus(ResponseStatus.ANSWERED)
    }

    private fun submitWithStatus(status: ResponseStatus) {
        val config = _surveyConfig.value ?: return
        val q = _question.value ?: return

        val response = MicroEmaResponse(
            surveyId = config.surveyId,
            questionId = q.id,
            answer = if (status == ResponseStatus.ANSWERED) answer else null,
            status = status,
            triggerTime = triggerTime,
            surveyStartTime = surveyStartTime,
            responseTime = if (status == ResponseStatus.ANSWERED) responseTime else null
        )

        viewModelScope.launch {
            try {
                responseManager.saveAndSync(listOf(response))
                Log.d(TAG, "Response persisted: status=$status, answer=$answer")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist response", e)
            }
        }

        _isComplete.value = true
    }

    // --- Countdown ---

    private fun startCountdown(totalMs: Long) {
        countdownJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                _remainingTimeMs.value = remaining
                delay(COUNTDOWN_TICK_MS)
                remaining -= COUNTDOWN_TICK_MS
            }
            _remainingTimeMs.value = 0L
            onExpired()
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCountdown()
    }
}
