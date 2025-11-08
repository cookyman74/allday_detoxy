# 2025-11-05: 위치 기반 스케줄 통계 리포트 가능 여부 분석

## 📌 작업 개요

### 목표
사용자가 요청한 위치 기반 스케줄의 다양한 통계 리포트가 현재 데이터 구조로 가능한지 확인

### 요청된 리포트 항목
1. 특정 위치 스케줄에서 정상 완료된 건수
2. 특정 위치 스케줄에서 차단된 앱과 그 통계
3. 특정 위치 스케줄에서 포기건수
4. 위치에서 벗어나서 종료된 처리된 건수

---

## 🔍 데이터 구조 분석

### 현재 저장된 데이터

#### 1. AutoRunLog (위치 기반 이벤트 기록)
```kotlin
data class AutoRunLog(
    val triggerType: String,           // "LOCATION"
    val triggerSourceId: String,       // LocationBasedAutoRun.id (위치 ID)
    val triggerTime: Long,              // Geofence 진입 시간
    val result: String,                 // "STARTED", "FAILED", "SKIPPED"
    val sessionId: String?,            // 생성된 FocusSession ID
    val gpsAccuracyMeters: Float?,     // GPS 정확도
    val dwellSeconds: Int?              // 체류 시간
)
```

#### 2. FocusSession (타이머 세션 기록)
```kotlin
data class FocusSession(
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val durationMinutes: Int,
    val success: Boolean,               // true: 완료, false: 포기
    val giveUpReason: String?           // 포기 사유
)
```

#### 3. FocusInterruption (차단된 앱 기록)
```kotlin
data class FocusInterruption(
    val sessionId: String,              // FocusSession ID
    val packageName: String,             // 차단된 앱 패키지명
    val category: String                 // 앱 카테고리
)
```

---

## ✅ 리포트 가능 여부 분석

### 1. 특정 위치 스케줄에서 정상 완료된 건수

**✅ 가능**

**데이터 흐름**:
```
AutoRunLog (triggerSourceId = locationId, result = "STARTED")
    ↓ sessionId
FocusSession (success = true)
```

**구현 방법**:
```kotlin
// 1. 특정 위치의 모든 AutoRunLog 조회
val locationLogs = autoRunLogDao.getBySourceId(locationId)

// 2. sessionId로 FocusSession 조회하여 success=true인 것만 필터링
locationLogs.collect { logs ->
    val completedCount = logs
        .filter { it.result == "STARTED" && it.sessionId != null }
        .mapNotNull { it.sessionId }
        .map { sessionDao.getSessionById(it) }
        .filter { it.success == true }
        .count()
}
```

**DAO 메서드**:
- `AutoRunLogDao.getBySourceId(sourceId: String)`: 특정 위치의 모든 로그 조회
- `FocusSessionDao.getSessionById(sessionId: String)`: 세션 조회

---

### 2. 특정 위치 스케줄에서 차단된 앱과 그 통계

**✅ 가능**

**데이터 흐름**:
```
AutoRunLog (triggerSourceId = locationId, result = "STARTED")
    ↓ sessionId
FocusInterruption (sessionId, packageName, category)
```

**구현 방법**:
```kotlin
// 1. 특정 위치의 모든 세션 찾기
val locationLogs = autoRunLogDao.getBySourceId(locationId)
val sessionIds = locationLogs
    .filter { it.result == "STARTED" && it.sessionId != null }
    .map { it.sessionId!! }

// 2. 각 세션의 차단 이벤트 조회
sessionIds.forEach { sessionId ->
    val interruptions = interruptionDao.getInterruptionsBySession(sessionId)
    
    // 카테고리별 통계
    val categoryStats = interruptionDao.getCategoryCountsBySession(sessionId)
    // → [CategoryCount("SNS", 5), CategoryCount("MESSENGER", 3), ...]
    
    // 패키지별 통계
    val packageStats = interruptions
        .groupBy { it.packageName }
        .mapValues { it.value.size }
}
```

**DAO 메서드**:
- `AutoRunLogDao.getBySourceId(sourceId: String)`: 위치 로그 조회
- `FocusInterruptionDao.getInterruptionsBySession(sessionId: String)`: 세션별 차단 이벤트
- `FocusInterruptionDao.getCategoryCountsBySession(sessionId: String)`: 카테고리별 통계

---

### 3. 특정 위치 스케줄에서 포기건수

**✅ 가능**

**데이터 흐름**:
```
AutoRunLog (triggerSourceId = locationId, result = "STARTED")
    ↓ sessionId
FocusSession (success = false)
```

