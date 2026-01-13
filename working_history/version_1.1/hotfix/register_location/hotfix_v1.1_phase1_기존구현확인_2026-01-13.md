# Phase 1 작업 결과서: 기존 구현 확인

> **작업일**: 2026-01-13  
> **작업자**: Antigravity (AI Pair Programmer)  
> **핫픽스 문서**: `working_history/version_1.1/hotfix/위치기반_개수제한_핫픽스_2026-01-13.md`

---

## 📋 작업 목적

위치 기반 스케줄 활성화 5개 제한 적용을 위한 핫픽스 작업 전, 기존 DAO/Repository/Manager 구현 상태를 파악합니다.

---

## ✅ 1. DAO 구현 확인

**파일**: [`LocationBasedAutoRunDao.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/data/local/dao/LocationBasedAutoRunDao.kt)

### 확인된 메서드 목록

| 메서드 | 시그니처 | 라인 | 용도 |
|--------|----------|------|------|
| `getEnabledCount()` | `suspend fun getEnabledCount(): Int` | L107-113 | 활성화된 개수 조회 |
| `getEnabledCountExcept(excludeId)` | `suspend fun getEnabledCountExcept(excludeId: String): Int` | L146-156 | 자신 제외 활성화 개수 |
| `getEnabled()` | `fun getEnabled(): Flow<List<LocationBasedAutoRun>>` | L82-88 | 활성화 목록 Flow |
| `getAllEnabled()` | `suspend fun getAllEnabled(): List<LocationBasedAutoRun>` | L115-123 | 활성화 목록 일회성 |

### SQL 쿼리 확인

```kotlin
// getEnabledCount() - L111
@Query("SELECT COUNT(*) FROM location_based_auto_run WHERE isEnabled = 1")
suspend fun getEnabledCount(): Int

// getEnabledCountExcept() - L154
@Query("SELECT COUNT(*) FROM location_based_auto_run WHERE isEnabled = 1 AND id != :excludeId")
suspend fun getEnabledCountExcept(excludeId: String): Int

// getEnabled() - L87
@Query("SELECT * FROM location_based_auto_run WHERE isEnabled = 1 ORDER BY createdAt DESC")
fun getEnabled(): Flow<List<LocationBasedAutoRun>>
```

> ✅ **결론**: Phase 2 작업에 필요한 모든 DAO 메서드가 이미 구현되어 있음

---

## ✅ 2. Repository 구현 확인

**파일**: [`LocationBasedAutoRunRepository.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/data/repository/LocationBasedAutoRunRepository.kt)

### 확인된 메서드 목록

| 메서드 | 시그니처 | 라인 | DAO 위임 |
|--------|----------|------|----------|
| `getEnabled()` | `fun getEnabled(): Flow<List<LocationBasedAutoRun>>` | L36-41 | `dao.getEnabled()` |
| `getEnabledCount()` | `suspend fun getEnabledCount(): Int` | L90-95 | `dao.getEnabledCount()` |

### 구현 코드 확인

```kotlin
// L40-41
fun getEnabled(): Flow<List<LocationBasedAutoRun>> = dao.getEnabled()

// L94-95
suspend fun getEnabledCount(): Int = dao.getEnabledCount()
```

> ⚠️ **참고**: Repository에 `getEnabledCountExcept()`는 없지만, DAO에 직접 접근 가능하므로 필요시 추가 가능

---

## ✅ 3. AutoRunGeofenceManager MAX_GEOFENCES 체크 로직 확인

