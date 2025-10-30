package com.allday.detoxy.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.allday.detoxy.data.local.DetoxyDatabase
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ScheduleGroupRepository 통합 테스트
 *
 * 2.5차 고도화 Week 4, Day 25-26: Repository 기능 검증
 *
 * ## 테스트 범위
 * - CRUD 작업 (insert, update, delete, getAll, getById)
 * - 활성화/비활성화 (toggleActive)
 * - 참조 무결성 (unlinkAllAutoRuns, getLinkedTimeBasedAutoRuns, getLinkedLocations)
 * - 카운트 조회 (getLinkedTimeBasedAutoRunCount, getLinkedLocationCount)
 *
 * ## 테스트 전략
 * - In-memory Room 데이터베이스 사용
 * - 각 테스트마다 DB 초기화
 * - Repository + DAO 통합 검증
 *
 * @see ScheduleGroupRepositoryImpl
 * @see ScheduleGroup
 */
@RunWith(AndroidJUnit4::class)
class ScheduleGroupRepositoryTest {

    private lateinit var database: DetoxyDatabase
    private lateinit var repository: ScheduleGroupRepositoryImpl

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, DetoxyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        repository = ScheduleGroupRepositoryImpl(
            scheduleGroupDao = database.scheduleGroupDao(),
            timeBasedAutoRunDao = database.timeBasedAutoRunDao(),
            locationBasedAutoRunDao = database.locationBasedAutoRunDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CRUD 작업 테스트 ==========

    @Test
    fun insert_addsScheduleGroupToDatabase() = runBlocking {
        // Given
        val scheduleGroup = ScheduleGroup(
            name = "업무 시간표",
            description = "회사에서 사용"
        )

        // When
        repository.insert(scheduleGroup)

        // Then
        val result = repository.getAll().first()
        assertEquals(1, result.size)
        assertEquals(scheduleGroup.name, result[0].name)
        assertEquals(scheduleGroup.description, result[0].description)
    }

    @Test
    fun getAll_returnsAllScheduleGroups() = runBlocking {
        // Given
        val group1 = ScheduleGroup(name = "그룹 1")
        val group2 = ScheduleGroup(name = "그룹 2")
        val group3 = ScheduleGroup(name = "그룹 3")
        
        repository.insert(group1)
        repository.insert(group2)
        repository.insert(group3)

        // When
        val result = repository.getAll().first()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "그룹 1" })
        assertTrue(result.any { it.name == "그룹 2" })
        assertTrue(result.any { it.name == "그룹 3" })
    }

    @Test
    fun getById_returnsCorrectScheduleGroup() = runBlocking {
        // Given
        val scheduleGroup = ScheduleGroup(name = "테스트 그룹")
        repository.insert(scheduleGroup)

        // When
        val result = repository.getById(scheduleGroup.id).first()

        // Then
        assertNotNull(result)
        assertEquals(scheduleGroup.id, result.id)
        assertEquals(scheduleGroup.name, result.name)
    }

    @Test
    fun getById_returnsNullForNonExistentId() = runBlocking {
        // When
        val result = repository.getById("non-existent-id").first()

        // Then
        assertNull(result)
    }

    @Test
    fun update_modifiesExistingScheduleGroup() = runBlocking {
        // Given
        val original = ScheduleGroup(name = "원본 그룹", description = "원본 설명")
        repository.insert(original)

        // When
        val updated = original.copy(
            name = "수정된 그룹",
            description = "수정된 설명"
        )
        repository.update(updated)

        // Then
        val result = repository.getById(original.id).first()
        assertNotNull(result)
        assertEquals("수정된 그룹", result.name)
        assertEquals("수정된 설명", result.description)
    }

    @Test
    fun delete_removesScheduleGroupFromDatabase() = runBlocking {
        // Given
        val scheduleGroup = ScheduleGroup(name = "삭제할 그룹")
        repository.insert(scheduleGroup)
        
        // 삭제 전 확인
        var result = repository.getAll().first()
        assertEquals(1, result.size)

        // When
        repository.delete(scheduleGroup.id)

        // Then
        result = repository.getAll().first()
        assertEquals(0, result.size)
    }

    // ========== 활성화/비활성화 테스트 ==========

    @Test
    fun toggleActive_activatesScheduleGroup() = runBlocking {
        // Given
        val scheduleGroup = ScheduleGroup(name = "테스트 그룹", isActive = false)
        repository.insert(scheduleGroup)

        // When
        repository.toggleActive(scheduleGroup.id, true)

        // Then
        val result = repository.getById(scheduleGroup.id).first()
        assertNotNull(result)
        assertTrue(result.isActive)
    }

    @Test
    fun toggleActive_deactivatesScheduleGroup() = runBlocking {
        // Given
        val scheduleGroup = ScheduleGroup(name = "테스트 그룹", isActive = true)
        repository.insert(scheduleGroup)

        // When
        repository.toggleActive(scheduleGroup.id, false)

        // Then
        val result = repository.getById(scheduleGroup.id).first()
        assertNotNull(result)
        assertFalse(result.isActive)
    }

    @Test
    fun toggleActive_alsotogglesDependentTimeBasedAutoRuns() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: 종속 모드 TimeBasedAutoRun 생성
        val dependentAutoRun = TimeBasedAutoRun(
            hour = 10, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\"]",
            label = "종속 시간표",
            scheduleGroupId = scheduleGroup.id,
            isIndependent = false,
            isEnabled = true
        )
        database.timeBasedAutoRunDao().insert(dependentAutoRun)

        // When: ScheduleGroup 비활성화
        repository.toggleActive(scheduleGroup.id, false)

        // Then: 종속 TimeBasedAutoRun도 비활성화
        val result = database.timeBasedAutoRunDao().getById(dependentAutoRun.id).first()
        assertNotNull(result)
        assertFalse(result.isEnabled)
    }

    @Test
    fun toggleActive_doesNotToggleIndependentTimeBasedAutoRuns() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "유연 시간표")
        repository.insert(scheduleGroup)

        // Given: 독립 모드 TimeBasedAutoRun 생성
        val independentAutoRun = TimeBasedAutoRun(
            hour = 9, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\"]",
            label = "독립 시간표",
            scheduleGroupId = scheduleGroup.id,
            isIndependent = true,
            isEnabled = true
        )
        database.timeBasedAutoRunDao().insert(independentAutoRun)

        // When: ScheduleGroup 비활성화
        repository.toggleActive(scheduleGroup.id, false)

        // Then: 독립 TimeBasedAutoRun은 여전히 활성화
        val result = database.timeBasedAutoRunDao().getById(independentAutoRun.id).first()
        assertNotNull(result)
        assertTrue(result.isEnabled)
    }

    // ========== 참조 무결성 테스트 ==========

    @Test
    fun getLinkedTimeBasedAutoRuns_returnsCorrectAutoRuns() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: TimeBasedAutoRun 3개 생성 (2개는 연결, 1개는 독립)
        val linked1 = TimeBasedAutoRun(
            hour = 10, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\"]",
            label = "연결1", scheduleGroupId = scheduleGroup.id
        )
        val linked2 = TimeBasedAutoRun(
            hour = 14, minute = 0, durationMinutes = 45,
            presetType = "BALANCED", enabledDays = "[\"TUE\"]",
            label = "연결2", scheduleGroupId = scheduleGroup.id
        )
        val independent = TimeBasedAutoRun(
            hour = 18, minute = 0, durationMinutes = 30,
            presetType = "RELAXED", enabledDays = "[\"WED\"]",
            label = "독립"
        )
        
        database.timeBasedAutoRunDao().insert(linked1)
        database.timeBasedAutoRunDao().insert(linked2)
        database.timeBasedAutoRunDao().insert(independent)

        // When
        val result = repository.getLinkedTimeBasedAutoRuns(scheduleGroup.id)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.label == "연결1" })
        assertTrue(result.any { it.label == "연결2" })
        assertFalse(result.any { it.label == "독립" })
    }

    @Test
    fun getLinkedLocations_returnsCorrectLocations() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: LocationBasedAutoRun 3개 생성 (2개는 연결, 1개는 독립)
        val linked1 = LocationBasedAutoRun(
            label = "회사", address = "서울", latitude = 37.5, longitude = 127.0,
            radiusMeters = 200, durationMinutes = 0, presetType = "BALANCED",
            triggerType = "ENTER", linkedScheduleGroupId = scheduleGroup.id
        )
        val linked2 = LocationBasedAutoRun(
            label = "지점", address = "부산", latitude = 35.1, longitude = 129.0,
            radiusMeters = 150, durationMinutes = 0, presetType = "STANDARD",
            triggerType = "ENTER", linkedScheduleGroupId = scheduleGroup.id
        )
        val independent = LocationBasedAutoRun(
            label = "집", address = "경기", latitude = 37.4, longitude = 127.1,
            radiusMeters = 100, durationMinutes = 30, presetType = "RELAXED",
            triggerType = "ENTER"
        )
        
        database.locationBasedAutoRunDao().insert(linked1)
        database.locationBasedAutoRunDao().insert(linked2)
        database.locationBasedAutoRunDao().insert(independent)

        // When
        val result = repository.getLinkedLocations(scheduleGroup.id)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.label == "회사" })
        assertTrue(result.any { it.label == "지점" })
        assertFalse(result.any { it.label == "집" })
    }

    @Test
    fun unlinkAllAutoRuns_removesAllReferences() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: 연결된 TimeBasedAutoRun 생성
        val timeAutoRun = TimeBasedAutoRun(
            hour = 10, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\"]",
            label = "시간표", scheduleGroupId = scheduleGroup.id
        )
        database.timeBasedAutoRunDao().insert(timeAutoRun)

        // Given: 연결된 LocationBasedAutoRun 생성
        val locationAutoRun = LocationBasedAutoRun(
            label = "회사", address = "서울", latitude = 37.5, longitude = 127.0,
            radiusMeters = 200, durationMinutes = 0, presetType = "BALANCED",
            triggerType = "ENTER", linkedScheduleGroupId = scheduleGroup.id
        )
        database.locationBasedAutoRunDao().insert(locationAutoRun)

        // When
        repository.unlinkAllAutoRuns(scheduleGroup.id)

        // Then: TimeBasedAutoRun 참조 해제
        val timeResult = database.timeBasedAutoRunDao().getById(timeAutoRun.id).first()
        assertNotNull(timeResult)
        assertNull(timeResult.scheduleGroupId)

        // Then: LocationBasedAutoRun 참조 해제
        val locationResult = database.locationBasedAutoRunDao().getById(locationAutoRun.id).first()
        assertNotNull(locationResult)
        assertNull(locationResult.linkedScheduleGroupId)
    }

    @Test
    fun delete_automaticallyUnlinksAllAutoRuns() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: 연결된 TimeBasedAutoRun 생성
        val timeAutoRun = TimeBasedAutoRun(
            hour = 10, minute = 0, durationMinutes = 25,
            presetType = "STANDARD", enabledDays = "[\"MON\"]",
            label = "시간표", scheduleGroupId = scheduleGroup.id
        )
        database.timeBasedAutoRunDao().insert(timeAutoRun)

        // When: ScheduleGroup 삭제
        repository.delete(scheduleGroup.id)

        // Then: ScheduleGroup 삭제됨
        val groupResult = repository.getById(scheduleGroup.id).first()
        assertNull(groupResult)

        // Then: TimeBasedAutoRun은 유지되지만 참조는 해제됨
        val timeResult = database.timeBasedAutoRunDao().getById(timeAutoRun.id).first()
        assertNotNull(timeResult)
        assertNull(timeResult.scheduleGroupId)
    }

    // ========== 카운트 조회 테스트 ==========

    @Test
    fun getLinkedTimeBasedAutoRunCount_returnsCorrectCount() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: TimeBasedAutoRun 3개 연결
        repeat(3) { index ->
            val autoRun = TimeBasedAutoRun(
                hour = 10 + index, minute = 0, durationMinutes = 25,
                presetType = "STANDARD", enabledDays = "[\"MON\"]",
                label = "시간표 $index", scheduleGroupId = scheduleGroup.id
            )
            database.timeBasedAutoRunDao().insert(autoRun)
        }

        // When
        val count = repository.getLinkedTimeBasedAutoRunCount(scheduleGroup.id)

        // Then
        assertEquals(3, count)
    }

    @Test
    fun getLinkedLocationCount_returnsCorrectCount() = runBlocking {
        // Given: ScheduleGroup 생성
        val scheduleGroup = ScheduleGroup(name = "업무 시간표")
        repository.insert(scheduleGroup)

        // Given: LocationBasedAutoRun 2개 연결
        repeat(2) { index ->
            val location = LocationBasedAutoRun(
                label = "위치 $index", address = "주소", latitude = 37.5, longitude = 127.0,
                radiusMeters = 200, durationMinutes = 0, presetType = "BALANCED",
                triggerType = "ENTER", linkedScheduleGroupId = scheduleGroup.id
            )
            database.locationBasedAutoRunDao().insert(location)
        }

        // When
        val count = repository.getLinkedLocationCount(scheduleGroup.id)

        // Then
        assertEquals(2, count)
    }

    @Test
    fun getLinkedCounts_returnZeroForNonExistentGroup() = runBlocking {
        // When
        val timeCount = repository.getLinkedTimeBasedAutoRunCount("non-existent")
        val locationCount = repository.getLinkedLocationCount("non-existent")

        // Then
        assertEquals(0, timeCount)
        assertEquals(0, locationCount)
    }
}

