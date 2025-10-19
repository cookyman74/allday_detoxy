package com.allday.detoxy.domain.repository

import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Focus 관련 데이터 Repository 인터페이스
 *
 * 세션 관리, 사용자 설정 관리를 위한 추상화된 데이터 접근 계층입니다.
 * 구현체는 data 계층에서 제공됩니다.
 */
interface FocusRepository {

    // ==================== FocusSession 관련 ====================

    /**
     * 새로운 세션 시작
     *
     * @param session 시작할 세션
     */
    suspend fun startSession(session: FocusSession)

    /**
     * 세션 종료
     *
     * @param sessionId 세션 ID
     * @param success 성공 여부
     * @param endTime 종료 시간
     */
    suspend fun endSession(sessionId: String, success: Boolean, endTime: Long)

    /**
     * 세션 종료 (확장 버전)
     *
     * @param sessionId 세션 ID
     * @param success 성공 여부
     * @param endTime 종료 시간
     * @param interruptedSeconds 중도 포기 시 경과 시간 (초)
     * @param giveUpReason 포기 사유
     */
    suspend fun endSessionWithDetails(
        sessionId: String,
        success: Boolean,
        endTime: Long,
        interruptedSeconds: Int = 0,
        giveUpReason: String? = null
    )

    /**
     * 특정 ID의 세션 조회
     *
     * @param sessionId 세션 ID
     * @return 세션 (Flow)
     */
    fun getSession(sessionId: String): Flow<FocusSession?>

    /**
     * 오늘의 세션 목록 조회
     *
     * @return 오늘의 세션 리스트 (Flow)
     */
    fun getTodaySessions(): Flow<List<FocusSession>>

    /**
     * 모든 세션 조회
     *
     * @return 모든 세션 리스트 (Flow)
     */
    fun getAllSessions(): Flow<List<FocusSession>>

    /**
     * 기간별 세션 조회
     *
     * @param startTime 시작 시간 (밀리초)
     * @param endTime 종료 시간 (밀리초)
     * @return 기간 내 세션 리스트
     *
     * Week 2B: Task 2B.3.1
     */
    suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<FocusSession>

    // ==================== FocusInterruption 관련 ====================

    /**
     * 차단 이벤트 로깅
     *
     * @param interruption 차단 이벤트
     */
    suspend fun logInterruption(interruption: FocusInterruption)

    /**
     * 특정 세션의 차단 이벤트 조회
     *
     * @param sessionId 세션 ID
     * @return 차단 이벤트 리스트 (Flow)
     */
    fun getInterruptionsBySession(sessionId: String): Flow<List<FocusInterruption>>

    /**
     * 특정 세션의 가장 많이 차단된 카테고리 조회
     *
     * @param sessionId 세션 ID
     * @return 가장 많이 차단된 카테고리명 (없으면 null)
     */
    suspend fun getPrimaryCategoryBySession(sessionId: String): String?

    /**
     * 최근 N일 동안의 차단 이벤트 조회
     *
     * @param days 조회할 일수 (예: 7, 30)
     * @return 최근 N일의 차단 이벤트 리스트
     *
     * Week 2B: Task 2B.3.1 Review Fix
     */
    suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption>

    // ==================== UserSettings 관련 ====================

    /**
     * 사용자 설정 조회
     *
     * @return 사용자 설정 (Flow)
     */
    fun getSettings(): Flow<UserSettings?>

    /**
     * 사용자 설정 초기화
     *
     * @param settings 초기 설정
     */
    suspend fun initializeSettings(settings: UserSettings)

    /**
     * 포인트 추가
     *
     * @param points 추가할 포인트
     */
    suspend fun addPoints(points: Int)

    /**
     * 스트릭 업데이트
     *
     * @param streak 새로운 스트릭 값
     * @param date 마지막 성공 날짜
     */
    suspend fun updateStreak(streak: Int, date: String)

    /**
     * 사용자 설정 업데이트
     *
     * @param settings 업데이트할 설정
     */
    suspend fun updateSettings(settings: UserSettings)
}
