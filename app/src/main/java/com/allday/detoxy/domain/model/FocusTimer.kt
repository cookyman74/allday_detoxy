package com.allday.detoxy.domain.model

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 타이머 상태 열거형
 */
enum class FocusState {
    IDLE,       // 대기 상태
    RUNNING,    // 실행 중
    FINISHED,   // 정상 완료
    FAILED      // 포기/실패
}

/**
 * 집중 타이머 관리 클래스
 *
 * 타이머의 시작, 종료, 포기 기능을 제공하며
 * 실시간으로 남은 시간과 상태를 StateFlow로 제공합니다.
 *
 * @property coroutineScope 코루틴 스코프
 */
class FocusTimer(
    private val coroutineScope: CoroutineScope
) {
    // 타이머 상태
    private val _state = MutableStateFlow(FocusState.IDLE)
    val state: StateFlow<FocusState> = _state.asStateFlow()

    // 남은 시간 (초 단위)
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // 전체 시간 (초 단위)
    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    // 타이머 Job
    private var timerJob: Job? = null

    /**
     * 타이머 시작
     *
     * @param durationMinutes 타이머 시간 (분 단위)
     * @param onFinish 타이머 완료 콜백
     */
    fun start(durationMinutes: Int, onFinish: (Boolean) -> Unit = {}) {
        // 이미 실행 중이면 무시
        if (_state.value == FocusState.RUNNING) return

        val totalSec = durationMinutes * 60
        _totalSeconds.value = totalSec
        _remainingSeconds.value = totalSec
        _state.value = FocusState.RUNNING

        // 타이머 Job 시작
        timerJob = coroutineScope.launch {
            try {
                while (_remainingSeconds.value > 0 && _state.value == FocusState.RUNNING) {
                    delay(1000) // 1초 대기
                    _remainingSeconds.value -= 1
                }

                // 정상 완료
                if (_state.value == FocusState.RUNNING && _remainingSeconds.value == 0) {
                    _state.value = FocusState.FINISHED
                    onFinish(true)
                }
            } catch (e: CancellationException) {
                // Job 취소 시
                throw e
            }
        }
    }

    /**
     * 타이머 정상 완료
     */
    fun finish() {
        if (_state.value != FocusState.RUNNING) return

        timerJob?.cancel()
        _state.value = FocusState.FINISHED
        _remainingSeconds.value = 0
    }

    /**
     * 타이머 포기
     */
    fun giveUp() {
        if (_state.value != FocusState.RUNNING) return

        timerJob?.cancel()
        _state.value = FocusState.FAILED
    }

    /**
     * 타이머 리셋
     */
    fun reset() {
        timerJob?.cancel()
        _state.value = FocusState.IDLE
        _remainingSeconds.value = 0
        _totalSeconds.value = 0
    }

    /**
     * 진행률 계산 (0.0 ~ 1.0)
     */
    fun getProgress(): Float {
        if (_totalSeconds.value == 0) return 0f
        val elapsed = _totalSeconds.value - _remainingSeconds.value
        return elapsed.toFloat() / _totalSeconds.value.toFloat()
    }

    /**
     * 남은 시간을 MM:SS 형식으로 반환
     */
    fun getFormattedTime(): String {
        val minutes = _remainingSeconds.value / 60
        val seconds = _remainingSeconds.value % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
