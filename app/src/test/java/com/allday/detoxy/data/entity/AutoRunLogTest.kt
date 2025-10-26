package com.allday.detoxy.data.entity

import com.allday.detoxy.data.local.entity.AutoRunLog
import org.junit.Assert.*
import org.junit.Test

/**
 * AutoRunLog 엔티티 단위 테스트
 *
 * 2차 고도화: 자동 실행 로그
 */
class AutoRunLogTest {

    @Test
    fun `AutoRunLog 생성 시 기본값이 올바르게 설정된다`() {
        // Given & When
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "auto-run-123",
            triggerTime = System.currentTimeMillis(),
            result = "NOTIFICATION_SHOWN"
        )

        // Then
        assertNotNull("ID가 생성되어야 함", log.id)
        assertEquals("TIME", log.triggerType)
        assertEquals("auto-run-123", log.triggerSourceId)
        assertEquals("NOTIFICATION_SHOWN", log.result)
        assertNull("실패 이유가 없을 수 있음", log.failureReason)
        assertNull("세션 ID가 없을 수 있음", log.sessionId)
        assertNull("GPS 정확도가 없을 수 있음", log.gpsAccuracyMeters)
    }

    @Test
    fun `시간 기반 자동 실행 로그가 생성된다`() {
        // Given & When
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "time-run-001",
            triggerTime = System.currentTimeMillis(),
            result = "STARTED",
            sessionId = "session-123"
        )

        // Then
        assertEquals("TIME", log.triggerType)
        assertEquals("STARTED", log.result)
        assertEquals("session-123", log.sessionId)
        assertNull("시간 기반은 GPS 정확도가 없음", log.gpsAccuracyMeters)
        assertNull("시간 기반은 체류 시간이 없음", log.dwellSeconds)
    }

    @Test
    fun `위치 기반 자동 실행 로그가 GPS 정보를 포함한다`() {
        // Given & When
        val log = AutoRunLog(
            triggerType = "LOCATION",
            triggerSourceId = "location-run-001",
            triggerTime = System.currentTimeMillis(),
            result = "STARTED",
            sessionId = "session-456",
            gpsAccuracyMeters = 15.5f,
            dwellSeconds = 120
        )

        // Then
        assertEquals("LOCATION", log.triggerType)
        assertEquals("STARTED", log.result)
        assertNotNull("GPS 정확도가 있어야 함", log.gpsAccuracyMeters)
        assertNotNull("체류 시간이 있어야 함", log.dwellSeconds)
        assertEquals(15.5f, log.gpsAccuracyMeters!!, 0.01f)
        assertEquals(120, log.dwellSeconds)
    }

    @Test
    fun `실패한 자동 실행 로그가 실패 이유를 포함한다`() {
        // Given & When
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "auto-run-failed",
            triggerTime = System.currentTimeMillis(),
            result = "FAILED",
            failureReason = "PERMISSION_DENIED"
        )

        // Then
        assertEquals("FAILED", log.result)
        assertEquals("PERMISSION_DENIED", log.failureReason)
    }

    @Test
    fun `건너뛴 자동 실행 로그가 기록된다`() {
        // Given & When
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "auto-run-skipped",
            triggerTime = System.currentTimeMillis(),
            result = "SKIPPED",
            failureReason = "TIMER_ALREADY_RUNNING"
        )

        // Then
        assertEquals("SKIPPED", log.result)
        assertEquals("TIMER_ALREADY_RUNNING", log.failureReason)
    }

    @Test
    fun `metaJson이 올바르게 저장된다`() {
        // Given & When
        val metaJson = """{"delay_seconds": 300, "notification_type": "PRE"}"""
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "auto-run-001",
            triggerTime = System.currentTimeMillis(),
            result = "NOTIFICATION_SHOWN",
            metaJson = metaJson
        )

        // Then
        assertEquals(metaJson, log.metaJson)
        assertTrue("metaJson이 JSON 형식이어야 함", 
            log.metaJson?.startsWith("{") == true && log.metaJson?.endsWith("}") == true)
    }

    @Test
    fun `트리거 시간이 올바르게 기록된다`() {
        // Given
        val triggerTime = System.currentTimeMillis()

        // When
        val log = AutoRunLog(
            triggerType = "TIME",
            triggerSourceId = "auto-run-001",
            triggerTime = triggerTime,
            result = "STARTED"
        )

        // Then
        assertEquals(triggerTime, log.triggerTime)
        assertTrue("트리거 시간이 현재 시간 근처여야 함",
            System.currentTimeMillis() - log.triggerTime < 1000)
    }
}

