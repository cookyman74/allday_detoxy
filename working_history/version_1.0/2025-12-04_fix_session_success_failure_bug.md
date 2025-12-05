# 작업 기록: 집중모드 성공/실패 데이터 저장 버그 수정

**작업 일시**: 2025-12-04  
**작업 범위**: 스케줄에 따라 실행된 집중모드가 성공했음에도 실패로 저장되는 버그 수정 및 접근성 서비스 오류 해결

## 문제 상황

### 증상
1. 스케줄에 따라 실행된 집중모드가 성공했음에도 불구하고 **실패로 데이터가 저장**됨
2. 이로 인해 **접근성 서비스 오류가 발생**하여 스크린 필터링이 작동하지 않음

### 버그 리스트
- [2025-12-04] 스케쥴에 따라 실행된 집중모드가 실패로 잡힘, 실제로는 성공하였음
- 이에 따라 접근성 오류가 발생된 것으로 추정

## 원인 분석

### 1. 정상 완료 시 `stopTimerInternal` 미호출

**문제점**:
- `FocusTimerService`에서 타이머가 정상 완료될 때 (라인 383-401):
  - `_state.value = FocusState.FINISHED` 설정
  - `sendTimerFinishedBroadcast(success = true)` 호출
  - `delay(500)` 후 `stopSelf()` 호출
  - **하지만 `stopTimerInternal`을 호출하지 않음!**

**영향**:
- `FocusAccessibilityService.isTimerRunning`이 여전히 `true`로 남아있음
- 접근성 서비스가 계속 작동하여 스크린 필터링이 계속됨
- `_currentSessionId.value = null`을 설정하지 않아서, 나중에 `onTimerFinish`가 호출될 때 세션 ID가 null이 될 수 있음

### 2. 세션 ID null 처리 부족

**문제점**:
- `stopTimerInternal`에서 `_currentSessionId.value = null`을 설정하는데, 이게 `onTimerFinish`가 호출되기 전에 실행되면 `sessionIdToUse`가 null이 될 수 있음
- `TimerViewModel`에서 StateFlow를 관찰하는 방식이므로, StateFlow가 업데이트되기 전에 세션 ID가 null이 될 수 있음

**영향**:
- `onTimerFinish`에서 세션 ID가 null이 되어 세션 종료가 실패함
- 결과적으로 세션이 `success = false` (초기값)로 남아있음

## 수정 사항

### 1. 정상 완료 시 `stopTimerInternal` 호출 추가

**파일**: `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`

**수정 전**:
```kotlin
// 정상 완료
if (_state.value == FocusState.RUNNING && _remainingSeconds.value == 0) {
    Log.d(TAG, "Timer finished successfully")
    _state.value = FocusState.FINISHED
    
    // ... 성공 피드백 처리 ...
    
    sendTimerFinishedBroadcast(success = true)
    
    delay(500)
    stopSelf()
}
```

**수정 후**:
```kotlin
// 정상 완료
if (_state.value == FocusState.RUNNING && _remainingSeconds.value == 0) {
    Log.d(TAG, "Timer finished successfully")
    
    // ... 성공 피드백 처리 ...
    
    // 🔥 버그 수정: 정상 완료 시에도 stopTimerInternal 호출
    // - 접근성 서비스 비활성화
    // - 세션 ID 유지 (onTimerFinish에서 사용하기 위해)
    // - 브로드캐스트 전송 및 서비스 종료
    stopTimerInternal(success = true)
}
```

**효과**:
- 정상 완료 시에도 접근성 서비스가 비활성화됨
- 세션 ID가 유지되어 `onTimerFinish`에서 사용 가능
- 브로드캐스트 전송 및 서비스 종료가 일관되게 처리됨

### 2. 세션 ID null 처리 개선

**파일**: `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`

**수정 전**:
```kotlin
// 타이머 완료/포기 브로드캐스트
if (previousState == FocusState.RUNNING) {
    sendTimerFinishedBroadcast(success)
}

// 상태 초기화
_remainingSeconds.value = 0
_currentSessionId.value = null  // ← 즉시 null로 설정
_currentAutoRunId.value = null
_currentScheduleGroupId.value = null

serviceScope?.launch {
    delay(500)
    stopSelf()
}
```

**수정 후**:
```kotlin
// 타이머 완료/포기 브로드캐스트
if (previousState == FocusState.RUNNING) {
    sendTimerFinishedBroadcast(success)
}

// 🔥 버그 수정: StateFlow가 업데이트되고 onTimerFinish가 호출될 시간을 주기 위해
// 세션 ID를 null로 설정하기 전에 지연
// 상태 초기화 (세션 ID는 나중에 null로 설정)
_remainingSeconds.value = 0
_currentAutoRunId.value = null
_currentScheduleGroupId.value = null

// 브로드캐스트가 전달되고 StateFlow가 업데이트될 시간을 주기 위해 지연 후 세션 ID 초기화 및 Service 종료
serviceScope?.launch {
    delay(1000) // StateFlow 업데이트 및 onTimerFinish 호출 대기 시간 증가
    _currentSessionId.value = null
    stopSelf()
}
```

**효과**:
- StateFlow가 업데이트되고 `onTimerFinish`가 호출될 시간을 확보
- 세션 ID가 null로 설정되기 전에 `onTimerFinish`에서 사용 가능
- 세션 종료가 정상적으로 처리됨

### 3. `onTimerFinish`에서 세션 ID null 처리 fallback 추가

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`

**수정 내용**:
- 세션 ID가 null인 경우 최근 5분 내에 시작된 세션 중 `endTime`이 null인 세션을 찾아서 처리하는 fallback 로직 추가

**효과**:
- 세션 ID가 null인 경우에도 세션 종료가 가능
- 데이터 일관성 유지

## 검증 방법

### 1. 수동 테스트

1. **스케줄 기반 자동 실행 테스트**:
   - 시간 기반 또는 위치 기반 스케줄 설정
   - 스케줄에 따라 타이머 자동 시작
   - 타이머 정상 완료 대기
   - 리포트 화면에서 세션이 **성공**으로 표시되는지 확인

2. **접근성 서비스 상태 확인**:
   - 타이머 완료 후 접근성 서비스가 비활성화되는지 확인
   - 차단된 앱 실행 시 스크린 필터링이 작동하지 않는지 확인

3. **로그 확인**:
   ```bash
   adb logcat | grep -E "(TimerViewModel|FocusTimerService|Session ended)"
   ```
   - `✅ Session ended: sessionId=xxx, success=true` 로그 확인
   - `✅ AccessibilityService deactivated` 로그 확인

### 2. 데이터베이스 확인

```sql
SELECT id, startTime, endTime, durationMinutes, success 
FROM focus_sessions 
ORDER BY startTime DESC 
LIMIT 10;
```

- 최근 세션의 `success` 값이 정확한지 확인
- `endTime`이 null이 아닌지 확인

## 관련 파일

- `app/src/main/java/com/allday/detoxy/service/timer/FocusTimerService.kt`
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`
- `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`

## 참고 사항

- 이전에도 유사한 문제가 있었음: `working_history/2025-11-08_fix_duplicate_session.md`
- StateFlow 기반 상태 관리를 사용하므로, StateFlow 업데이트 타이밍을 고려해야 함
- 접근성 서비스는 시스템이 직접 인스턴스를 생성하므로, 상태 관리에 주의 필요

---

**작업 완료일**: 2025-12-04  
**작업자**: AI Assistant

