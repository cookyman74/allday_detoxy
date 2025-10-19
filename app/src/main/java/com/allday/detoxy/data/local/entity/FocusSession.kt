package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 집중 세션 엔티티
 *
 * 사용자의 집중 타이머 세션 기록을 저장합니다.
 * 세션의 시작/종료 시간, 목표 시간, 성공 여부를 추적합니다.
 *
 * @property id 세션 고유 ID (UUID)
 * @property startTime 세션 시작 시간 (Unix timestamp, milliseconds)
 * @property endTime 세션 종료 시간 (Unix timestamp, milliseconds) - null이면 진행 중
 * @property durationMinutes 목표 시간 (분 단위)
 * @property success 성공 여부 (true: 완료, false: 포기)
 * @property interruptedSeconds 중도 포기 시 경과 시간 (초 단위, v2+)
 * @property primaryDistractionCategory 주요 방해요인 카테고리 (v2+)
 * @property giveUpReason 포기 사유 (v2+)
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val startTime: Long,

    val endTime: Long? = null,

    val durationMinutes: Int,

    val success: Boolean = false,

    // v2 추가 필드: 중도 포기 및 방해요인 분석
    val interruptedSeconds: Int = 0,

    val primaryDistractionCategory: String? = null,

    val giveUpReason: String? = null
)
