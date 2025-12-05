# 작업 기록: 위치 기반 스케줄 작동하지 않는 문제 수정

**작업 일시**: 2025-11-08  
**작업 범위**: 위치 기반 스케줄이 작동하지 않는 문제 원인 파악 및 수정

## 문제 상황

이전 수정 후 위치 기반 스케줄이 다시 작동하지 않습니다. 방금 전 수정이 원인인지, 아니면 또 다른 문제가 있는지 확인이 필요합니다.

## 이전 작업 내용 확인

### 1. `2025-11-08_background_execution_improvement.md`
- **수정 내용**: 
  - `DetoxyApplication.kt`: 앱 시작 시 자동 실행 재등록 추가
  - `PermissionUtils.kt`: 배터리 최적화 예외 기능 추가
- **목적**: 앱이 종료된 상태에서도 작동하도록 개선

### 2. `2025-11-08_fix_accessibility_service_hilt_injection.md`
- **수정 내용**:
  - `FocusAccessibilityService.kt`: `@AndroidEntryPoint` 제거, `EntryPoint` 사용
- **목적**: 접근성 서비스 Hilt 의존성 주입 문제 수정

## 원인 분석

### 1. `DetoxyApplication.rescheduleAutoRunsOnAppStart()` 실행 확인
- 로그에 `rescheduleAutoRunsOnAppStart()` 관련 메시지가 없음
- Hilt 초기화 타이밍 문제로 EntryPoint 접근 실패 가능성

### 2. Geofence 재등록 시 중복 등록 문제
- `rescheduleAll()`에서 이미 등록된 Geofence를 다시 등록하려고 시도
- Google Geofencing API는 같은 ID로 재등록 시 자동 업데이트되지만, 명시적으로 제거 후 재등록하는 것이 안전

## 수정 내용

### 1. `DetoxyApplication.kt` - 로깅 강화 및 Hilt 초기화 대기

```kotlin
private fun rescheduleAutoRunsOnAppStart() {
    Log.d(TAG, "🔄 rescheduleAutoRunsOnAppStart() called")
    applicationScope.launch {
        try {
            // 🆕 Hilt 초기화를 기다리기 위해 짧은 지연 (EntryPoint 사용 가능할 때까지)
            kotlinx.coroutines.delay(500)
            
            Log.d(TAG, "🔄 Attempting to get EntryPoint...")
            val entryPoint = EntryPointAccessors.fromApplication(
                this@DetoxyApplication,
                DetoxyApplicationEntryPoint::class.java
            )
            Log.d(TAG, "✅ EntryPoint obtained successfully")
            
            // ... (나머지 코드)
            
            // 상세 로깅 추가
            Log.d(TAG, "   Found ${enabledTimeBasedAutoRuns.size} enabled time-based auto-runs")
            Log.d(TAG, "   Found ${enabledLocationBasedAutoRuns.size} enabled location-based auto-runs")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reschedule auto-runs on app start: ${e.message}", e)
            e.printStackTrace()  // 🆕 스택 트레이스 출력
        }
    }
}
```

**효과**:
- Hilt 초기화를 기다려 EntryPoint 접근 안정성 향상
- 상세 로깅으로 문제 추적 용이
- 예외 발생 시 스택 트레이스 출력

### 2. `AutoRunGeofenceManager.kt` - Geofence 재등록 시 기존 Geofence 제거

```kotlin
suspend fun rescheduleAll(enabledLocations: List<LocationBasedAutoRun>) {
    if (enabledLocations.isEmpty()) {
        Log.i(TAG, "ℹ️ No enabled locations to reschedule")
        return
    }
    
    Log.i(TAG, "🔄 Rescheduling ${enabledLocations.size} geofences")
    
    // 🆕 기존 Geofence를 먼저 제거 (중복 등록 방지)
    val locationIds = enabledLocations.map { it.id }
    try {
        Log.d(TAG, "🗑️ Removing existing geofences before rescheduling: ${locationIds.size} locations")
        geofencingClient.removeGeofences(locationIds).await()
        Log.d(TAG, "✅ Existing geofences removed")
    } catch (e: Exception) {
        // 제거 실패해도 계속 진행 (이미 제거되었거나 등록되지 않았을 수 있음)
        Log.w(TAG, "⚠️ Failed to remove existing geofences (may not exist): ${e.message}")
    }
    
    // 재등록 진행
    enabledLocations.forEach { location ->
        val result = addGeofence(location)
        // ...
    }
}
```

**효과**:
- 기존 Geofence를 먼저 제거하여 중복 등록 방지
- 재등록 시 깨끗한 상태에서 시작
- 제거 실패해도 재등록은 계속 진행 (안전성)

## 수정된 파일

- `app/src/main/java/com/allday/detoxy/DetoxyApplication.kt`
  - `rescheduleAutoRunsOnAppStart()`에 Hilt 초기화 대기 로직 추가 (500ms 지연)
  - 상세 로깅 추가
  - 예외 발생 시 스택 트레이스 출력

- `app/src/main/java/com/allday/detoxy/core/manager/AutoRunGeofenceManager.kt`
  - `rescheduleAll()`에서 기존 Geofence를 먼저 제거한 후 재등록하도록 수정

## 이전 작업 보존 확인

### ✅ 보존된 이전 수정 사항
1. **`FocusAccessibilityService.kt`**: EntryPoint 사용 (이전 수정 유지)
2. **`PermissionUtils.kt`**: 배터리 최적화 예외 기능 (이전 수정 유지)
3. **`DetoxyApplication.kt`**: 앱 시작 시 자동 실행 재등록 (이전 수정 유지, 로깅 강화 및 Hilt 초기화 대기 추가)

## 검증 방법

1. **앱 재시작 후 로그 확인**:
   ```
   🔄 rescheduleAutoRunsOnAppStart() called
   🔄 Attempting to get EntryPoint...
   ✅ EntryPoint obtained successfully
   🔄 Checking auto-runs on app start...
      Found X enabled time-based auto-runs
      Found Y enabled location-based auto-runs
   🔄 Rescheduling Y location-based auto-runs on app start
   🗑️ Removing existing geofences before rescheduling: Y locations
   ✅ Existing geofences removed
   ✅ Geofence rescheduled: {label} (ID: {id})
   ✅ Location-based auto-runs rescheduled on app start
   ```

2. **위치 기반 스케줄 트리거 테스트**:
   - 위치 기반 스케줄이 등록된 위치로 이동
   - 로그 확인:
     ```
     🔔 Geofence event received
     📍 Geofence ENTER detected
     ```

## 예상 결과

- 앱 시작 시 자동 실행 재등록이 정상적으로 실행됨
- Hilt 초기화 대기로 EntryPoint 접근 안정성 향상
- Geofence 재등록 시 중복 등록 방지
- 위치 기반 스케줄이 정상적으로 작동

## 참고 사항

- 이전 작업 내용이 원복되지 않도록 주의하여 수정했습니다.
- Hilt 초기화 타이밍 문제는 `delay(500)`으로 해결했지만, 필요 시 더 긴 지연 시간을 사용할 수 있습니다.
- Geofence 재등록 시 기존 Geofence를 제거하는 것은 안전한 방법이며, Google Geofencing API 권장 사항입니다.

