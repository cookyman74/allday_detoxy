# 작업 기록: 앱 중단 상태에서도 작동하도록 개선

**작업 일시**: 2025-11-08  
**작업 범위**: 앱이 종료된 상태에서도 자동 실행이 정상 작동하도록 개선

## 문제 상황

앱이 완전히 종료되거나 프로세스가 kill된 상태에서도 위치 기반 스케줄과 시간 기반 자동 실행이 정상적으로 작동해야 합니다.

## 현재 상태 분석

### 이미 구현된 기능
1. **BootCompletedReceiver**: 기기 재시작 시 자동 실행 재등록 ✅
2. **Geofence**: 시스템 레벨에서 작동하므로 앱 종료 후에도 작동 ✅
3. **AlarmManager**: 시스템 레벨에서 작동하므로 앱 종료 후에도 작동 ✅
4. **WorkManager**: 백그라운드에서 작동 ✅

### 부족한 부분
1. **앱 시작 시 자동 실행 재등록**: 앱이 종료된 후 다시 시작될 때 알람/Geofence 손실 가능
2. **배터리 최적화 예외 요청**: 배터리 최적화로 인한 백그라운드 실행 제한 대응
3. **백그라운드 제한 대응**: Doze 모드, App Standby 대응

## 수정 내용

### 1. 배터리 최적화 예외 기능 추가 (`PermissionUtils.kt`)

```kotlin
// ==================== 배터리 최적화 예외 (Battery Optimization) ====================

/**
 * 배터리 최적화 예외 여부 확인
 *
 * 앱이 배터리 최적화에서 제외되어 있는지 확인합니다.
 * 배터리 최적화가 활성화되어 있으면 백그라운드 실행이 제한될 수 있습니다.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else {
        true  // Android 6.0 미만에서는 배터리 최적화 기능이 없음
    }
}

/**
 * 배터리 최적화 예외 요청 화면 열기
 *
 * 사용자가 배터리 최적화에서 앱을 제외할 수 있는 설정 화면으로 이동합니다.
 */
fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings", e)
            openAppDetailsSettings(context)  // 대체 방법
        }
    }
}

/**
 * 앱 상세 설정 화면 열기
 */
fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open app details settings", e)
    }
}
```

**효과**: 배터리 최적화로 인한 백그라운드 실행 제한을 사용자가 직접 해제할 수 있습니다.

### 2. 앱 시작 시 자동 실행 재등록 (`DetoxyApplication.kt`)

```kotlin
/**
 * 앱 시작 시 자동 실행 재등록
 *
 * 앱이 종료된 후 다시 시작될 때 자동 실행(알람, Geofence)을 재등록합니다.
 * 배터리 최적화나 시스템에 의한 프로세스 종료로 인한 알람/Geofence 손실을 방지합니다.
 */
private fun rescheduleAutoRunsOnAppStart() {
    applicationScope.launch {
        try {
            // EntryPoint를 통해 의존성 가져오기
            val entryPoint = EntryPointAccessors.fromApplication(
                this@DetoxyApplication,
                DetoxyApplicationEntryPoint::class.java
            )
            val alarmManager = entryPoint.alarmManager()
            val geofenceManager = entryPoint.geofenceManager()
            val timeBasedAutoRunDao = entryPoint.timeBasedAutoRunDao()
            val locationBasedAutoRunDao = entryPoint.locationBasedAutoRunDao()
            
            // 1. 시간 기반 자동 실행 재등록
            val enabledTimeBasedAutoRuns = timeBasedAutoRunDao.getAllEnabled()
            if (enabledTimeBasedAutoRuns.isNotEmpty()) {
                alarmManager.rescheduleAll(enabledTimeBasedAutoRuns)
                Log.i(TAG, "✅ Time-based auto-runs rescheduled on app start")
            }
            
            // 2. 위치 기반 자동 실행 재등록
            val enabledLocationBasedAutoRuns = locationBasedAutoRunDao.getAllEnabled()
            if (enabledLocationBasedAutoRuns.isNotEmpty()) {
                geofenceManager.rescheduleAll(enabledLocationBasedAutoRuns)
                Log.i(TAG, "✅ Location-based auto-runs rescheduled on app start")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reschedule auto-runs on app start: ${e.message}", e)
        }
    }
}
```

