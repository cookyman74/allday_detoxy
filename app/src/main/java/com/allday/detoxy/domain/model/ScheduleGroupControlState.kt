package com.allday.detoxy.domain.model

/**
 * 스케줄 그룹 통합 제어 상태 (v8+)
 *
 * 스케줄 그룹의 활성/일시중지/비활성 상태를 나타냅니다.
 * UI에서 ScheduleControlButton의 상태 표시에 사용됩니다.
 *
 * ## 상태 정의
 * - ACTIVE: 정상 작동 (위치 감지 + 알람 트리거)
 * - PAUSED: 일시중지 (위치 감지 O, 알람 트리거 X)
 * - INACTIVE: 비활성화 (위치 감지 X, 알람 트리거 X)
 *
 * ## DB 매핑
 * - ACTIVE: manualOverrideState = null (또는 pauseUntil 만료)
 * - PAUSED: manualOverrideState = "PAUSED" (pauseUntil 미만료)
 * - INACTIVE: manualOverrideState = "INACTIVE"
 *
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 */
enum class ScheduleGroupControlState {
    /**
     * 활성 상태
     *
     * - 위치 기반: Geofence 활성화, 위치 진입 시 알람 등록
     * - 시간 기반: 알람 정상 트리거
     * - UI: 녹색 배경, 체크 아이콘
     */
    ACTIVE,

    /**
     * 일시중지 상태
     *
     * - 위치 기반: Geofence 유지, 위치 진입 시 알람 건너뛰기
     * - 시간 기반: 알람 건너뛰기
     * - pauseUntil 시각 이후 자동으로 ACTIVE 전환
     * - UI: 주황색 배경, 일시정지 아이콘 + 남은 시간 표시
     */
    PAUSED,

    /**
     * 비활성 상태
     *
     * - 위치 기반: Geofence 해제, 위치 감지 안됨
     * - 시간 기반: 알람 건너뛰기
     * - UI: 회색 배경, 비활성 아이콘
     */
    INACTIVE;

    companion object {
        /**
         * ScheduleGroup 엔티티에서 제어 상태 계산
         *
         * @param manualOverrideState DB의 manualOverrideState 값
         * @param pauseUntil DB의 pauseUntil 값 (timestamp)
         * @return 현재 제어 상태
         */
        fun fromEntity(manualOverrideState: String?, pauseUntil: Long?): ScheduleGroupControlState {
            return when (manualOverrideState) {
                "INACTIVE" -> INACTIVE
                "PAUSED" -> {
                    // pauseUntil 만료 확인
                    if (pauseUntil != null && System.currentTimeMillis() >= pauseUntil) {
                        // 만료됨 → ACTIVE로 간주 (백그라운드에서 DB 정리 필요)
                        ACTIVE
                    } else {
                        PAUSED
                    }
                }
                else -> ACTIVE // null 또는 기타 값은 ACTIVE
            }
        }
    }
}

