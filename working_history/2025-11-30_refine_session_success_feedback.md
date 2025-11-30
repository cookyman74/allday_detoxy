# 작업 기록: 집중 모드 성공 피드백 개선

**작업 일시**: 2025-11-30
**작업 범위**: `PreferenceManager`, `FocusTimerService`, `LockOverlayService`, `TimerViewModel`, `TimerScreen`

## 문제 상황

**사용자 피드백**:
- 이전 구현에서는 타이머 완료 시 잠금 화면(오버레이)에 3초간 "성공!" 메시지가 표시되었음.
- 디지털 디톡시 앱의 철학과 맞지 않음: 사용자가 계속 화면을 보도록 강제하는 것은 부적절함.
- 알림이 시스템 트레이에 남지 않아 나중에 확인할 수 없었음.

**개선 요구사항**:
1.  오버레이 애니메이션 제거 (조용히 사라지도록)
2.  시스템 알림 센터에 성공 알림 발송
3.  앱을 다시 열었을 때 축하 다이얼로그 표시

## 수정 내용

### 1. `PreferenceManager` 수정: 성공 상태 저장
- **파일**: `app/src/main/java/com/allday/detoxy/core/utils/PreferenceManager.kt`
- **변경**:
    - `KEY_PENDING_SUCCESS_ANIMATION` 상수 추가.
    - `hasPendingSuccessAnimation()`: 성공 애니메이션 대기 상태 확인.
    - `setPendingSuccessAnimation(boolean)`: 성공 애니메이션 대기 상태 설정.

```kotlin
fun hasPendingSuccessAnimation(): Boolean {
    return prefs.getBoolean(KEY_PENDING_SUCCESS_ANIMATION, false)
}

fun setPendingSuccessAnimation(pending: Boolean) {
    prefs.edit().putBoolean(KEY_PENDING_SUCCESS_ANIMATION, pending).apply()
}
```

### 2. `FocusTimerService` 수정: 성공 시 플래그 설정
- **파일**: `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`
- **변경**:
    - `LockOverlayService.showSuccess()` 호출 제거.
    - 타이머 정상 완료 시:
        - `PreferenceManager.setPendingSuccessAnimation(true)` 호출.
        - `showSuccessNotification()` 호출 (시스템 트레이에 알림 발송).
        - 서비스 종료 지연 시간을 3.5초에서 0.5초로 단축.

```kotlin
// 정상 완료
if (_state.value == FocusState.RUNNING && _remainingSeconds.value == 0) {
    // 성공 상태 저장
    val preferenceManager = PreferenceManager(applicationContext)
    preferenceManager.setPendingSuccessAnimation(true)
    
    // 알림 발송
    showSuccessNotification()
    
    // ...
    delay(500)
    stopSelf()
}
```

### 3. `LockOverlayService` 수정: 성공 애니메이션 제거
- **파일**: `app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt`
- **변경**:
    - `ACTION_SHOW_SUCCESS` 상수 제거.
    - `showSuccess()` 메서드 제거.
    - `showSuccessOverlay()` 메서드 제거.
    - `onStartCommand`에서 `ACTION_SHOW_SUCCESS` 처리 제거.

### 4. `TimerViewModel` 수정: 성공 애니메이션 상태 관리
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`
- **변경**:
    - `_showSuccessAnimation` StateFlow 추가.
    - `checkPendingSuccessAnimation()` 메서드 추가: `init` 블록에서 호출하여 앱 실행 시 플래그 확인.
    - `onSuccessAnimationShown()` 메서드 추가: 다이얼로그 표시 후 플래그 초기화.

```kotlin
private val _showSuccessAnimation = MutableStateFlow(false)
val showSuccessAnimation: StateFlow<Boolean> = _showSuccessAnimation.asStateFlow()

fun checkPendingSuccessAnimation() {
    val preferenceManager = PreferenceManager(application)
    if (preferenceManager.hasPendingSuccessAnimation()) {
        _showSuccessAnimation.value = true
    }
}

fun onSuccessAnimationShown() {
    val preferenceManager = PreferenceManager(application)
    preferenceManager.setPendingSuccessAnimation(false)
    _showSuccessAnimation.value = false
}
```

### 5. `TimerScreen` 수정: 축하 다이얼로그 표시
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/TimerScreen.kt`
- **변경**:
    - `showSuccessAnimation` 상태 관찰.
    - `SuccessCelebrationDialog` Composable 추가 및 조건부 표시.

```kotlin
val showSuccessAnimation by viewModel.showSuccessAnimation.collectAsState()

if (showSuccessAnimation) {
    SuccessCelebrationDialog(
        onDismiss = { viewModel.onSuccessAnimationShown() }
    )
}
```

## 검증 결과

### 빌드 검증
```bash
./gradlew compileDebugKotlin
```
- **결과**: `BUILD SUCCESSFUL`

### 기대 효과
- **타이머 완료 시 (백그라운드)**:
    - 잠금 화면이 조용히 사라짐 (애니메이션 없음).
    - "🎉 집중 성공!" 알림이 시스템 트레이에 발송됨.
- **앱 재실행 시**:
    - 타이머 화면에서 축하 다이얼로그가 자동으로 표시됨.
    - 사용자가 "확인"을 누르면 플래그가 초기화되고 다이얼로그가 닫힘.
- **사용자 경험**:
    - 디지털 디톡시 철학에 부합: 화면을 강제로 보게 하지 않음.
    - 사용자가 원할 때 앱을 열어 성취를 확인할 수 있음.
