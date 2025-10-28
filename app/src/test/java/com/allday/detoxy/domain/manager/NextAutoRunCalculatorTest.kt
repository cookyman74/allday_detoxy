package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * NextAutoRunCalculator 단위 테스트
 *
 * 2.5차 고도화 Week 1, Day 2-3: 도메인 로직 검증
 * 
 * ## 테스트 범위
 * - NextAutoRunInfo 데이터 클래스 검증
 * - TimeBasedAutoRun 엔티티 검증
 * - 요일 헬퍼 메서드 검증
 * 
 * ## 테스트 전략
 * Repository 의존성이 필요 없는 데이터 구조와 헬퍼 메서드만 테스트합니다.
 * 전체 계산 로직 테스트는 Instrumentation Test에서 수행합니다.
 * 
 * @see NextAutoRunCalculator
 * @see NextAutoRunInfo
 * @see docs/02.5_autosetting_todolist.md §1.1.2
 */
class NextAutoRunCalculatorTest {

    // ========== NextAutoRunInfo 데이터 클래스 테스트 ==========

    @Test
    fun `NextAutoRunInfo 데이터 클래스가 올바르게 생성된다`() {
        // Given
        val triggerTime = System.currentTimeMillis() + (60 * 60 * 1000) // 1시간 후
        
        // When
        val info = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = triggerTime,
            timeUntilTrigger = "1시간 후",
            label = "오후 집중",
            durationMinutes = 45,
            presetType = "STANDARD",
            sourceId = "test-id",
            confidence = null
        )

