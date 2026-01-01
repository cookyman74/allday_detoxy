package com.allday.detoxy.presentation.util

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.presentation.model.HeatmapLevel
import com.allday.detoxy.presentation.model.Period
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek

/**
 * WeeklyHeatmapCalculator 단위 테스트
 *
 * PRD 섹션 4️⃣ "데이터 집계 규칙"에 정의된 로직을 검증합니다.
 */
class WeeklyHeatmapCalculatorTest {

    // ==================== 4-1. 시간 슬롯 분배 테스트 ====================

    @Test
    fun `9시 15분 시작 90분 duration은 9시에 45분, 10시에 45분 분배`() {
        // Given
        val startHour = 9
        val startMinute = 15
        val durationMinutes = 90

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(2, result.size)
        assertEquals(0, result[0].dayOffset)
        assertEquals(9, result[0].hour)
        assertEquals(45, result[0].minutes)  // 09:15~10:00
        assertEquals(0, result[1].dayOffset)
        assertEquals(10, result[1].hour)
        assertEquals(45, result[1].minutes)  // 10:00~10:45
    }

    @Test
    fun `정시 시작 60분 duration은 해당 시간에 60분 분배`() {
        // Given
        val startHour = 14
        val startMinute = 0
        val durationMinutes = 60

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(1, result.size)
        assertEquals(14, result[0].hour)
        assertEquals(60, result[0].minutes)
    }

    @Test
    fun `30분 시작 30분 duration은 해당 시간에 30분 분배`() {
        // Given
        val startHour = 10
        val startMinute = 30
        val durationMinutes = 30

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(1, result.size)
        assertEquals(10, result[0].hour)
        assertEquals(30, result[0].minutes)
    }

    @Test
    fun `3시간짜리 긴 스케줄은 3개 슬롯에 분배`() {
        // Given
        val startHour = 9
        val startMinute = 0
        val durationMinutes = 180  // 3시간

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(3, result.size)
        assertEquals(9, result[0].hour)
        assertEquals(60, result[0].minutes)
        assertEquals(10, result[1].hour)
        assertEquals(60, result[1].minutes)
        assertEquals(11, result[2].hour)
        assertEquals(60, result[2].minutes)
    }

    // ==================== 4-2. 자정 넘김 처리 테스트 ====================

    @Test
    fun `23시 30분 시작 60분 duration은 23시 30분과 다음날 0시 30분 분배`() {
        // Given
        val startHour = 23
        val startMinute = 30
        val durationMinutes = 60

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(2, result.size)
        // 당일 23시
        assertEquals(0, result[0].dayOffset)
        assertEquals(23, result[0].hour)
        assertEquals(30, result[0].minutes)
        // 다음날 0시
        assertEquals(1, result[1].dayOffset)
        assertEquals(0, result[1].hour)
        assertEquals(30, result[1].minutes)
    }

    @Test
    fun `23시 30분 시작 120분 duration은 자정을 넘어 다음날까지 분배`() {
        // Given
        val startHour = 23
        val startMinute = 30
        val durationMinutes = 120

        // When
        val result = WeeklyHeatmapCalculator.distributeToSlots(startHour, startMinute, durationMinutes)

        // Then
        assertEquals(3, result.size)
        // 당일 23시: 30분
        assertEquals(0, result[0].dayOffset)
        assertEquals(23, result[0].hour)
        assertEquals(30, result[0].minutes)
        // 다음날 0시: 60분
        assertEquals(1, result[1].dayOffset)
        assertEquals(0, result[1].hour)
        assertEquals(60, result[1].minutes)
        // 다음날 1시: 30분
        assertEquals(1, result[2].dayOffset)
        assertEquals(1, result[2].hour)
        assertEquals(30, result[2].minutes)
    }

    // ==================== 4-2-1. 자정 넘김 상세 리스트 노출 테스트 ====================

