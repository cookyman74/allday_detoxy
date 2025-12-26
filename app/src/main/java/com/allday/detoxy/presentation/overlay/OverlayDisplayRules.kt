package com.allday.detoxy.presentation.overlay

import com.allday.detoxy.domain.model.ScheduleInfo

/**
 * 오버레이 표시 규칙
 * 
 * 차단 오버레이에서 목표/할일 표시 시 사용되는 상수와 유틸리티 함수
 */
object OverlayDisplayRules {
    /** 오버레이에 표시할 최대 할일 개수 */
    const val MAX_TODO_DISPLAY_COUNT = 3
    
    /** 할일 텍스트 최대 길이 (초과 시 "...") */
    const val MAX_TODO_TEXT_LENGTH = 20
    
    /** 목표 텍스트 최대 길이 */
    const val MAX_GOAL_TEXT_LENGTH = 30
}

/**
 * 오버레이 표시용 텍스트 자르기
 * 
 * @param text 원본 텍스트
 * @param maxLength 최대 길이
 * @return 자른 텍스트 (초과 시 "..." 추가)
 */
fun truncateForOverlay(text: String, maxLength: Int): String =
    if (text.length > maxLength) "${text.take(maxLength)}..." else text

/**
 * 오버레이에 표시할 내용 결정
 * 
 * @param info 스케줄 정보
 * @param showTodo 할일 표시 여부
 * @param showDetailedTodo 상세 할일 표시 여부
 * @param hideGoal 목표 숨기기 (이모지만 표시)
 * @return 표시 모드
 */
fun determineOverlayDisplayMode(
    info: ScheduleInfo?,
    showTodo: Boolean,
    showDetailedTodo: Boolean,
    hideGoal: Boolean
): OverlayDisplayMode {
    // 스케줄 정보가 없거나 비어있으면 숨김
    if (info == null || !info.hasContent()) {
        return OverlayDisplayMode.HIDDEN
    }
    
    // 🔧 우선순위 수정: showTodo OFF가 최우선 (목표 표시 자체를 끈 경우)
    return when {
        !showTodo -> OverlayDisplayMode.HIDDEN        // 1순위: 목표 표시 OFF
        hideGoal -> OverlayDisplayMode.EMOJI_ONLY     // 2순위: 이모지만 표시
        showDetailedTodo && info.todos.isNotEmpty() -> OverlayDisplayMode.GOAL_AND_TODOS
        else -> OverlayDisplayMode.GOAL_ONLY
    }
}

/**
 * 오버레이 표시 모드
 */
enum class OverlayDisplayMode {
    /** 이모지만 표시 (프라이버시 보호) */
    EMOJI_ONLY,
    
    /** 모두 숨김 */
    HIDDEN,
    
    /** 목표만 표시 */
    GOAL_ONLY,
    
    /** 목표 + 할일 목록 표시 */
    GOAL_AND_TODOS
}
