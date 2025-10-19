package com.allday.detoxy.domain.manager

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
    fun toDisplayString(): String = when (this) {
        RECOVERY -> "회복 중"
        WARNING -> "주의 필요"
        HIGH_RISK -> "고위험"
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

