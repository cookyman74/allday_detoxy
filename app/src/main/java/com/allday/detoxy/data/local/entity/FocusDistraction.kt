package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 집중 세션 중 감지된 모든 앱 진입 기록 (차단/허용 포함)
 *
 * FocusInterruption과 차이점:
 * - FocusInterruption: 차단된 앱만 기록
 * - FocusDistraction: 모든 주의 분산 앱 진입 기록 (차단/허용 모두)
 *
 * v3 신규 엔티티 (Week 2B)
 *
 * @property id 고유 ID (UUID)
 * @property sessionId 연결된 FocusSession ID (nullable, 세션 외에도 기록 가능)
 * @property timestamp 앱 진입 시각 (Unix timestamp, milliseconds)
 * @property packageName 앱 패키지명
 * @property category 앱 카테고리 (AppCategory.name: SNS, WEB, etc.)
 * @property wasBlocked 차단 여부 (true: 차단됨, false: 허용됨)
 * @property dwellTimeSeconds 체류 시간 (초, UsageStats 연동 시, nullable)
 */
@Entity(
    tableName = "focus_distractions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL  // 세션 삭제 시 sessionId만 NULL로 설정
        )
    ],
    indices = [
        Index(value = ["sessionId"]),   // 세션별 조회 성능 최적화
        Index(value = ["timestamp"])     // 시간별 조회 성능 최적화
    ]
)
data class FocusDistraction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,
    val timestamp: Long,
    val packageName: String,
    val category: String,
    val wasBlocked: Boolean,
    val dwellTimeSeconds: Int? = null
)

