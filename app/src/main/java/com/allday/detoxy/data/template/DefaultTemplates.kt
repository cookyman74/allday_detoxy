package com.allday.detoxy.data.template

import com.allday.detoxy.domain.model.ScheduleTemplate
import com.allday.detoxy.domain.model.TimeSlot
import java.time.DayOfWeek

/**
 * 기본 시간표 템플릿 (Phase 2)
 *
 * 사용자가 빠르게 시간표를 생성할 수 있도록 미리 정의된 6개 템플릿을 제공합니다.
 *
 * ## 템플릿 목록
 * 1. **매일 (DAILY)**: 매일 같은 시간에 집중하는 루틴
 *    - 09:00 (3시간, 표준 차단)
 *    - 14:00 (4시간, 표준 차단)
 *    - 요일: 매일 (7일)
 *
 * 2. **평일 (WEEKDAY)**: 평일에만 집중하는 루틴
 *    - 09:00 (3시간, 표준 차단)
 *    - 14:00 (4시간, 표준 차단)
 *    - 요일: 월~금 (5일)
 *
 * 3. **주말 (WEEKEND)**: 주말에만 집중하는 루틴
 *    - 10:00 (2시간, 표준 차단)
 *    - 14:00 (3시간, 표준 차단)
 *    - 요일: 토~일 (2일)
 *
 * 4. **집 (HOME)**: 집에서 저녁 시간 집중
 *    - 19:00 (3시간, 중간 차단)
 *    - 요일: 매일 (7일)
 *    - 위치: 필요 (반경 100m)
 *
 * 5. **학교 (SCHOOL)**: 학교에서 공부 시간
 *    - 09:00 (3시간, 표준 차단)
 *    - 14:00 (3시간, 표준 차단)
 *    - 요일: 월~금 (5일)
 *    - 위치: 필요 (반경 150m)
 *
 * 6. **회사 (OFFICE)**: 회사에서 업무 집중 시간
 *    - 10:00 (2시간, 표준 차단)
 *    - 14:00 (2시간, 완전 차단)
 *    - 16:00 (1.5시간, 표준 차단)
 *    - 요일: 월~금 (5일)
 *    - 위치: 필요 (반경 100m)
 *
 * ## 중요: 요일 설정 방식 (Phase 2.1 Review)
 * - **TimeSlot의 enabledDays는 명시하지 않음** (기본값 사용)
 * - **ScheduleTemplate.defaultDays가 단일 소스** 역할
 * - createFromTemplate() 메서드에서 defaultDays를 TimeSlot.enabledDays에 자동 적용
 * - 이 방식으로 요일 정보의 중복과 불일치를 방지합니다
 *
 * ## 사용 예시
 * ```kotlin
 * val template = DefaultTemplates.WEEKDAY
 * val scheduleGroup = viewModel.createFromTemplate(
 *     name = "평일 업무",
 *     template = template
 * )
 * // 결과: 모든 TimeSlot이 월~금으로 설정됨 (template.defaultDays 적용)
 * ```
 *
 * @see com.allday.detoxy.domain.model.ScheduleTemplate
 * @see com.allday.detoxy.presentation.viewmodel.ScheduleGroupViewModel.createFromTemplate
 */
object DefaultTemplates {
    
    /**
     * 매일 템플릿
     *
     * 매일 같은 시간에 집중하는 루틴입니다.
     * 오전(9시)과 오후(2시)에 집중 시간대를 설정합니다.
     */
    val DAILY = ScheduleTemplate(
        id = "daily",
        name = "매일",
        description = "매일 같은 시간에 집중하는 루틴",
        iconType = ScheduleTemplate.IconType.CALENDAR,
        colorHex = ScheduleTemplate.Colors.GREEN,
        hasLocation = false,
        timeSlots = listOf(
            TimeSlot(
                startHour = 9,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "STANDARD"
            ),
            TimeSlot(
                startHour = 14,
                startMinute = 0,
                durationMinutes = 240,  // 4시간
                presetType = "STANDARD"
            )
        ),
        defaultDays = DayOfWeek.values().toList()  // 매일 (7일)
    )
    
