package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 집중 세션 중 차단 이벤트 엔티티
 *
 * 세션 진행 중 발생한 차단 앱 실행 시도를 기록합니다.
 * 이를 통해 사용자의 주요 방해요인과 차단 패턴을 분석할 수 있습니다.
 *
 * @property id 차단 이벤트 고유 ID (UUID)
 * @property sessionId 연결된 세션 ID (외래키)
 * @property timestamp 차단 발생 시간 (Unix timestamp, milliseconds)
 * @property packageName 차단된 앱의 패키지명
 * @property category 차단된 앱의 카테고리 (SNS, MESSENGER, WEB, VIDEO_SHORTS, OTHER)
 */
@Entity(
    tableName = "focus_interruptions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"])
    ]
)
data class FocusInterruption(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val sessionId: String,

    val timestamp: Long,

    val packageName: String,

    val category: String
)

