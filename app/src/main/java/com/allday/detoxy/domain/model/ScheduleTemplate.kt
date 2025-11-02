package com.allday.detoxy.domain.model

import java.time.DayOfWeek

/**
 * 시간표 템플릿 (Phase 2)
 *
 * 사용자가 빠르게 시간표를 생성할 수 있도록 미리 정의된 템플릿입니다.
 * 기본 시간대와 요일 설정이 포함되어 있습니다.
 *
 * ## 템플릿 종류
 * 1. **시간 기반 템플릿** (hasLocation = false)
 *    - 매일 (DAILY): 매일 같은 시간에 집중
 *    - 평일 (WEEKDAY): 평일에만 집중
 *    - 주말 (WEEKEND): 주말에만 집중
 *
 * 2. **위치 기반 템플릿** (hasLocation = true)
 *    - 집 (HOME): 집에서 저녁 시간 집중
 *    - 학교 (SCHOOL): 학교에서 공부 시간
 *    - 회사 (OFFICE): 회사에서 업무 집중 시간
 *
 * ## 사용 예시
 * ```kotlin
 * val template = DefaultTemplates.WEEKDAY
 * val scheduleGroup = createFromTemplate(
 *     name = "평일 업무",
 *     template = template
 * )
 * ```
 *
 * @property id 템플릿 고유 ID ("daily", "weekday", "weekend", "home", "school", "office")
 * @property name 템플릿 이름 ("매일", "평일", "주말", "집", "학교", "회사")
 * @property description 템플릿 설명
 * @property iconType 아이콘 타입 ("HOME", "WORK", "STUDY", "CALENDAR")
 * @property colorHex 색상 코드 (예: "#4CAF50")
 * @property hasLocation 위치 필요 여부 (true: 위치 기반, false: 시간만)
 * @property defaultRadius 기본 반경 (미터, 위치 템플릿만 사용)
 * @property timeSlots 기본 시간대 목록
 * @property defaultDays 기본 활성화 요일
 *
 * @see com.allday.detoxy.data.template.DefaultTemplates
 * @see com.allday.detoxy.domain.model.TimeSlot
 */
data class ScheduleTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconType: String,
    val colorHex: String,
    val hasLocation: Boolean,
    val defaultRadius: Int = 100,
    val timeSlots: List<TimeSlot>,
    val defaultDays: List<DayOfWeek>
) {
    companion object {
        /**
         * 지원하는 아이콘 타입
         */
        object IconType {
            const val HOME = "HOME"
            const val WORK = "WORK"
            const val STUDY = "STUDY"
            const val CALENDAR = "CALENDAR"
        }
        
        /**
         * 기본 색상 (Material Design Colors)
         */
        object Colors {
            const val GREEN = "#4CAF50"      // 매일
            const val BLUE = "#2196F3"       // 평일
            const val ORANGE = "#FF9800"     // 주말
            const val PURPLE = "#9C27B0"     // 집
            const val INDIGO = "#3F51B5"     // 학교
            const val BLUE_GREY = "#607D8B"  // 회사
        }
    }
}