**구현 방법**:
```kotlin
// 1. 특정 위치의 모든 AutoRunLog 조회
val locationLogs = autoRunLogDao.getBySourceId(locationId)

// 2. sessionId로 FocusSession 조회하여 success=false인 것만 필터링
locationLogs.collect { logs ->
    val giveUpCount = logs
        .filter { it.result == "STARTED" && it.sessionId != null }
        .mapNotNull { it.sessionId }
        .map { sessionDao.getSessionById(it) }
        .filter { it.success == false }
        .count()
    
    // 포기 사유별 통계도 가능
    val giveUpReasons = logs
        .filter { it.result == "STARTED" && it.sessionId != null }
        .mapNotNull { it.sessionId }
        .mapNotNull { sessionDao.getSessionById(it) }
        .filter { !it.success }
        .groupBy { it.giveUpReason ?: "unknown" }
        .mapValues { it.value.size }
}
```

**포기 사유**:
- `giveUpReason`: "user_give_up", "location_exit" 등 (현재는 "user_give_up"만 사용)

---

### 4. 위치에서 벗어나서 종료된 처리된 건수

**⚠️ 현재 구조로는 구분 어려움**

**문제점**:
1. `AutoRunLog`에는 위치 이탈 정보가 없음
2. `FocusSession.giveUpReason`에 위치 이탈 정보가 저장되지 않음
3. Geofence EXIT 이벤트와 타이머 종료의 연계 추적이 없음

**현재 Geofence EXIT 처리** (`GeofenceTransitionsReceiver.handleGeofenceExit`):
```kotlin
// 위치 이탈 시 시간표 비활성화만 수행
// 타이머가 실행 중이어도 종료하지 않음
scheduleManager.deactivateGroup(scheduleGroupId)
```

**해결 방법 (제안)**:

#### 방법 1: FocusSession에 위치 이탈 플래그 추가
```kotlin
data class FocusSession(
    // ... 기존 필드
    val endedByLocationExit: Boolean = false  // 위치 이탈로 종료됨
)
```

#### 방법 2: AutoRunLog에 EXIT 이벤트 기록
```kotlin
// Geofence EXIT 시 AutoRunLog 기록
AutoRunLog(
    triggerType = "LOCATION",
    triggerSourceId = locationId,
    result = "EXIT",  // 또는 "ENDED_BY_EXIT"
    sessionId = currentSessionId  // 실행 중인 세션 ID
)
```

#### 방법 3: 타이머 종료 시 위치 상태 확인
```kotlin
// 타이머 종료 시점에 사용자가 해당 위치 반경 내에 있는지 확인
// 위치 이탈 시 giveUpReason = "location_exit" 설정
```

---

## 📊 리포트 구현을 위한 추가 DAO 메서드 제안

### 1. 위치별 통계 조회를 위한 메서드

```kotlin
// AutoRunLogDao.kt
@Query("""
    SELECT COUNT(*) 
    FROM auto_run_log 
    WHERE triggerType = 'LOCATION' 
      AND triggerSourceId = :locationId
      AND result = 'STARTED'
      AND sessionId IS NOT NULL
""")
suspend fun getStartedCountByLocation(locationId: String): Int

@Query("""
    SELECT * FROM auto_run_log 
    WHERE triggerType = 'LOCATION' 
      AND triggerSourceId = :locationId
      AND result = 'STARTED'
      AND sessionId IS NOT NULL
    ORDER BY triggerTime DESC
""")
suspend fun getStartedLogsByLocation(locationId: String): List<AutoRunLog>
```

### 2. 위치별 세션 조회

```kotlin
// FocusSessionDao.kt (간접 조회)
// AutoRunLog → sessionId → FocusSession
```

### 3. 위치별 차단 통계 조회

```kotlin
// FocusInterruptionDao.kt
// AutoRunLog → sessionId → FocusInterruption
// 이미 getInterruptionsBySession() 메서드로 가능
```

---

## 🎯 결론 및 권장사항

### ✅ 가능한 리포트 (즉시 구현 가능)
1. ✅ 특정 위치 스케줄에서 정상 완료된 건수
2. ✅ 특정 위치 스케줄에서 차단된 앱과 그 통계
3. ✅ 특정 위치 스케줄에서 포기건수

### ⚠️ 추가 작업 필요한 리포트
4. ⚠️ 위치에서 벗어나서 종료된 처리된 건수
   - Geofence EXIT 이벤트와 타이머 종료 연계 필요
   - `FocusSession.endedByLocationExit` 필드 추가 또는
   - `AutoRunLog`에 EXIT 이벤트 기록

### 📝 다음 단계
1. 위치별 통계 리포트 계산기 구현
2. 위치 이탈 종료 추적 기능 추가 (선택)
3. 리포트 UI 구현

---

**작성일**: 2025-11-05  
**작성자**: AI Assistant  
**상태**: ✅ 분석 완료

