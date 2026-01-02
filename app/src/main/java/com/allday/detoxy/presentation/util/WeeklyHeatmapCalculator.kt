package com.allday.detoxy.presentation.util

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.presentation.model.*
import java.time.DayOfWeek

/**
 * 주간 히트맵 계산기
 *
 * TimeBasedAutoRun 목록을 기반으로 주간 히트맵 UI 모델을 생성합니다.
 *
 * ## 주요 기능
 * - 시간대를 1시간 슬롯에 분배 (예: 09:15 시작 90분 → 9시(45분), 10시(45분))
 * - 자정 넘김 처리 (예: 23:30 시작 60분 → 23시(30분), 다음날 0시(30분))
 * - 겹치는 시간대 합산 (최대 60분으로 cap)
 * - 비활성 시간대(isEnabled=false) 제외
 *
 * @see WeeklyHeatmapUiModel
 */
object WeeklyHeatmapCalculator {

    private const val MAX_MINUTES_PER_SLOT = 60

    /**
     * 시간 슬롯 분배 결과
     *
     * @property dayOffset 요일 오프셋 (0=당일, 1=다음날, ...)
     * @property hour 시간 (0-23)
     * @property minutes 해당 슬롯에 할당된 분
     */
    data class SlotDistribution(
        val dayOffset: Int,
        val hour: Int,
        val minutes: Int
    )

    /**
     * 시간대를 1시간 슬롯에 분배
     *
     * 시간대가 1시간을 초과하거나 시간 경계를 넘는 경우,
     * 각 시간 슬롯에 적절한 분을 배분합니다.
     *
     * ## 예시
     * - 09:15 시작 90분 → 9시(45분), 10시(45분)
     * - 23:30 시작 90분 → (dayOffset=0, 23시:30분), (dayOffset=1, 0시:60분)
     *
     * @param startHour 시작 시간 (0-23)
     * @param startMinute 시작 분 (0-59)
     * @param durationMinutes 총 지속 시간 (분)
     * @return 각 슬롯의 요일 오프셋과 분 정보 리스트
     */
    fun distributeToSlots(
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int
    ): List<SlotDistribution> {
        val result = mutableListOf<SlotDistribution>()
        var remainingMinutes = durationMinutes
        var currentHour = startHour
        var currentMinute = startMinute
        var dayOffset = 0

        while (remainingMinutes > 0) {
            val minutesInThisSlot = minOf(60 - currentMinute, remainingMinutes)
            val hour = currentHour % 24

            // 자정 넘김 감지: 24시간 이상이 되면 다음 요일로 이동
            if (currentHour >= 24 && hour == 0 && result.lastOrNull()?.hour != 0) {
                dayOffset++
            }

            result.add(SlotDistribution(dayOffset, hour, minutesInThisSlot))

            remainingMinutes -= minutesInThisSlot
            currentHour++
            currentMinute = 0
        }

        return result
    }

