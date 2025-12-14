# 작업 기록: ViewModel/Repository 수정

**작업 일시**: 2025-12-10  
**작업 범위**: 일시중지 만료 자동 해제 로직 추가

## 작업 목적

단계 3에서 이미 구현한 ViewModel/Repository 기능에 일시중지 만료 자동 해제 로직을 추가합니다.

### 해결하고자 하는 문제
- 일시중지 시간이 만료되어도 DB에 PAUSED 상태가 남아있음
- UI에서는 `fromEntity()`로 ACTIVE로 표시되지만 DB 정리 필요

### 관련 PRD
- [04_버튼역할변경_prd.md](../../docs/04_버튼역할변경_prd.md)

## 단계 3에서 이미 완료된 작업

단계 3에서 아래 작업들이 이미 완료되었습니다:

### 1. ScheduleGroupRepository 인터페이스 (4.1)
```kotlin
suspend fun updateManualOverride(groupId: String, overrideState: String?, pauseUntil: Long?)
```

### 2. ScheduleGroupRepositoryImpl 구현 (4.2)
```kotlin
override suspend fun updateManualOverride(groupId: String, overrideState: String?, pauseUntil: Long?) {
    scheduleGroupDao.updateManualOverride(groupId, overrideState, pauseUntil)
}
```

### 3. ScheduleGroupViewModel 메서드 추가 (4.3, 4.4)
```kotlin
fun changeControlState(groupId: String, newState: ScheduleGroupControlState)
fun pauseScheduleGroup(groupId: String, duration: PauseDuration)
```

### 4. 일시중지 만료 체크 (4.5) - fromEntity()에서 처리
```kotlin
// ScheduleGroupControlState.fromEntity()
if (pauseUntil != null && System.currentTimeMillis() >= pauseUntil) {
    ACTIVE  // 만료됨 → ACTIVE로 간주
}
```

## 단계 4에서 추가한 작업

### 1. clearExpiredPauses 메서드 추가

#### Repository 인터페이스
**파일**: `app/src/main/java/com/allday/detoxy/domain/repository/ScheduleGroupRepository.kt`

```kotlin
/**
 * 만료된 일시중지 자동 해제 (v8+)
 *
 * @return 업데이트된 행 수
 */
suspend fun clearExpiredPauses(): Int
```

#### Repository 구현체
**파일**: `app/src/main/java/com/allday/detoxy/data/repository/ScheduleGroupRepositoryImpl.kt`

```kotlin
override suspend fun clearExpiredPauses(): Int {
    return scheduleGroupDao.clearExpiredPauses(System.currentTimeMillis())
}
```

### 2. ViewModel 초기화 시 자동 정리

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`

```kotlin
init {
    // v8: 앱 시작 시 만료된 일시중지 상태 자동 해제
    clearExpiredPausesOnInit()
}

private fun clearExpiredPausesOnInit() {
    viewModelScope.launch {
        try {
            val clearedCount = repository.clearExpiredPauses()
            if (clearedCount > 0) {
                Log.d("ScheduleGroupVM", "v8: Cleared $clearedCount expired pauses")
            }
        } catch (e: Exception) {
            Log.w("ScheduleGroupVM", "Failed to clear expired pauses: ${e.message}")
        }
    }
}
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
| `ScheduleGroupRepository.kt` | `clearExpiredPauses()` 인터페이스 추가 |
| `ScheduleGroupRepositoryImpl.kt` | `clearExpiredPauses()` 구현 |
| `ScheduleGroupViewModel.kt` | `init` 블록 + `clearExpiredPausesOnInit()` 추가 |

## 전체 v8 통합 제어 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│                        UI Layer                               │
├──────────────────────────────────────────────────────────────┤
│  ScheduleGroupScreen                                          │
│    └─ ScheduleGroupCard                                       │
│         └─ ScheduleControlButton (탭/롱프레스/드롭다운)        │
│              ├─ onStateChange(ACTIVE/INACTIVE)               │
│              └─ onPause(PauseDuration)                       │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                     ViewModel Layer                           │
├──────────────────────────────────────────────────────────────┤
│  ScheduleGroupViewModel                                       │
│    ├─ init { clearExpiredPausesOnInit() }                    │
│    ├─ changeControlState(groupId, newState)                  │
│    │     ├─ ACTIVE  → updateManualOverride(null, null)       │
│    │     │            + scheduleManager.activateGroup()      │
│    │     └─ INACTIVE → updateManualOverride("INACTIVE", null)│
│    │                   + scheduleManager.deactivateGroup()   │
│    └─ pauseScheduleGroup(groupId, duration)                  │
│          └─ updateManualOverride("PAUSED", pauseUntil)       │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                    Repository Layer                           │
├──────────────────────────────────────────────────────────────┤
│  ScheduleGroupRepository (Interface)                          │
│    ├─ updateManualOverride(groupId, state, pauseUntil)       │
│    └─ clearExpiredPauses(): Int                              │
│                                                               │
│  ScheduleGroupRepositoryImpl (Implementation)                 │
│    └─ DAO 호출                                                │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────┐
│                       Data Layer                              │
├──────────────────────────────────────────────────────────────┤
│  ScheduleGroupDao                                             │
│    ├─ updateManualOverride(groupId, state, pauseUntil)       │
│    └─ clearExpiredPauses(currentTime): Int                   │
│                                                               │
│  ScheduleGroup Entity (v8)                                    │
│    ├─ manualOverrideState: String? (null/INACTIVE/PAUSED)    │
│    └─ pauseUntil: Long?                                      │
└──────────────────────────────────────────────────────────────┘
```

## 상태 전환 플로우

```
┌─────────┐      탭        ┌──────────┐
│ ACTIVE  │ ◀──────────── │ INACTIVE │
│         │ ──────────▶   │          │
└────┬────┘      탭        └──────────┘
     │
     │ 드롭다운 → 일시중지
     ▼
┌─────────┐
│ PAUSED  │ ──────────▶ pauseUntil 만료 → ACTIVE
│         │
└─────────┘
     │
     │ 탭
     ▼
┌─────────┐
│ ACTIVE  │
└─────────┘
```

## 다음 단계 주의사항

### 단계 5 (GeofenceTransitionsReceiver 수정) 시 참고

1. **manualOverrideState 확인**
   - 위치 진입 시 `manualOverrideState` 먼저 확인
   - "INACTIVE": 진입 무시 (로그만 기록)
   - "PAUSED": pauseUntil 확인 후 처리

2. **Geofence 해제 로직**
   - INACTIVE로 변경 시 해당 위치의 Geofence 해제
   - ACTIVE로 변경 시 Geofence 재등록

## 교훈 및 참고사항

1. **init 블록에서 비동기 작업**
   - `viewModelScope.launch`로 안전하게 실행
   - 에러 발생 시 UI에 영향 없이 로그만 기록

2. **만료 체크 이중화**
   - UI: `fromEntity()`에서 실시간 만료 체크 → ACTIVE로 표시
   - DB: `clearExpiredPauses()`로 백그라운드 정리

---

**작업 완료일**: 2025-12-10  
**커밋 ID**: f09b5d2

