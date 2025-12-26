package com.allday.detoxy.domain.util

import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleTodo
import com.allday.detoxy.domain.model.TodoCompletionStatus
import com.allday.detoxy.domain.model.TodoStatus

/**
 * 할일 결과 빌더
 * 
 * 세션 종료 시 사용자 응답과 ScheduleInfo를 기반으로 TodoStatus 목록을 생성합니다.
 */
object TodoResultBuilder {

    /**
     * 🆕 v9: 무응답(30초 타임아웃) 시 기본 상태 맵 생성
     * 
     * - 필수 항목: NOT_COMPLETED (미완료)
     * - 일반 항목: NO_RESPONSE (미응답)
     * 
     * @param todos 할일 목록
     * @return 할일ID -> 상태 맵
     */
    fun handleNoResponse(todos: List<ScheduleTodo>): Map<String, TodoCompletionStatus> {
        return todos.associate { todo ->
            todo.id to if (todo.isRequired) {
                TodoCompletionStatus.NOT_COMPLETED  // 필수 항목은 미완료
            } else {
                TodoCompletionStatus.NO_RESPONSE    // 일반 항목은 미응답
            }
        }
    }

    /**
     * 할일 결과 생성
     * 
     * ## 반환값 시맨틱
     * - null: 목표/할일이 없어서 저장 생략
     * - List<TodoStatus>: 저장할 할일 결과
     * 
     * ## 목표 상태 결정 규칙
     * - 목표만 있음: 사용자 입력 또는 fallback
     * - 목표+할일 있음: 할일 결과에서 자동 도출
     * 
     * @param scheduleId 스케줄 ID (목표 ID 생성용)
     * @param info 스케줄 정보
     * @param userResponses 사용자 응답 Map (할일ID -> 완료상태)
     * @return TodoStatus 목록 또는 null
     */
    fun buildTodoResult(
        scheduleId: String,
        info: ScheduleInfo,
        userResponses: Map<String, TodoCompletionStatus>
    ): List<TodoStatus>? {
        // 목표/할일 없는 경우: 저장 생략
        if (info.todos.isEmpty() && info.title.isBlank()) {
            return null
        }
        
        val result = mutableListOf<TodoStatus>()
        
        // 세부 할일 추가
        val todoStatuses = info.todos.map { todo ->
            TodoStatus(
                id = todo.id,
                content = todo.content,
                isRequired = todo.isRequired,
                status = getStatusWithRequiredFallback(userResponses[todo.id], todo.isRequired),
                isGoal = false
            )
        }
        
        // 목표 처리
        if (info.title.isNotBlank()) {
            val goalId = "goal:$scheduleId"
            val goalStatus = if (todoStatuses.isEmpty()) {
                // 목표만 있는 경우: 사용자 입력 또는 fallback
                getStatusWithRequiredFallback(userResponses[goalId], isRequired = true)
            } else {
                // 목표+할일 있는 경우: 할일 결과에서 자동 도출
                deriveGoalStatusFromTodos(todoStatuses)
            }
            result.add(
                TodoStatus(
                    id = goalId,
                    content = info.title,
                    isRequired = true,
                    status = goalStatus,
                    isGoal = true
                )
            )
        }
        
        result.addAll(todoStatuses)
        return result
    }

    /**
     * 할일 결과에서 목표 상태 자동 도출
     * 
     * ## 도출 규칙
     * - 필수 할일이 있는 경우: 모든 필수 할일 COMPLETED → 목표 COMPLETED
     * - 필수 할일이 없는 경우: 전체 할일 상태로 판단
     * 
     * @param todos 할일 상태 목록
     * @return 도출된 목표 상태
     */
    fun deriveGoalStatusFromTodos(todos: List<TodoStatus>): TodoCompletionStatus {
        val requiredTodos = todos.filter { it.isRequired }
        
        return if (requiredTodos.isNotEmpty()) {
            // 필수 할일 기준
            if (requiredTodos.all { it.status == TodoCompletionStatus.COMPLETED }) {
                TodoCompletionStatus.COMPLETED
            } else {
                TodoCompletionStatus.NOT_COMPLETED
            }
        } else {
            // 필수 할일 없으면 전체 기준
            when {
                todos.all { it.status == TodoCompletionStatus.COMPLETED } -> 
                    TodoCompletionStatus.COMPLETED
                todos.all { it.status == TodoCompletionStatus.NO_RESPONSE } -> 
                    TodoCompletionStatus.NO_RESPONSE  // 모두 미응답 시 목표도 미응답
                todos.any { it.status == TodoCompletionStatus.NOT_COMPLETED } -> 
                    TodoCompletionStatus.NOT_COMPLETED
                else -> 
                    TodoCompletionStatus.NO_RESPONSE  // 혼합 상태
            }
        }
    }

    /**
     * 사용자 응답에 따른 상태 반환 (무응답 시 fallback 적용)
     * 
     * ## Fallback 규칙
     * - 필수 항목 무응답: NOT_COMPLETED
     * - 일반 항목 무응답: NO_RESPONSE
     * 
     * @param userResponse 사용자 응답 (null = 무응답)
     * @param isRequired 필수 항목 여부
     * @return 최종 상태
     */
    fun getStatusWithRequiredFallback(
        userResponse: TodoCompletionStatus?,
        isRequired: Boolean
    ): TodoCompletionStatus {
        return userResponse ?: if (isRequired) {
            TodoCompletionStatus.NOT_COMPLETED
        } else {
            TodoCompletionStatus.NO_RESPONSE
        }
    }

    /**
     * 스케줄 제목 스냅샷 생성
     * 
     * 우선순위: 목표(title) > 스케줄명 > 기본값
     * 
     * @param info 스케줄 정보
     * @param scheduleName 스케줄 기본 이름 (label)
     * @return 스냅샷 제목
     */
    fun getScheduleTitleSnapshot(info: ScheduleInfo, scheduleName: String): String {
        return when {
            info.title.isNotBlank() -> info.title
            scheduleName.isNotBlank() -> scheduleName
            else -> "미지정 스케줄"
        }
    }
}
