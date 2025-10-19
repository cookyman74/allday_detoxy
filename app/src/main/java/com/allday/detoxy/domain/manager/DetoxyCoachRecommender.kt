package com.allday.detoxy.domain.manager

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
            RecoveryTrendType.IMPROVING -> "회복률이 계속 상승하고 있어요! 🎉"
            RecoveryTrendType.STABLE -> "안정적인 디톡시를 유지하고 있어요."
            RecoveryTrendType.DECLINING -> "최근 회복률이 다소 하락했지만, 여전히 좋은 상태입니다."
        }

        return RecommendationMessage(
            title = "훌륭해요! 디톡시가 잘 진행되고 있습니다 ✨",
            message = "$trendMessage\n현재의 습관을 유지하면서 조금씩 난이도를 높여보는 것은 어떨까요?"
        )
    }

    /**
     * 주의 단계 메시지
     */
    private fun getWarningMessages(
        riskIndex: DetoxyRiskIndex,
        topDistraction: DistractionItem?
    ): RecommendationMessage {
        val distractionMessage = if (topDistraction != null) {
            "${topDistraction.name} 카테고리가 가장 큰 방해 요인입니다."
        } else {
            "주의가 필요한 상황입니다."
        }

        return RecommendationMessage(
            title = "주의가 필요해요 ⚠️",
            message = "$distractionMessage\n" +
                    "잠시 페이스를 조정하고, 차단 시간을 늘려보는 것을 추천드려요."
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
            "최근 실패율이 ${riskIndex.failureRate.toInt()}%로 매우 높습니다."
        } else {
            "디톡시에 어려움을 겪고 있는 것 같아요."
        }

        val distractionMessage = if (topDistraction != null) {
            "\n특히 ${topDistraction.name}이(가) 주요 방해 요인입니다."
        } else ""

        return RecommendationMessage(
            title = "즉각적인 개입이 필요합니다 🚨",
            message = "$failureRateMessage$distractionMessage\n" +
                    "디톡시 강도를 일시적으로 낮추거나, 야간 차단 시간을 설정해 보세요."
        )
    }

    /**
     * 회복 단계 행동 제안
     */
    private fun getRecoveryActions(): List<ActionItem> {
        return listOf(
            ActionItem(
                id = "maintain_routine",
                title = "현재 루틴 유지하기",
                description = "지금의 습관을 꾸준히 이어가세요",
                actionType = ActionType.MAINTAIN,
                priority = 1
            ),
            ActionItem(
                id = "increase_difficulty",
                title = "난이도 높이기",
                description = "차단 시간을 10분 늘려보세요",
                actionType = ActionType.CHALLENGE,
                priority = 2
            ),
            ActionItem(
                id = "add_category",
                title = "새로운 카테고리 차단 시도",
                description = "한 가지 카테고리를 추가로 차단해 보세요",
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
                title = "세션 시간 조정하기",
                description = "일시적으로 세션 시간을 줄여보세요 (예: 25분 → 15분)",
                actionType = ActionType.ADJUST,
                priority = 1
            ),
            ActionItem(
                id = "night_mode",
                title = "야간 차단 시간 설정",
                description = "22시~07시 SNS 차단 루틴을 추가하세요",
                actionType = ActionType.ROUTINE,
                priority = 2
            )
        )

        // 주요 방해요인이 있으면 특화된 제안 추가
        if (topDistraction != null) {
            actions.add(
                ActionItem(
                    id = "focus_on_distraction",
                    title = "${topDistraction.name} 집중 차단",
                    description = "${topDistraction.name} 카테고리를 우선적으로 차단하세요",
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
                title = "긴급 디톡시 모드",
                description = "24시간 전체 카테고리 차단을 시도해 보세요",
                actionType = ActionType.EMERGENCY,
                priority = 1
            ),
            ActionItem(
                id = "reset_to_basic",
                title = "기본 설정으로 되돌리기",
                description = "너무 어려운 목표를 설정했을 수 있어요. 5분부터 다시 시작해 보세요",
                actionType = ActionType.RESET,
                priority = 2
            ),
            ActionItem(
                id = "seek_support",
                title = "지원 요청하기",
                description = "가족이나 친구에게 도움을 요청하는 것도 좋은 방법입니다",
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
                    title = "${topDistraction.name} 즉시 차단",
                    description = "당장 ${topDistraction.name} 앱을 제거하거나 24시간 차단하세요",
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
    val title: String,
    val message: String
)

/**
 * 코치 추천 결과
 */
data class CoachRecommendation(
    val level: RiskLevel,                       // 위험 단계
    val title: String,                          // 제목
    val message: String,                        // 메시지
    val actionItems: List<ActionItem>,          // 행동 제안 목록
    val priority: RecommendationPriority        // 우선순위
)

/**
 * 행동 제안 항목
 */
data class ActionItem(
    val id: String,                 // 고유 ID
    val title: String,              // 제목
    val description: String,        // 설명
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

    fun toDisplayString(): String = when (this) {
        MAINTAIN -> "유지"
        CHALLENGE -> "도전"
        EXPAND -> "확장"
        ADJUST -> "조정"
        ROUTINE -> "루틴"
        TARGET -> "집중"
        EMERGENCY -> "긴급"
        RESET -> "재설정"
        SUPPORT -> "지원"
        IMMEDIATE -> "즉시"
    }
}

/**
 * 추천 우선순위
 */
enum class RecommendationPriority {
    LOW,     // 낮음 (recovery)
    MEDIUM,  // 중간 (warning)
    HIGH;    // 높음 (high_risk)

    fun toDisplayString(): String = when (this) {
        LOW -> "권장"
        MEDIUM -> "주의"
        HIGH -> "긴급"
    }
}