**파일**: [`AutoRunGeofenceManager.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/core/manager/AutoRunGeofenceManager.kt)

### MAX_GEOFENCES 상수

```kotlin
// L54
const val MAX_GEOFENCES = 5 // 배터리 효율성을 위해 5개로 제한
```

### addGeofence() 체크 로직 (L149-208)

```kotlin
suspend fun addGeofence(
    autoRun: LocationBasedAutoRun, 
    skipCountCheck: Boolean = false  // ⚠️ reschedule 시 체크 생략 가능
): Result<Unit> {
    // 사전 조건 체크 (L151-174)
    // - isPlayServicesAvailable()
    // - isLocationEnabled()
    // - hasLocationPermission()
    // - hasBackgroundLocationPermission()
    
    // 개수 체크 (L176-188)
    if (!skipCountCheck) {
        val otherEnabledCount = locationBasedAutoRunDao.getEnabledCountExcept(autoRun.id)
        
        if (otherEnabledCount >= MAX_GEOFENCES) {
            Log.w(TAG, "⚠️ Max geofences limit reached: Others=$otherEnabledCount, MAX=$MAX_GEOFENCES")
            return Result.failure(
                GeofenceException("최대 ${MAX_GEOFENCES}개까지만 등록할 수 있습니다. 기존 위치를 삭제한 후 다시 시도해주세요.")
            )
        }
    }
    
    // Geofence 등록 (L190-199)
    // ...
}
```

### 체크 로직 특징

| 항목 | 내용 |
|------|------|
| **체크 시점** | Geofence 등록 직전 (최종 방어) |
| **체크 방식** | `getEnabledCountExcept(autoRun.id)` - 자기 자신 제외 |
| **체크 조건** | `otherEnabledCount >= MAX_GEOFENCES` |
| **실패 시** | `Result.failure(GeofenceException(...))` 반환 |
| **skipCountCheck** | `true`면 reschedule 상황으로 체크 생략 |

### rescheduleAll() 추가 방어 (L273-279)

```kotlin
// ⚠️ 핫픽스: 최대 MAX_GEOFENCES개만 등록 (초과분은 무시)
val locationsToRegister = enabledLocations.take(MAX_GEOFENCES)
val skippedCount = enabledLocations.size - locationsToRegister.size

if (skippedCount > 0) {
    Log.w(TAG, "⚠️ ${skippedCount}개 위치가 제한 초과로 등록 제외됨 (MAX=$MAX_GEOFENCES)")
}
```

> ✅ **결론**: Manager에 MAX_GEOFENCES 체크 로직이 최종 방어 계층으로 구현되어 있음

---

## 📊 Phase 2 작업에 필요한 사항

### ViewModel에서 사용할 메서드

| 위치 | 메서드 | 용도 |
|------|--------|------|
| **Repository** | `getEnabledCount()` | 선제 개수 체크 (정책 체크용) |
| **Repository** | `getEnabled()` | Flow 기반 enabledCount 표시 (UX용) |
| **AutoRunGeofenceManager** | `MAX_GEOFENCES` | 상수 참조 (에러 메시지) |
| **AutoRunGeofenceManager** | `addGeofence()` | Geofence 등록 + 최종 방어 |

### 주의 사항

1. **UX 표시용 vs 정책 체크용 분리**
   - UX 표시용: `repository.getEnabled().collect { }` (Flow, 비동기)
   - 정책 체크용: `repository.getEnabledCount()` (suspend, 최신 값)

2. **Manager 최종 방어 유지**
   - ViewModel 선제 체크 실패 시에도 Manager가 최종 방어
   - 두 계층 모두 유지하여 안정성 확보

3. **에러 메시지 일관성**
   - Manager: "최대 5개까지만 등록할 수 있습니다. 기존 위치를 삭제한 후 다시 시도해주세요."
   - ViewModel: "최대 5개까지만 활성화할 수 있습니다." (활성화 관점)

---

## ✅ 체크리스트 완료

- [x] **[VERIFY]** DAO에 `getEnabledCount()` 존재 확인
- [x] **[VERIFY]** DAO에 `getEnabledCountExcept(id)` 존재 확인
- [x] **[VERIFY]** Repository에 `getEnabled()` Flow 존재 확인
- [x] **[VERIFY]** Repository에 `getEnabledCount()` 존재 확인  
- [x] **[VERIFY]** Manager의 MAX_GEOFENCES 체크 로직 확인

---

**작성 완료**: 2026-01-13  
**다음 작업**: Phase 2 - ViewModel 활성화 개수 체크 + Geofence 실패 정책 수정
