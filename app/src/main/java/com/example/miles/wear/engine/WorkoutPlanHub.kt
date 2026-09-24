package com.example.miles.wear.engine

import com.example.miles.wear.data.model.IntervalPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared live state for goal/interval workouts, written by the tracking
 * service and read by the active-workout HUD screen.
 */
object WorkoutPlanHub {
    private val _goalProgress = MutableStateFlow(0f)
    val goalProgress: StateFlow<Float> = _goalProgress.asStateFlow()

    private val _intervalPhase = MutableStateFlow<IntervalPhase?>(null)
    val intervalPhase: StateFlow<IntervalPhase?> = _intervalPhase.asStateFlow()

    private val _intervalDone = MutableStateFlow(false)
    val intervalDone: StateFlow<Boolean> = _intervalDone.asStateFlow()

    fun reset(progress: Float, phase: IntervalPhase?) {
        _goalProgress.value = progress
        _intervalPhase.value = phase
        _intervalDone.value = false
    }

    fun setGoalProgress(value: Float) {
        _goalProgress.value = value.coerceIn(0f, 1f)
    }

    fun setIntervalPhase(value: IntervalPhase?) {
        _intervalPhase.value = value
        if (value == null) {
            _intervalDone.value = true
        }
    }
}