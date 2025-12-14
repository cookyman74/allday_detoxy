# 작업 기록: AutoRunAlarmReceiver 수정

**작업 일시**: 2025-12-10  
**작업 범위**: v8 manualOverrideState 확인 로직 추가 (시간 기반 알람)

## 작업 목적

시간 기반 알람 트리거 시 스케줄 그룹의 수동 제어 상태(manualOverrideState)를 확인하여 사용자 의도를 우선 존중합니다.

### 해결하고자 하는 문제
- 기존: 사용자가 UI에서 비활성화/일시중지해도 알람이 정상 트리거됨
- 개선: INACTIVE 또는 PAUSED 상태에서는 알람 건너뛰기

### 관련 PRD
- [04_버튼역할변경_prd.md](../../docs/04_버튼역할변경_prd.md)

## 변경 사항

### 1. handleAutoRunAlarm 수정

**파일**: `app/src/main/java/com/allday/detoxy/receiver/AutoRunAlarmReceiver.kt`

기존 `isActive` 체크를 v8 `manualOverrideState` 체크로 확장:

```kotlin
// v8: manualOverrideState 확인 (사용자 의도 우선)
val overrideState = scheduleGroup.manualOverrideState
val pauseUntil = scheduleGroup.pauseUntil
val currentTime = System.currentTimeMillis()

when (overrideState) {
    "INACTIVE" -> {
        // 사용자가 명시적으로 비활성화 → 알람 건너뛰기
        Log.i(TAG, "⏹️ v8: Alarm SKIPPED - ScheduleGroup manually INACTIVE")
        logAutoRunSkipped(autoRunId, "SCHEDULE_GROUP_INACTIVE", entryPoint)
        rescheduleNextAlarm(autoRunId, entryPoint)
        return@launch
    }
    "PAUSED" -> {
        if (pauseUntil != null && currentTime < pauseUntil) {
            // 아직 만료 안됨 → 알람 건너뛰기
            Log.i(TAG, "⏸️ v8: Alarm SKIPPED - ScheduleGroup PAUSED")
            logAutoRunSkipped(autoRunId, "SCHEDULE_GROUP_PAUSED", entryPoint)
            rescheduleNextAlarm(autoRunId, entryPoint)
            return@launch
        } else {
            // 만료됨 → manualOverrideState 해제 후 정상 처리
            Log.i(TAG, "✅ v8: PAUSED expired, clearing override state")
            scheduleGroupDao.updateManualOverride(scheduleGroupId, null, null)
            // 정상 처리로 진행
        }
    }
    else -> {
        // null → 자동 모드, 기존 isActive 체크 유지 (하위 호환성)
        if (!scheduleGroup.isActive) {
            logAutoRunSkipped(autoRunId, "SCHEDULE_GROUP_NOT_ACTIVE", entryPoint)
            return@launch
        }
    }
}
```

## v8 시간 기반 알람 동작 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│                     알람 트리거 처리                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  알람 트리거                                                         │
│      │                                                              │
│      ▼                                                              │
│  ┌──────────────────────────────────────────┐                       │
│  │ 1. 마스터 스위치 확인                      │                       │
│  │    (isAutoRunEnabled)                    │                       │
│  └──────────────────────────────────────────┘                       │
│      │                                                              │
│      ├── false ──▶ SKIPPED (AUTO_RUN_DISABLED_OR_PAUSED)            │
│      │                                                              │
│      ▼ true                                                         │
│  ┌──────────────────────────────────────────┐                       │
│  │ 2. manualOverrideState 확인              │                       │
│  └──────────────────────────────────────────┘                       │
│      │                                                              │
│      ├── "INACTIVE" ──▶ SKIPPED (SCHEDULE_GROUP_INACTIVE)           │
│      │                                                              │
│      ├── "PAUSED" ──┬── pauseUntil < now ──▶ SKIPPED                │
│      │              │   (SCHEDULE_GROUP_PAUSED)                     │
│      │              │                                               │
│      │              └── pauseUntil >= now ──▶ 만료 처리 ──▶ 정상 진행│
│      │                                                              │
│      └── null (AUTO) ──▶ isActive 확인 ──▶ 정상 진행                │
│                                                                     │
│      ▼                                                              │
│  ┌──────────────────────────────────────────┐                       │
│  │ 3. 타이머 실행 중 확인                    │                       │
│  └──────────────────────────────────────────┘                       │
│      │                                                              │
│      ├── 실행 중 ──▶ SKIPPED (TIMER_ALREADY_RUNNING)                │
│      │                                                              │
│      ▼ IDLE                                                         │
│  ┌──────────────────────────────────────────┐                       │
│  │ 4. 알림 표시 + 타이머 시작                │                       │
│  └──────────────────────────────────────────┘                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 스킵 사유 (failureReason) 목록

