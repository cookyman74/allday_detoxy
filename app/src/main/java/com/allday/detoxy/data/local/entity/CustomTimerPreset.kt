package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 커스텀 타이머 프리셋 엔티티
 *
 * 사용자가 도넛 그래프로 생성한 커스텀 타이머 프리셋을 저장합니다.
 *
 * ## 주요 기능
 * - 사용자 지정 타이머 시간 (5-180분)
 * - 차단 프리셋 연결 (옵션)
 * - 사용 횟수 추적 (자동 증가)
 * - 표시 순서 관리 (드래그로 변경 가능)
 *
 * ## 예시
 * ```kotlin
 * CustomTimerPreset(
 *     name = "오후 집중",
 *     durationMinutes = 35,
 *     presetType = "STANDARD",
 *     displayOrder = 0
 * )
 * ```
 *
 * @property id 고유 ID (UUID)
 * @property name 프리셋 이름
 * @property durationMinutes 타이머 시간 (분)
 * @property presetType 연결된 차단 프리셋 (옵션)
 * @property usageCount 사용 횟수 (자동 증가)
 * @property displayOrder 표시 순서 (0부터 시작, 작을수록 앞)
 * @property createdAt 생성 시간 (timestamp)
 */
@Entity(
    tableName = "custom_timer_preset",
    indices = [
        Index(value = ["displayOrder"])
    ]
)
data class CustomTimerPreset(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 프리셋 이름
     *
     * 사용자가 지정한 이름 (예: "오후 집중", "짧은 휴식")
     */
    val name: String,

    /**
     * 타이머 시간 (분)
     *
     * 허용 범위: 5-180분 (5분 단위)
     */
    val durationMinutes: Int,

    /**
     * 연결된 차단 프리셋 (옵션)
     *
     * - FULL_BLOCK: 완전 차단
     * - STANDARD: 표준 (메신저 허용)
     * - RELAXED: 완화 (SNS 허용)
     * - null: 차단 프리셋 연결 없음
     */
    val presetType: String? = null,

    /**
     * 사용 횟수
     *
     * 프리셋 선택 시 자동 증가.
     * 통계 및 추천 정렬에 활용.
     */
    val usageCount: Int = 0,

    /**
     * 표시 순서
     *
     * 0부터 시작, 작을수록 앞에 표시.
     * 사용자가 드래그로 순서 변경 가능.
     */
    val displayOrder: Int = 0,

    /**
     * 생성 시간 (timestamp)
     */
    val createdAt: Long = System.currentTimeMillis()
)

