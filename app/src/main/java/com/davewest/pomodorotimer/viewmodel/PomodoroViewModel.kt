package com.davewest.pomodorotimer.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.davewest.pomodorotimer.model.Phase
import com.davewest.pomodorotimer.model.PomodoroConfig
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Pomodoro timer logic and state.
 */
class PomodoroViewModel (
    private val config: PomodoroConfig = PomodoroConfig()
) : ViewModel() {

    val cyclesBeforeLongBreak: Int = config.cyclesBeforeLongBreak
    private var currentIntervalSec: Long = config.workDurationSec

    // Current phase (Work, Short Break, Long Break)
    private val _phase = MutableStateFlow(Phase.WORK)
    val phase: StateFlow<Phase> = _phase

    // How many work cycles have completed
    private val _cycleCount = MutableStateFlow(0)
    val cycleCount: StateFlow<Int> = _cycleCount

    // Remaining time in milliseconds for current interval
    private val _secondsLeft = MutableStateFlow(config.workDurationSec)
    val secondsLeft: StateFlow<Long> = _secondsLeft

    // Progress fraction [0f..1f] showing elapsed/total
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    // Weather the timer is actively counting down
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    // For notification on interval finish
    private val _finishEvents = MutableSharedFlow<Phase>(replay = 0)
    val finishEvents = _finishEvents.asSharedFlow()

    /**
     * Toggle between starting and pausing the timer.
     */
    fun startPause() {
        if (_isRunning.value) pauseTimer() else startTimer()
    }

    /**
     * Reset all state to initial work phase.
     */
    fun reset() {
        countDownTimer?.cancel()

        _isRunning.value = false
        _phase.value = Phase.WORK
        _cycleCount.value = 0
        _secondsLeft.value = config.workDurationSec
        _progress.value = 0f
    }

    private var countDownTimer: CountDownTimer? = null

    /**
     * Start the timer
     */
    private fun startTimer() {
        _isRunning.value = true
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(currentIntervalSec * 1_000L, 1_000L) {
            override fun onTick(msUntilFinished: Long) {
                _secondsLeft.value = msUntilFinished / 1_000L
                _progress.value = ((currentIntervalSec  - _secondsLeft.value) / currentIntervalSec .toFloat())
            }
            override fun onFinish() {
                onIntervalFinished()
            }
        }.start()
    }

    private fun pauseTimer() {
        _isRunning.value = false
        countDownTimer?.cancel()
    }

    /**
     * Called when a countdown reaches zero: advance to next phase.
     */
    private fun onIntervalFinished() {
        // Advance phase and cycle count
        if (_phase.value == Phase.WORK) {
            _cycleCount.value += 1
            if (_cycleCount.value % config.cyclesBeforeLongBreak == 0) {
                _phase.value = Phase.LONG_BREAK
                _cycleCount.value = 1
            } else {
                _phase.value = Phase.SHORT_BREAK
            }
        } else {
            _phase.value = Phase.WORK
        }

        // fire the event for Compose to pick up:
        viewModelScope.launch {
            _finishEvents.emit(_phase.value)
        }

        // reset timers & restart
        currentIntervalSec = getCurrentIntervalDuration()
        _progress.value = 0f
        startTimer()
    }

    /**
     * Get the duration of the current interval.
     */
    private fun getCurrentIntervalDuration(): Long = when (_phase.value) {
        Phase.WORK -> config.workDurationSec
        Phase.SHORT_BREAK -> config.shortBreakDurationSec
        Phase.LONG_BREAK -> config.longBreakDurationSec
    }
}