| failureReason | 의미 | 우선순위 |
|---------------|------|---------|
| `AUTO_RUN_DISABLED_OR_PAUSED` | 마스터 스위치 비활성화 또는 전역 일시중지 | 1 (최우선) |
| `SCHEDULE_GROUP_NOT_FOUND` | v8 신규 - 스케줄 그룹 없음 | 2 |
| `SCHEDULE_GROUP_INACTIVE` | v8 신규 - 사용자가 명시적으로 비활성화 | 3 |
| `SCHEDULE_GROUP_PAUSED` | v8 신규 - 사용자가 일시중지 설정 | 3 |
| `SCHEDULE_GROUP_NOT_ACTIVE` | 기존 - 스케줄 그룹 isActive=false | 4 |
| `TIMER_ALREADY_RUNNING` | 타이머가 이미 실행 중 | 5 |

## 우선순위 계층 구조

```
┌─────────────────────────────────────────┐
│ 레벨 1: 마스터 스위치                    │
│ (UserSettings.isAutoRunEnabled)        │
├─────────────────────────────────────────┤
│ 레벨 2: 스케줄 그룹 수동 제어 (v8)       │
│ (manualOverrideState)                  │
│   - INACTIVE: 무조건 건너뛰기            │
│   - PAUSED: pauseUntil까지 건너뛰기     │
├─────────────────────────────────────────┤
│ 레벨 3: 스케줄 그룹 자동 제어            │
│ (isActive)                             │
│   - 위치 기반 자동 활성화/비활성화        │
├─────────────────────────────────────────┤
│ 레벨 4: 개별 시간대 설정                 │
│ (TimeBasedAutoRun.isEnabled)           │
└─────────────────────────────────────────┘
```

## 검증 방법

### 1. 컴파일 테스트

```bash
./gradlew compileDebugKotlin
```

**결과**: BUILD SUCCESSFUL ✅

## 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `AutoRunAlarmReceiver.kt` | handleAutoRunAlarm에 v8 manualOverrideState 체크 추가 |

## 테스트 시나리오

### 시나리오 1: INACTIVE 상태에서 알람 트리거

1. 스케줄 그룹을 "비활성" 상태로 설정
2. 해당 그룹의 시간대 알람 시간 도래
3. **기대 결과**: 알람 건너뜀, "SKIPPED (SCHEDULE_GROUP_INACTIVE)" 로그

### 시나리오 2: PAUSED 상태에서 알람 트리거

1. 스케줄 그룹을 "1시간 일시중지" 설정
2. 해당 그룹의 시간대 알람 시간 도래
3. **기대 결과**: 알람 건너뜀, "SKIPPED (SCHEDULE_GROUP_PAUSED)" 로그

### 시나리오 3: PAUSED 만료 후 알람 트리거

1. 스케줄 그룹을 "1시간 일시중지" 설정
2. 1시간 경과 후 알람 시간 도래
3. **기대 결과**: manualOverrideState 자동 해제, 정상 알람 처리

### 시나리오 4: AUTO 모드에서 알람 트리거

1. 스케줄 그룹 "활성" 상태 (manualOverrideState = null)
2. 해당 그룹의 시간대 알람 시간 도래
3. **기대 결과**: 정상 알람 처리, 타이머 시작

## 하위 호환성

- 기존 `isActive` 체크는 `manualOverrideState = null`일 때만 적용
- v8 이전 데이터는 `manualOverrideState = null`이므로 기존 동작 유지
- 마이그레이션에서 `isActive = false`인 경우 `manualOverrideState = "INACTIVE"`로 변환됨

## 다음 단계 주의사항

### 단계 7 (통합 테스트) 시 참고

1. **위치 + 시간 복합 테스트**
   - 위치 진입 → PAUSED 설정 → 알람 시간 도래 → 건너뛰기 확인
   - 위치 이탈 → INACTIVE 유지 → 재진입 → 여전히 비활성화 확인

2. **마스터 스위치 vs 그룹 제어**
   - 마스터 OFF → 그룹 ACTIVE → 건너뛰기 (마스터 우선)
   - 마스터 ON → 그룹 INACTIVE → 건너뛰기 (그룹 제어 적용)

---

**작업 완료일**: 2025-12-10  
**커밋 ID**: d390706

