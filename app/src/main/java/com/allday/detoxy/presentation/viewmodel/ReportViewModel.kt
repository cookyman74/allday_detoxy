package com.allday.detoxy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.repository.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: FocusRepository
) : ViewModel() {

    // 오늘 세션 목록
    private val _todaySessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val todaySessions: StateFlow<List<FocusSession>> = _todaySessions.asStateFlow()

    // 사용자 설정 (포인트, 스트릭)
    private val _settings = MutableStateFlow(
        UserSettings(
            id = 1,
            totalPoints = 0,
            currentStreak = 0,
            lastSuccessDate = null
        )
    )
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // 오늘 세션 로드
            repository.getTodaySessions().collect { sessions ->
                _todaySessions.value = sessions
            }
        }

        viewModelScope.launch {
            // 사용자 설정 로드
            repository.getSettings().collect { userSettings ->
                userSettings?.let {
                    _settings.value = it
                }
            }

            _isLoading.value = false
        }
    }

    // 통계 계산 함수들
    fun getSuccessSessionCount(): Int {
        return _todaySessions.value.count { it.success }
    }

    fun getTotalFocusMinutes(): Int {
        return _todaySessions.value
            .filter { it.success }
            .sumOf { it.durationMinutes }
    }

    fun getFailedSessionCount(): Int {
        return _todaySessions.value.count { !it.success }
    }

    fun getSuccessRate(): Float {
        val total = _todaySessions.value.size
        if (total == 0) return 0f
        return (getSuccessSessionCount().toFloat() / total) * 100
    }
}