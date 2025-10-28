package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.data.local.entity.FocusSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * AutoRunStatisticsCalculator 단위 테스트
 *
 * Fake Repositories를 사용하여 통계 계산 로직을 검증합니다.
 */
class AutoRunStatisticsCalculatorTest {

    private lateinit var calculator: AutoRunStatisticsCalculator
    private lateinit var fakeAutoRunLogDao: FakeAutoRunLogDao
    private lateinit var fakeFocusSessionDao: FakeFocusSessionDao

    @Before
    fun setup() {
        fakeAutoRunLogDao = FakeAutoRunLogDao()
        fakeFocusSessionDao = FakeFocusSessionDao()
        calculator = AutoRunStatisticsCalculator(fakeAutoRunLogDao, fakeFocusSessionDao)
    }

    // ========== 데이터 클래스 테스트 ==========

    @Test
    fun `AutoRunStatistics 데이터 클래스 생성`() {
        val statistics = AutoRunStatistics(
            weeklyTimeBasedCount = 15,
            weeklyLocationBasedCount = 8,
            successRate = 0.85f,
            totalFocusTimeMinutes = 450,
            weeklyTrend = emptyList()
        )

        assertEquals(15, statistics.weeklyTimeBasedCount)
        assertEquals(8, statistics.weeklyLocationBasedCount)
        assertEquals(0.85f, statistics.successRate, 0.01f)
        assertEquals(450, statistics.totalFocusTimeMinutes)
        assertTrue(statistics.weeklyTrend.isEmpty())
    }

    @Test
    fun `DailyStats 데이터 클래스 생성`() {
        val dailyStats = DailyStats(
            date = "2025-10-28",
            timeBasedCount = 3,
            locationBasedCount = 1,
            successCount = 3,
            totalCount = 4
        )

        assertEquals("2025-10-28", dailyStats.date)
        assertEquals(3, dailyStats.timeBasedCount)
        assertEquals(1, dailyStats.locationBasedCount)
        assertEquals(3, dailyStats.successCount)
        assertEquals(4, dailyStats.totalCount)
    }

    // ========== 비즈니스 로직 테스트 ==========

    @Test
    fun `로그가 없을 때 빈 통계 반환`() = runBlocking {
        // Given: 로그 없음
        fakeAutoRunLogDao.logs = emptyList()

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        assertEquals(0, statistics.weeklyTimeBasedCount)
        assertEquals(0, statistics.weeklyLocationBasedCount)
        assertEquals(0.0f, statistics.successRate, 0.01f)
        assertEquals(0, statistics.totalFocusTimeMinutes)
        assertEquals(7, statistics.weeklyTrend.size) // 7일간 데이터
    }

