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
    val createdAt: Long = System.currentTimeMillis()
)

