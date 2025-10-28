package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.domain.repository.IFocusSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 집중 세션 Repository 구현체
 *
 * IFocusSessionRepository 인터페이스를 구현하여
 * domain 레이어에서 사용할 수 있도록 합니다.
 *
 * @param dao FocusSessionDao
 */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val dao: FocusSessionDao
) : IFocusSessionRepository {

    override suspend fun getSessionById(sessionId: String): FocusSession? {
        return dao.getSessionByIdSync(sessionId)
    }
}

