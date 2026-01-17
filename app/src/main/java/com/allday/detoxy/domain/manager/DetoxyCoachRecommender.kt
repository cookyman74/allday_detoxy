package com.allday.detoxy.domain.manager

import com.allday.detoxy.R
import com.allday.detoxy.core.utils.UiText
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 디톡시 코치 추천 시스템
 *
 * 위험 지수 변화에 따라 개인화된 행동 제안 제공:
 * - recovery: 유지 격려
 * - warning: 주의 및 개선 제안
 * - high_risk: 즉각적인 개입 제안
 *
 * Week 2B: Task 2B.2
 */
@Singleton
class DetoxyCoachRecommender @Inject constructor() {

    /**
     * 코치 추천 메시지 생성
     *
     * @param riskIndex 위험 지수
     * @param recoveryTrend 회복률 추세
     * @param topDistraction 주요 방해요인 (nullable)
     * @return CoachRecommendation 객체
     */
    fun generateRecommendation(
        riskIndex: DetoxyRiskIndex,
        recoveryTrend: DetoxyRecoveryTrend,
        topDistraction: DistractionItem? = null
    ): CoachRecommendation {
        val messages = when (riskIndex.level) {
            RiskLevel.RECOVERY -> getRecoveryMessages(recoveryTrend)
            RiskLevel.WARNING -> getWarningMessages(riskIndex, topDistraction)
            RiskLevel.HIGH_RISK -> getHighRiskMessages(riskIndex, topDistraction)
        }

        val actionItems = when (riskIndex.level) {
            RiskLevel.RECOVERY -> getRecoveryActions()
            RiskLevel.WARNING -> getWarningActions(topDistraction)
            RiskLevel.HIGH_RISK -> getHighRiskActions(topDistraction)
        }

        return CoachRecommendation(
            level = riskIndex.level,
            title = messages.title,
            message = messages.message,
            actionItems = actionItems,
            priority = getPriority(riskIndex.level)
        )
    }

    /**
     * 회복 단계 메시지
     */
    private fun getRecoveryMessages(recoveryTrend: DetoxyRecoveryTrend): RecommendationMessage {
        val trendMessage = when (recoveryTrend.trend) {
            RecoveryTrendType.IMPROVING -> UiText.StringResource(R.string.coach_recovery_trend_improving)
            RecoveryTrendType.STABLE -> UiText.StringResource(R.string.coach_recovery_trend_stable)
            RecoveryTrendType.DECLINING -> UiText.StringResource(R.string.coach_recovery_trend_declining)
        }

        return RecommendationMessage(
            title = UiText.StringResource(R.string.coach_recovery_title),
            message = UiText.StringResource(R.string.coach_recovery_message, trendMessage) // 주의: message 포맷팅 처리 필요
        )
    }

    /**
     * 주의 단계 메시지
     */
    private fun getWarningMessages(
        @Suppress("UNUSED_PARAMETER") riskIndex: DetoxyRiskIndex,
        topDistraction: DistractionItem?
    ): RecommendationMessage {
        val distractionMessage = if (topDistraction != null) {
            UiText.StringResource(R.string.coach_warning_distraction, topDistraction.name)
        } else {
            UiText.StringResource(R.string.coach_warning_general)
        }

        return RecommendationMessage(
            title = UiText.StringResource(R.string.coach_warning_title),
            message = UiText.StringResource(R.string.coach_warning_message, distractionMessage)
        )
    }

    /**
     * 고위험 단계 메시지
     */
    private fun getHighRiskMessages(
        riskIndex: DetoxyRiskIndex,
        topDistraction: DistractionItem?
    ): RecommendationMessage {
        val failureRateMessage = if (riskIndex.failureRate > 70f) {
            UiText.StringResource(R.string.coach_high_risk_failure_rate, riskIndex.failureRate.toInt())
        } else {
            UiText.StringResource(R.string.coach_high_risk_general)
        }

        val distractionMessage = if (topDistraction != null) {
            UiText.StringResource(R.string.coach_high_risk_distraction, topDistraction.name)
        } else UiText.DynamicString("")

        return RecommendationMessage(
            title = UiText.StringResource(R.string.coach_high_risk_title),
            message = UiText.StringResource(R.string.coach_high_risk_message, failureRateMessage, distractionMessage)
        )
    }

    /**
     * 회복 단계 행동 제안
     */
    private fun getRecoveryActions(): List<ActionItem> {
        return listOf(
            ActionItem(
                id = "maintain_routine",
                title = UiText.StringResource(R.string.action_maintain_routine_title),
                description = UiText.StringResource(R.string.action_maintain_routine_desc),
                actionType = ActionType.MAINTAIN,
                priority = 1
            ),
            ActionItem(
                id = "increase_difficulty",
                title = UiText.StringResource(R.string.action_increase_difficulty_title),
                description = UiText.StringResource(R.string.action_increase_difficulty_desc),
                actionType = ActionType.CHALLENGE,
                priority = 2
            ),
            ActionItem(
                id = "add_category",
                title = UiText.StringResource(R.string.action_add_category_title),
                description = UiText.StringResource(R.string.action_add_category_desc),
                actionType = ActionType.EXPAND,
                priority = 3
            )
        )
    }

