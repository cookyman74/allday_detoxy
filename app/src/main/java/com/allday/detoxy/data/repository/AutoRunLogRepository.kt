package com.allday.detoxy.data.repository

import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.entity.AutoRunLog
import com.allday.detoxy.domain.repository.IAutoRunLogRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자동 실행 로그 Repository 구현체
 *
 * IAutoRunLogRepository 인터페이스를 구현하여
 * domain 레이어에서 사용할 수 있도록 합니다.
 *
 * @param dao AutoRunLogDao
 */
@Singleton
class AutoRunLogRepository @Inject constructor(
    private val dao: AutoRunLogDao
) : IAutoRunLogRepository {

    override suspend fun getLogsInRange(startTime: Long, endTime: Long): List<AutoRunLog> {
        return dao.getLogsInRangeList(startTime, endTime)
    }
}

