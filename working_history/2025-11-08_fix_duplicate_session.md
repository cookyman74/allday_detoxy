# 작업 기록: 위치 기반 스케줄 실행 시 세션 중복 저장 문제 수정

**작업 일시**: 2025-11-08  
**작업 범위**: 위치 기반 스케줄 실행 시 성공/실패 세션이 중복으로 저장되는 문제 수정

## 문제 상황

사용자가 위치 기반 스케줄을 실행한 후 통계 정보를 확인했을 때, **동일한 타이머 세션이 성공과 실패로 중복 저장**되는 문제가 발생했습니다.

### 증상
- 하나의 타이머 세션에 대해 2개의 `FocusSession` 레코드가 생성됨
  - Session A: `success = false` (실패)
  - Session B: `success = true` (성공)
- 통계 리포트에서 실패 세션과 성공 세션이 모두 표시됨

## 원인 분석

### 1. 코드 분석

`AutoStartTimerWorker.kt`에서 타이머를 시작할 때마다 **무조건 새로운 세션을 생성**하고 있었습니다:

```kotlin
// AutoStartTimerWorker.kt:75-85 (수정 전)
val sessionId = UUID.randomUUID().toString()  // ← 매번 새로운 ID 생성
focusRepository.startSession(
    FocusSession(
        id = sessionId,
        startTime = System.currentTimeMillis(),
        endTime = null,
        durationMinutes = durationMinutes,
        success = false  // ← 초기값: 실패
    )
)
```

### 2. 중복 발생 시나리오

위치 정보를 수정하면 Geofence가 재등록되면서 Worker가 중복 실행됩니다:

```
1. 위치 진입 
   → Geofence 트리거 
   → AutoStartTimerWorker 실행 
   → Session A 생성 (success=false)
   → 타이머 시작

2. 위치 정보 수정 (예: 반경 변경)
   → Geofence 재등록 및 재트리거
   → AutoStartTimerWorker 다시 실행
   → Session B 생성 (success=false)  ❌ 중복!
   → 타이머는 이미 실행 중이므로 무시됨

3. 타이머 정상 완료
   → onTimerFinish(success=true) 호출
   → Session B만 업데이트 (success=true)

4. 결과
   - Session A: success=false, endTime=null (실패로 남음 ❌)
   - Session B: success=true, endTime=[완료시각] (정상 완료 ✅)
```

### 3. 근본 원인

- `AutoStartTimerWorker`가 **타이머 실행 상태를 확인하지 않고** 무조건 새 세션을 생성
- 위치 정보 수정, Geofence 재등록 등의 이벤트로 Worker가 중복 실행될 때 방어 로직 부재

## 수정 내용

### `AutoStartTimerWorker.kt` 수정

**Before:**
```kotlin
// 항상 새로운 세션 생성
val sessionId = UUID.randomUUID().toString()
focusRepository.startSession(
    FocusSession(
        id = sessionId,
        startTime = System.currentTimeMillis(),
        endTime = null,
        durationMinutes = durationMinutes,
        success = false
    )
)
```

**After:**
```kotlin
// 🆕 이미 타이머가 실행 중인지 확인 (중복 세션 생성 방지)
val isTimerRunning = FocusTimerService.isTimerRunning.value
val existingSessionId = FocusTimerService.currentSessionId.value

val sessionId: String
if (isTimerRunning && existingSessionId != null) {
    // 이미 타이머 실행 중 → 기존 세션 재사용
    sessionId = existingSessionId
    Log.d(TAG, "⚠️ Timer already running, reusing existing sessionId: $sessionId")
} else {
    // 새로운 타이머 시작 → 새 세션 생성
    sessionId = UUID.randomUUID().toString()
    try {
        focusRepository.startSession(
            FocusSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                endTime = null,
                durationMinutes = durationMinutes,
                success = false
            )
        )
        Log.d(TAG, "✅ FocusSession created: sessionId=$sessionId, duration=$durationMinutes")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to create FocusSession: ${e.message}", e)
    }
}
```

### 수정 효과

1. **중복 세션 생성 방지**: 타이머가 이미 실행 중이면 기존 세션을 재사용
2. **통계 정확성 향상**: 하나의 타이머 실행에 대해 하나의 세션만 생성
3. **데이터 일관성 보장**: 성공/실패가 동일한 세션에 기록됨

