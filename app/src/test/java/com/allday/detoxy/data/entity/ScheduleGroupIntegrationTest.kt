package com.allday.detoxy.data.entity

import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import org.junit.Assert.*
import org.junit.Test

/**
 * ScheduleGroup 연동 엔티티 단위 테스트
 *
 * 2.5차 고도화 Week 4, Day 25-26: ScheduleGroup 연동 필드 검증
 *
 * ## 테스트 범위
 * - TimeBasedAutoRun의 scheduleGroupId 필드 검증
 * - TimeBasedAutoRun의 isIndependent 필드 검증
 * - LocationBasedAutoRun의 linkedScheduleGroupId 필드 검증
 * - 독립 모드 vs 종속 모드 검증
 *
 * ## 테스트 전략
 * v5 마이그레이션으로 추가된 ScheduleGroup 연동 필드의 동작을 검증합니다.
 *
 * @see ScheduleGroup
 * @see TimeBasedAutoRun
 * @see LocationBasedAutoRun
 * @see docs/02.5_autosetting_todolist.md §4.2.2
 */
class ScheduleGroupIntegrationTest {

    // ========== TimeBasedAutoRun 연동 테스트 ==========

    @Test
    fun `TimeBasedAutoRun은 기본적으로 독립 모드이다`() {
        // Given & When
        val autoRun = TimeBasedAutoRun(
            hour = 10,
            minute = 0,
            durationMinutes = 25,
            presetType = "STANDARD",
            enabledDays = "[\"MON\",\"TUE\",\"WED\"]",
            label = "오전 집중"
        )

        // Then
        assertNull("기본적으로 scheduleGroupId는 null", autoRun.scheduleGroupId)
        assertTrue("기본적으로 isIndependent는 true", autoRun.isIndependent)
    }

    @Test
    fun `TimeBasedAutoRun은 ScheduleGroup에 종속될 수 있다`() {
        // Given
        val scheduleGroupId = "group-123"

        // When
        val autoRun = TimeBasedAutoRun(
            hour = 14,
            minute = 0,
            durationMinutes = 45,
            presetType = "BALANCED",
            enabledDays = "[\"MON\",\"WED\",\"FRI\"]",
            label = "오후 집중",
            scheduleGroupId = scheduleGroupId,
            isIndependent = false
        )

        // Then
        assertEquals("scheduleGroupId 설정", scheduleGroupId, autoRun.scheduleGroupId)
        assertFalse("종속 모드로 설정", autoRun.isIndependent)
    }

    @Test
    fun `TimeBasedAutoRun은 ScheduleGroup에 속하지만 독립 실행할 수 있다`() {
        // Given
        val scheduleGroupId = "group-456"

        // When
        val autoRun = TimeBasedAutoRun(
            hour = 18,
            minute = 0,
            durationMinutes = 30,
            presetType = "RELAXED",
            enabledDays = "[\"SAT\",\"SUN\"]",
            label = "저녁 집중",
            scheduleGroupId = scheduleGroupId,
            isIndependent = true // 그룹에 속하지만 항상 실행
        )

        // Then
        assertEquals("scheduleGroupId 설정", scheduleGroupId, autoRun.scheduleGroupId)
        assertTrue("독립 모드로 설정", autoRun.isIndependent)
    }

    @Test
    fun `TimeBasedAutoRun의 scheduleGroupId가 null이면 isIndependent는 무시된다`() {
        // Given & When
        val autoRunWithFalseIndependent = TimeBasedAutoRun(
            hour = 9,
            minute = 0,
            durationMinutes = 25,
            presetType = "STANDARD",
            enabledDays = "[\"MON\"]",
            label = "테스트",
            scheduleGroupId = null,
            isIndependent = false // scheduleGroupId가 null이므로 의미 없음
        )

        // Then
        assertNull("scheduleGroupId는 null", autoRunWithFalseIndependent.scheduleGroupId)
        // isIndependent는 false지만, scheduleGroupId가 null이므로 항상 독립 실행
        assertFalse(autoRunWithFalseIndependent.isIndependent)
    }

    // ========== LocationBasedAutoRun 연동 테스트 ==========

    @Test
    fun `LocationBasedAutoRun은 기본적으로 ScheduleGroup과 연결되지 않는다`() {
        // Given & When
        val location = LocationBasedAutoRun(
            label = "회사",
            address = "서울시 강남구",
            latitude = 37.5,
            longitude = 127.0,
            radiusMeters = 200,
            durationMinutes = 25,
            presetType = "BALANCED",
            triggerType = "ENTER"
        )

        // Then
        assertNull("기본적으로 linkedScheduleGroupId는 null", location.linkedScheduleGroupId)
    }

