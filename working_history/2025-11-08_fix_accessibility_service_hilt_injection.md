# 작업 기록: 접근성 서비스 Hilt 의존성 주입 문제 수정

**작업 일시**: 2025-11-08  
**작업 범위**: 위치 기반 스케줄 실행 시 앱 차단이 작동하지 않는 문제 수정

## 문제 상황

위치 기반 스케줄이 실행되어도 앱 차단이 작동하지 않았습니다. USB 디바이스의 접근성 설정에서 "이 서비스가 제대로 작동하지 않습니다"라는 오류가 발견되었습니다.

## 원인 분석

### 1. 접근성 서비스 연결 실패
로그 분석 결과:
- `FocusAccessibilityService`가 활성화되어 있지만 실행 중이 아님
- `onServiceConnected()` 로그가 전혀 출력되지 않음
- 접근성 서비스가 초기화 중 크래시하거나 연결에 실패

### 2. Hilt 의존성 주입 실패
**핵심 원인**: `FocusAccessibilityService`가 `@AndroidEntryPoint`를 사용하고 있었지만, `AccessibilityService`는 시스템이 직접 인스턴스를 생성하므로 Hilt가 제대로 작동하지 않습니다.

```kotlin
// ❌ 문제가 있던 코드
@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {
    @Inject
    lateinit var repository: FocusRepository  // 초기화되지 않아 크래시 가능
    // ...
}
```

**문제점**:
- `AccessibilityService`는 시스템이 직접 인스턴스를 생성하므로 `@AndroidEntryPoint`가 작동하지 않음
- `lateinit var repository`가 초기화되지 않아 `onServiceConnected()` 또는 `handleBlockedApp()`에서 크래시 발생 가능
- 크래시로 인해 접근성 서비스가 연결되지 않고 "이 서비스가 제대로 작동하지 않습니다" 오류 발생

## 수정 내용

### 1. `@AndroidEntryPoint` 제거 및 `EntryPoint` 사용

```kotlin
// ✅ 수정된 코드
class FocusAccessibilityService : AccessibilityService() {
    /**
     * Hilt EntryPoint for manual dependency injection
     *
     * AccessibilityService는 시스템이 직접 인스턴스를 생성하므로
     * @AndroidEntryPoint를 사용할 수 없습니다.
     * EntryPointAccessors를 통해 수동으로 의존성을 가져옵니다.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FocusAccessibilityServiceEntryPoint {
        fun repository(): FocusRepository
    }

    private var repository: FocusRepository? = null  // nullable로 변경
    // ...
}
```

### 2. `onServiceConnected()`에서 수동 의존성 주입

```kotlin
override fun onServiceConnected() {
    super.onServiceConnected()
    Log.d(TAG, "✅ AccessibilityService connected")
    
    // 🆕 Hilt 의존성 주입 (EntryPoint 사용)
    try {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            FocusAccessibilityServiceEntryPoint::class.java
        )
        repository = entryPoint.repository()
        Log.d(TAG, "✅ Repository injected successfully")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to inject repository: ${e.message}", e)
        // repository가 null이어도 앱 차단 기능은 작동 (로깅만 실패)
    }
}
```

**효과**: 접근성 서비스 연결 시 수동으로 의존성을 주입하여 크래시를 방지합니다.

### 3. `repository` 사용 시 null 체크 추가

```kotlin
// 1. 차단 이벤트 로깅 (FocusInterruption 엔티티)
currentSessionId?.let { sessionId ->
    repository?.let { repo ->  // 🆕 null 체크 추가
        serviceScope.launch {
            try {
                repo.logInterruption(...)
                Log.d(TAG, "✅ Interruption logged: $packageName ($categoryName)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to log interruption", e)
            }
        }
    } ?: Log.w(TAG, "⚠️ Repository is null, interruption not logged")
} ?: Log.w(TAG, "⚠️ currentSessionId is null, interruption not logged")
```

**효과**: `repository`가 null이어도 앱 차단 기능은 정상 작동하며, 로깅만 실패합니다.

## 수정된 파일

- `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`
  - `@AndroidEntryPoint` 제거
  - `EntryPoint` 인터페이스 추가
  - `onServiceConnected()`에서 수동 의존성 주입
  - `repository`를 nullable로 변경하고 null 체크 추가

## 검증 방법

1. 앱 재설치 또는 접근성 서비스 재설정
2. 위치 기반 스케줄 실행
3. 로그 확인:
   ```
   ✅ AccessibilityService connected
   ✅ Repository injected successfully
   ```
4. 차단된 앱 실행 시:
   ```
   🔍 Checking app: {packageName} (Timer: RUNNING, ...)
   ⚠️ BLOCKED APP DETECTED: {packageName} (Category: ...)
   🚫 App blocked: {packageName} (...)
   ✅ Interruption logged: {packageName} (...)
   ```

## 전체적인 충돌 부분 검토

### 확인된 사항
1. **`BootCompletedReceiver`**: 이미 `EntryPoint`를 사용하여 수동 의존성 주입 구현 ✅
2. **`FocusTimerService`**: `@AndroidEntryPoint` 사용 - 정상 작동 (Service는 Hilt 지원) ✅
3. **`TimerViewModel`**: `FocusTimerService.currentSessionId` 관찰 - 접근성 서비스와 직접적인 충돌 없음 ✅

### 잠재적 문제점
- **접근성 서비스 재시작 필요**: 수정 후 접근성 서비스를 재설정하거나 앱을 재설치해야 할 수 있음
- **의존성 주입 실패 시**: `repository`가 null이어도 앱 차단 기능은 작동하지만, 차단 이벤트 로깅은 실패

## 예상 결과

- 접근성 서비스가 정상적으로 연결됨
- 위치 기반 스케줄 실행 시 앱 차단 기능 정상 작동
- 차단 이벤트 로깅 정상 작동
- "이 서비스가 제대로 작동하지 않습니다" 오류 해결

## 참고 사항

- 이 문제는 종종 발생하는 것으로 보고되었으므로, 향후 유사한 문제 발생 시 이 수정 사항을 참고하세요.
- `AccessibilityService`는 시스템이 직접 인스턴스를 생성하므로 `@AndroidEntryPoint`를 사용할 수 없습니다. 항상 `EntryPoint`를 사용하여 수동으로 의존성을 주입해야 합니다.

