package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 설정 엔티티
 *
 * 사용자의 포인트, 스트릭 등 게임화 관련 정보를 저장합니다.
 * 단일 레코드로 관리되며, ID는 항상 1입니다.
 *
 * @property id 고유 ID (항상 1)
 * @property totalPoints 총 획득 포인트
 * @property currentStreak 현재 연속 성공 일수
 * @property lastSuccessDate 마지막 성공 날짜 (YYYY-MM-DD 형식)
 * @property autoRunMasterEnabled 자동 실행 마스터 토글 (v4+)
 * @property autoRunPauseUntil 자동 실행 일시중지 종료 시간 (timestamp, v4+)
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,

    val totalPoints: Int = 0,

    val currentStreak: Int = 0,

    val lastSuccessDate: String? = null,

    /**
     * 자동 실행 마스터 토글 (v4+)
     *
     * true: 모든 자동 실행 활성화
     * false: 모든 자동 실행 비활성화 (마스터 스위치)
     */
    val autoRunMasterEnabled: Boolean = true,

    /**
     * 자동 실행 일시중지 종료 시간 (v4+)
     *
     * null: 일시중지 없음
     * timestamp: 해당 시간까지 모든 자동 실행 일시중지
     *
     * 사용 예:
     * - "오늘 하루 중지": 자정까지
     * - "N시간 동안 중지": 현재 + N시간
     * - "다음 자동 실행까지 중지": 가장 가까운 예정 시간까지
     */
    val autoRunPauseUntil: Long? = null
)
