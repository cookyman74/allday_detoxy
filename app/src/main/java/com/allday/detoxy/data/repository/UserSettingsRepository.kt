package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.repository.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserSettings Repository
 *
 * 사용자 설정 데이터를 관리합니다.
 * DAO를 통해 Room 데이터베이스와 통신합니다.
 *
 * ## 주요 기능
 * - 포인트 및 스트릭 관리
 * - 자동 실행 마스터 토글 관리 (v4+)
 * - 자동 실행 일시중지 관리 (v4+)
 *
 * ## Clean Architecture
 * domain.repository.IUserSettingsRepository 인터페이스 구현
 *
 * @property dao UserSettingsDao
 */
@Singleton
class UserSettingsRepository @Inject constructor(
    private val dao: UserSettingsDao
) : IUserSettingsRepository {

    /**
     * 사용자 설정 조회 (반응형)
     *
     * @return 사용자 설정 (Flow)
     */
    fun getSettings(): Flow<UserSettings?> {
        return dao.getSettings()
    }

    /**
     * 사용자 설정 추가
     *
     * @param settings 추가할 설정
     */
    suspend fun insert(settings: UserSettings) {
        dao.insert(settings)
    }

    /**
     * 사용자 설정 업데이트
     *
     * @param settings 업데이트할 설정
     */
    suspend fun update(settings: UserSettings) {
        dao.update(settings)
    }

    /**
     * 포인트 추가
     *
     * @param points 추가할 포인트
     */
    suspend fun addPoints(points: Int) {
        dao.addPoints(points)
    }

    /**
     * 스트릭 업데이트
     *
     * @param streak 새로운 스트릭
     * @param date 마지막 성공 날짜 (YYYY-MM-DD)
     */
    suspend fun updateStreak(streak: Int, date: String) {
        dao.updateStreak(streak, date)
    }

    /**
     * 스트릭 초기화
     */
    suspend fun resetStreak() {
        dao.resetStreak()
    }

    // ========== v4: 자동 실행 제어 ==========

    /**
     * 자동 실행 마스터 스위치 설정
     *
     * @param enabled true면 모든 자동 실행 활성화, false면 전체 비활성화
     */
    override suspend fun setAutoRunMasterEnabled(enabled: Boolean) {
        dao.setAutoRunMasterEnabled(enabled)
    }

    /**
     * 자동 실행 마스터 스위치 조회
     *
     * @return 마스터 스위치 상태 (기본값: true)
     */
    override suspend fun getAutoRunMasterEnabled(): Boolean {
        return dao.getAutoRunMasterEnabled() ?: true
    }

    /**
     * 자동 실행 일시중지 설정
     *
     * @param pauseUntil null이면 일시중지 해제, 값이 있으면 해당 시각까지 일시중지
     */
    override suspend fun setAutoRunPauseUntil(pauseUntil: Long?) {
        dao.setAutoRunPauseUntil(pauseUntil)
    }

    /**
     * 자동 실행 일시중지 시각 조회
     *
     * @return 일시중지 해제 시각 (null이면 일시중지되지 않음)
     */
    override suspend fun getAutoRunPauseUntil(): Long? {
        return dao.getAutoRunPauseUntil()
    }

    /**
     * 자동 실행 가능 여부 확인
     *
     * 마스터 스위치가 켜져 있고, 일시중지되지 않았을 때만 true 반환
     *
     * @return true: 자동 실행 가능, false: 자동 실행 불가
     */
    suspend fun isAutoRunEnabled(): Boolean {
        // 마스터 스위치 체크
        if (!getAutoRunMasterEnabled()) {
            return false
        }

        // 일시중지 체크
        val pauseUntil = getAutoRunPauseUntil()
        if (pauseUntil != null && System.currentTimeMillis() < pauseUntil) {
            return false
        }

        return true
    }

    /**
     * "오늘 하루 중지" 설정
     *
     * 오늘 자정(23:59:59)까지 일시중지
     */
    suspend fun pauseUntilMidnight() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        setAutoRunPauseUntil(calendar.timeInMillis)
    }

    /**
     * "N시간 동안 중지" 설정
     *
     * @param hours 중지할 시간 (시간 단위)
     */
    suspend fun pauseForHours(hours: Int) {
        val pauseUntil = System.currentTimeMillis() + (hours * 60 * 60 * 1000L)
        setAutoRunPauseUntil(pauseUntil)
    }

    /**
     * 일시중지 해제
     */
    suspend fun resumeAutoRun() {
        setAutoRunPauseUntil(null)
    }
}

