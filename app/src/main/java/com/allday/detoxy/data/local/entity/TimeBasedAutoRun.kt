package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 시간 기반 자동 실행 엔티티
 *
 * 특정 시간에 집중 모드를 자동으로 시작하는 설정을 저장합니다.
 *
 * ## 주요 기능
 * - 매일 특정 시간에 자동 실행
 * - 요일별 활성화/비활성화
 * - 타이머 시간 및 차단 프리셋 설정
 * - 라벨 지정 가능 (예: "아침 집중", "오후 집중")
 *
 * ## 예시
 * ```kotlin
 * TimeBasedAutoRun(
 *     hour = 14,
 *     minute = 0,
 *     durationMinutes = 45,
 *     presetType = "STANDARD",
 *     enabledDays = "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]",
 *     label = "오후 집중 시간"
 * )
 * ```
 *
 * @property id 고유 ID (UUID)
 * @property hour 시간 (0-23)
 * @property minute 분 (0-59)
 * @property durationMinutes 타이머 시간 (분)
 * @property presetType 차단 프리셋 (FULL_BLOCK, STANDARD, RELAXED)
 * @property enabledDays 활성 요일 JSON 배열 (예: ["MON", "TUE", ...])
 * @property label 라벨 (옵션, 사용자 지정 이름)
 * @property isEnabled 활성화 여부
 * @property createdAt 생성 시간 (timestamp)
 */
@Entity(
    tableName = "time_based_auto_run",
    indices = [
        Index(value = ["hour", "minute"]),
        Index(value = ["isEnabled"])
    ]
)
data class TimeBasedAutoRun(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 시간 (0-23)
     */
    val hour: Int,

    /**
     * 분 (0-59)
     */
    val minute: Int,

    /**
     * 타이머 시간 (분)
     */
    val durationMinutes: Int,

    /**
     * 차단 프리셋 타입
     * - FULL_BLOCK: 완전 차단
     * - STANDARD: 표준 (메신저 허용)
     * - RELAXED: 완화 (SNS 허용)
     */
    val presetType: String,

    /**
     * 활성 요일 (JSON 배열)
     *
     * 예: "[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]"
     */
    val enabledDays: String,

    /**
     * 라벨 (옵션)
     *
     * 사용자 지정 이름 (예: "아침 집중", "오후 집중")
     */
    val label: String? = null,

    /**
     * 활성화 여부
     */
    val isEnabled: Boolean = true,

    /**
     * 생성 시간 (timestamp)
     */
    val createdAt: Long = System.currentTimeMillis()
)

