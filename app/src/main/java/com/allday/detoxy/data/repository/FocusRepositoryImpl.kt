package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.FocusInterruptionDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings
import com.allday.detoxy.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FocusRepository 구현체
 *
 * DAO를 사용하여 실제 데이터베이스 작업을 수행합니다.
 * Hilt를 통해 싱글톤으로 제공됩니다.
 *
 * @property sessionDao FocusSession DAO
 * @property settingsDao UserSettings DAO
 * @property interruptionDao FocusInterruption DAO
 */
@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val sessionDao: FocusSessionDao,
    private val settingsDao: UserSettingsDao,
    private val interruptionDao: FocusInterruptionDao
) : FocusRepository {

    // ==================== FocusSession 관련 ====================

    override suspend fun startSession(session: FocusSession) {
        sessionDao.insert(session)
    }

    override suspend fun endSession(sessionId: String, success: Boolean, endTime: Long) {
        val session = sessionDao.getSessionById(sessionId).first()
        session?.let {
            val updatedSession = it.copy(
                endTime = endTime,
                success = success
            )
            sessionDao.update(updatedSession)
        }
    }

    override suspend fun endSessionWithDetails(
        sessionId: String,
        success: Boolean,
        endTime: Long,
        interruptedSeconds: Int,
        giveUpReason: String?
    ) {
        val session = sessionDao.getSessionById(sessionId).first()
        session?.let {
            // 주요 방해요인 카테고리 조회 (차단 이벤트가 있는 경우)
            val primaryCategory = interruptionDao.getPrimaryCategoryBySession(sessionId)
            
            val updatedSession = it.copy(
                endTime = endTime,
                success = success,
                interruptedSeconds = interruptedSeconds,
                primaryDistractionCategory = primaryCategory,
                giveUpReason = giveUpReason
            )
            sessionDao.update(updatedSession)
        }
    }

    override fun getSession(sessionId: String): Flow<FocusSession?> {
        return sessionDao.getSessionById(sessionId)
    }

    override fun getTodaySessions(): Flow<List<FocusSession>> {
        return sessionDao.getTodaySessions()
    }

    override fun getAllSessions(): Flow<List<FocusSession>> {
        return sessionDao.getAllSessions()
    }

    override suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<FocusSession> {
        return sessionDao.getSessionsInRange(startTime, endTime)
    }

    // ==================== FocusInterruption 관련 ====================

    override suspend fun logInterruption(interruption: FocusInterruption) {
        interruptionDao.insert(interruption)
    }

    override fun getInterruptionsBySession(sessionId: String): Flow<List<FocusInterruption>> {
        return interruptionDao.getInterruptionsBySession(sessionId)
    }

    override suspend fun getPrimaryCategoryBySession(sessionId: String): String? {
        return interruptionDao.getPrimaryCategoryBySession(sessionId)
    }

    override suspend fun getInterruptionsInLastDays(days: Int): List<FocusInterruption> {
        return interruptionDao.getInterruptionsInLastDays(days)
    }

    // ==================== UserSettings 관련 ====================

    override fun getSettings(): Flow<UserSettings?> {
        return settingsDao.getSettings()
    }

    override suspend fun initializeSettings(settings: UserSettings) {
        settingsDao.insert(settings)
    }

    override suspend fun addPoints(points: Int) {
        settingsDao.addPoints(points)
    }

    override suspend fun updateStreak(streak: Int, date: String) {
        settingsDao.updateStreak(streak, date)
    }

    override suspend fun updateSettings(settings: UserSettings) {
        settingsDao.update(settings)
    }
}
