# 작업 기록: GeofenceTransitionsReceiver 수정

**작업 일시**: 2025-12-10  
**작업 범위**: v8 manualOverrideState 확인 로직 추가

## 작업 목적

위치 진입/이탈 시 스케줄 그룹의 수동 제어 상태(manualOverrideState)를 확인하여 사용자 의도를 우선 존중합니다.

### 해결하고자 하는 문제
- 기존: 사용자가 UI에서 비활성화해도 위치 진입 시 자동으로 스케줄이 활성화됨
- 개선: INACTIVE 또는 PAUSED 상태에서는 위치 진입 무시

### 관련 PRD
- [04_버튼역할변경_prd.md](../../docs/04_버튼역할변경_prd.md)

## 변경 사항

### 1. EntryPoint에 ScheduleGroupDao 추가

**파일**: `app/src/main/java/com/allday/detoxy/receiver/GeofenceTransitionsReceiver.kt`

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceReceiverEntryPoint {
    // ... 기존 ...
    fun scheduleGroupDao(): ScheduleGroupDao  // v8: manualOverrideState 확인용
}
```

### 2. handleGeofenceTrigger (ENTER/DWELL) 수정

위치 진입 시 스케줄 그룹의 manualOverrideState 확인:

```kotlin
// v8: 스케줄 그룹의 manualOverrideState 확인
val scheduleGroup = scheduleGroupDao.getByIdOnce(scheduleGroupId)
val overrideState = scheduleGroup.manualOverrideState
val pauseUntil = scheduleGroup.pauseUntil
val currentTime = System.currentTimeMillis()

when (overrideState) {
    "INACTIVE" -> {
        // 사용자가 명시적으로 비활성화 → 위치 진입 무시
        Log.i(TAG, "⏹️ v8: Location SKIPPED - ScheduleGroup manually INACTIVE")
        // AutoRunLog(result = "SKIPPED", failureReason = "SCHEDULE_GROUP_INACTIVE")
        return@forEach
    }
    "PAUSED" -> {
        if (pauseUntil != null && currentTime < pauseUntil) {
            // 아직 만료 안됨 → 위치 진입 무시
            Log.i(TAG, "⏸️ v8: Location SKIPPED - ScheduleGroup PAUSED")
            // AutoRunLog(result = "SKIPPED", failureReason = "SCHEDULE_GROUP_PAUSED")
            return@forEach
        } else {
            // 만료됨 → 자동으로 ACTIVE로 전환
            scheduleGroupDao.updateManualOverride(scheduleGroupId, null, null)
        }
    }
    else -> {
        // null → 자동 모드 (정상 처리)
    }
}
```

### 3. handleGeofenceExit 수정

위치 이탈 시 manualOverrideState 확인:

```kotlin
// v8: INACTIVE 상태에서 이탈 시 로그만 기록 (이미 비활성화 상태)
if (overrideState == "INACTIVE") {
    Log.i(TAG, "ℹ️ v8: Location EXIT but ScheduleGroup already INACTIVE")
    return@forEach
}

// v8: 위치 이탈 시 manualOverrideState는 유지 (사용자 의도 존중)
Log.d(TAG, "ℹ️ v8: manualOverrideState preserved on EXIT: $overrideState")
```

## v8 위치 기반 동작 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Geofence ENTER 처리                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  위치 진입                                                          │
│      │                                                              │
│      ▼                                                              │
│  ┌──────────────────────────────────────────┐                       │
│  │ manualOverrideState 확인                  │                       │
│  └──────────────────────────────────────────┘                       │
│      │                                                              │
│      ├── "INACTIVE" ──▶ SKIPPED (로그) ──▶ 종료                     │
│      │                                                              │
│      ├── "PAUSED" ──┬── pauseUntil < now ──▶ SKIPPED ──▶ 종료      │
│      │              │                                               │
│      │              └── pauseUntil >= now ──▶ 만료 처리 ──▶ 정상 진행│
│      │                                                              │
│      └── null (AUTO) ──▶ 정상 진행                                  │
│                                                              │
│      ▼                                                              │
│  ┌──────────────────────────────────────────┐                       │
│  │ 충돌 해소 + 스케줄 활성화                  │                       │
│  └──────────────────────────────────────────┘                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Geofence EXIT 처리                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  위치 이탈                                                          │
│      │                                                              │
│      ▼                                                              │
│  ┌──────────────────────────────────────────┐                       │
│  │ manualOverrideState 확인                  │                       │
│  └──────────────────────────────────────────┘                       │
│      │                                                              │
│      ├── "INACTIVE" ──▶ 로그만 기록 ──▶ 종료 (이미 비활성화)         │
│      │                                                              │
│      └── null/PAUSED ──▶ 스케줄 비활성화                            │
│                             │                                       │
│                             ▼                                       │
│                       manualOverrideState 유지                      │
│                       (사용자 의도 존중)                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## AutoRunLog 결과 값

| result | 의미 |
|--------|------|
| STARTED | 스케줄 활성화 성공 |
| FAILED | 스케줄 활성화 실패 (시스템 오류) |
| **SKIPPED** | v8 신규 - 사용자 의도로 건너뜀 |

| failureReason | 의미 |
|---------------|------|
| SCHEDULE_GROUP_INACTIVE | 사용자가 명시적으로 비활성화 |
| SCHEDULE_GROUP_PAUSED | 사용자가 일시중지 설정 |

## 검증 방법

### 1. 컴파일 테스트

```bash
./gradlew compileDebugKotlin
```

**결과**: BUILD SUCCESSFUL ✅

## 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `GeofenceTransitionsReceiver.kt` | EntryPoint에 ScheduleGroupDao 추가, ENTER/EXIT 로직에 manualOverrideState 확인 |

## 테스트 시나리오

### 시나리오 1: INACTIVE 상태에서 위치 진입

1. 스케줄 그룹을 "비활성" 상태로 설정
2. 해당 위치로 이동
3. **기대 결과**: 스케줄 활성화 안됨, "SKIPPED" 로그 기록

### 시나리오 2: PAUSED 상태에서 위치 진입

1. 스케줄 그룹을 "1시간 일시중지" 설정
2. 해당 위치로 이동
3. **기대 결과**: 스케줄 활성화 안됨, "SKIPPED" 로그 기록

### 시나리오 3: PAUSED 만료 후 위치 진입

1. 스케줄 그룹을 "1시간 일시중지" 설정
2. 1시간 경과 후 해당 위치로 이동
3. **기대 결과**: manualOverrideState 자동 해제, 스케줄 정상 활성화

### 시나리오 4: ACTIVE 상태에서 위치 이탈

1. 스케줄 그룹이 "활성" 상태
2. 해당 위치에서 이탈
3. **기대 결과**: 스케줄 비활성화, manualOverrideState 유지 (null)

## 다음 단계 주의사항

### 단계 6 (AutoRunAlarmReceiver 수정) 시 참고

1. **시간 기반 알람에서도 manualOverrideState 확인**
   - INACTIVE: 알람 건너뛰기
   - PAUSED: pauseUntil 확인 후 처리

2. **PAUSED 만료 시 자동 해제**
   - 알람 시점에 만료 확인
   - 만료됨 → DB 업데이트 후 정상 처리

---

**작업 완료일**: 2025-12-10  
**커밋 ID**: 7abedff

