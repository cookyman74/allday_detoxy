package com.allday.detoxy.domain.repository

/**
 * 사용자 설정 Repository 인터페이스
 *
 * Clean Architecture의 domain 계층 인터페이스
 * 구현체는 data 계층에 위치 (UserSettingsRepository)
 *
 * ## 주요 기능
 * - 자동 실행 마스터 스위치 관리
 * - 자동 실행 일시중지 관리
 *
 * ## Clean Architecture 준수
 * domain 계층이 data 계층에 의존하지 않도록 인터페이스로 분리
 *
 * @see com.allday.detoxy.data.repository.UserSettingsRepository
 */
interface IUserSettingsRepository {

    /**
     * 자동 실행 마스터 스위치 설정
     *
     * @param enabled true면 모든 자동 실행 활성화, false면 전체 비활성화
     */
    suspend fun setAutoRunMasterEnabled(enabled: Boolean)

    /**
     * 자동 실행 마스터 스위치 조회
     *
     * @return 마스터 스위치 상태 (기본값: true)
     */
    suspend fun getAutoRunMasterEnabled(): Boolean

    /**
     * 자동 실행 일시중지 해제 시각 설정
     *
     * @param pauseUntil 일시중지 해제 시각 (epoch millis), null이면 즉시 해제
     */
    suspend fun setAutoRunPauseUntil(pauseUntil: Long?)

    /**
     * 자동 실행 일시중지 해제 시각 조회
     *
     * @return 일시중지 해제 시각 (epoch millis), null이면 일시중지 중이 아님
     */
    suspend fun getAutoRunPauseUntil(): Long?
}