    @Test
    fun `시간 기반 자동 실행 횟수 계산`() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        fakeAutoRunLogDao.logs = listOf(
            createAutoRunLog("TIME", "STARTED", now),
            createAutoRunLog("TIME", "STARTED", now - 3600000), // 1시간 전
            createAutoRunLog("TIME", "FAILED", now - 7200000), // 2시간 전
            createAutoRunLog("LOCATION", "STARTED", now)
        )

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        assertEquals(3, statistics.weeklyTimeBasedCount)
        assertEquals(1, statistics.weeklyLocationBasedCount)
    }

    @Test
    fun `성공률 계산 - 모두 성공`() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        fakeAutoRunLogDao.logs = listOf(
            createAutoRunLog("TIME", "STARTED", now),
            createAutoRunLog("TIME", "STARTED", now - 3600000),
            createAutoRunLog("LOCATION", "STARTED", now - 7200000)
        )

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        assertEquals(1.0f, statistics.successRate, 0.01f) // 3/3 = 100%
    }

    @Test
    fun `성공률 계산 - 일부 실패`() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        fakeAutoRunLogDao.logs = listOf(
            createAutoRunLog("TIME", "STARTED", now, "session-1"),
            createAutoRunLog("TIME", "FAILED", now - 3600000),
            createAutoRunLog("LOCATION", "STARTED", now - 7200000, "session-2"),
            createAutoRunLog("LOCATION", "SKIPPED", now - 10800000)
        )

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        assertEquals(0.5f, statistics.successRate, 0.01f) // 2/4 = 50%
    }

    @Test
    fun `총 집중 시간 계산 - 세션 연계`() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        val session1 = FocusSession(
            id = "session-1",
            startTime = now - 3600000, // 1시간 전 시작
            endTime = now - 1800000, // 30분 후 종료
            durationMinutes = 30,
            success = true
        )
        val session2 = FocusSession(
            id = "session-2",
            startTime = now - 7200000, // 2시간 전 시작
            endTime = now - 4500000, // 45분 후 종료
            durationMinutes = 45,
            success = true
        )

        fakeFocusSessionDao.sessions = mapOf(
            "session-1" to session1,
            "session-2" to session2
        )

        fakeAutoRunLogDao.logs = listOf(
            createAutoRunLog("TIME", "STARTED", now - 3600000, "session-1"),
            createAutoRunLog("LOCATION", "STARTED", now - 7200000, "session-2")
        )

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        // session1: 30분, session2: 45분 = 75분
        assertEquals(75, statistics.totalFocusTimeMinutes)
    }

    @Test
    fun `주간 트렌드 생성 - 7일간 데이터`() = runBlocking {
        // Given: 최근 3일간 로그 추가
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -1) // 어제
        val yesterday = calendar.timeInMillis

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.DAY_OF_YEAR, -2) // 그저께
        val dayBefore = calendar.timeInMillis

        fakeAutoRunLogDao.logs = listOf(
            createAutoRunLog("TIME", "STARTED", today),
            createAutoRunLog("TIME", "STARTED", today),
            createAutoRunLog("LOCATION", "STARTED", yesterday),
            createAutoRunLog("TIME", "FAILED", yesterday),
            createAutoRunLog("TIME", "STARTED", dayBefore)
        )

        // When
        val statistics = calculator.calculateStatistics()

        // Then
        assertEquals(7, statistics.weeklyTrend.size) // 7일간 데이터
        
        // 첫 번째 날 (가장 오래된 날)부터 마지막 날 (오늘)까지
        val trend = statistics.weeklyTrend
        
        // 트렌드 데이터 구조 확인
        trend.forEach { daily ->
            assertNotNull(daily.date)
            assertTrue(daily.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
            assertTrue(daily.totalCount >= 0)
            assertTrue(daily.successCount >= 0)
            assertTrue(daily.successCount <= daily.totalCount)
        }
    }

    // ========== Helper Methods ==========

    private fun createAutoRunLog(
        triggerType: String,
        result: String,
        triggerTime: Long,
        sessionId: String? = null
    ): AutoRunLog {
        return AutoRunLog(
            id = "log-${System.nanoTime()}",
            triggerType = triggerType,
            triggerSourceId = "source-${System.nanoTime()}",
            triggerTime = triggerTime,
            result = result,
            sessionId = sessionId
        )
    }

    // ========== Fake DAOs ==========

    private class FakeAutoRunLogDao : AutoRunLogDao {
        var logs: List<AutoRunLog> = emptyList()

        override suspend fun getLogsInRangeList(startTime: Long, endTime: Long): List<AutoRunLog> {
            return logs.filter { it.triggerTime in startTime..endTime }
        }

        // 나머지 메서드는 사용하지 않으므로 구현하지 않음
        override suspend fun insert(log: AutoRunLog) = throw NotImplementedError()
        override fun getAll() = throw NotImplementedError()
        override fun getByTriggerType(triggerType: String) = throw NotImplementedError()
        override fun getRecentLogs(limit: Int) = throw NotImplementedError()
        override fun getLogsInRange(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getBySourceId(sourceId: String) = throw NotImplementedError()
        override suspend fun getTotalCount() = throw NotImplementedError()
        override suspend fun getSuccessCount() = throw NotImplementedError()
        override suspend fun getFailureCount() = throw NotImplementedError()
        override suspend fun getSkippedCount() = throw NotImplementedError()
        override suspend fun getSuccessRate() = throw NotImplementedError()
        override suspend fun getSuccessRateByType(triggerType: String) = throw NotImplementedError()
        override suspend fun getSuccessRateBySource(sourceId: String) = throw NotImplementedError()
        override suspend fun getTodayCount() = throw NotImplementedError()
        override suspend fun getWeekCount() = throw NotImplementedError()
    }

    private class FakeFocusSessionDao : FocusSessionDao {
        var sessions: Map<String, FocusSession> = emptyMap()

        override suspend fun getSessionByIdSync(sessionId: String): FocusSession? {
            return sessions[sessionId]
        }

        // 나머지 메서드는 사용하지 않으므로 구현하지 않음
        override suspend fun insert(session: FocusSession) = throw NotImplementedError()
        override suspend fun update(session: FocusSession) = throw NotImplementedError()
        override fun getTodaySessions() = throw NotImplementedError()
        override fun getAllSessions() = throw NotImplementedError()
        override fun getSessionById(sessionId: String) = throw NotImplementedError()
        override fun getSuccessfulSessions() = throw NotImplementedError()
        override suspend fun getSessionsInLastDays(days: Int) = throw NotImplementedError()
        override suspend fun getSessionsInRange(startTimestamp: Long, endTimestamp: Long) = throw NotImplementedError()
    }
}

