# 작업 기록: 정확 알람 권한 설정 화면 갱신 버그 수정

**작업 일시**: 2025-12-04  
**작업 범위**: 권한 설정 화면에서 돌아올 때 권한 상태가 갱신되지 않는 버그 수정

## 문제 상황

### 증상
1. 권한 설정을 완료하고 앱으로 돌아와도 화면이 갱신되지 않음
2. 권한 설정이 필요하다는 문구가 그대로 남아있음
3. 앱을 재실행하면 문구가 사라짐
4. 사용자에게 혼란을 줄 수 있음

### 버그 리스트
- [2025-12-04] 알림 설정 권한 설정하기 오류: 설정을 완료하고 다시 앱으로 돌아와도 업데이트가 화면이 갱신이 안되어 있다.

## 원인 분석

### 문제점
- `TimeBasedAutoRunScreen`에서 `canScheduleExactAlarms`를 StateFlow로 관찰하고 있음
- 하지만 앱이 백그라운드에서 포그라운드로 돌아올 때 권한 상태를 다시 확인하는 로직이 없음
- `AutoRunAlarmManager`의 `canScheduleExactAlarms()` 메서드는 권한 상태를 확인하고 StateFlow를 업데이트하지만, 화면이 다시 활성화될 때 자동으로 호출되지 않음

### 비교: 다른 권한 화면
- `PermissionCheckScreen`에서는 `DisposableEffect`와 `LifecycleEventObserver`를 사용하여 `ON_RESUME` 이벤트에서 권한 상태를 업데이트하는 패턴을 사용하고 있음
- 같은 패턴을 `TimeBasedAutoRunScreen`에도 적용해야 함

## 수정 사항

### 1. TimeBasedAutoRunScreen에 생명주기 관찰 추가

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/TimeBasedAutoRunScreen.kt`

**추가된 import**:
```kotlin
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
```

**추가된 코드**:
```kotlin
// 🔥 버그 수정: 화면 재진입 시 정확 알람 권한 상태 갱신
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            // 권한 설정 화면에서 돌아올 때 권한 상태 다시 확인
            viewModel.refreshExactAlarmPermission()
        }
    }
    
    lifecycleOwner.lifecycle.addObserver(observer)
    
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

**효과**:
- 화면이 `ON_RESUME` 상태가 될 때마다 권한 상태를 다시 확인
- 권한 설정 화면에서 돌아올 때 자동으로 권한 상태 갱신
- StateFlow가 업데이트되어 UI가 자동으로 갱신됨

### 2. TimeBasedAutoRunViewModel에 권한 상태 갱신 메서드 추가

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimeBasedAutoRunViewModel.kt`

**추가된 메서드**:
```kotlin
/**
 * 정확 알람 권한 상태 갱신
 * 
 * 화면이 포그라운드로 돌아올 때 권한 상태를 다시 확인합니다.
 * 권한 설정 화면에서 돌아온 경우 권한 상태가 변경되었을 수 있으므로
 * 이 메서드를 호출하여 StateFlow를 업데이트합니다.
 */
fun refreshExactAlarmPermission() {
    alarmManager.canScheduleExactAlarms()
}
```

**효과**:
- `AutoRunAlarmManager.canScheduleExactAlarms()`를 호출하여 권한 상태를 다시 확인
- 내부적으로 StateFlow(`_canScheduleExactAlarms`)가 업데이트됨
- UI가 자동으로 갱신됨

## 검증 방법

### 1. 수동 테스트

1. **권한 미설정 상태에서 테스트**:
   - 앱 실행
   - "예약설정" 화면으로 이동
   - "정확한 실행을 위해 권한을 허용하세요" 배너 확인
   - "권한 설정하기" 버튼 클릭
   - 설정 화면에서 권한 허용
   - 앱으로 돌아오기
   - **배너가 사라지는지 확인** ✅

2. **권한 설정 상태에서 테스트**:
   - 권한이 이미 설정된 상태
   - "예약설정" 화면에서 배너가 표시되지 않는지 확인
   - 설정 화면에서 권한 해제
   - 앱으로 돌아오기
   - **배너가 다시 나타나는지 확인** ✅

### 2. 로그 확인

```bash
adb logcat | grep -E "(TimeBasedAutoRunScreen|AutoRunAlarmManager|canScheduleExactAlarms)"
```

- `ON_RESUME` 이벤트 발생 시 로그 확인
- `canScheduleExactAlarms()` 호출 확인
- StateFlow 업데이트 확인

## 관련 파일

- `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/TimeBasedAutoRunScreen.kt`
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimeBasedAutoRunViewModel.kt`
- `app/src/main/java/com/allday/detoxy/core/manager/AutoRunAlarmManager.kt`
- `app/src/main/java/com/allday/detoxy/presentation/ui/permission/PermissionCheckScreen.kt` (참고)

## 참고 사항

- `PermissionCheckScreen`에서 사용하는 동일한 패턴을 적용
- `DisposableEffect`와 `LifecycleEventObserver`를 사용하여 생명주기 이벤트 관찰
- `ON_RESUME` 이벤트에서만 권한 상태를 갱신하여 불필요한 호출 방지
- StateFlow를 사용하여 UI가 자동으로 갱신되도록 함

---

**작업 완료일**: 2025-12-04  
**작업자**: AI Assistant

