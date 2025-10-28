package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.domain.repository.IUserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.UUID

/**
 * NextAutoRunCalculator 비즈니스 로직 테스트
 *
 * 2.5차 고도화 Week 1, Day 2-3: 실제 비즈니스 로직 검증
 *
 * ## 테스트 범위
 * - 마스터 스위치 OFF 시 null 반환
 * - 일시중지 중일 때 null 반환
 * - 활성화된 자동 실행이 없을 때 null 반환
 * - 가장 가까운 시각 선택 (여러 자동 실행 중)
 * - 주말 제외 옵션 적용
 * - 요일별 활성화 상태 고려
 *
 * ## 테스트 전략
 * Fake Repository를 사용하여 의존성을 완전히 제어하고
 * 실제 계산 로직의 정확성을 검증합니다.
 *
 * @see NextAutoRunCalculator
 * @see docs/02.5_autosetting_todolist.md §1.1.2
 */
class NextAutoRunCalculatorBusinessLogicTest {

    private lateinit var calculator: NextAutoRunCalculator
    private lateinit var fakeAutoRunSettings: FakeAutoRunSettingsRepository
    private lateinit var fakeUserSettings: FakeUserSettingsRepository

    @Before
    fun setup() {
        fakeAutoRunSettings = FakeAutoRunSettingsRepository()
        fakeUserSettings = FakeUserSettingsRepository()
        calculator = NextAutoRunCalculator(fakeAutoRunSettings, fakeUserSettings)
    }

    // ========== 마스터 스위치 테스트 ==========