    /**
     * enabledDays JSON 파싱
     *
     * JSON 배열 형식의 요일 문자열을 DayOfWeek 리스트로 변환합니다.
     * Android 단위 테스트 호환을 위해 정규식 기반으로 구현.
     *
     * ## 예시
     * - "[\"MON\",\"TUE\"]" → listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
     *
     * @param enabledDaysJson 요일 JSON 배열 문자열
     * @return 파싱된 요일 리스트
     */
    fun parseEnabledDays(enabledDaysJson: String): List<DayOfWeek> {
        return try {
            // 정규식으로 MON, TUE 등의 요일 코드 추출
            val dayPattern = Regex(""""(MON|TUE|WED|THU|FRI|SAT|SUN)"""")
            dayPattern.findAll(enabledDaysJson).mapNotNull { match ->
                when (match.groupValues[1]) {
                    "MON" -> DayOfWeek.MONDAY
                    "TUE" -> DayOfWeek.TUESDAY
                    "WED" -> DayOfWeek.WEDNESDAY
                    "THU" -> DayOfWeek.THURSDAY
                    "FRI" -> DayOfWeek.FRIDAY
                    "SAT" -> DayOfWeek.SATURDAY
                    "SUN" -> DayOfWeek.SUNDAY
                    else -> null
                }
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 전체 히트맵 계산
     *
     * TimeBasedAutoRun 목록을 기반으로 주간 히트맵 UI 모델을 생성합니다.
     *
     * ## 처리 규칙
     * 1. isEnabled=true인 시간대만 포함
     * 2. 각 시간대의 enabledDays에 해당하는 요일만 합산
     * 3. 겹치는 시간대는 합산 후 최대 60분으로 cap
     * 4. 자정을 넘기는 시간대는 다음 요일의 AM으로 분배
     *
     * @param autoRuns TimeBasedAutoRun 목록
     * @return WeeklyHeatmapUiModel
     */
    fun calculate(autoRuns: List<TimeBasedAutoRun>): WeeklyHeatmapUiModel {
        // isEnabled=true만 필터링
        val enabledAutoRuns = autoRuns.filter { it.isEnabled }

        // v1.1: 빈 데이터일 때도 14개 빈 행 생성 (early return 제거)
        // 요일별, 시간별 분 합산
        val dayHourMinutes = mutableMapOf<DayOfWeek, MutableMap<Int, Int>>()
        // 요일+Period별 TimeSlotInfo 저장 (AM/PM 분리)
        // Key: Pair<DayOfWeek, Period>
        val dayPeriodTimeSlots = mutableMapOf<Pair<DayOfWeek, Period>, MutableList<TimeSlotInfo>>()

        enabledAutoRuns.forEach { autoRun ->
            val days = parseEnabledDays(autoRun.enabledDays)
            val slots = distributeToSlots(autoRun.hour, autoRun.minute, autoRun.durationMinutes)

            days.forEach { day ->
                // 히트맵 셀 분배 (요일별 시간 합산)
                slots.forEach { slot ->
                    val targetDay = day.plus(slot.dayOffset.toLong())
                    val hourMap = dayHourMinutes.getOrPut(targetDay) { mutableMapOf() }
                    hourMap[slot.hour] = minOf(
                        (hourMap[slot.hour] ?: 0) + slot.minutes,
                        MAX_MINUTES_PER_SLOT
                    )
                }

                // TimeSlotInfo를 분배된 슬롯 기준으로 요일+Period에 저장
                // 자정 넘김 시 다음 요일 AM에도 정확히 포함됨
                val timeSlotInfo = TimeSlotInfo(
                    id = autoRun.id,
                    hour = autoRun.hour,
                    minute = autoRun.minute,
                    durationMinutes = autoRun.durationMinutes,
                    presetType = autoRun.presetType,
                    label = autoRun.label
                )

                // 기여하는 모든 요일+Period 조합에 저장
                val contributedDayPeriods = slots.map { slot ->
                    val targetDay = day.plus(slot.dayOffset.toLong())
                    val period = if (slot.hour in 0..11) Period.AM else Period.PM
                    Pair(targetDay, period)
                }.toSet()

                contributedDayPeriods.forEach { key ->
                    val list = dayPeriodTimeSlots.getOrPut(key) { mutableListOf() }
                    // 중복 방지 (동일 ID가 이미 있으면 추가하지 않음)
                    if (list.none { it.id == timeSlotInfo.id }) {
                        list.add(timeSlotInfo)
                    }
                }
            }
        }

        // v1.1: HeatmapRow 생성 (7행 × 24셀 컴팩트 구조)
        val rows = DayOfWeek.values().map { day ->
            createRow(
                day = day,
                hourMinutes = dayHourMinutes[day] ?: emptyMap(),
                allTimeSlots = (dayPeriodTimeSlots[Pair(day, Period.AM)] ?: emptyList()) +
                        (dayPeriodTimeSlots[Pair(day, Period.PM)] ?: emptyList())
            )
        }

        // Summary 계산
        val amTotal = rows.sumOf { row ->
            row.cells.filter { it.hour in 0..11 }.sumOf { it.totalMinutes }
        }
        val pmTotal = rows.sumOf { row ->
            row.cells.filter { it.hour in 12..23 }.sumOf { it.totalMinutes }
        }

        return WeeklyHeatmapUiModel(
            rows = rows,
            summary = HeatmapSummary(amTotal, pmTotal, findPeakTimeRange(dayHourMinutes))
        )
    }

    /**
     * 히트맵 행 생성 (v1.1: 24셀 컴팩트 구조)
     * 
     * @param day 요일
     * @param hourMinutes 해당 요일의 시간별 분 맵
     * @param allTimeSlots 해당 요일의 전체 TimeSlotInfo 목록
     */
    private fun createRow(
        day: DayOfWeek,
        hourMinutes: Map<Int, Int>,
        allTimeSlots: List<TimeSlotInfo>
    ): HeatmapRow {
        val cells = (0..23).map { hour ->
            val minutes = hourMinutes[hour] ?: 0
            HeatmapCell(hour, minutes, HeatmapLevel.fromMinutes(minutes))
        }

        return HeatmapRow(
            dayOfWeek = day,
            cells = cells,
            timeSlots = allTimeSlots.sortedWith(compareBy({ it.hour }, { it.minute })),
            totalMinutes = cells.sumOf { it.totalMinutes }
        )
    }

    /**
     * 가장 밀집된 시간대 찾기
     *
     * 모든 요일을 합산하여 가장 집중이 높은 시간대를 반환합니다.
     *
     * @return 예: "오전 9~10시", "오후 12~1시"
     */
    private fun findPeakTimeRange(dayHourMinutes: Map<DayOfWeek, Map<Int, Int>>): String {
        val hourTotals = mutableMapOf<Int, Int>()
        dayHourMinutes.values.forEach { hourMap ->
            hourMap.forEach { (hour, minutes) ->
                hourTotals[hour] = (hourTotals[hour] ?: 0) + minutes
            }
        }

        val peakHour = hourTotals.maxByOrNull { it.value }?.key ?: return ""
        val period = if (peakHour < 12) "오전" else "오후"
        // 12시간 표기 (0시=오전 12시, 12시=오후 12시, 13시=오후 1시)
        val displayHour = when {
            peakHour == 0 -> 12   // 오전 12시
            peakHour == 12 -> 12  // 오후 12시
            peakHour < 12 -> peakHour
            else -> peakHour - 12
        }
        // 다음 시간 계산 (12시 다음은 1시)
        val nextHour = if (displayHour == 12) 1 else displayHour + 1
        return "$period ${displayHour}~${nextHour}시"
    }
}
