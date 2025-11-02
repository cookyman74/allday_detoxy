package com.allday.detoxy.domain.model

/**
 * 시간표 생성 모드 (Phase 1)
 *
 * QuickCreateScheduleDialog에서 사용자가 선택하는 시간표 생성 방식을 정의합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * when (mode) {
 *     CreationMode.TEMPLATE -> {
 *         // 템플릿으로 시간표 생성 (Phase 2)
 *     }
 *     CreationMode.CUSTOM -> {
 *         // 커스텀 시간대로 시간표 생성 (Phase 1)
 *     }
 * }
 * ```
 *
 * @see com.allday.detoxy.presentation.ui.autorun.components.QuickCreateScheduleDialog
 */
enum class CreationMode {
    /**
     * 템플릿으로 시작
     *
     * 기본 시간대가 포함된 템플릿을 선택하여 시간표를 생성합니다.
     * Phase 2에서 구현됩니다.
     *
     * ## 제공되는 템플릿
     * - 매일: 09-12시, 14-18시 (매일)
     * - 평일: 09-12시, 14-18시 (월-금)
     * - 주말: 10-12시, 14-17시 (토-일)
     * - 집: 19-22시 (매일, 위치 필요)
     * - 학교: 09-12시, 14-17시 (월-금, 위치 필요)
     * - 회사: 10-12시, 14-16시, 16-17시 30분 (월-금, 위치 필요)
     */
    TEMPLATE,
    
    /**
     * 시간대 직접 추가
     *
     * 사용자가 원하는 시간대를 직접 입력하여 시간표를 생성합니다.
     * Phase 1에서 구현됩니다.
     *
     * ## 입력 항목
     * - 시작 시간 (시:분)
     * - 기간 (15분~12시간)
     * - 차단 강도 (표준/중간/완전)
     * - 요일 선택 (매일/평일/주말/개별 선택)
     */
    CUSTOM
}

