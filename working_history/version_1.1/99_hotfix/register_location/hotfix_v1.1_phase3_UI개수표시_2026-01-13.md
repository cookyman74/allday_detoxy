# Phase 3 작업 결과서: UI 활성화 개수 표시

> **작업일**: 2026-01-13  
> **작업자**: Antigravity (AI Pair Programmer)  
> **핫픽스 문서**: `working_history/version_1.1/hotfix/위치기반_개수제한_핫픽스_2026-01-13.md`

---

## 📋 작업 목적

ViewModel에 UX 표시용 활성화 개수 StateFlow를 추가하여, UI에서 현재 활성화 상태를 표시할 수 있도록 합니다.

---

## ✅ 변경 내용

### 변경 파일
- [`LocationBasedAutoRunViewModel.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/presentation/viewmodel/LocationBasedAutoRunViewModel.kt)
- [`ScheduleTabScreen.kt`](file:///Users/junghojang/Developments/myProject/allday_detoxy/app/src/main/java/com/allday/detoxy/presentation/ui/schedule/ScheduleTabScreen.kt)

---

## 📝 TASK-001: StateFlow 추가

### 새로 추가된 StateFlow (L124-148)

```kotlin
// ==================== Phase 3: UX 표시용 활성화 개수 ====================

/**
 * 추가 활성화 가능 여부 (UX 표시용)
 * 
 * ⚠️ Flow 기반으로 일시적인 지연이 있을 수 있음
 * 정책 체크는 getEnabledCount() 직접 호출로 수행
 */
private val _canActivateMore = MutableStateFlow(true)
val canActivateMore: StateFlow<Boolean> = _canActivateMore.asStateFlow()

/**
 * 현재 활성화된 위치 개수 (UX 표시용)
 * 
 * UI에서 "활성화: 3/5" 등의 형태로 표시 가능
 */
private val _enabledCount = MutableStateFlow(0)
val enabledCount: StateFlow<Int> = _enabledCount.asStateFlow()

/**
 * 최대 활성화 가능 개수
 */
val maxGeofences: Int = AutoRunGeofenceManager.MAX_GEOFENCES
```

### init 블록 Flow 수집 (L150-157)

```kotlin
init {
    checkPermissions()
    
    // ⚠️ 핫픽스 Phase 3: 활성화 개수 Flow 수집 (UX 표시용)
    viewModelScope.launch {
        repository.getEnabled().collect { list ->
            _enabledCount.value = list.size
            _canActivateMore.value = list.size < AutoRunGeofenceManager.MAX_GEOFENCES
        }
    }
}
```

---

## 📊 추가된 API

| StateFlow | 타입 | 설명 |
|-----------|------|------|
| `enabledCount` | `StateFlow<Int>` | 현재 활성화된 위치 개수 |
| `canActivateMore` | `StateFlow<Boolean>` | 추가 활성화 가능 여부 |
| `maxGeofences` | `Int` | 최대 활성화 가능 개수 (5) |

### UI 사용 예시

```kotlin
// Compose UI에서 사용
val viewModel: LocationBasedAutoRunViewModel = hiltViewModel()
val enabledCount by viewModel.enabledCount.collectAsState()
val maxGeofences = viewModel.maxGeofences
val canActivateMore by viewModel.canActivateMore.collectAsState()

Text("활성화: $enabledCount/$maxGeofences")

// 토글 스위치 비활성화
Switch(
    enabled = canActivateMore || location.isEnabled,
    checked = location.isEnabled,
    onCheckedChange = { viewModel.toggleLocation(location.id, it) }
)
```

---

## 📊 빌드 결과

```
BUILD SUCCESSFUL in 4s
18 actionable tasks: 3 executed, 15 up-to-date
```

---

## 🎯 전체 핫픽스 완료 요약

### Phase 1: 기존 구현 확인 ✅
- DAO/Repository/Manager의 기존 메서드 확인
- `MAX_GEOFENCES = 5` 체크 로직 확인

### Phase 2: ViewModel 개수 체크 ✅
- `addLocation()`: 선제 개수 체크 + Geofence 실패 시 isEnabled=false
- `updateLocation()`: 선제 개수 체크 (자기 자신 제외 보정) + Geofence 실패 시 isEnabled=false
- `toggleLocation()`: 선제 개수 체크 (자기 자신 제외 보정) + Geofence 실패 시 DB 토글 안 함
- 리뷰 피드백 반영: 주석 수정, max(0) 방어

### Phase 3: UI 활성화 개수 표시 ✅
- **ViewModel 수정**: `canActivateMore`, `enabledCount`, `maxGeofences` StateFlow 추가
- **UI 구현**: `ScheduleTabScreen` 상단에 "위치 모니터링: 3/5" 배지 추가 (사용자 혼선 해소)

---

## 🔐 최종 방어 계층 구조

```
┌─────────────────────────────────────────────────────────────┐
│ 1차 방어 (UX): ViewModel 선제 개수 체크                      │
│    - addLocation(), updateLocation(), toggleLocation()      │
│    - 사용자에게 빠른 피드백 제공                              │
│    - 에러 메시지: "최대 5개까지만 활성화할 수 있습니다."       │
├─────────────────────────────────────────────────────────────┤
│ 2차 방어 (최종): AutoRunGeofenceManager.addGeofence()        │
│    - getEnabledCountExcept(id) >= MAX_GEOFENCES 체크         │
│    - 에러 메시지: "최대 5개까지만 등록할 수 있습니다."         │
└─────────────────────────────────────────────────────────────┘
```

---

## 📌 향후 개선 사항

1. **테스트 커버리지**
   - Unit Test: 선제 개수 체크 로직
   - Integration Test: Geofence 실패 시나리오

---

**작성 완료**: 2026-01-13  
**전체 핫픽스 상태**: ✅ 완료 (수동 테스트 및 커밋 필요)
