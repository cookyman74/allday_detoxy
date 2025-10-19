package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 디톡시 루틴 실행 기록
 *
 * 사용자가 설정한 루틴 (예: 매일 저녁 10시 이후 SNS 차단)의
 * 실행 여부와 성공/실패를 기록합니다.
 *
 * v3 신규 엔티티 (Week 2B)
 *
 * @property id 고유 ID (UUID)
 * @property scheduledTime 예정 시각 (Unix timestamp, milliseconds)
 * @property executedTime 실제 실행 시각 (Unix timestamp, milliseconds, nullable)
 * @property success 성공 여부 (true: 실행 성공, false: 실행 실패 또는 건너뜀)
 * @property failureReason 실패 사유 (nullable, 실패 시에만 기록)
 */
@Entity(tableName = "detoxy_routine_logs")
data class DetoxyRoutineLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scheduledTime: Long,
    val executedTime: Long? = null,
    val success: Boolean,
    val failureReason: String? = null
)

/**
 * 루틴 실패 사유
 */
enum class RoutineFailureReason {
    USER_SKIPPED,           // 사용자가 건너뜀
    PERMISSION_DENIED,      // 권한 부족
    ALREADY_RUNNING,        // 이미 실행 중
    SYSTEM_ERROR,           // 시스템 오류
    UNKNOWN;                // 알 수 없음

    fun toDisplayString(): String = when (this) {
        USER_SKIPPED -> "사용자 건너뜀"
        PERMISSION_DENIED -> "권한 부족"
        ALREADY_RUNNING -> "이미 실행 중"
        SYSTEM_ERROR -> "시스템 오류"
        UNKNOWN -> "알 수 없음"
    }
}

