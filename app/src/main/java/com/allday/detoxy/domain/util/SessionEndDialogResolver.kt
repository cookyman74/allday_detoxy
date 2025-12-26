package com.allday.detoxy.domain.util

import com.allday.detoxy.domain.model.ScheduleInfo

/**
 * 🆕 v9: 세션 종료 다이얼로그 유형 결정
 * 
 * 스케줄 정보에 따라 표시할 다이얼로그 유형을 결정합니다.
 */
sealed class SessionEndDialogType {
    /** 다이얼로그 생략 (목표/할일 없음) */
    object Skip : SessionEndDialogType()
    
    /** 목표만 있는 경우 - 예/아니오 다이얼로그 */
    data class GoalOnly(val goal: String) : SessionEndDialogType()
    
    /** 할일이 있는 경우 - 체크리스트 다이얼로그 */
    data class TodoChecklist(val scheduleInfo: ScheduleInfo) : SessionEndDialogType()
}

/**
 * 스케줄 정보에 따라 세션 종료 다이얼로그 유형 결정
 * 
 * @param scheduleInfo 스케줄 정보 (목표/할일)
 * @return 표시할 다이얼로그 유형
 * 
 * 규칙:
 * - 스케줄 정보 없음 → Skip
 * - 목표/할일 모두 비어있음 → Skip
 * - 목표만 있고 할일 없음 → GoalOnly
 * - 할일이 있음 → TodoChecklist
 */
fun resolveSessionEndDialogType(scheduleInfo: ScheduleInfo?): SessionEndDialogType {
    if (scheduleInfo == null) return SessionEndDialogType.Skip
    
    return when {
        scheduleInfo.title.isBlank() && scheduleInfo.todos.isEmpty() -> 
            SessionEndDialogType.Skip
        scheduleInfo.todos.isEmpty() && scheduleInfo.title.isNotBlank() -> 
            SessionEndDialogType.GoalOnly(scheduleInfo.title)
        else -> 
            SessionEndDialogType.TodoChecklist(scheduleInfo)
    }
}
