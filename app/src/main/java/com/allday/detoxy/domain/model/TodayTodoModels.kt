package com.allday.detoxy.domain.model

import java.time.LocalTime

/**
 * 오늘의 할일 관리용 통합 모델
 * 
 * 계획된 할일과 완료된 세션 결과를 통합하여 표시하기 위한 데이터 클래스
 */
data class TodayTodoItem(
    val scheduleId: String,
    val scheduleTitle: String,
    val scheduledTime: LocalTime?,      // 예정 시간 (null = 위치 기반 또는 이미 완료됨)
    val todoContent: String,
    val status: TodayTodoStatus,
    val isRequired: Boolean,
    val isGoal: Boolean,
    val sourceType: TodoSourceType,      // 어디서 온 데이터인지
    val sessionId: String? = null,       // 완료된 경우 세션 ID
    val originalTodoId: String? = null   // 원본 할일 ID (수정 시 사용)
)

/**
 * 오늘의 할일 상태
 */
enum class TodayTodoStatus {
    PENDING,        // 대기 (계획된 할일, 세션 미완료)
    COMPLETED,      // 완료
    NOT_COMPLETED,  // 미완료
    NO_RESPONSE     // 미응답
}

/**
 * 할일 데이터 출처
 */
enum class TodoSourceType {
    PLANNED,        // 계획된 할일 (TimeBasedAutoRun/LocationBasedAutoRun)
    SESSION_RESULT  // 세션 결과 (FocusSessionTodoResult)
}

/**
 * 할일 필터
 */
enum class TodoFilter {
    ALL,           // 전체
    PENDING,       // 대기 (아직 세션 안 함)
    COMPLETED,     // 완료
    INCOMPLETE     // 미완료 + 미응답
}

/**
 * 스케줄별 그룹화된 오늘의 할일
 */
data class TodayScheduleGroup(
    val scheduleId: String,
    val scheduleTitle: String,
    val scheduledTime: LocalTime?,
    val isLocationBased: Boolean,
    val goals: List<String>,            // 병합된 목표들
    val items: List<TodayTodoItem>
) {
    // 통계 계산
    val totalCount: Int get() = items.size
    val pendingCount: Int get() = items.count { it.status == TodayTodoStatus.PENDING }
    val completedCount: Int get() = items.count { it.status == TodayTodoStatus.COMPLETED }
    val incompleteCount: Int get() = items.count { 
        it.status == TodayTodoStatus.NOT_COMPLETED || it.status == TodayTodoStatus.NO_RESPONSE 
    }
}
