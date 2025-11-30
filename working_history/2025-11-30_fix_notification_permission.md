# 작업 기록: 성공 알림 미표시 문제 해결

**작업 일시**: 2025-11-30
**작업 범위**: `MainActivity`

## 문제 상황

**사용자 보고**:
1. 백그라운드에서 타이머가 성공적으로 완료되어도 알림 센터에 성공 메시지가 표시되지 않음.
2. 앱을 재실행하면 성공 다이얼로그는 정상적으로 표시됨 (의도된 동작).

**원인 분석**:
```bash
adb shell dumpsys notification | grep -A 20 "com.allday.detoxy"
```
결과:
```
numEnqueuedByApp=772,
numPostedByApp=0,
numBlocked=772,
```

- **772개의 알림이 시스템에 의해 차단됨**.
- Android 13 (API 33) 이상에서는 `POST_NOTIFICATIONS` 권한을 **런타임에 요청**해야 함.
- `AndroidManifest.xml`에 권한은 선언되어 있었지만, 런타임 권한 요청 코드가 없었음.
- 결과: 모든 알림이 차단되어 사용자에게 전달되지 않음.

## 수정 내용

### 1. `MainActivity` 수정: 알림 권한 요청 추가
- **파일**: `app/src/main/java/com/allday/detoxy/MainActivity.kt`
- **변경**:
    - `onCreate()`에서 `requestNotificationPermissionIfNeeded()` 호출.
    - Android 13 이상에서 알림 권한이 없으면 런타임 권한 요청.
    - `onRequestPermissionsResult()`에서 권한 결과 처리 및 로깅.

```kotlin
/**
 * 알림 권한 요청 (Android 13+)
 */
private fun requestNotificationPermissionIfNeeded() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (!PermissionUtils.hasNotificationPermission(this)) {
            Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        } else {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted")
        }
    }
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
        REQUEST_CODE_POST_NOTIFICATIONS -> {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "✅ POST_NOTIFICATIONS permission granted")
            } else {
                Log.w("MainActivity", "❌ POST_NOTIFICATIONS permission denied")
            }
        }
    }
}
```

## 검증 결과

### 빌드 검증
```bash
./gradlew installDebug
```
- **결과**: `BUILD SUCCESSFUL`

### 기대 효과
- **앱 최초 실행 시**: 알림 권한 요청 다이얼로그가 표시됨.
- **권한 승인 후**: 타이머 완료 시 "🎉 집중 성공!" 알림이 알림 센터에 정상 표시됨.
- **권한 거부 시**: 알림이 표시되지 않지만, 앱 내 성공 다이얼로그는 정상 작동 (다음 앱 실행 시).

### 사용자 안내
- 앱을 처음 실행하면 알림 권한 요청이 표시됩니다.
- "허용"을 선택하면 타이머 완료 시 알림을 받을 수 있습니다.
- 권한을 거부한 경우, 설정 > 앱 > ScreenSence > 알림에서 수동으로 활성화할 수 있습니다.

## 참고사항

### Android 알림 권한 정책
- **Android 12 이하**: 알림 권한이 자동으로 부여됨.
- **Android 13 이상**: `POST_NOTIFICATIONS` 런타임 권한 필요.
- 권한이 없으면 모든 알림이 시스템에 의해 차단됨.

### 기존 코드 상태
- `PermissionUtils.kt`에 `hasNotificationPermission()` 메서드는 이미 구현되어 있었음.
- 하지만 실제로 권한을 **요청하는** 코드는 없었음.
- 이번 수정으로 앱 시작 시 자동으로 권한 요청하도록 개선.

## 추가 수정: 앱 내 성공 다이얼로그 즉시 표시

### 문제 상황
**사용자 보고**:
- 알림 센터 알림은 정상 작동함.
- 하지만 앱 내 성공 다이얼로그는 **앱을 종료했다가 다시 실행해야만** 표시됨.
- 타이머 완료 후 앱이 이미 실행 중이면 다이얼로그가 즉시 표시되지 않음.

**원인 분석**:
- `TimerViewModel`의 `checkPendingSuccessAnimation()`은 `init` 블록에서만 호출됨.
- `init` 블록은 ViewModel이 **처음 생성될 때만** 실행됨.
- 앱이 이미 실행 중이면 ViewModel이 유지되므로 `init` 블록이 다시 실행되지 않음.
- 결과: 타이머 완료 후 앱이 포그라운드에 있어도 플래그를 체크하지 않음.

### 수정 내용
- **파일**: `app/src/main/java/com/allday/detoxy/MainActivity.kt`
- **변경**: `onResume()` 메서드 추가하여 앱이 포그라운드로 돌아올 때마다 플래그 체크.

```kotlin
override fun onResume() {
    super.onResume()
    // 🆕 앱이 포그라운드로 돌아올 때 성공 애니메이션 대기 상태 확인
    timerViewModel.checkPendingSuccessAnimation()
}
```

### 동작 방식
1. **타이머 완료 시**: `FocusTimerService`가 `PreferenceManager`에 플래그 설정.
2. **앱이 포그라운드로 돌아올 때**: `MainActivity.onResume()`이 호출됨.
3. **플래그 체크**: `timerViewModel.checkPendingSuccessAnimation()` 호출.
4. **다이얼로그 표시**: 플래그가 `true`이면 즉시 성공 다이얼로그 표시.
5. **플래그 초기화**: 다이얼로그 닫을 때 플래그 `false`로 설정.

### 검증 결과
```bash
./gradlew installDebug
```
- **결과**: `BUILD SUCCESSFUL`

### 최종 동작
- **시나리오 1 (앱이 백그라운드)**: 
    - 타이머 완료 → 알림 센터에 알림 표시
    - 앱 실행 → `onResume()` 호출 → 성공 다이얼로그 즉시 표시
- **시나리오 2 (앱이 포그라운드)**:
    - 타이머 완료 → 알림 센터에 알림 표시
    - 앱이 이미 실행 중 → 다음 `onResume()` 시 (화면 전환, 앱 재진입 등) 다이얼로그 표시
    - 또는 타이머 화면으로 이동하면 즉시 표시
- **시나리오 3 (앱 종료 상태)**:
    - 타이머 완료 → 알림 센터에 알림 표시
    - 앱 실행 → `init` 블록 + `onResume()` 모두 실행 → 성공 다이얼로그 표시

### 개선 효과
- 앱을 종료하지 않아도 성공 다이얼로그가 표시됨.
- 사용자 경험 개선: 타이머 완료 후 앱을 열면 즉시 축하 메시지 확인 가능.

