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
        Index(value = ["isEnabled"]),
        Index(value = ["scheduleGroupId"])
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
    val createdAt: Long = System.currentTimeMillis(),

    // ==================== v5 추가: ScheduleGroup 지원 ====================

    /**
     * 연결된 ScheduleGroup ID (v5+)
     *
     * null: 독립 실행 모드 (기본값, 기존 동작 유지)
     * UUID: 해당 그룹에 종속
     *
     * ## 독립 모드 vs 종속 모드
     * - 독립 모드: scheduleGroupId가 null이거나 isIndependent=true일 때 항상 실행
     * - 종속 모드: scheduleGroupId가 있고 isIndependent=false일 때 그룹 활성화 시에만 실행
     */
    val scheduleGroupId: String? = null,

    /**
     * 독립 실행 여부 (v5+)
     *
     * true: scheduleGroupId와 무관하게 항상 실행 (기본값)
     * false: scheduleGroupId 그룹 활성화 시에만 실행 (종속 모드)
     *
     * ## 주의사항
     * - scheduleGroupId가 null이면 이 필드는 무시됨 (항상 독립 실행)
     * - scheduleGroupId가 있을 때만 의미 있음
     */
    val isIndependent: Boolean = true,

    // ==================== v6 추가: 그룹 내 우선순위 ====================

    /**
     * 그룹 내 우선순위 (v6+)
     *
     * scheduleGroupId가 있을 때 그룹 내 시간대 정렬용.
     * 낮은 숫자가 높은 우선순위 (0이 최우선).
     * 기본값: 0
     *
     * ## 주의사항
     * - scheduleGroupId가 null이면 의미 없음
     * - 3차 고도화 MVP에서는 선택적 필드
     */
    val groupPriority: Int = 0,

    // ==================== v7 추가: 수정 시각 ====================

    /**
     * 마지막 수정 시각 (v7+)
     *
     * 우선순위 규칙 적용을 위한 필드.
     * "어디서나 적용" 스케줄 충돌 시 "최근 수정" 기준으로 사용.
     */
    val updatedAt: Long = createdAt,

    // ==================== v9 추가: 할일 관리 기능 ====================

    /**
     * 스케줄 정보 JSON (v9+)
     *
     * 목표(title) + 할일(todos) 정보를 JSON 형태로 저장.
     * ScheduleInfo 객체를 직렬화하여 저장.
     *
     * null: 목표/할일 없음 (기존 스케줄 호환)
     * JSON: ScheduleInfo 객체 직렬화
     *
     * @see com.allday.detoxy.domain.model.ScheduleInfo
     */
    val scheduleInfoJson: String? = null
)

