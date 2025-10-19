package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 집중 방해요인 분석기
 *
 * FocusInterruption 데이터를 기반으로:
 * - 방해요인 Top 3 (카테고리별, 앱별)
 * - 유혹 저항 시간 분석
 * - 포기 지점 분석
 *
 * Week 2B: Task 2B.2
 */
@Singleton
class FocusInterruptionAnalyzer @Inject constructor() {

    /**
     * 방해요인 Top 3 분석 (카테고리별)
     *
     * @param interruptions 차단 이벤트 리스트
     * @return 카테고리별 차단 횟수 Top 3
     */
    fun getTopDistractionCategories(
        interruptions: List<FocusInterruption>,
        limit: Int = 3
    ): List<DistractionItem> {
        return interruptions
            .groupBy { it.category }
            .map { (category, events) ->
                DistractionItem(
                    name = category,
                    count = events.size,
                    percentage = (events.size.toFloat() / interruptions.size) * 100
                )
            }
            .sortedByDescending { it.count }
            .take(limit)
    }

    /**
     * 방해요인 Top 3 분석 (앱별)
     *
     * @param interruptions 차단 이벤트 리스트
     * @param packageNameMapper 패키지명을 앱 이름으로 변환하는 함수 (nullable)
     * @return 앱별 차단 횟수 Top 3
     */
    fun getTopDistractionApps(
        interruptions: List<FocusInterruption>,
        packageNameMapper: ((String) -> String)? = null,
        limit: Int = 3
    ): List<DistractionItem> {
        return interruptions
            .groupBy { it.packageName }
            .map { (packageName, events) ->
                DistractionItem(
                    name = packageNameMapper?.invoke(packageName) ?: packageName,
                    count = events.size,
                    percentage = (events.size.toFloat() / interruptions.size) * 100
                )
            }
            .sortedByDescending { it.count }
            .take(limit)
    }

    /**
     * 유혹 저항 시간 분석
     *
     * 성공/실패 세션별 평균 지속 시간
     *
     * @param sessions FocusSession 리스트
     * @return ResistanceTimeAnalysis 객체
     */
    fun analyzeResistanceTime(sessions: List<FocusSession>): ResistanceTimeAnalysis {
        val successfulSessions = sessions.filter { it.success }
        val failedSessions = sessions.filter { !it.success }

        val avgSuccessTime = if (successfulSessions.isNotEmpty()) {
            successfulSessions.map { it.durationMinutes * 60 }.average().toInt()
        } else 0

        val avgFailureTime = if (failedSessions.isNotEmpty()) {
            failedSessions.map { it.interruptedSeconds }.average().toInt()
        } else 0

        // 성공률
        val successRate = if (sessions.isNotEmpty()) {
            (successfulSessions.size.toFloat() / sessions.size) * 100
        } else 0f

        return ResistanceTimeAnalysis(
            avgSuccessTimeSeconds = avgSuccessTime,
            avgFailureTimeSeconds = avgFailureTime,
            successRate = successRate,
            totalSessions = sessions.size
        )
    }

    /**
     * 포기 지점 분석
     *
     * 실패 세션에서 몇 %에서 포기했는지 분석
     *
     * @param sessions FocusSession 리스트
     * @return GiveUpPointAnalysis 객체
     */
    fun analyzeGiveUpPoint(sessions: List<FocusSession>): GiveUpPointAnalysis {
        val failedSessions = sessions.filter { !it.success && it.interruptedSeconds > 0 }

        if (failedSessions.isEmpty()) {
            return GiveUpPointAnalysis(
                avgGiveUpPercentage = 0f,
                earlyGiveUpCount = 0,    // 0-25%
                midGiveUpCount = 0,       // 25-75%
                lateGiveUpCount = 0,      // 75-100%
                totalFailures = 0
            )
        }

        var earlyGiveUpCount = 0
        var midGiveUpCount = 0
        var lateGiveUpCount = 0

        val giveUpPercentages = failedSessions.map { session ->
            val targetSeconds = session.durationMinutes * 60
            val giveUpPercentage = (session.interruptedSeconds.toFloat() / targetSeconds) * 100

            // 포기 시점 분류
            when {
                giveUpPercentage < 25f -> earlyGiveUpCount++
                giveUpPercentage < 75f -> midGiveUpCount++
                else -> lateGiveUpCount++
            }

            giveUpPercentage
        }

        val avgGiveUpPercentage = giveUpPercentages.average().toFloat()

        return GiveUpPointAnalysis(
            avgGiveUpPercentage = avgGiveUpPercentage,
            earlyGiveUpCount = earlyGiveUpCount,
            midGiveUpCount = midGiveUpCount,
            lateGiveUpCount = lateGiveUpCount,
            totalFailures = failedSessions.size
        )
    }

