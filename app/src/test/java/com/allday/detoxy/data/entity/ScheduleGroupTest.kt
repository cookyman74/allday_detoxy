package com.allday.detoxy.data.entity

import com.allday.detoxy.data.local.entity.ScheduleGroup
import org.junit.Assert.*
import org.junit.Test

/**
 * ScheduleGroup 엔티티 단위 테스트
 *
 * 2.5차 고도화 Week 4, Day 25-26: ScheduleGroup 데이터 구조 검증
 *
 * ## 테스트 범위
 * - ScheduleGroup 엔티티 필드 검증
 * - 기본값 검증
 * - UUID 생성 검증
 * - createdAt 타임스탬프 검증
 *
 * ## 테스트 전략
 * Repository나 DAO 의존성이 필요 없는 데이터 구조만 테스트합니다.
 *
 * @see ScheduleGroup
 * @see docs/02.5_autosetting_todolist.md §4.2.2
 */
class ScheduleGroupTest {

    // ========== ScheduleGroup 엔티티 기본 테스트 ==========

    @Test
    fun `ScheduleGroup 엔티티가 올바르게 생성된다`() {
        // Given
        val name = "업무 시간표"
        val description = "회사에 있을 때 사용할 시간표"
        
        // When
        val scheduleGroup = ScheduleGroup(
            name = name,
            description = description
        )

        // Then
        assertNotNull("ID는 자동 생성되어야 함", scheduleGroup.id)
        assertEquals(name, scheduleGroup.name)
        assertEquals(description, scheduleGroup.description)
        assertTrue("기본적으로 활성화 상태여야 함", scheduleGroup.isActive)
        assertTrue("createdAt은 현재 시간이어야 함", scheduleGroup.createdAt > 0)
    }

    @Test
    fun `ScheduleGroup은 description이 선택적이다`() {
        // Given & When
        val scheduleGroupWithDescription = ScheduleGroup(
            name = "공부 루틴",
            description = "집중 공부 시간표"
        )
        
        val scheduleGroupWithoutDescription = ScheduleGroup(
            name = "공부 루틴"
        )

        // Then
        assertEquals("집중 공부 시간표", scheduleGroupWithDescription.description)
        assertNull("description은 null일 수 있음", scheduleGroupWithoutDescription.description)
    }

    @Test
    fun `ScheduleGroup은 고유한 ID를 가진다`() {
        // Given & When
        val group1 = ScheduleGroup(name = "그룹 1")
        val group2 = ScheduleGroup(name = "그룹 2")

        // Then
        assertNotEquals("각 ScheduleGroup은 고유한 ID를 가져야 함", group1.id, group2.id)
        assertTrue("ID는 UUID 형식이어야 함 (최소 32자)", group1.id.length >= 32)
        assertTrue("ID는 UUID 형식이어야 함 (최소 32자)", group2.id.length >= 32)
    }

    @Test
    fun `ScheduleGroup은 기본적으로 활성화 상태이다`() {
        // Given & When
        val scheduleGroup = ScheduleGroup(name = "테스트 그룹")

        // Then
        assertTrue("기본 isActive는 true여야 함", scheduleGroup.isActive)
    }

    @Test
    fun `ScheduleGroup은 비활성화 상태로 생성할 수 있다`() {
        // Given & When
        val scheduleGroup = ScheduleGroup(
            name = "테스트 그룹",
            isActive = false
        )

        // Then
        assertFalse("isActive를 false로 설정할 수 있음", scheduleGroup.isActive)
    }

    @Test
    fun `ScheduleGroup의 createdAt은 현재 시간이다`() {
        // Given
        val beforeCreation = System.currentTimeMillis()
        
        // When
        Thread.sleep(10) // 최소 10ms 차이 보장
        val scheduleGroup = ScheduleGroup(name = "테스트 그룹")
        Thread.sleep(10)
        val afterCreation = System.currentTimeMillis()

        // Then
        assertTrue("createdAt은 생성 전 시간보다 커야 함", scheduleGroup.createdAt >= beforeCreation)
        assertTrue("createdAt은 생성 후 시간보다 작아야 함", scheduleGroup.createdAt <= afterCreation)
    }

    @Test
    fun `ScheduleGroup은 모든 필드를 수동으로 설정할 수 있다`() {
        // Given
        val id = "test-id-123"
        val name = "사용자 정의 그룹"
        val description = "설명"
        val isActive = false
        val createdAt = 1234567890L

        // When
        val scheduleGroup = ScheduleGroup(
            id = id,
            name = name,
            description = description,
            isActive = isActive,
            createdAt = createdAt
        )

        // Then
        assertEquals(id, scheduleGroup.id)
        assertEquals(name, scheduleGroup.name)
        assertEquals(description, scheduleGroup.description)
        assertEquals(isActive, scheduleGroup.isActive)
        assertEquals(createdAt, scheduleGroup.createdAt)
    }

    @Test
    fun `ScheduleGroup의 name은 필수 필드이다`() {
        // Given
        val name = "필수 이름"

        // When
        val scheduleGroup = ScheduleGroup(name = name)

        // Then
        assertEquals("name은 반드시 설정되어야 함", name, scheduleGroup.name)
        assertFalse("name은 빈 문자열이 아니어야 함", scheduleGroup.name.isEmpty())
    }

    @Test
    fun `ScheduleGroup은 data class copy를 지원한다`() {
        // Given
        val original = ScheduleGroup(
            name = "원본 그룹",
            description = "원본 설명"
        )

        // When
        val copied = original.copy(name = "복사된 그룹")

        // Then
        assertEquals("복사된 그룹", copied.name)
        assertEquals(original.description, copied.description)
        assertEquals(original.id, copied.id)
        assertEquals(original.isActive, copied.isActive)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    fun `ScheduleGroup은 equals와 hashCode를 올바르게 구현한다`() {
        // Given
        val id = "same-id"
        val group1 = ScheduleGroup(
            id = id,
            name = "그룹 1",
            description = "설명 1"
        )
        val group2 = ScheduleGroup(
            id = id,
            name = "그룹 1",
            description = "설명 1"
        )
        val group3 = ScheduleGroup(
            id = "different-id",
            name = "그룹 1",
            description = "설명 1"
        )

        // Then
        assertEquals("같은 데이터를 가진 객체는 같아야 함", group1, group2)
        assertNotEquals("ID가 다르면 다른 객체여야 함", group1, group3)
        assertEquals("같은 객체는 같은 hashCode를 가져야 함", group1.hashCode(), group2.hashCode())
    }
}

