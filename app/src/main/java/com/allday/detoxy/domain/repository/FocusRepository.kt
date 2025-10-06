package com.allday.detoxy.domain.repository

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
