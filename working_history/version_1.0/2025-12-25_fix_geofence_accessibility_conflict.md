# 작업 기록: 위치 기반 자동 스케줄 ↔ 접근성 서비스 충돌 수정

**작업 일시**: 2025-12-25
**작업 범위**: Geofence 기반 자동 스케줄과 접근성 서비스 간 충돌 해결 및 디버깅 로그 강화

## 문제 상황

**증상**:
- 위치 기반 자동 스케줄이 작동하는 동안 접근성 서비스에서 충돌 발생
- 접근성 설정에서 "이 서비스가 제대로 작동하지 않습니다" 메시지 표시
- `adb shell dumpsys accessibility`에서 `Crashed services`에 앱이 표시됨
- 접근성 서비스를 껐다 켜면 임시 복구되지만, 일정 시간 후 다시 발생

**원인 분석**:
1. **`GeofenceTransitionsReceiver`의 예외 처리 취약점**: 
   - `scope.launch` 내 예외가 전체 scope에 영향을 줄 수 있음
   - `Throwable`(Error 포함)을 catch하지 않아 크래시 가능성

2. **`ScheduleGroupManager`의 GlobalScope 사용**:
   - `startAutoMode()`에서 `GlobalScope.launch` 사용
   - 구조화된 동시성 위반, 예외가 제대로 전파되지 않음

3. **Hilt EntryPoint 초기화 타이밍 문제**:
   - Geofence 이벤트 수신 시 앱이 완전히 초기화되지 않은 상태에서 `EntryPointAccessors` 호출 가능성

4. **디버깅 정보 부족**:
   - 기존 로그가 충분하지 않아 정확한 원인 추적 어려움

## 수정 내용

### 1. `GeofenceTransitionsReceiver` 개선 (`receiver/GeofenceTransitionsReceiver.kt`)

- **CoroutineExceptionHandler 추가**: 예외가 scope 전체를 크래시시키지 않도록 함
- **Throwable 예외 처리 추가**: `Exception` 외에 `Error`도 catch하여 로깅
- **상세 디버깅 로그 추가**: `[GEOFENCE_ENTER]`, `[GEOFENCE_EXIT]` 접두사로 추적 용이
- **처리 시간 측정**: 코루틴 시작/완료 시간 로깅

```kotlin
// 변경 전
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// 변경 후
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
    Log.e(TAG, "❌ Uncaught exception in GeofenceTransitionsReceiver scope: ${throwable.message}", throwable)
})
```

### 2. `ScheduleGroupManager` 개선 (`core/manager/ScheduleGroupManager.kt`)

- **GlobalScope 제거**: 구조화된 `managerScope` 사용으로 대체
- **예외 처리 강화**: `activateGroup` 호출 시 try-catch로 감싸고 상세 로깅
- **`[AUTO_MODE]` 접두사 로그 추가**: 자동 모드 동작 추적 용이

```kotlin
// 변경 전
kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
    activateGroup(groupId, updateGeofences = false)
}

// 변경 후
managerScope.launch {
    try {
        Log.d(TAG, "🚀 [AUTO_MODE] Calling activateGroup for: $groupId")
        activateGroup(groupId, updateGeofences = false)
        Log.i(TAG, "✅ [AUTO_MODE] activateGroup completed for: $groupId")
    } catch (e: Exception) {
        Log.e(TAG, "❌ [AUTO_MODE] Failed to activate group: ${e.message}", e)
    } catch (t: Throwable) {
        Log.e(TAG, "❌ [AUTO_MODE] CRITICAL: Throwable caught: ${t.message}", t)
    }
}
```

### 3. `FocusAccessibilityService` 개선 (`service/accessibility/FocusAccessibilityService.kt`)

- **상세 라이프사이클 로깅 추가**: `[CONNECT]`, `[INTERRUPT]`, `[UNBIND]` 접두사로 추적 용이
- **스레드 정보 포함**: `Thread.currentThread().name` 로깅
- **상태 정보 포함**: 각 라이프사이클 시점의 `isTimerRunning`, `sessionId` 등 상태 로깅
- **스택 트레이스 로깅**: 예외 발생 시 `stackTraceToString()` 호출

```kotlin
// onServiceConnected 예시
Log.i(TAG, "🔄 [CONNECT] AccessibilityService connecting... (Thread: ${Thread.currentThread().name})")
Log.d(TAG, "🔄 [CONNECT] Current state: isTimerRunning=$isTimerRunning, categories=${enabledCategories.size}")
```

## 검증 결과

### 빌드 검증
```bash
./gradlew compileDebugKotlin
```
- **결과**: `BUILD SUCCESSFUL`

### 예상 효과
1. **충돌 방지**: `SupervisorJob` + `CoroutineExceptionHandler`로 자식 코루틴 예외가 다른 코루틴에 영향을 주지 않음
2. **원인 추적 용이**: 상세 로그로 향후 유사한 문제 발생 시 정확한 원인 파악 가능
3. **안정성 향상**: `GlobalScope` 제거로 구조화된 동시성 보장

## 향후 모니터링

문제가 다시 발생할 경우 다음 로그를 확인하세요:

```bash
adb logcat | grep -E "GEOFENCE_ENTER|GEOFENCE_EXIT|AUTO_MODE|CONNECT|INTERRUPT|UNBIND|FocusAccessibilityService|GeofenceTransitionsReceiver|ScheduleGroupManager"
```

**주요 확인 포인트**:
- `❌ [GEOFENCE_ENTER] CRITICAL: Throwable caught` - Geofence 처리 중 심각한 오류
- `❌ [AUTO_MODE] CRITICAL: Throwable caught` - 자동 모드 활성화 중 심각한 오류
- `❌ [CONNECT] CRITICAL: onServiceConnected failed` - 접근성 서비스 연결 실패
- `⚠️ [INTERRUPT] AccessibilityService interrupted` - 접근성 서비스 인터럽트 발생
- `Crashed services:{{com.allday.detoxy...}}` - 접근성 서비스가 여전히 크래시 상태

## 변경된 파일

1. `app/src/main/java/com/allday/detoxy/receiver/GeofenceTransitionsReceiver.kt`
2. `app/src/main/java/com/allday/detoxy/core/manager/ScheduleGroupManager.kt`
3. `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`