    /**
     * 평일 템플릿
     *
     * 평일(월~금)에만 집중하는 루틴입니다.
     * 주중 업무나 학습에 적합합니다.
     */
    val WEEKDAY = ScheduleTemplate(
        id = "weekday",
        name = "평일",
        description = "평일에만 집중하는 루틴",
        iconType = ScheduleTemplate.IconType.WORK,
        colorHex = ScheduleTemplate.Colors.BLUE,
        hasLocation = false,
        timeSlots = listOf(
            TimeSlot(
                startHour = 9,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "STANDARD"
            ),
            TimeSlot(
                startHour = 14,
                startMinute = 0,
                durationMinutes = 240,  // 4시간
                presetType = "STANDARD"
            )
        ),
        defaultDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )  // 평일 (5일)
    )
    
    /**
     * 주말 템플릿
     *
     * 주말(토~일)에만 집중하는 루틴입니다.
     * 여유로운 주말 학습이나 취미 활동에 적합합니다.
     */
    val WEEKEND = ScheduleTemplate(
        id = "weekend",
        name = "주말",
        description = "주말에만 집중하는 루틴",
        iconType = ScheduleTemplate.IconType.HOME,
        colorHex = ScheduleTemplate.Colors.ORANGE,
        hasLocation = false,
        timeSlots = listOf(
            TimeSlot(
                startHour = 10,
                startMinute = 0,
                durationMinutes = 120,  // 2시간
                presetType = "STANDARD"
            ),
            TimeSlot(
                startHour = 14,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "STANDARD"
            )
        ),
        defaultDays = listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )  // 주말 (2일)
    )
    
    /**
     * 집 템플릿
     *
     * 집에서 저녁 시간 집중하는 루틴입니다.
     * 위치 기반으로 집 반경 100m 내에서 자동 활성화됩니다.
     * 저녁 식사 후 학습이나 독서에 적합합니다.
     */
    val HOME = ScheduleTemplate(
        id = "home",
        name = "집",
        description = "집에서 저녁 시간 집중",
        iconType = ScheduleTemplate.IconType.HOME,
        colorHex = ScheduleTemplate.Colors.PURPLE,
        hasLocation = true,
        defaultRadius = 100,  // 100m
        timeSlots = listOf(
            TimeSlot(
                startHour = 19,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "MEDIUM"   // 중간 차단
            )
        ),
        defaultDays = DayOfWeek.values().toList()  // 매일 (7일)
    )
    
    /**
     * 학교 템플릿
     *
     * 학교에서 공부 시간 집중하는 루틴입니다.
     * 위치 기반으로 학교 반경 150m 내에서 자동 활성화됩니다.
     * 평일 수업 시간과 자습 시간에 적합합니다.
     */
    val SCHOOL = ScheduleTemplate(
        id = "school",
        name = "학교",
        description = "학교에서 공부 시간",
        iconType = ScheduleTemplate.IconType.STUDY,
        colorHex = ScheduleTemplate.Colors.INDIGO,
        hasLocation = true,
        defaultRadius = 150,  // 150m (학교는 넓은 반경)
        timeSlots = listOf(
            TimeSlot(
                startHour = 9,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "STANDARD"
            ),
            TimeSlot(
                startHour = 14,
                startMinute = 0,
                durationMinutes = 180,  // 3시간
                presetType = "STANDARD"
            )
        ),
        defaultDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )  // 평일 (5일)
    )
    
    /**
     * 회사 템플릿
     *
     * 회사에서 업무 집중 시간 루틴입니다.
     * 위치 기반으로 회사 반경 100m 내에서 자동 활성화됩니다.
     * 오전 업무, 점심 후 집중 시간, 오후 마무리 작업에 적합합니다.
     * 점심 후 시간대는 완전 차단으로 설정되어 있습니다.
     */
    val OFFICE = ScheduleTemplate(
        id = "office",
        name = "회사",
        description = "회사에서 업무 집중 시간",
        iconType = ScheduleTemplate.IconType.WORK,
        colorHex = ScheduleTemplate.Colors.BLUE_GREY,
        hasLocation = true,
        defaultRadius = 100,  // 100m
        timeSlots = listOf(
            TimeSlot(
                startHour = 10,
                startMinute = 0,
                durationMinutes = 120,  // 2시간
                presetType = "STANDARD"
            ),
            TimeSlot(
                startHour = 14,
                startMinute = 0,
                durationMinutes = 120,  // 2시간
                presetType = "COMPLETE"  // 완전 차단
            ),
            TimeSlot(
                startHour = 16,
                startMinute = 0,
                durationMinutes = 90,    // 1.5시간
                presetType = "STANDARD"
            )
        ),
        defaultDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )  // 평일 (5일)
    )
    
    /**
     * 모든 템플릿 목록
     *
     * UI에서 템플릿 선택 시 이 리스트를 사용합니다.
     * 순서: 시간 기반 템플릿 (3개) → 위치 기반 템플릿 (3개)
     */
    val ALL_TEMPLATES = listOf(
        DAILY,    // 매일
        WEEKDAY,  // 평일
        WEEKEND,  // 주말
        HOME,     // 집
        SCHOOL,   // 학교
        OFFICE    // 회사
    )
    
    /**
     * ID로 템플릿 찾기
     *
     * @param id 템플릿 ID
     * @return 템플릿 객체, 없으면 null
     */
    fun findById(id: String): ScheduleTemplate? {
        return ALL_TEMPLATES.find { it.id == id }
    }
    
    /**
     * 시간 기반 템플릿 목록
     *
     * @return 위치가 필요하지 않은 템플릿 목록
     */
    fun getTimeBasedTemplates(): List<ScheduleTemplate> {
        return ALL_TEMPLATES.filter { !it.hasLocation }
    }
    
    /**
     * 위치 기반 템플릿 목록
     *
     * @return 위치가 필요한 템플릿 목록
     */
    fun getLocationBasedTemplates(): List<ScheduleTemplate> {
        return ALL_TEMPLATES.filter { it.hasLocation }
    }
}