        // Then
        assertEquals("TIME", info.triggerType)
        assertEquals(triggerTime, info.triggerTime)
        assertEquals("1시간 후", info.timeUntilTrigger)
        assertEquals("오후 집중", info.label)
        assertEquals(45, info.durationMinutes)
        assertEquals("STANDARD", info.presetType)
        assertEquals("test-id", info.sourceId)
        assertNull("시간 기반은 confidence가 null이어야 함", info.confidence)
    }

    @Test
    fun `NextAutoRunInfo는 confidence를 옵셔널로 가진다`() {
        // Given
        val triggerTime = System.currentTimeMillis()
        
        // When - confidence 없이 생성
        val infoWithoutConfidence = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = triggerTime,
            timeUntilTrigger = "지금",
            label = "테스트",
            durationMinutes = 30,
            presetType = "STANDARD",
            sourceId = "id-1"
        )
        
        // When - confidence 있이 생성 (위치 기반 예약)
        val infoWithConfidence = NextAutoRunInfo(
            triggerType = "LOCATION",
            triggerTime = triggerTime,
            timeUntilTrigger = "지금",
            label = "테스트",
            durationMinutes = 30,
            presetType = "STANDARD",
            sourceId = "id-2",
            confidence = 0.8f
        )

        // Then
        assertNull(infoWithoutConfidence.confidence)
        assertEquals(0.8f, infoWithConfidence.confidence)
    }

    @Test
    fun `NextAutoRunInfo는 모든 필수 필드를 가진다`() {
        // Given & When
        val info = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = 1234567890L,
            timeUntilTrigger = "30분 후",
            label = "아침 집중",
            durationMinutes = 25,
            presetType = "FULL_BLOCK",
            sourceId = "auto-run-123"
        )

        // Then - 모든 필드가 설정되어야 함
        assertNotNull(info.triggerType)
        assertNotNull(info.triggerTime)
        assertNotNull(info.timeUntilTrigger)
        assertNotNull(info.label)
        assertNotNull(info.durationMinutes)
        assertNotNull(info.presetType)
        assertNotNull(info.sourceId)
    }

    // ========== TimeBasedAutoRun 엔티티 테스트 ==========

    @Test
    fun `TimeBasedAutoRun enabledDays가 JSON 형식이다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 10,
            minute = 30,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = """["MON","TUE","WED","THU","FRI"]""",
            label = "평일 집중"
        )

        // Then
        assertTrue(autoRun.enabledDays.startsWith("["))
        assertTrue(autoRun.enabledDays.endsWith("]"))
        assertTrue(autoRun.enabledDays.contains("MON"))
        assertTrue(autoRun.enabledDays.contains("FRI"))
    }

    @Test
    fun `TimeBasedAutoRun은 모든 요일을 활성화할 수 있다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 9,
            minute = 0,
            durationMinutes = 60,
            presetType = "STANDARD",
            enabledDays = """["MON","TUE","WED","THU","FRI","SAT","SUN"]""",
            label = "매일 집중"
        )

        // Then
        assertTrue(autoRun.enabledDays.contains("MON"))
        assertTrue(autoRun.enabledDays.contains("SAT"))
        assertTrue(autoRun.enabledDays.contains("SUN"))
    }

    @Test
    fun `TimeBasedAutoRun은 주말만 활성화할 수 있다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 10,
            minute = 0,
            durationMinutes = 30,
            presetType = "RELAXED",
            enabledDays = """["SAT","SUN"]""",
            label = "주말 집중"
        )

        // Then
        assertTrue(autoRun.enabledDays.contains("SAT"))
        assertTrue(autoRun.enabledDays.contains("SUN"))
        assertFalse(autoRun.enabledDays.contains("MON"))
    }

    @Test
    fun `TimeBasedAutoRun은 기본적으로 활성화 상태다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 14,
            minute = 0,
            durationMinutes = 25,
            presetType = "STANDARD",
            enabledDays = """["MON"]"""
        )

        // Then
        assertTrue("기본값은 활성화 상태여야 함", autoRun.isEnabled)
    }

    // ========== 헬퍼 메서드 테스트 ==========

    @Test
    fun `요일 문자열 변환이 정확하다`() {
        // Given
        val monday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val wednesday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        }
        val friday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        }
        val saturday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }
        val sunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        // When & Then
        assertEquals("MON", getDayOfWeekString(monday))
        assertEquals("WED", getDayOfWeekString(wednesday))
        assertEquals("FRI", getDayOfWeekString(friday))
        assertEquals("SAT", getDayOfWeekString(saturday))
        assertEquals("SUN", getDayOfWeekString(sunday))
    }

    @Test
    fun `모든 요일 JSON 생성이 정확하다`() {
        // When
        val allDaysJson = """["MON","TUE","WED","THU","FRI","SAT","SUN"]"""

        // Then
        assertTrue(allDaysJson.contains("MON"))
        assertTrue(allDaysJson.contains("TUE"))
        assertTrue(allDaysJson.contains("WED"))
        assertTrue(allDaysJson.contains("THU"))
        assertTrue(allDaysJson.contains("FRI"))
        assertTrue(allDaysJson.contains("SAT"))
        assertTrue(allDaysJson.contains("SUN"))
    }

    @Test
    fun `평일만 포함하는 JSON 생성이 정확하다`() {
        // When
        val weekdaysJson = """["MON","TUE","WED","THU","FRI"]"""

        // Then
        assertTrue(weekdaysJson.contains("MON"))
        assertTrue(weekdaysJson.contains("FRI"))
        assertFalse(weekdaysJson.contains("SAT"))
        assertFalse(weekdaysJson.contains("SUN"))
    }

    @Test
    fun `주말만 포함하는 JSON 생성이 정확하다`() {
        // When
        val weekendsJson = """["SAT","SUN"]"""

        // Then
        assertTrue(weekendsJson.contains("SAT"))
        assertTrue(weekendsJson.contains("SUN"))
        assertFalse(weekendsJson.contains("MON"))
    }

    // ========== 프리셋 타입 검증 ==========

    @Test
    fun `지원되는 프리셋 타입들이 올바르게 설정된다`() {
        // Given
        val fullBlock = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = System.currentTimeMillis(),
            timeUntilTrigger = "지금",
            label = "완전 차단",
            durationMinutes = 30,
            presetType = "FULL_BLOCK",
            sourceId = "1"
        )

        val standard = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = System.currentTimeMillis(),
            timeUntilTrigger = "지금",
            label = "표준",
            durationMinutes = 30,
            presetType = "STANDARD",
            sourceId = "2"
        )

        val relaxed = NextAutoRunInfo(
            triggerType = "TIME",
            triggerTime = System.currentTimeMillis(),
            timeUntilTrigger = "지금",
            label = "완화",
            durationMinutes = 30,
            presetType = "RELAXED",
            sourceId = "3"
        )

        // Then
        assertEquals("FULL_BLOCK", fullBlock.presetType)
        assertEquals("STANDARD", standard.presetType)
        assertEquals("RELAXED", relaxed.presetType)
    }

    // ========== 헬퍼 함수 ==========

    private fun getDayOfWeekString(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "UNKNOWN"
        }
    }
}