    @Test
    fun `자정 넘김 스케줄은 당일 PM과 다음날 AM 모두 상세 리스트에 포함`() {
        // Given: 월요일 23시 30분 시작 60분 스케줄 (자정을 넘김)
        val autoRuns = listOf(
            createAutoRun(
                hour = 23,
                minute = 30,
                durationMinutes = 60,
                enabledDays = """["MON"]""",
                label = "야간 집중"
            )
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then: 월요일 PM 상세 리스트에 포함
        val mondayPmRow = result.rows.find { it.dayOfWeek == DayOfWeek.MONDAY && it.period == Period.PM }
        assertNotNull("Monday PM row should exist", mondayPmRow)
        assertEquals(
            "Monday PM should have 1 timeSlot",
            1, mondayPmRow!!.timeSlots.size
        )
        assertEquals("야간 집중", mondayPmRow.timeSlots[0].label)

        // Then: 화요일 AM 상세 리스트에도 포함 (자정 넘김 기여)
        val tuesdayAmRow = result.rows.find { it.dayOfWeek == DayOfWeek.TUESDAY && it.period == Period.AM }
        assertNotNull("Tuesday AM row should exist", tuesdayAmRow)
        assertEquals(
            "Tuesday AM should have 1 timeSlot (midnight crossing contribution)",
            1, tuesdayAmRow!!.timeSlots.size
        )
        assertEquals("야간 집중", tuesdayAmRow.timeSlots[0].label)

        // Then: 셀 데이터도 정확한지 확인
        val monday23Cell = mondayPmRow.cells.find { it.hour == 23 }
        assertEquals(30, monday23Cell!!.totalMinutes)
        val tuesday0Cell = tuesdayAmRow.cells.find { it.hour == 0 }
        assertEquals(30, tuesday0Cell!!.totalMinutes)
    }

    @Test
    fun `자정 넘김 스케줄이 당일 PM에만 있고 AM에는 없어야 할 때 정확히 분리`() {
        // Given: 월요일 23시 시작 30분 스케줄 (자정 안 넘김)
        val autoRuns = listOf(
            createAutoRun(
                hour = 23,
                minute = 0,
                durationMinutes = 30,
                enabledDays = """["MON"]"""
            )
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then: 월요일 PM에만 포함
        val mondayPmRow = result.rows.find { it.dayOfWeek == DayOfWeek.MONDAY && it.period == Period.PM }
        assertEquals(1, mondayPmRow!!.timeSlots.size)

        // Then: 화요일 AM에는 포함되지 않음
        val tuesdayAmRow = result.rows.find { it.dayOfWeek == DayOfWeek.TUESDAY && it.period == Period.AM }
        assertEquals(0, tuesdayAmRow!!.timeSlots.size)
    }

    @Test
    fun `자정 넘김 시 동일 스케줄이 중복 저장되지 않음`() {
        // Given: 월요일 22시 시작 180분 스케줄 (22, 23, 0시에 분배 - PM과 AM 모두 기여)
        val autoRuns = listOf(
            createAutoRun(
                hour = 22,
                minute = 0,
                durationMinutes = 180,
                enabledDays = """["MON"]""",
                label = "장시간 집중"
            )
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then: 월요일 PM (22시, 23시 기여)
        val mondayPmRow = result.rows.find { it.dayOfWeek == DayOfWeek.MONDAY && it.period == Period.PM }
        assertEquals("Monday PM should have exactly 1 unique timeSlot", 1, mondayPmRow!!.timeSlots.size)

        // Then: 화요일 AM (0시 기여)
        val tuesdayAmRow = result.rows.find { it.dayOfWeek == DayOfWeek.TUESDAY && it.period == Period.AM }
        assertEquals("Tuesday AM should have exactly 1 unique timeSlot", 1, tuesdayAmRow!!.timeSlots.size)

        // Then: 동일한 ID인지 확인 (같은 스케줄)
        assertEquals(mondayPmRow.timeSlots[0].id, tuesdayAmRow.timeSlots[0].id)
    }

    // ==================== enabledDays 파싱 테스트 ====================

    @Test
    fun `enabledDays JSON 배열을 DayOfWeek 리스트로 파싱`() {
        // Given
        val enabledDaysJson = """["MON","TUE","WED","THU","FRI"]"""

        // When
        val result = WeeklyHeatmapCalculator.parseEnabledDays(enabledDaysJson)

        // Then
        assertEquals(5, result.size)
        assertTrue(result.contains(DayOfWeek.MONDAY))
        assertTrue(result.contains(DayOfWeek.TUESDAY))
        assertTrue(result.contains(DayOfWeek.WEDNESDAY))
        assertTrue(result.contains(DayOfWeek.THURSDAY))
        assertTrue(result.contains(DayOfWeek.FRIDAY))
    }

    @Test
    fun `잘못된 JSON은 빈 리스트 반환`() {
        // Given
        val invalidJson = "invalid json"

        // When
        val result = WeeklyHeatmapCalculator.parseEnabledDays(invalidJson)

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== 4-3. 겹치는 시간대 합산 테스트 ====================

    @Test
    fun `같은 시간 슬롯에 2개 시간대는 합산되고 최대 60분으로 cap`() {
        // Given: 평일 월요일에만 활성화된 두 개의 스케줄이 9시에 각각 40분씩
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 40, enabledDays = """["MON"]"""),
            createAutoRun(hour = 9, minute = 0, durationMinutes = 40, enabledDays = """["MON"]""")
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        val mondayAmRow = result.rows.find { it.dayOfWeek == DayOfWeek.MONDAY && it.period == Period.AM }
        assertNotNull(mondayAmRow)
        val nineOclockCell = mondayAmRow!!.cells.find { it.hour == 9 }
        assertNotNull(nineOclockCell)
        // 40 + 40 = 80 → cap 60
        assertEquals(60, nineOclockCell!!.totalMinutes)
        assertEquals(HeatmapLevel.MAX, nineOclockCell.level)
    }

    // ==================== 4-4. 비활성 시간대 처리 테스트 ====================

    @Test
    fun `isEnabled=false 시간대는 히트맵에서 제외`() {
        // Given
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 60, enabledDays = """["MON"]""", isEnabled = true),
            createAutoRun(hour = 10, minute = 0, durationMinutes = 60, enabledDays = """["MON"]""", isEnabled = false)
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        val mondayAmRow = result.rows.find { it.dayOfWeek == DayOfWeek.MONDAY && it.period == Period.AM }
        assertNotNull(mondayAmRow)
        val nineOclockCell = mondayAmRow!!.cells.find { it.hour == 9 }
        val tenOclockCell = mondayAmRow.cells.find { it.hour == 10 }
        assertEquals(60, nineOclockCell!!.totalMinutes)
        assertEquals(0, tenOclockCell!!.totalMinutes)  // isEnabled=false이므로 제외
    }

    @Test
    fun `모든 시간대가 비활성화되면 EMPTY 반환`() {
        // Given
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 60, enabledDays = """["MON"]""", isEnabled = false)
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        assertEquals(0, result.rows.size)  // EMPTY
    }

    // ==================== 전체 계산 테스트 ====================

    @Test
    fun `14행(7요일 x AM,PM) 생성 검증`() {
        // Given
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 60, enabledDays = """["MON","TUE","WED","THU","FRI"]""")
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        assertEquals(14, result.rows.size)
        // 각 요일마다 AM, PM 2개씩
        DayOfWeek.values().forEach { day ->
            val amRow = result.rows.find { it.dayOfWeek == day && it.period == Period.AM }
            val pmRow = result.rows.find { it.dayOfWeek == day && it.period == Period.PM }
            assertNotNull("$day AM row should exist", amRow)
            assertNotNull("$day PM row should exist", pmRow)
        }
    }