    /**
     * 세션별 평균 차단 이벤트 수 계산
     *
     * @param sessions 세션 리스트
     * @param interruptions 차단 이벤트 리스트
     * @return 세션당 평균 차단 이벤트 수
     */
    fun calculateAvgInterruptionsPerSession(
        sessions: List<FocusSession>,
        interruptions: List<FocusInterruption>
    ): Float {
        if (sessions.isEmpty()) return 0f

        // 세션 ID별 차단 이벤트 그룹화
        val interruptionsBySession = interruptions.groupBy { it.sessionId }

        // 각 세션의 차단 이벤트 수
        val interruptionCounts = sessions.map { session ->
            interruptionsBySession[session.id]?.size ?: 0
        }

        return interruptionCounts.average().toFloat()
    }
}

/**
 * 방해요인 항목 (카테고리 또는 앱)
 */
data class DistractionItem(
    val name: String,       // 카테고리명 또는 앱명
    val count: Int,         // 차단 횟수
    val percentage: Float   // 전체 대비 비율 (%)
)

/**
 * 유혹 저항 시간 분석 결과
 */
data class ResistanceTimeAnalysis(
    val avgSuccessTimeSeconds: Int,  // 성공 세션 평균 지속 시간 (초)
    val avgFailureTimeSeconds: Int,  // 실패 세션 평균 지속 시간 (초)
    val successRate: Float,          // 성공률 (%)
    val totalSessions: Int           // 총 세션 수
) {
    /**
     * 저항력 점수 (0-100)
     *
     * 성공 세션이 실패 세션보다 얼마나 오래 지속되는지
     */
    fun getResistanceScore(): Int {
        if (avgFailureTimeSeconds == 0) return 100

        val ratio = avgSuccessTimeSeconds.toFloat() / avgFailureTimeSeconds
        val score = (ratio * 20).coerceIn(0f, 100f)
        return score.roundToInt()
    }
}

/**
 * 포기 지점 분석 결과
 */
data class GiveUpPointAnalysis(
    val avgGiveUpPercentage: Float,  // 평균 포기 시점 (%)
    val earlyGiveUpCount: Int,       // 초반 포기 (0-25%)
    val midGiveUpCount: Int,         // 중반 포기 (25-75%)
    val lateGiveUpCount: Int,        // 후반 포기 (75-100%)
    val totalFailures: Int           // 총 실패 세션 수
) {
    /**
     * 포기 패턴 판단
     */
    fun getGiveUpPattern(): GiveUpPattern {
        return when {
            earlyGiveUpCount > midGiveUpCount && earlyGiveUpCount > lateGiveUpCount -> GiveUpPattern.EARLY
            midGiveUpCount > lateGiveUpCount -> GiveUpPattern.MID
            else -> GiveUpPattern.LATE
        }
    }
}

/**
 * 포기 패턴
 */
enum class GiveUpPattern {
    EARLY,  // 초반 포기 (0-25%)
    MID,    // 중반 포기 (25-75%)
    LATE;   // 후반 포기 (75-100%)

    fun toDisplayString(): String = when (this) {
        EARLY -> "초반에 포기하는 경향"
        MID -> "중반에 포기하는 경향"
        LATE -> "후반에 포기하는 경향"
    }
}