### 수정 후 플로우

```
1. 위치 진입
   → Geofence 트리거
   → AutoStartTimerWorker 실행
   → 타이머 실행 중? No
   → Session A 생성 (success=false)
   → 타이머 시작

2. 위치 정보 수정
   → Geofence 재등록 및 재트리거
   → AutoStartTimerWorker 다시 실행
   → 타이머 실행 중? Yes ✅
   → 기존 sessionId 재사용 (Session A)
   → 타이머는 이미 실행 중이므로 무시됨

3. 타이머 정상 완료
   → onTimerFinish(success=true) 호출
   → Session A 업데이트 (success=true)

4. 결과
   - Session A: success=true, endTime=[완료시각] (정상 완료 ✅)
   - 중복 세션 없음! ✅
```

## 수정된 파일

- `app/src/main/java/com/allday/detoxy/worker/AutoStartTimerWorker.kt`
  - 타이머 실행 상태 확인 로직 추가
  - 이미 실행 중이면 기존 세션 재사용
  - 새로 시작할 때만 새 세션 생성

## 검증 방법

### 1. 위치 정보 수정 시나리오
```bash
# 1. 위치 기반 스케줄 생성
# 2. 해당 위치로 이동 (타이머 시작)
# 3. 타이머 실행 중 위치 정보 수정 (반경 변경)
# 4. 타이머 정상 완료
# 5. DB 확인: FocusSession 1개만 존재, success=true
```

### 2. 로그 확인
```bash
adb logcat -s "AutoStartTimerWorker:*" | grep -E "Timer already running|FocusSession created"
```

**예상 로그:**
```
AutoStartTimerWorker: ✅ FocusSession created: sessionId=xxx, duration=30
AutoStartTimerWorker: ⚠️ Timer already running, reusing existing sessionId: xxx
```

### 3. 통계 확인
- 통계 화면에서 성공 세션만 1개 표시
- 실패 세션 없음

## 관련 이슈

- 이전 수정: `2025-11-06_fix_session_success_flag.md` - 세션 성공 플래그 수정
- 이전 수정: `2025-11-08_background_execution_improvement.md` - 백그라운드 실행 개선
- 이전 수정: `2025-11-08_fix_accessibility_service_hilt_injection.md` - AccessibilityService Hilt 주입 수정

## 주의사항

이 수정은 `AutoStartTimerWorker`가 중복 실행될 때 **기존 세션을 재사용**하도록 하므로, 다음과 같은 경우에도 영향을 미칩니다:

1. **스누즈 기능**: 스누즈로 재실행될 때도 기존 세션 재사용
   - 현재는 스누즈 시 `isSnooze=true` 플래그로 구분되므로 문제없음

2. **알림 클릭 재실행**: 사용자가 알림을 여러 번 클릭할 때도 기존 세션 재사용
   - 의도된 동작: 같은 타이머 세션으로 처리

3. **시간표 겹침**: 여러 시간표가 동시에 트리거될 때
   - 첫 번째 시간표의 타이머가 실행 중이면 나머지는 무시됨
   - 의도된 동작: 한 번에 하나의 타이머만 실행

## 후속 작업 (선택사항)

1. **통계 정정**: 기존에 중복 저장된 세션 데이터 정리
   - DB에서 `success=false`이고 `endTime=null`인 오래된 세션 삭제
   
2. **모니터링 강화**: 세션 중복 생성 감지 로그 추가
   - 개발 단계에서 조기 발견

3. **단위 테스트 추가**: Worker 중복 실행 시나리오 테스트
   - `AutoStartTimerWorkerTest.kt` 작성

## 커밋 메시지

```
fix(worker): prevent duplicate FocusSession creation in AutoStartTimerWorker

위치 기반 스케줄 실행 시 타이머가 이미 실행 중이면 기존 세션을 재사용하도록 수정.
위치 정보 수정으로 인한 Geofence 재트리거 시 중복 세션 생성 방지.

- Check if timer is already running before creating new session
- Reuse existing sessionId if timer is running
- Prevents duplicate success/failed sessions in statistics

Closes: #[이슈번호]
Related: 2025-11-06_fix_session_success_flag.md
```

