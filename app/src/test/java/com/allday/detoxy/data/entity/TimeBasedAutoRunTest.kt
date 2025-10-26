package com.allday.detoxy.data.entity

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import org.junit.Assert.*
import org.junit.Test

/**
 * TimeBasedAutoRun 엔티티 단위 테스트
 *
 * 2차 고도화: 자동 실행 기능
 */
class TimeBasedAutoRunTest {

    @Test
    fun `TimeBasedAutoRun 생성 시 기본값이 올바르게 설정된다`() {
        // Given
        val autoRun = TimeBasedAutoRun(
            hour = 9,
            minute = 0,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]"
        )

        // Then
        assertNotNull("ID가 생성되어야 함", autoRun.id)
        assertEquals(9, autoRun.hour)
        assertEquals(0, autoRun.minute)
        assertEquals(45, autoRun.durationMinutes)
        assertEquals("STANDARD", autoRun.presetType)
        assertTrue("기본적으로 활성화되어야 함", autoRun.isEnabled)
        assertNull("라벨이 없을 수 있음", autoRun.label)
        assertTrue("생성 시간이 현재 시간 근처여야 함", 
            System.currentTimeMillis() - autoRun.createdAt < 1000)
    }

    @Test
    fun `시간 범위가 올바른지 검증한다`() {
        // Given & When & Then
        // 정상 범위
        val valid1 = TimeBasedAutoRun(
            hour = 0,
            minute = 0,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = "[]"
        )
        assertEquals(0, valid1.hour)
        
        val valid2 = TimeBasedAutoRun(
            hour = 23,
            minute = 59,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = "[]"
        )
        assertEquals(23, valid2.hour)
        assertEquals(59, valid2.minute)
    }

    @Test
    fun `활성화와 비활성화 토글이 작동한다`() {
        // Given
        val autoRun = TimeBasedAutoRun(
            hour = 14,
            minute = 30,
            durationMinutes = 60,
            presetType = "RELAXED",
            enabledDays = "[\"SAT\",\"SUN\"]",
            isEnabled = true
        )

        // When
        val disabled = autoRun.copy(isEnabled = false)

        // Then
        assertTrue(autoRun.isEnabled)
        assertFalse(disabled.isEnabled)
        assertEquals(autoRun.id, disabled.id)
        assertEquals(autoRun.hour, disabled.hour)
    }

    @Test
    fun `라벨이 있는 자동 실행 생성이 가능하다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 9,
            minute = 0,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "오전 집중"
        )

        // Then
        assertEquals("오전 집중", autoRun.label)
    }

    @Test
    fun `enabledDays JSON 형식이 올바르다`() {
        // Given
        val autoRun = TimeBasedAutoRun(
            hour = 9,
            minute = 0,
            durationMinutes = 45,
            presetType = "STANDARD",
            enabledDays = "[\"MON\",\"TUE\",\"WED\"]"
        )

        // Then
        assertTrue("enabledDays가 JSON 배열 형식이어야 함", 
            autoRun.enabledDays.startsWith("[") && autoRun.enabledDays.endsWith("]"))
        assertTrue("MON이 포함되어야 함", autoRun.enabledDays.contains("MON"))
        assertTrue("TUE가 포함되어야 함", autoRun.enabledDays.contains("TUE"))
        assertTrue("WED가 포함되어야 함", autoRun.enabledDays.contains("WED"))
    }
}

