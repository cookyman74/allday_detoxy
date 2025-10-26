package com.allday.detoxy.data.entity

import com.allday.detoxy.data.local.entity.CustomTimerPreset
import org.junit.Assert.*
import org.junit.Test

/**
 * CustomTimerPreset 엔티티 단위 테스트
 *
 * 2차 고도화: 커스텀 타이머 프리셋
 */
class CustomTimerPresetTest {

    @Test
    fun `CustomTimerPreset 생성 시 기본값이 올바르게 설정된다`() {
        // Given & When
        val preset = CustomTimerPreset(
            name = "집중 시간",
            durationMinutes = 37,
            presetType = null
        )

        // Then
        assertNotNull("ID가 생성되어야 함", preset.id)
        assertEquals("집중 시간", preset.name)
        assertEquals(37, preset.durationMinutes)
        assertNull("차단 프리셋이 없을 수 있음", preset.presetType)
        assertEquals(0, preset.usageCount)
        assertEquals(0, preset.displayOrder)
        assertTrue("생성 시간이 현재 시간 근처여야 함",
            System.currentTimeMillis() - preset.createdAt < 1000)
    }

    @Test
    fun `차단 프리셋이 연결된 커스텀 프리셋을 생성할 수 있다`() {
        // Given & When
        val preset = CustomTimerPreset(
            name = "업무 집중",
            durationMinutes = 90,
            presetType = "FULL_BLOCK"
        )

        // Then
        assertEquals("FULL_BLOCK", preset.presetType)
    }

    @Test
    fun `사용 횟수가 증가한다`() {
        // Given
        val preset = CustomTimerPreset(
            name = "짧은 집중",
            durationMinutes = 15,
            presetType = null,
            usageCount = 5
        )

        // When
        val used = preset.copy(usageCount = preset.usageCount + 1)

        // Then
        assertEquals(5, preset.usageCount)
        assertEquals(6, used.usageCount)
    }

    @Test
    fun `표시 순서가 올바르게 설정된다`() {
        // Given
        val preset1 = CustomTimerPreset(
            name = "첫 번째",
            durationMinutes = 25,
            presetType = null,
            displayOrder = 0
        )

        val preset2 = CustomTimerPreset(
            name = "두 번째",
            durationMinutes = 45,
            presetType = null,
            displayOrder = 1
        )

        // Then
        assertEquals(0, preset1.displayOrder)
        assertEquals(1, preset2.displayOrder)
        assertTrue("첫 번째 프리셋이 앞에 와야 함", 
            preset1.displayOrder < preset2.displayOrder)
    }

    @Test
    fun `프리셋 이름 길이 검증`() {
        // Given & When
        val shortName = CustomTimerPreset(name = "짧", durationMinutes = 25)
        val mediumName = CustomTimerPreset(name = "보통 길이", durationMinutes = 25)
        val longName = CustomTimerPreset(name = "아주 긴 프리셋 이름입니다", durationMinutes = 25)

        // Then
        assertEquals(1, shortName.name.length)
        assertEquals(5, mediumName.name.length)
        assertTrue("긴 이름도 허용되어야 함", longName.name.length > 10)
    }

    @Test
    fun `시간 범위가 유효하다`() {
        // Given & When
        val min = CustomTimerPreset(name = "최소", durationMinutes = 5)
        val mid = CustomTimerPreset(name = "중간", durationMinutes = 45)
        val max = CustomTimerPreset(name = "최대", durationMinutes = 180)

        // Then
        assertEquals(5, min.durationMinutes)
        assertEquals(45, mid.durationMinutes)
        assertEquals(180, max.durationMinutes)
    }
}

