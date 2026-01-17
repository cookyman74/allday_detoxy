package com.allday.detoxy.domain.manager

import com.allday.detoxy.R
import com.allday.detoxy.core.utils.UiText

/**
 * 디톡시 위험 단계
 *
 * 위험 지수(0-100)를 3단계로 분류
 *
 * Week 2B: Task 2B.3.4 - RiskLevel enum 분리
 */
enum class RiskLevel {
    RECOVERY,   // 회복: 0-33
    WARNING,    // 주의: 34-66
    HIGH_RISK;  // 고위험: 67-100

    /**
     * 사용자에게 표시할 텍스트
     */
    fun toUiText(): UiText = when (this) {
        RECOVERY -> UiText.StringResource(R.string.risk_level_recovery)
        WARNING -> UiText.StringResource(R.string.risk_level_warning)
        HIGH_RISK -> UiText.StringResource(R.string.risk_level_high_risk)
    }

    /**
     * Analytics 이벤트에 사용할 문자열
     */
    fun toAnalyticsString(): String = when (this) {
        RECOVERY -> "recovery"
        WARNING -> "warning"
        HIGH_RISK -> "high_risk"
    }
}