    @Test
    fun `LocationBasedAutoRun은 ScheduleGroup과 연결될 수 있다`() {
        // Given
        val scheduleGroupId = "work-schedule-group"

        // When
        val location = LocationBasedAutoRun(
            label = "회사",
            address = "서울시 강남구",
            latitude = 37.5,
            longitude = 127.0,
            radiusMeters = 200,
            durationMinutes = 25,
            presetType = "BALANCED",
            triggerType = "ENTER",
            linkedScheduleGroupId = scheduleGroupId
        )

        // Then
        assertEquals("linkedScheduleGroupId 설정", scheduleGroupId, location.linkedScheduleGroupId)
    }

    @Test
    fun `LocationBasedAutoRun은 ScheduleGroup 연결을 해제할 수 있다`() {
        // Given
        val original = LocationBasedAutoRun(
            label = "집",
            address = "서울시 서초구",
            latitude = 37.4,
            longitude = 127.1,
            radiusMeters = 150,
            durationMinutes = 30,
            presetType = "RELAXED",
            triggerType = "ENTER",
            linkedScheduleGroupId = "home-schedule"
        )

        // When
        val unlinked = original.copy(linkedScheduleGroupId = null)

        // Then
        assertNull("linkedScheduleGroupId 해제", unlinked.linkedScheduleGroupId)
        assertEquals("다른 필드는 유지", original.label, unlinked.label)
    }

    // ========== 시나리오 기반 테스트 ==========

    @Test
    fun `시나리오 회사 위치 진입 시 업무 시간표 활성화`() {
        // Given: ScheduleGroup 생성
        val workSchedule = ScheduleGroup(
            name = "업무 시간표",
            description = "회사에서 사용할 집중 시간표"
        )

        // Given: TimeBasedAutoRun 3개 생성 (모두 종속 모드)
        val morning = TimeBasedAutoRun(
            hour = 10, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "오전 집중",
            scheduleGroupId = workSchedule.id,
            isIndependent = false
        )
        val afternoon = TimeBasedAutoRun(
            hour = 14, minute = 0, durationMinutes = 45,
            presetType = "BALANCED", enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "오후 집중",
            scheduleGroupId = workSchedule.id,
            isIndependent = false
        )
        val evening = TimeBasedAutoRun(
            hour = 16, minute = 30, durationMinutes = 30,
            presetType = "RELAXED", enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "저녁 집중",
            scheduleGroupId = workSchedule.id,
            isIndependent = false
        )

        // Given: LocationBasedAutoRun 생성 (회사 위치)
        val office = LocationBasedAutoRun(
            label = "회사",
            address = "서울시 강남구 테헤란로",
            latitude = 37.5,
            longitude = 127.0,
            radiusMeters = 200,
            durationMinutes = 0, // 위치 기반은 duration 0
            presetType = "BALANCED",
            triggerType = "ENTER",
            linkedScheduleGroupId = workSchedule.id
        )

        // Then: 연결 검증
        assertEquals("회사 위치는 업무 시간표와 연결", workSchedule.id, office.linkedScheduleGroupId)
        assertEquals("오전 집중은 업무 시간표에 속함", workSchedule.id, morning.scheduleGroupId)
        assertEquals("오후 집중은 업무 시간표에 속함", workSchedule.id, afternoon.scheduleGroupId)
        assertEquals("저녁 집중은 업무 시간표에 속함", workSchedule.id, evening.scheduleGroupId)
        
        // Then: 모드 검증
        assertFalse("오전 집중은 종속 모드", morning.isIndependent)
        assertFalse("오후 집중은 종속 모드", afternoon.isIndependent)
        assertFalse("저녁 집중은 종속 모드", evening.isIndependent)
    }

    @Test
    fun `시나리오 일부 시간표는 항상 실행되고 일부는 위치 기반으로 활성화`() {
        // Given: ScheduleGroup 생성
        val flexibleSchedule = ScheduleGroup(
            name = "유연 시간표",
            description = "상황에 따라 활성화"
        )

        // Given: 독립 모드 TimeBasedAutoRun (항상 실행)
        val alwaysRun = TimeBasedAutoRun(
            hour = 9, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "아침 루틴 (항상)",
            scheduleGroupId = flexibleSchedule.id,
            isIndependent = true // 그룹에 속하지만 항상 실행
        )

        // Given: 종속 모드 TimeBasedAutoRun (위치 기반 활성화)
        val locationBased = TimeBasedAutoRun(
            hour = 15, minute = 0, durationMinutes = 45,
            presetType = "BALANCED", enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
            label = "오후 집중 (위치 기반)",
            scheduleGroupId = flexibleSchedule.id,
            isIndependent = false // 그룹 활성화 시에만 실행
        )

        // Then: 모드 검증
        assertEquals("두 시간표 모두 같은 그룹에 속함", flexibleSchedule.id, alwaysRun.scheduleGroupId)
        assertEquals("두 시간표 모두 같은 그룹에 속함", flexibleSchedule.id, locationBased.scheduleGroupId)
        assertTrue("아침 루틴은 항상 실행", alwaysRun.isIndependent)
        assertFalse("오후 집중은 위치 기반", locationBased.isIndependent)
    }
}

