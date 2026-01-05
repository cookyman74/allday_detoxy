package com.allday.detoxy.core.manager

/**
 * 흑백 모드 결과 타입
 * 
 * GrayscaleManager 메서드의 결과를 타입 안전하게 표현
 * 내부 메서드는 Boolean 반환, UI 레이어에서 필요시 Result 타입 활용
 */
sealed class GrayscaleResult {
    
    /** 성공 */
    data object Success : GrayscaleResult()
    
    /** 권한 거부됨 (ACCESS_NOTIFICATION_POLICY 필요) */
    data object PermissionDenied : GrayscaleResult()
    
    /** 지원하지 않는 Android 버전 (Android 15 미만) */
    data object NotSupported : GrayscaleResult()
    
    /** 시스템에서 사용자 관리 모드로 전환됨 (룰 생성 불가) */
    data object UserManaged : GrayscaleResult()
    
    /** 이미 활성/비활성 상태 (중복 작업 스킵) */
    data object AlreadyInState : GrayscaleResult()
    
    /** 룰이 시스템에서 삭제됨 */
    data object RuleDeleted : GrayscaleResult()
    
    /** 알 수 없는 오류 */
    data class Error(val errorMessage: String) : GrayscaleResult()
    
    /**
     * 성공 여부 확인
     */
    val isSuccess: Boolean
        get() = this is Success || this is AlreadyInState
    
    /**
     * 사용자에게 보여줄 메시지
     */
    fun getDisplayMessage(): String = when (this) {
        is Success -> "흑백 모드가 적용되었습니다"
        is PermissionDenied -> "흑백 모드 권한이 필요합니다"
        is NotSupported -> "이 기기는 자동 흑백 모드를 지원하지 않습니다 (Android 15 이상 필요)"
        is UserManaged -> "시스템 설정에서 방해금지 모드를 직접 관리하고 있습니다"
        is AlreadyInState -> "이미 해당 상태입니다"
        is RuleDeleted -> "흑백 모드 설정이 시스템에서 삭제되었습니다"
        is Error -> "오류가 발생했습니다: $errorMessage"
    }
}
