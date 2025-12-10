package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 스케줄 그룹 엔티티 (v5+)
 *
 * 여러 TimeBasedAutoRun을 그룹으로 묶어 위치 기반으로 활성화/비활성화합니다.
 *
 * ## 사용 예시
 * 1. "업무 시간표" 그룹 생성
 * 2. 오전 10시, 오후 2시, 오후 4시 TimeBasedAutoRun 추가 (scheduleGroupId 설정)
 * 3. "회사" LocationBasedAutoRun에 linkedScheduleGroupId 설정
 * 4. 회사 진입 → "업무 시간표" 그룹 활성화 → 3개 알람 등록
 * 5. 회사 이탈 → "업무 시간표" 그룹 비활성화 → 3개 알람 취소
 *
 * ## 관계
 * - TimeBasedAutoRun (N) ← scheduleGroupId → ScheduleGroup (1)
 * - LocationBasedAutoRun (N) ← linkedScheduleGroupId → ScheduleGroup (1)
 *
 * ## 참조 무결성 (Soft Reference)
 * - Foreign Key 없음
 * - 그룹 삭제 시 연결된 자동 실행은 독립 모드로 전환 (groupId → NULL)
 * - 애플리케이션 레벨에서 참조 무결성 보장 (ViewModel/Repository)
 *
 * @property id 고유 ID (UUID)
 * @property name 그룹 이름 (예: "업무 시간표", "공부 루틴")
 * @property description 그룹 설명 (옵션)
 * @property isActive 활성화 여부
 * @property createdAt 생성 시간 (timestamp)
 *
 * @see TimeBasedAutoRun.scheduleGroupId
 * @see LocationBasedAutoRun.linkedScheduleGroupId
 */
@Entity(
    tableName = "schedule_group",
    indices = [Index(value = ["isActive"])]
)
data class ScheduleGroup(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 그룹 이름
     *
     * 사용자가 지정한 그룹 이름 (예: "업무 시간표", "공부 루틴", "주말 루틴")
     */
    val name: String,

    /**
     * 그룹 설명 (옵션)
     *
     * 그룹에 대한 부가 설명 (예: "평일 회사 업무용", "주말 집중 학습용")
     */
    val description: String? = null,

    /**
     * 활성화 여부
     *
     * false: 그룹 비활성화 → 연결된 종속 자동 실행(isIndependent=false)도 비활성화
     * true: 그룹 활성화 (기본값)
     */
    val isActive: Boolean = true,

    /**
     * 생성 시간 (timestamp)
     */
    val createdAt: Long = System.currentTimeMillis(),

    // ==================== v6 추가: UI 및 통계 필드 ====================

    /**
     * 아이콘 타입 (v6+)
     *
     * UI 표시용 아이콘 타입.
     * 허용값: WORK, STUDY, GYM, HOME, CUSTOM 등
     * 기본값: "WORK"
     */
    val iconType: String = "WORK",

    /**
     * 색상 코드 (v6+)
     *
     * UI 표시용 색상 (HEX 형식).
     * 기본값: "#4CAF50" (녹색)
     */
    val colorHex: String = "#4CAF50",

    /**
     * 마지막 활성화 시각 (v6+)
     *
     * 시간표가 마지막으로 활성화된 시각 (timestamp).
     * 통계 및 분석 용도.
     * null: 아직 활성화된 적 없음
     */
    val lastActivatedAt: Long? = null,

    // ==================== v7 추가: 수정 시각 ====================

    /**
     * 마지막 수정 시각 (v7+)
     *
     * 우선순위 규칙 적용을 위한 필드.
     * 충돌 해소 시 "최근 수정" 기준으로 사용.
     */
    val updatedAt: Long = System.currentTimeMillis(),

    // ==================== v8 추가: 통합 제어 ====================

    /**
     * 사용자 수동 제어 상태 (v8+)
     *
     * 스케줄 그룹의 활성/비활성/일시중지 상태를 사용자가 명시적으로 제어할 때 사용합니다.
     * 위치기반 스케줄에서 자동 활성화를 차단하는 데 사용됩니다.
     *
     * - null: 자동 모드 (기본값, 기존 동작 유지 - 위치에 따라 자동 활성화/비활성화)
     * - "INACTIVE": 사용자가 명시적으로 비활성화 (Geofence 해제, 위치 감지 안됨)
     * - "PAUSED": 사용자가 명시적으로 일시중지 (Geofence 유지, 알람만 건너뛰기)
     *
     * @see pauseUntil 일시중지 해제 시각
     */
    val manualOverrideState: String? = null,

    /**
     * 일시중지 해제 시각 (v8+)
     *
     * manualOverrideState가 "PAUSED"일 때 자동 해제 시각을 지정합니다.
     *
     * - null: 일시중지 아님 또는 무기한 일시중지
     * - timestamp: 해당 시각이 지나면 자동으로 활성 상태로 전환
     *
     * @see manualOverrideState
     */
    val pauseUntil: Long? = null
)

