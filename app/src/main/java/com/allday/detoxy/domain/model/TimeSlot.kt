package com.allday.detoxy.domain.model

import java.time.DayOfWeek

/**
 * 시간대 데이터 클래스 (Phase 1)
 *
 * 사용자가 QuickCreateScheduleDialog에서 직접 입력하는 시간대 정보를 담습니다.
 * TimeBasedAutoRun 생성 시 이 데이터를 사용합니다.
 *
 * ## Clean Architecture 준수
 * - 도메인 모델은 순수 데이터만 담습니다
 * - UI 포맷팅 로직은 presentation 레이어로 분리 (TimeSlotFormatter.kt 참조)
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
 * @see com.allday.detoxy.presentation.util.TimeSlotFormatter
 */
data class TimeSlot(
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val presetType: String,
    val enabledDays: List<DayOfWeek> = DayOfWeek.values().toList()
) {
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

