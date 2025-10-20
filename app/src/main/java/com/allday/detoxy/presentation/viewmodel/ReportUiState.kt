package com.allday.detoxy.presentation.viewmodel

import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.manager.CoachRecommendation
import com.allday.detoxy.domain.manager.DetoxyRecoveryTrend
import com.allday.detoxy.domain.manager.DetoxyRiskIndex
import com.allday.detoxy.domain.manager.DistractionItem
import com.allday.detoxy.domain.manager.GiveUpPointAnalysis
import com.allday.detoxy.domain.manager.ResistanceTimeAnalysis

/**
 * 리포트 화면 UI 상태
 *
 * Week 2B: Task 2B.3.1
 */
data class ReportUiState(
    // 기본 데이터
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 기존 일간 데이터
    val todaySessions: List<FocusSession> = emptyList(),
    val settings: UserSettings = UserSettings(
        id = 1,
        totalPoints = 0,
        currentStreak = 0,
        lastSuccessDate = null
    ),
    
    // 신규 고급 통계 (Week 2B)
    val riskIndex: DetoxyRiskIndex? = null,
    val recoveryTrend: DetoxyRecoveryTrend? = null,
    val topDistractions: List<DistractionItem> = emptyList(),
    val resistanceAnalysis: ResistanceTimeAnalysis? = null,
    val giveUpAnalysis: GiveUpPointAnalysis? = null,
    val coachRecommendation: CoachRecommendation? = null,
    
    // 빈 상태 플래그
    val hasData: Boolean = false
) {
    /**
     * 기본 통계 계산
     * 
     * Bug Fix (2025-10-20): 실제 집중 시간을 계산하도록 수정
     */
    fun getSuccessSessionCount(): Int = todaySessions.count { it.success }
    
    fun getTotalFocusMinutes(): Int = todaySessions
        .filter { it.endTime != null }  // 종료된 세션만
        .sumOf { 
            // 실제 집중 시간 = (종료 시간 - 시작 시간) / 1000ms / 60s
            val actualMinutes = ((it.endTime!! - it.startTime) / 1000 / 60).toInt()
            actualMinutes
        }
    
    fun getFailedSessionCount(): Int = todaySessions.count { !it.success }
    
    fun getSuccessRate(): Float {
        val total = todaySessions.size
        if (total == 0) return 0f
        return (getSuccessSessionCount().toFloat() / total) * 100
    }
}

