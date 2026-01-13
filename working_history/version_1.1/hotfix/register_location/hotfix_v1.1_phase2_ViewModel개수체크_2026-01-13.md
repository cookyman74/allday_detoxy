# Phase 2 작업 결과서: ViewModel 활성화 개수 체크 + Geofence 실패 정책 수정

> **작업일**: 2026-01-13  
> **작업자**: Antigravity (AI Pair Programmer)  
> **핫픽스 문서**: `working_history/version_1.1/hotfix/위치기반_개수제한_핫픽스_2026-01-13.md`

---

## 📋 작업 목적

ViewModel 레벨에서 선제 개수 체크를 추가하여 사용자에게 빠른 UX 피드백을 제공하고, Geofence 실패 시 일관된 정책(isEnabled=false 저장)을 적용합니다.

---

## ✅ 변경 내용

### 변경 파일
- [`LocationBasedAutoRunViewModel.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/presentation/viewmodel/LocationBasedAutoRunViewModel.kt)

---

## 📝 TASK-001: addLocation() 수정

### 선제 개수 체크 추가 (L193-207)

```kotlin
// ⚠️ 핫픽스: 활성화된 상태로 등록 시 선제 개수 체크 (UX 빠른 피드백)
if (location.isEnabled) {
    val currentEnabledCount = repository.getEnabledCount()
    if (currentEnabledCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
        Log.w(TAG, "⚠️ Pre-check: Max geofences limit reached ($currentEnabledCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
        _errorState.value = LocationError.GeofenceError(
            "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
        )
        return
    }
}
```

### Geofence 실패 시 isEnabled=false 저장 (L215-224)

```diff
- // 🐛 버그 수정: Geofence 실패해도 사용자가 설정한 isEnabled 값 유지
- repository.insert(location)  // 사용자가 설정한 값 그대로 저장
+ // ⚠️ 핫픽스: Geofence 실패 시 isEnabled=false로 저장
+ val disabledLocation = location.copy(isEnabled = false)
+ repository.insert(disabledLocation)
```

---

## 📝 TASK-002: updateLocation() 수정

### 선제 개수 체크 추가 (L288-305)

```kotlin
// ⚠️ 핫픽스: 활성화 상태로 업데이트 시 선제 개수 체크 (자기 자신 제외)
// 기존에 비활성화였다가 활성화로 변경하는 경우를 위한 체크
if (location.isEnabled) {
    val currentEnabledCount = repository.getEnabledCount()
    // 자기 자신이 이미 활성화 상태면 카운트에서 1을 빼야 함
    val existing = repository.getById(location.id).first()
    val adjustedCount = if (existing?.isEnabled == true) currentEnabledCount - 1 else currentEnabledCount
    
    if (adjustedCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
        Log.w(TAG, "⚠️ Pre-check: Max geofences limit reached (adjusted: $adjustedCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
        _errorState.value = LocationError.GeofenceError(
            "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
        )
        return
    }
}
```

### Geofence 실패 시 isEnabled=false 저장 (L318-329)

```diff
- repository.update(location)  // 사용자가 설정한 값 그대로 저장
+ val disabledLocation = location.copy(isEnabled = false)
+ repository.update(disabledLocation)
```

---

## 📝 TASK-003: toggleLocation() 수정

### 선제 개수 체크 추가 (L497-506)

```kotlin
// ⚠️ 핫픽스: 활성화 시도 시 선제 개수 체크 (UX 빠른 피드백)
val currentEnabledCount = repository.getEnabledCount()
if (currentEnabledCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
    Log.w(TAG, "⚠️ Toggle pre-check: Max geofences limit reached ($currentEnabledCount >= ${AutoRunGeofenceManager.MAX_GEOFENCES})")
    _errorState.value = LocationError.GeofenceError(
        "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지만 활성화할 수 있습니다."
    )
    return@launch
}
```

### Geofence 실패 시 에러 표시 (L513-520)

```diff
- _errorState.value = LocationError.GeofenceError(
-     exception?.message ?: "Geofence 등록 실패. 위치 권한과 Play Services를 확인해주세요."
- )
+ Log.e(TAG, "❌ Toggle Geofence registration FAILED: ${exception?.message}")
+ // ⚠️ 핫픽스: Geofence 실패 시 DB 토글 안 함, 에러 표시
+ _errorState.value = LocationError.GeofenceError(
+     "위치 모니터링 활성화에 실패했습니다.\n" +
+     "설정에서 다시 시도해주세요."
+ )
```

---

## 📊 빌드 결과

```
BUILD SUCCESSFUL in 6s
18 actionable tasks: 2 executed, 16 up-to-date
```

---

## 📋 수정 정책 요약

| 메서드 | 선제 개수 체크 | Geofence 실패 시 |
|--------|---------------|-----------------|
| `addLocation()` | ✅ `getEnabledCount() >= MAX(5)` | isEnabled=false로 DB 저장 |
| `updateLocation()` | ✅ 자기 자신 제외 보정 후 체크 | isEnabled=false로 DB 저장 |
| `toggleLocation()` | ✅ `getEnabledCount() >= MAX(5)` | DB 토글 안 함 + 에러 표시 |

---

## 🔐 방어 계층 구조

```
┌─────────────────────────────────────────────────────────────┐
│ 1차 방어 (UX): ViewModel 선제 개수 체크                      │
│    - 사용자에게 빠른 피드백 제공                              │
│    - 에러 메시지: "최대 5개까지만 활성화할 수 있습니다."       │
├─────────────────────────────────────────────────────────────┤
│ 2차 방어 (최종): AutoRunGeofenceManager.addGeofence()        │
│    - getEnabledCountExcept(id) >= MAX_GEOFENCES 체크         │
│    - 에러 메시지: "최대 5개까지만 등록할 수 있습니다."         │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ 주의 사항

1. **enabledCount 불일치 가능성**
   - UX 표시용 Flow와 정책 체크용 `getEnabledCount()`는 일시적으로 불일치할 수 있음
   - 최종 방어는 Manager가 담당하므로 안전함

2. **updateLocation() 자기 자신 제외 로직**
   - 기존에 활성화된 위치를 수정할 때, 자기 자신을 카운트에서 빼야 함
   - `adjustedCount` 변수로 보정 처리

---

## 📌 Phase 3 작업에 필요한 사항

1. **ViewModel에 추가할 StateFlow**
   - `canActivateMore: StateFlow<Boolean>` (UX 표시용)
   - `enabledCount: StateFlow<Int>` (UX 표시용)

2. **UI 개선 (선택)**
   - 활성화 개수 표시: "활성화: 3/5"

---

**작성 완료**: 2026-01-13  
**다음 작업**: Phase 3 - UI 활성화 개수 상태 표시

---

## ⚠️ 리뷰 피드백 반영 (2026-01-13 20:42)

### 접수된 이슈

| 심각도 | 이슈 | 조치 |
|--------|------|------|
| **High** | 주석/문서가 실제 동작과 불일치 (L185, L279) | ✅ 주석 업데이트 |
| **Medium** | toggleLocation() 선제 체크에 자기 자신 제외 보정 누락 | ✅ 보정 로직 추가 |
| **Low** | updateLocation() adjustedCount가 0 이하가 될 수 있음 | ✅ maxOf(0, ...) 적용 |

### 수정 내역

#### 1. [High] 주석 업데이트

**addLocation() (L185)**
```diff
- * 1. Geofence 등록 (활성화된 경우만) - 실패 시 DB 저장 안 함
+ * 1. Geofence 등록 (활성화된 경우만) - 실패 시 isEnabled=false로 DB 저장
```

**updateLocation() (L279)**
```diff
- * 2. 새 Geofence 등록 (활성화된 경우만) - 실패 시 DB 업데이트 안 함
+ * 2. 새 Geofence 등록 (활성화된 경우만) - 실패 시 isEnabled=false로 DB 업데이트
```

#### 2. [Medium] toggleLocation() 자기 자신 제외 보정 (L497-520)

```kotlin
// ⚠️ 핫픽스: 활성화 시도 시 선제 개수 체크 (자기 자신 제외 보정)
// UI와 DB 상태가 일시적으로 불일치하거나, 동일 상태 토글 요청이 들어올 수 있음
val location = repository.getById(locationId).first()
if (location == null) {
    Log.w(TAG, "⚠️ Toggle: Location not found: $locationId")
    return@launch
}

val currentEnabledCount = repository.getEnabledCount()
// 자기 자신이 이미 활성화 상태면 카운트에서 제외해야 함
val adjustedCount = maxOf(0, if (location.isEnabled) currentEnabledCount - 1 else currentEnabledCount)

if (adjustedCount >= AutoRunGeofenceManager.MAX_GEOFENCES) { ... }
```

**개선 효과**:
- UI와 DB 상태가 일시적으로 불일치해도 정상 동작
- 동일 상태 토글 요청(ON → ON)이 들어와도 오차단 없음

#### 3. [Low] updateLocation() max(0) 방어 (L295)

```diff
- val adjustedCount = if (existing?.isEnabled == true) currentEnabledCount - 1 else currentEnabledCount
+ val adjustedCount = maxOf(0, if (existing?.isEnabled == true) currentEnabledCount - 1 else currentEnabledCount)
```

### 빌드 결과

```
BUILD SUCCESSFUL in 6s
18 actionable tasks: 3 executed, 15 up-to-date
```
