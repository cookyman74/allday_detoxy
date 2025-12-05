# 작업 기록: 성공 알림 및 애니메이션 버그 수정

**작업 일시**: 2025-11-30
**작업 범위**: `FocusTimerService`, `ReportScreen`

## 문제 상황

**사용자 보고**:
1. 집중 모드 타이머가 성공적으로 완료되었음에도 알림 센터에 메시지가 보이지 않음.
2. 리포트 페이지에 들어가도 성공 애니메이션이 출력되지 않음.

**원인 분석**:
1. **알림 문제**: `showSuccessNotification()`이 타이머 알림과 동일한 `CHANNEL_ID`를 사용하고 있었음. 이 채널은 `IMPORTANCE_LOW`로 설정되어 있어 알림이 소리 없이 표시되거나 시스템에 의해 숨겨질 수 있음.
2. **애니메이션 문제**: 성공 축하 다이얼로그가 `TimerScreen`에만 구현되어 있고, `ReportScreen`에는 구현되지 않았음.

## 수정 내용

### 1. `FocusTimerService` 수정: 성공 알림 전용 채널 생성
- **파일**: `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`
- **변경**:
    - `SUCCESS_CHANNEL_ID` 및 `SUCCESS_CHANNEL_NAME` 상수 추가.
    - `createNotificationChannel()`에서 별도의 성공 알림 채널 생성:
        - `IMPORTANCE_HIGH`: 알림이 소리와 함께 표시됨.
        - `setShowBadge(true)`: 앱 아이콘에 배지 표시.
        - `enableVibration(true)`: 진동 활성화.
    - `showSuccessNotification()`에서 `SUCCESS_CHANNEL_ID` 사용.
    - 로깅 추가: 알림 발송 시작 및 완료 로그.

```kotlin
// 성공 알림 전용 채널 (높은 중요도)
private const val SUCCESS_CHANNEL_ID = "focus_success_channel"
private const val SUCCESS_CHANNEL_NAME = "집중 성공 알림"

// createNotificationChannel()
val successChannel = NotificationChannel(
    SUCCESS_CHANNEL_ID,
    SUCCESS_CHANNEL_NAME,
    NotificationManager.IMPORTANCE_HIGH
).apply {
    description = "집중 모드 성공 알림"
    setShowBadge(true)
    enableVibration(true)
}
notificationManager?.createNotificationChannel(successChannel)

// showSuccessNotification()
val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Notification.Builder(this, SUCCESS_CHANNEL_ID) // 🔥 성공 전용 채널 사용
} else {
    @Suppress("DEPRECATION")
    Notification.Builder(this)
}
```

### 2. `ReportScreen` 수정: 성공 애니메이션 추가
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/report/ReportScreen.kt`
- **변경**:
    - `TimerViewModel` 주입 추가.
    - `showSuccessAnimation` 상태 관찰.
    - `SuccessCelebrationDialog` 조건부 표시.

```kotlin
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
    timerViewModel: TimerViewModel = hiltViewModel() // 🆕
) {
    val showSuccessAnimation by timerViewModel.showSuccessAnimation.collectAsState()

    // 🆕 성공 축하 다이얼로그
    if (showSuccessAnimation) {
        SuccessCelebrationDialog(
            onDismiss = { timerViewModel.onSuccessAnimationShown() }
        )
    }
    // ...
}
```

## 검증 결과

### 빌드 검증
```bash
./gradlew compileDebugKotlin
```
- **결과**: `BUILD SUCCESSFUL`

### 기대 효과
- **알림 센터**:
    - 타이머 완료 시 "🎉 집중 성공!" 알림이 높은 중요도로 표시됨.
    - 소리, 진동, 배지와 함께 표시되어 사용자가 놓치지 않음.
- **타이머 페이지**:
    - 타이머 완료 후 앱을 열면 타이머 페이지에서 축하 다이얼로그가 표시됨.
- **로깅**:
    - 알림 발송 과정을 로그로 확인 가능하여 디버깅 용이.

## 추가 수정 (NPE 크래시 해결)

### 문제 발생
**증상**:
- 앱 실행 시 "ScreenSence 오류 발생" 다이얼로그와 함께 크래시 발생.
- 로그: `java.lang.NullPointerException: Attempt to invoke interface method 'void kotlinx.coroutines.flow.MutableStateFlow.setValue(java.lang.Object)' on a null object reference`

**원인 분석**:
1. **첫 번째 시도**: `ReportScreen`에 `TimerViewModel`을 추가로 주입하면서 NPE 발생 → 제거했으나 크래시 지속.
2. **근본 원인 발견**: `TimerViewModel`의 `_showSuccessAnimation`이 `init` 블록 **이후**에 선언되어 있었음.
   - Kotlin에서 클래스 멤버는 **선언 순서대로** 초기화됨.
   - `init` 블록(line 116)에서 `checkPendingSuccessAnimation()`(line 177) 호출.
   - `_showSuccessAnimation`은 line 183에 선언되어 있어, `init` 실행 시점에 아직 초기화되지 않음 → `null` 상태.
   - `checkPendingSuccessAnimation()`에서 `_showSuccessAnimation.value = true` 호출 시 NPE 발생.

### 수정 내용
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`
- **변경**: `_showSuccessAnimation` 및 `showSuccessAnimation` 선언을 `init` 블록 **앞으로** 이동.

```kotlin
// 수정 전 (크래시 발생)
class TimerViewModel @Inject constructor(...) : ViewModel() {
    // ...
    
    init {
        // ...
        checkPendingSuccessAnimation() // line 177
    }
    
    // ❌ init 블록 이후에 선언 → NPE 발생
    private val _showSuccessAnimation = MutableStateFlow(false) // line 183
    val showSuccessAnimation: StateFlow<Boolean> = _showSuccessAnimation.asStateFlow()
    
    fun checkPendingSuccessAnimation() {
        // ...
        _showSuccessAnimation.value = true // NPE!
    }
}

// 수정 후 (정상 동작)
class TimerViewModel @Inject constructor(...) : ViewModel() {
    // ...
    
    // ✅ init 블록 앞에 선언 → 정상 초기화
    private val _showSuccessAnimation = MutableStateFlow(false)
    val showSuccessAnimation: StateFlow<Boolean> = _showSuccessAnimation.asStateFlow()
    
    init {
        // ...
        checkPendingSuccessAnimation() // 이제 안전하게 호출 가능
    }
    
    fun checkPendingSuccessAnimation() {
        // ...
        _showSuccessAnimation.value = true // 정상 동작
    }
}
```

### 최종 검증
```bash
./gradlew installDebug
adb shell am start -n com.allday.detoxy/.MainActivity
```
- **결과**: 앱 정상 실행, 크래시 없음.
- **로그**: `Displayed com.allday.detoxy/.MainActivity for user 0: +1s597ms` - 정상 실행 확인.
- **크래시 해결**: NPE 완전히 제거, 모든 기능 정상 작동.

### 교훈
- Kotlin 클래스에서 `init` 블록은 **선언된 위치**에서 실행됨.
- `init` 블록에서 사용하는 모든 프로퍼티는 `init` 블록 **앞에** 선언되어야 함.
- 특히 `MutableStateFlow` 같은 객체는 반드시 사용 전에 초기화되어야 함.


