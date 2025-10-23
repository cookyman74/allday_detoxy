package com.allday.detoxy.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 자동 실행 글로벌 설정 Repository 인터페이스
 *
 * Clean Architecture의 domain 계층 인터페이스
 * 구현체는 data 계층에 위치 (AutoRunSettingsRepositoryImpl)
 *
 * 주요 기능:
 * 1. 주말 제외 설정 (주말에 자동 실행 비활성화)
 * 2. 자동 시작 딜레이 (알림 후 자동 시작까지 대기 시간)
 * 3. 사전 알림 시간 (실행 시간 N분 전 알림)
 *
 * @see com.allday.detoxy.data.repository.AutoRunSettingsRepositoryImpl
 */
interface AutoRunSettingsRepository {

    // ==================== Flow (실시간 관찰) ====================

    /**
     * 주말 제외 설정 Flow
     * true: 주말(토, 일)에는 자동 실행하지 않음
     * false: 주말에도 자동 실행 (기본값)
     */
    val excludeWeekendsFlow: Flow<Boolean>

    /**
     * 자동 시작 딜레이 Flow (분)
     * 알림 후 사용자 응답이 없을 때 자동으로 타이머를 시작하기까지 대기 시간
     * 0: 즉시 시작 (기본값)
     * 5: 5분 후 시작
     * 10: 10분 후 시작
     */
    val autoStartDelayMinutesFlow: Flow<Int>

    /**
     * 사전 알림 시간 Flow (분)
     * 실행 시간 N분 전에 사전 알림을 보냄
     * 0: 사전 알림 없음
     * 5: 5분 전 알림 (기본값)
     * 10: 10분 전 알림
     * 15: 15분 전 알림
     */
    val preNotificationMinutesFlow: Flow<Int>

    // ==================== 저장 메서드 ====================

    /**
     * 주말 제외 설정 저장
     */
    suspend fun saveExcludeWeekends(exclude: Boolean)

    /**
     * 자동 시작 딜레이 저장
     *
     * @param minutes 0, 5, 10 중 하나
     */
    suspend fun saveAutoStartDelayMinutes(minutes: Int)

    /**
     * 사전 알림 시간 저장
     *
     * @param minutes 0, 5, 10, 15 중 하나
     */
    suspend fun savePreNotificationMinutes(minutes: Int)

    // ==================== 동기 조회 (suspend) ====================

    /**
     * 현재 주말 제외 설정 가져오기 (suspend)
     */
    suspend fun getExcludeWeekends(): Boolean

    /**
     * 현재 자동 시작 딜레이 가져오기 (suspend)
     */
    suspend fun getAutoStartDelayMinutes(): Int

    /**
     * 현재 사전 알림 시간 가져오기 (suspend)
     */
    suspend fun getPreNotificationMinutes(): Int
}

