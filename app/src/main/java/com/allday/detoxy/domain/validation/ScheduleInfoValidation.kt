package com.allday.detoxy.domain.validation

import com.allday.detoxy.domain.model.ScheduleInfo
import com.allday.detoxy.domain.model.ScheduleTodo

/**
 * ScheduleInfo 검증 상수 및 함수
 * 
 * 목표(title) 및 할일(todos) 입력값에 대한 검증 규칙을 정의합니다.
 */
object ScheduleInfoValidation {
    /** 목표 최대 길이 */
    const val MAX_TITLE_LENGTH = 50
    
    // MVP에서 description/memo 미사용으로 상수 제거
    // v2에서 UI 추가 시 아래 상수 복원:
    // const val MAX_DESCRIPTION_LENGTH = 200
    // const val MAX_MEMO_LENGTH = 500
}

/**
 * ScheduleInfo 검증 결과
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    /** 성공 여부 확인 */
    val isSuccess: Boolean get() = this is Success
    
    /** 에러 메시지 반환 (에러인 경우) */
    val errorMessage: String? get() = (this as? Error)?.message
}

/**
 * ScheduleInfo 입력값 검증
 * 
 * ## 검증 규칙
 * - 목표(title): 최대 50자
 * - 할일 개수: 최대 5개
 * - 할일 내용: 비어있지 않고 최대 100자
 * 
 * @param info 검증할 ScheduleInfo
 * @return ValidationResult.Success 또는 ValidationResult.Error
 */
fun validateScheduleInfo(info: ScheduleInfo): ValidationResult {
    // 목표 길이 검증
    if (info.title.length > ScheduleInfoValidation.MAX_TITLE_LENGTH) {
        return ValidationResult.Error(
            "목표는 ${ScheduleInfoValidation.MAX_TITLE_LENGTH}자 이내로 입력해주세요"
        )
    }
    
    // 할일 개수 검증
    if (info.todos.size > ScheduleTodo.MAX_TODO_COUNT) {
        return ValidationResult.Error(
            "할일은 최대 ${ScheduleTodo.MAX_TODO_COUNT}개까지 등록 가능합니다"
        )
    }
    
    // 각 할일 검증
    info.todos.forEachIndexed { index, todo ->
        // 내용 비어있는지 검증
        if (todo.content.isBlank()) {
            return ValidationResult.Error("${index + 1}번째 할일 내용을 입력해주세요")
        }
        
        // 내용 길이 검증
        if (todo.content.length > ScheduleTodo.MAX_CONTENT_LENGTH) {
            return ValidationResult.Error(
                "할일 내용은 ${ScheduleTodo.MAX_CONTENT_LENGTH}자 이내로 입력해주세요"
            )
        }
    }
    
    return ValidationResult.Success
}
