package com.allday.detoxy.domain.model

import java.time.DayOfWeek

/**
 * 시간대 데이터 클래스 (Phase 1)
 *
 * 사용자가 QuickCreateScheduleDialog에서 직접 입력하는 시간대 정보를 담습니다.
 * TimeBasedAutoRun 생성 시 이 데이터를 사용합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val slot = TimeSlot(
 *     startHour = 9,
 *     startMinute = 0,
 *     durationMinutes = 180,  // 3시간
 *     presetType = "STANDARD",
 *     enabledDays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, ..., DayOfWeek.FRIDAY)
 * )
 * ```
 *
 * @property startHour 시작 시간 (0-23)
 * @property startMinute 시작 분 (0-59)
 * @property durationMinutes 기간 (분, 15-720)
 * @property presetType 차단 프리셋 ("STANDARD", "MEDIUM", "COMPLETE")
 * @property enabledDays 활성화된 요일 목록 (기본: 매일)
 *
 * @see com.allday.detoxy.data.local.entity.TimeBasedAutoRun
 */
data class TimeSlot(
    val startHour: Int,                  // 0-23
    val startMinute: Int,                // 0-59
    val durationMinutes: Int,            // 15-720 (15분~12시간)
    val presetType: String,              // "STANDARD", "MEDIUM", "COMPLETE"
    val enabledDays: List<DayOfWeek> = DayOfWeek.values().toList()  // 기본: 매일
) {
    /**
     * 포맷팅된 시작 시간
     *
     * @return "10:00" 형식의 시간 문자열
     */
    fun formatStartTime(): String {
        return String.format("%02d:%02d", startHour, startMinute)
    }
    
    /**
     * 포맷팅된 기간
     *
     * @return "1시간 30분" 또는 "90분" 형식의 기간 문자열
     */
    fun formatDuration(): String {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            else -> "${minutes}분"
        }
    }
    
    /**
     * 포맷팅된 요일
     *
     * @return "매일", "평일", "주말", "월,수,금" 등의 요일 문자열
     */
    fun formatEnabledDays(): String {
        return when {
            enabledDays.size == 7 -> "매일"
            enabledDays.size == 5 && enabledDays.containsAll(listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )) -> "평일"
            enabledDays.size == 2 && enabledDays.containsAll(listOf(
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            )) -> "주말"
            else -> enabledDays.joinToString(",") { 
                when (it) {
                    DayOfWeek.MONDAY -> "월"
                    DayOfWeek.TUESDAY -> "화"
                    DayOfWeek.WEDNESDAY -> "수"
                    DayOfWeek.THURSDAY -> "목"
                    DayOfWeek.FRIDAY -> "금"
                    DayOfWeek.SATURDAY -> "토"
                    DayOfWeek.SUNDAY -> "일"
                }
            }
        }
    }
    
    companion object {
        /**
         * 최소 기간 (분)
         */
        const val MIN_DURATION = 15
        
        /**
         * 최대 기간 (분)
         */
        const val MAX_DURATION = 720
        
        /**
         * 기본 기간 (분)
         */
        const val DEFAULT_DURATION = 90
    }
}

