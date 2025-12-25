package com.allday.detoxy.domain.model

import java.util.UUID

/**
 * 스케줄 정보 모델
 *
 * 스케줄에 연결된 목표와 할일 정보를 저장합니다.
 * JSON 형태로 직렬화되어 TimeBasedAutoRun/LocationBasedAutoRun에 저장됩니다.
 *
 * @property title 메인 목표 (예: "보고서 초안 완성하기")
 * @property description 목표 상세 설명 (MVP: 항상 빈 문자열)
 * @property memo 개인 메모 (MVP: 항상 빈 문자열)
 * @property todos 세부 할일 목록 (최대 5개)
 */
data class ScheduleInfo(
    val title: String = "",
    val description: String = "",  // MVP: 항상 빈 문자열
    val memo: String = "",         // MVP: 항상 빈 문자열
    val todos: List<ScheduleTodo> = emptyList()
) {
    companion object {
        /** 빈 ScheduleInfo 인스턴스 */
        val EMPTY = ScheduleInfo()
    }
    
    /** 목표 또는 할일이 하나라도 있는지 확인 */
    fun hasContent(): Boolean = title.isNotBlank() || todos.isNotEmpty()
}

/**
 * 개별 할일 항목 모델
 *
 * @property id 고유 ID (UUID)
 * @property content 할일 내용 (최대 100자)
 * @property isRequired 필수 항목 여부 (true: 미응답 시 미완료 처리)
 * @property orderIndex 표시 순서
 */
data class ScheduleTodo(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isRequired: Boolean = false,
    val orderIndex: Int = 0
) {
    companion object {
        /** 최대 할일 개수 */
        const val MAX_TODO_COUNT = 5
        
        /** 할일 내용 최대 길이 */
        const val MAX_CONTENT_LENGTH = 100
    }
}

/**
 * 할일 상태 저장 모델
 *
 * 세션 종료 시 각 할일의 완료 상태를 기록합니다.
 * FocusSessionTodoResultEntity의 todoResultsJson에 List<TodoStatus> 형태로 저장됩니다.
 *
 * @property id 원본 할일 ID 또는 목표 ID ("goal:{scheduleId}")
 * @property content 할일/목표 내용 (스냅샷)
 * @property isRequired 필수 항목 여부
 * @property status 완료 상태
 * @property isGoal 메인 목표 여부 (true: 목표, false: 세부 할일)
 */
data class TodoStatus(
    val id: String,
    val content: String,
    val isRequired: Boolean,
    val status: TodoCompletionStatus,
    val isGoal: Boolean = false
)

/**
 * 할일 완료 상태 enum
 */
enum class TodoCompletionStatus {
    /** 사용자가 명시적으로 완료 표시 */
    COMPLETED,
    
    /** 사용자가 명시적으로 미완료 표시 또는 필수 항목 무응답 */
    NOT_COMPLETED,
    
    /** 30초 무응답 (일반 항목만, 통계에서 별도 처리) */
    NO_RESPONSE
}

/**
 * 스케줄 타입 enum
 */
enum class ScheduleType {
    TIME_BASED,
    LOCATION_BASED
}