    /**
     * 주의 단계 행동 제안
     */
    private fun getWarningActions(topDistraction: DistractionItem?): List<ActionItem> {
        val actions = mutableListOf(
            ActionItem(
                id = "reduce_session_time",
                title = UiText.StringResource(R.string.action_reduce_session_time_title),
                description = UiText.StringResource(R.string.action_reduce_session_time_desc),
                actionType = ActionType.ADJUST,
                priority = 1
            ),
            ActionItem(
                id = "night_mode",
                title = UiText.StringResource(R.string.action_night_mode_title),
                description = UiText.StringResource(R.string.action_night_mode_desc),
                actionType = ActionType.ROUTINE,
                priority = 2
            )
        )

        // 주요 방해요인이 있으면 특화된 제안 추가
        if (topDistraction != null) {
            actions.add(
                ActionItem(
                    id = "focus_on_distraction",
                    title = UiText.StringResource(R.string.action_focus_on_distraction_title, topDistraction.name),
                    description = UiText.StringResource(R.string.action_focus_on_distraction_desc, topDistraction.name),
                    actionType = ActionType.TARGET,
                    priority = 3
                )
            )
        }

        return actions
    }

    /**
     * 고위험 단계 행동 제안
     */
    private fun getHighRiskActions(topDistraction: DistractionItem?): List<ActionItem> {
        val actions = mutableListOf(
            ActionItem(
                id = "emergency_mode",
                title = UiText.StringResource(R.string.action_emergency_mode_title),
                description = UiText.StringResource(R.string.action_emergency_mode_desc),
                actionType = ActionType.EMERGENCY,
                priority = 1
            ),
            ActionItem(
                id = "reset_to_basic",
                title = UiText.StringResource(R.string.action_reset_to_basic_title),
                description = UiText.StringResource(R.string.action_reset_to_basic_desc),
                actionType = ActionType.RESET,
                priority = 2
            ),
            ActionItem(
                id = "seek_support",
                title = UiText.StringResource(R.string.action_seek_support_title),
                description = UiText.StringResource(R.string.action_seek_support_desc),
                actionType = ActionType.SUPPORT,
                priority = 3
            )
        )

        // 주요 방해요인에 대한 긴급 제안
        if (topDistraction != null) {
            actions.add(
                0,  // 최우선 순위
                ActionItem(
                    id = "block_distraction_now",
                    title = UiText.StringResource(R.string.action_block_distraction_now_title, topDistraction.name),
                    description = UiText.StringResource(R.string.action_block_distraction_now_desc, topDistraction.name),
                    actionType = ActionType.IMMEDIATE,
                    priority = 1
                )
            )
        }

        return actions
    }

    /**
     * 우선순위 판단
     */
    private fun getPriority(level: RiskLevel): RecommendationPriority {
        return when (level) {
            RiskLevel.RECOVERY -> RecommendationPriority.LOW
            RiskLevel.WARNING -> RecommendationPriority.MEDIUM
            RiskLevel.HIGH_RISK -> RecommendationPriority.HIGH
        }
    }
}

/**
 * 추천 메시지
 */
private data class RecommendationMessage(
    val title: UiText,
    val message: UiText
)

/**
 * 코치 추천 결과
 */
data class CoachRecommendation(
    val level: RiskLevel,                       // 위험 단계
    val title: UiText,                          // 제목
    val message: UiText,                        // 메시지
    val actionItems: List<ActionItem>,          // 행동 제안 목록
    val priority: RecommendationPriority        // 우선순위
)

/**
 * 행동 제안 항목
 */
data class ActionItem(
    val id: String,                 // 고유 ID
    val title: UiText,              // 제목
    val description: UiText,        // 설명
    val actionType: ActionType,     // 행동 유형
    val priority: Int               // 우선순위 (1이 가장 높음)
)

/**
 * 행동 유형
 */
enum class ActionType {
    MAINTAIN,    // 유지
    CHALLENGE,   // 도전
    EXPAND,      // 확장
    ADJUST,      // 조정
    ROUTINE,     // 루틴
    TARGET,      // 집중
    EMERGENCY,   // 긴급
    RESET,       // 재설정
    SUPPORT,     // 지원
    IMMEDIATE;   // 즉시

    fun toUiText(): UiText = when (this) {
        MAINTAIN -> UiText.StringResource(R.string.action_type_maintain)
        CHALLENGE -> UiText.StringResource(R.string.action_type_challenge)
        EXPAND -> UiText.StringResource(R.string.action_type_expand)
        ADJUST -> UiText.StringResource(R.string.action_type_adjust)
        ROUTINE -> UiText.StringResource(R.string.action_type_routine)
        TARGET -> UiText.StringResource(R.string.action_type_target)
        EMERGENCY -> UiText.StringResource(R.string.action_type_emergency)
        RESET -> UiText.StringResource(R.string.action_type_reset)
        SUPPORT -> UiText.StringResource(R.string.action_type_support)
        IMMEDIATE -> UiText.StringResource(R.string.action_type_immediate)
    }
}

/**
 * 추천 우선순위
 */
enum class RecommendationPriority {
    LOW,     // 낮음 (recovery)
    MEDIUM,  // 중간 (warning)
    HIGH;    // 높음 (high_risk)

    fun toUiText(): UiText = when (this) {
        LOW -> UiText.StringResource(R.string.priority_low)
        MEDIUM -> UiText.StringResource(R.string.priority_medium)
        HIGH -> UiText.StringResource(R.string.priority_high)
    }
}