**효과**: 
- 앱이 종료된 후 다시 시작될 때 자동으로 알람과 Geofence를 재등록
- 배터리 최적화나 시스템에 의한 프로세스 종료로 인한 알람/Geofence 손실 방지
- 사용자가 앱을 다시 열 때마다 자동 실행 상태 복구

## 수정된 파일

- `app/src/main/java/com/allday/detoxy/core/utils/PermissionUtils.kt`
  - 배터리 최적화 예외 확인 함수 추가
  - 배터리 최적화 예외 요청 함수 추가
  - 앱 상세 설정 화면 열기 함수 추가

- `app/src/main/java/com/allday/detoxy/DetoxyApplication.kt`
  - `DetoxyApplicationEntryPoint` 인터페이스 추가
  - `rescheduleAutoRunsOnAppStart()` 메서드 추가
  - `onCreate()`에서 자동 실행 재등록 호출

## 작동 방식

### 1. 앱 시작 시 자동 재등록
```
앱 시작 (DetoxyApplication.onCreate())
  ↓
rescheduleAutoRunsOnAppStart()
  ↓
시간 기반 자동 실행 재등록 (AlarmManager)
위치 기반 자동 실행 재등록 (Geofence)
  ↓
✅ 자동 실행 상태 복구 완료
```

### 2. 기기 재시작 시 자동 재등록
```
기기 재시작
  ↓
BootCompletedReceiver.onReceive()
  ↓
시간 기반 자동 실행 재등록
위치 기반 자동 실행 재등록
  ↓
✅ 자동 실행 상태 복구 완료
```

### 3. 배터리 최적화 대응
```
사용자가 배터리 최적화 예외 요청
  ↓
PermissionUtils.requestBatteryOptimizationExemption()
  ↓
설정 화면에서 "제외" 선택
  ↓
✅ 백그라운드 실행 제한 해제
```

## 검증 방법

1. **앱 시작 시 재등록 확인**:
   ```
   앱 완전 종료 → 앱 재시작
   로그 확인:
   🔄 Checking auto-runs on app start...
   🔄 Rescheduling X time-based auto-runs on app start
   ✅ Time-based auto-runs rescheduled on app start
   ✅ Location-based auto-runs rescheduled on app start
   ```

2. **기기 재시작 시 재등록 확인**:
   ```
   기기 재시작
   로그 확인:
   📱 Device booted, rescheduling all auto-run alarms and geofences
   ✅ Time-based auto-run alarms rescheduled successfully
   ✅ Location-based auto-run geofences rescheduled successfully
   ```

3. **배터리 최적화 예외 확인**:
   ```kotlin
   val isExempt = PermissionUtils.isIgnoringBatteryOptimizations(context)
   // true: 배터리 최적화 예외됨
   // false: 배터리 최적화 적용됨 (요청 필요)
   ```

## 예상 결과

- 앱이 종료된 후 다시 시작될 때 자동으로 알람과 Geofence 재등록
- 배터리 최적화로 인한 알람/Geofence 손실 방지
- 기기 재시작 시에도 자동 실행 정상 작동
- 사용자가 배터리 최적화 예외를 설정할 수 있음

## 추가 고려사항

### 향후 개선 가능한 부분
1. **UI에서 배터리 최적화 예외 안내**: 설정 화면에서 배터리 최적화 예외 상태를 확인하고 요청할 수 있는 UI 추가
2. **자동 실행 실패 감지**: 알람/Geofence가 제대로 등록되지 않았을 때 사용자에게 알림
3. **WorkManager 백업**: AlarmManager 실패 시 WorkManager를 백업으로 사용

### 현재 작동 방식
- **시간 기반 자동 실행**: AlarmManager 사용 (시스템 레벨, 앱 종료 후에도 작동)
- **위치 기반 자동 실행**: Geofence API 사용 (시스템 레벨, 앱 종료 후에도 작동)
- **타이머 실행**: Foreground Service 사용 (앱 종료 후에도 작동)
- **알림**: WorkManager 사용 (백그라운드 실행 보장)

## 참고 사항

- `BootCompletedReceiver`는 이미 구현되어 있어 기기 재시작 시 자동 실행이 재등록됩니다.
- 이번 수정으로 앱이 종료된 후 다시 시작될 때도 자동 실행이 재등록됩니다.
- 배터리 최적화 예외는 사용자가 직접 설정해야 하며, UI에서 안내할 수 있습니다.

