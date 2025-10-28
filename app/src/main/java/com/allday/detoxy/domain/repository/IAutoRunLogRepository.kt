package com.allday.detoxy.domain.repository

import com.allday.detoxy.data.local.entity.AutoRunLog

/**
 * 자동 실행 로그 Repository 인터페이스 (Domain Layer)
 *
 * Clean Architecture 경계를 유지하기 위해 domain 레이어에서 정의된 인터페이스입니다.
 * 통계 계산 등 도메인 로직에서 필요한 메서드만 노출합니다.
 *
 * ## 설계 원칙
 * - domain 레이어는 data 레이어의 구현체에 직접 의존하지 않음
 * - 필요한 메서드만 인터페이스로 정의
 * - data 레이어의 구현체가 이 인터페이스를 구현
 *
 * @see com.allday.detoxy.data.repository.AutoRunLogRepository
 */
interface IAutoRunLogRepository {
    /**
     * 특정 기간의 자동 실행 로그 조회
     *
     * @param startTime 시작 시간 (timestamp)
     * @param endTime 종료 시간 (timestamp)
     * @return 로그 리스트
     */
    suspend fun getLogsInRange(startTime: Long, endTime: Long): List<AutoRunLog>
}

