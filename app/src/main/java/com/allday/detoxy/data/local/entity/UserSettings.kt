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
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,

    val totalPoints: Int = 0,

    val currentStreak: Int = 0,

    val lastSuccessDate: String? = null
)