    @Test
    fun `각 행은 12개 셀 포함`() {
        // Given
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 60, enabledDays = """["MON"]""")
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        result.rows.forEach { row ->
            assertEquals(12, row.cells.size)
        }
    }

    @Test
    fun `Summary에 오전,오후 총 시간과 밀집 시간대 포함`() {
        // Given: 평일 오전 9시에 60분, 오후 2시에 30분
        val autoRuns = listOf(
            createAutoRun(hour = 9, minute = 0, durationMinutes = 60, enabledDays = """["MON","TUE","WED","THU","FRI"]"""),
            createAutoRun(hour = 14, minute = 0, durationMinutes = 30, enabledDays = """["MON","TUE","WED","THU","FRI"]""")
        )

        // When
        val result = WeeklyHeatmapCalculator.calculate(autoRuns)

        // Then
        // 오전 총 시간: 60분 * 5일 = 300분
        assertEquals(300, result.summary.amTotalMinutes)
        // 오후 총 시간: 30분 * 5일 = 150분
        assertEquals(150, result.summary.pmTotalMinutes)
        // 밀집 시간대 확인 (오전 9시가 가장 밀집)
        assertEquals("오전 9~10시", result.summary.peakTimeRange)
    }

    // ==================== HeatmapLevel 테스트 ====================

    @Test
    fun `HeatmapLevel fromMinutes 정확성 검증`() {
        assertEquals(HeatmapLevel.NONE, HeatmapLevel.fromMinutes(0))
        assertEquals(HeatmapLevel.LIGHT, HeatmapLevel.fromMinutes(1))
        assertEquals(HeatmapLevel.LIGHT, HeatmapLevel.fromMinutes(15))
        assertEquals(HeatmapLevel.MEDIUM, HeatmapLevel.fromMinutes(16))
        assertEquals(HeatmapLevel.MEDIUM, HeatmapLevel.fromMinutes(30))
        assertEquals(HeatmapLevel.HIGH, HeatmapLevel.fromMinutes(31))
        assertEquals(HeatmapLevel.HIGH, HeatmapLevel.fromMinutes(45))
        assertEquals(HeatmapLevel.MAX, HeatmapLevel.fromMinutes(46))
        assertEquals(HeatmapLevel.MAX, HeatmapLevel.fromMinutes(60))
    }

    // ==================== 헬퍼 함수 ====================

    private fun createAutoRun(
        hour: Int,
        minute: Int,
        durationMinutes: Int,
        enabledDays: String,
        isEnabled: Boolean = true,
        presetType: String = "STANDARD",
        label: String? = null
    ): TimeBasedAutoRun {
        return TimeBasedAutoRun(
            id = java.util.UUID.randomUUID().toString(),
            hour = hour,
            minute = minute,
            durationMinutes = durationMinutes,
            presetType = presetType,
            enabledDays = enabledDays,
            label = label,
            isEnabled = isEnabled
        )
    }
}