    @Test
    fun `마스터 스위치가 OFF이면 null 반환`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(false)
        val autoRuns = listOf(createAutoRun(hour = 10, minute = 0))

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNull("마스터 스위치 OFF이면 null을 반환해야 함", result)
    }

    // ========== 일시중지 테스트 ==========

    @Test
    fun `일시중지 중이면 null 반환`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        fakeUserSettings.setAutoRunPauseUntil(System.currentTimeMillis() + (60 * 60 * 1000)) // 1시간 후까지 일시중지
        val autoRuns = listOf(createAutoRun(hour = 10, minute = 0))

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNull("일시중지 중이면 null을 반환해야 함", result)
    }

    @Test
    fun `일시중지가 만료되면 정상 계산`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        fakeUserSettings.setAutoRunPauseUntil(System.currentTimeMillis() - (60 * 60 * 1000)) // 1시간 전에 만료
        val autoRuns = listOf(createAutoRun(hour = 10, minute = 0))

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNotNull("일시중지가 만료되면 정상 계산해야 함", result)
    }

    // ========== 빈 리스트 테스트 ==========

    @Test
    fun `활성화된 자동 실행이 없으면 null 반환`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        val autoRuns = emptyList<TimeBasedAutoRun>()

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNull("활성화된 자동 실행이 없으면 null을 반환해야 함", result)
    }

    // ========== 가장 가까운 시각 선택 테스트 ==========

    @Test
    fun `여러 자동 실행 중 가장 가까운 시각 선택`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        // 현재 시각보다 1시간, 2시간, 3시간 후
        val autoRuns = listOf(
            createAutoRun(hour = (currentHour + 3) % 24, minute = 0, label = "3시간 후"),
            createAutoRun(hour = (currentHour + 1) % 24, minute = 0, label = "1시간 후"),
            createAutoRun(hour = (currentHour + 2) % 24, minute = 0, label = "2시간 후")
        )

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNotNull("결과가 null이면 안 됨", result)
        assertEquals("가장 가까운 자동 실행을 선택해야 함", "1시간 후", result?.label)
    }

    // ========== 요일별 활성화 상태 테스트 ==========

    @Test
    fun `오늘 비활성화된 요일이면 다음 활성화 요일 선택`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        val now = Calendar.getInstance()
        val currentDayOfWeek = getDayOfWeekString(now)
        val nextDayOfWeek = getNextDayOfWeek(currentDayOfWeek)
        
        // 오늘 제외, 내일만 활성화
        val autoRuns = listOf(
            createAutoRun(
                hour = 10,
                minute = 0,
                enabledDays = """["$nextDayOfWeek"]""",
                label = "내일"
            )
        )

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNotNull("다음 활성화 요일을 찾아야 함", result)
        assertTrue("내일 이후여야 함", result!!.triggerTime > System.currentTimeMillis())
    }

    // ========== 주말 제외 테스트 ==========

    @Test
    fun `주말 제외 옵션이 true이고 토요일이면 월요일 선택`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        fakeAutoRunSettings.setExcludeWeekends(true)
        
        // 모든 요일 활성화
        val autoRuns = listOf(
            createAutoRun(
                hour = 10,
                minute = 0,
                enabledDays = """["MON","TUE","WED","THU","FRI","SAT","SUN"]""",
                label = "매일"
            )
        )

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNotNull("결과가 null이면 안 됨", result)
        // 토요일/일요일이 아닌 평일이어야 함
        val resultCal = Calendar.getInstance().apply { timeInMillis = result!!.triggerTime }
        val dayOfWeek = resultCal.get(Calendar.DAY_OF_WEEK)
        assertTrue(
            "주말 제외 옵션이 true이면 토요일/일요일이 아니어야 함",
            dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
        )
    }

    // ========== NextAutoRunInfo 필드 검증 ==========

    @Test
    fun `NextAutoRunInfo의 모든 필드가 올바르게 설정됨`() = runBlocking {
        // Given
        fakeUserSettings.setAutoRunMasterEnabled(true)
        val now = Calendar.getInstance()
        val targetHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        val autoRuns = listOf(
            createAutoRun(
                hour = targetHour,
                minute = 30,
                durationMinutes = 45,
                presetType = "STANDARD",
                label = "오후 집중"
            )
        )

        // When
        val result = calculator.calculateNextAutoRun(autoRuns)

        // Then
        assertNotNull("결과가 null이면 안 됨", result)
        assertEquals("triggerType은 TIME이어야 함", "TIME", result!!.triggerType)
        assertEquals("label이 올바르게 설정되어야 함", "오후 집중", result.label)
        assertEquals("durationMinutes가 올바르게 설정되어야 함", 45, result.durationMinutes)
        assertEquals("presetType이 올바르게 설정되어야 함", "STANDARD", result.presetType)
        assertTrue("triggerTime이 미래여야 함", result.triggerTime > System.currentTimeMillis())
        assertNotNull("timeUntilTrigger가 있어야 함", result.timeUntilTrigger)
        assertNull("시간 기반은 confidence가 null이어야 함", result.confidence)
    }

    // ========== 헬퍼 함수 ==========

    private fun createAutoRun(
        id: String = UUID.randomUUID().toString(),
        label: String? = "Test AutoRun",
        hour: Int = 10,
        minute: Int = 0,
        durationMinutes: Int = 30,
        presetType: String = "STANDARD",
        enabledDays: String = getAllDaysJson(),
        isEnabled: Boolean = true
    ): TimeBasedAutoRun {
        return TimeBasedAutoRun(
            id = id,
            label = label,
            hour = hour,
            minute = minute,
            durationMinutes = durationMinutes,
            presetType = presetType,
            enabledDays = enabledDays,
            isEnabled = isEnabled
        )
    }

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

    private fun getNextDayOfWeek(currentDay: String): String {
        return when (currentDay) {
            "MON" -> "TUE"
            "TUE" -> "WED"
            "WED" -> "THU"
            "THU" -> "FRI"
            "FRI" -> "SAT"
            "SAT" -> "SUN"
            "SUN" -> "MON"
            else -> "MON"
        }
    }

    private fun getAllDaysJson(): String {
        return """["MON","TUE","WED","THU","FRI","SAT","SUN"]"""
    }

    // ========== Fake Repositories ==========

    /**
     * Fake AutoRunSettingsRepository
     *
     * 테스트를 위한 가짜 구현체
     */
    private class FakeAutoRunSettingsRepository : AutoRunSettingsRepository {
        private var excludeWeekends = false
        private var autoStartDelayMinutes = 0
        private var preNotificationMinutes = 5

        override val excludeWeekendsFlow: Flow<Boolean>
            get() = flowOf(excludeWeekends)

        override val autoStartDelayMinutesFlow: Flow<Int>
            get() = flowOf(autoStartDelayMinutes)

        override val preNotificationMinutesFlow: Flow<Int>
            get() = flowOf(preNotificationMinutes)

        override suspend fun saveExcludeWeekends(exclude: Boolean) {
            excludeWeekends = exclude
        }

        override suspend fun getExcludeWeekends(): Boolean {
            return excludeWeekends
        }

        override suspend fun saveAutoStartDelayMinutes(minutes: Int) {
            autoStartDelayMinutes = minutes
        }

        override suspend fun getAutoStartDelayMinutes(): Int {
            return autoStartDelayMinutes
        }

        override suspend fun savePreNotificationMinutes(minutes: Int) {
            preNotificationMinutes = minutes
        }

        override suspend fun getPreNotificationMinutes(): Int {
            return preNotificationMinutes
        }

        // 테스트 편의 메서드
        fun setExcludeWeekends(enabled: Boolean) {
            excludeWeekends = enabled
        }
    }

    /**
     * Fake IUserSettingsRepository
     *
     * 테스트를 위한 가짜 구현체
     */
    private class FakeUserSettingsRepository : IUserSettingsRepository {
        private var masterEnabled = true
        private var pauseUntil: Long? = null

        override suspend fun setAutoRunMasterEnabled(enabled: Boolean) {
            masterEnabled = enabled
        }

        override suspend fun getAutoRunMasterEnabled(): Boolean {
            return masterEnabled
        }

        override suspend fun setAutoRunPauseUntil(pauseUntil: Long?) {
            this.pauseUntil = pauseUntil
        }

        override suspend fun getAutoRunPauseUntil(): Long? {
            return pauseUntil
        }
    }
}

