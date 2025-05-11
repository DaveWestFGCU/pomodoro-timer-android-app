package com.davewest.pomodorotimer.model

data class PomodoroConfig(
    val workDurationSec: Long = 25 * 60,
    val shortBreakDurationSec: Long = 5 * 60,
    val longBreakDurationSec: Long = 30 * 60,
    val cyclesBeforeLongBreak: Int = 4
)