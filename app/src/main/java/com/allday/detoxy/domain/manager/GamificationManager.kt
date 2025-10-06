package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.UserSettings
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 게임화 관리자
 *
 * 포인트 계산과 스트릭(연속 성공 일수) 업데이트 로직을 담당합니다.
 * MVP 버전으로 간소화되어 1분 = 1포인트 단순 공식을 사용합니다.
 *
 * @see UserSettings
 */
@Singleton
class GamificationManager @Inject constructor() {

    /**
     * 포인트 계산
     *
     * MVP 간단 공식: 1분 = 1포인트
     *
     * @param durationMinutes 집중 시간 (분)
     * @return 획득 포인트
     */
    fun calculatePoints(durationMinutes: Int): Int {
        return durationMinutes
    }

    /**
     * 스트릭 업데이트
     *
     * 성공 시:
     * - 어제 성공했으면: 스트릭 +1
     * - 그 외: 스트릭 1로 초기화
     *
     * 실패 시:
     * - 스트릭 0으로 초기화
     *
     * @param settings 현재 사용자 설정
     * @param success 성공 여부
     * @return 업데이트된 사용자 설정
     */
    fun updateStreak(settings: UserSettings, success: Boolean): UserSettings {
        val today = LocalDate.now().toString()

        return if (success) {
            val yesterday = LocalDate.now().minusDays(1).toString()

            if (settings.lastSuccessDate == yesterday) {
                // 어제도 성공 → 스트릭 증가
                settings.copy(
                    currentStreak = settings.currentStreak + 1,
                    lastSuccessDate = today
                )
            } else {
                // 처음이거나 하루 이상 건너뛰었음 → 스트릭 1로 초기화
                settings.copy(
                    currentStreak = 1,
                    lastSuccessDate = today
                )
            }
        } else {
            // 실패 → 스트릭 초기화
            settings.copy(currentStreak = 0)
        }
    }
}
