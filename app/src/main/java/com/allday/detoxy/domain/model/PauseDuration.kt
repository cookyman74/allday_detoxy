package com.allday.detoxy.domain.model

import java.util.Calendar

/**
 * 스케줄 그룹 일시중지 기간 (v8+)
 *
 * 드롭다운 메뉴에서 선택 가능한 일시중지 옵션을 정의합니다.
 *
 * @property displayName UI에 표시되는 이름
 * @property durationMillis 기간 (밀리초), 음수는 특수 계산 필요
 *
 * @see ScheduleGroupControlState.PAUSED
 */
enum class PauseDuration(
    val displayName: String,
    private val durationMillis: Long
) {
    /**
     * 1시간 동안 일시중지
     */
    ONE_HOUR("1시간", 3600_000L),

    /**
     * 2시간 동안 일시중지
     */
    TWO_HOURS("2시간", 7200_000L),

    /**
     * 오늘 자정까지 일시중지
     */
    TODAY("오늘 하루", -1L),

    /**
     * 내일 자정까지 일시중지
     */
    TOMORROW("내일까지", -2L);

    /**
     * 일시중지 해제 시각 계산
     *
     * @return pauseUntil timestamp
     */
    fun calculatePauseUntil(): Long {
        return when (this) {
            ONE_HOUR, TWO_HOURS -> System.currentTimeMillis() + durationMillis
            TODAY -> calculateMidnight()
            TOMORROW -> calculateTomorrowMidnight()
        }
    }

    companion object {
        /**
         * 오늘 자정 시각 계산
         *
         * @return 오늘 23:59:59.999 timestamp
         */
        private fun calculateMidnight(): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return calendar.timeInMillis
        }

        /**
         * 내일 자정 시각 계산
         *
         * @return 내일 23:59:59.999 timestamp
         */
        private fun calculateTomorrowMidnight(): Long {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return calendar.timeInMillis
        }
    }
}

