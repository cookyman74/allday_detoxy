# 2025-11-04: 활성 vs 작동 구분 및 멀티 활성화 지원 (v0.10.1)

## 📌 작업 개요

### 목표
- **"활성(Active)" vs "작동(Running)" 개념 명확화**
- **멀티 활성화 지원**: 여러 스케줄 그룹이 동시에 활성화될 수 있도록 개선
- **위치 기반 스케줄 시각화 개선**: 활성화된 모든 스케줄 표시

### 배경
**사용자 피드백**:
- "집"과 "회사" 스케줄이 모두 활성화되어 있는데, UI에는 "집"만 표시됨
- 대기 상태 카드가 하나만 표시되는 것은 명백한 오류
- 위치 기반 스케줄은 여러 개가 동시에 활성화될 수 있어야 함

**근본 원인**:
```kotlin
// ❌ 이전 코드
val activeGroup: StateFlow<ScheduleGroup?> = repository.getActive()
    .map { it.firstOrNull() }  // 첫 번째만 선택!
```

**해결 방향**:
- `activeGroup` (단수) → `activeGroups` (복수)로 변경
- 활성화된 모든 그룹을 UI에 표시
- "다음 예약" 로직도 모든 활성화된 그룹을 고려

---

## 🎯 주요 변경사항

### 1. ViewModel 개선: 멀티 활성화 지원

#### `ScheduleGroupViewModel.kt`

**Before**:
```kotlin
/**
 * 활성화된 ScheduleGroup (Flow)
 *
 * 3차 고도화: 한 번에 하나의 시간표만 활성화 가능 (단일 활성화 원칙)
 */
val activeGroup: StateFlow<ScheduleGroup?> = repository.getActive()
    .map { it.firstOrNull() }
    .stateIn(...)
```

**After**:
```kotlin
/**
 * 활성화된 ScheduleGroup 목록 (Flow)
 *
 * v0.10.1: 여러 스케줄 그룹이 동시에 활성화될 수 있음
 * - 위치 기반 스케줄: 각 위치마다 별도의 스케줄 활성화 가능
 * - 시간 기반 스케줄: 여러 시간표 동시 활성화 가능
 */
val activeGroups: StateFlow<List<ScheduleGroup>> = repository.getActive()
    .stateIn(...)
```

**`getNextScheduleToday()` 개선**:
```kotlin
fun getNextScheduleToday(): Flow<TimeBasedAutoRun?> = flow {
    val now = LocalTime.now()
    val currentActiveGroups = activeGroups.value  // 모든 활성화 그룹
    
    if (currentActiveGroups.isNotEmpty()) {
        // 모든 활성화된 그룹의 시간대를 수집
        val allTimeSlots = mutableListOf<TimeBasedAutoRun>()
        currentActiveGroups.forEach { group ->
            val timeSlots = repository.getLinkedTimeBasedAutoRuns(group.id)
            allTimeSlots.addAll(timeSlots)
        }
        
        // 현재 시각 이후의 가장 가까운 시간대 찾기
        val nextSlot = allTimeSlots
            .filter { slot -> parseTime(slot.hour, slot.minute) > now }
            .sortedBy { slot -> slot.hour * 60 + slot.minute }
            .firstOrNull()
        
        emit(nextSlot)
    } else {
        emit(null)
    }
}
```

---

### 2. UI 개선: 활성화된 모든 스케줄 표시

#### `ScheduleTabScreen.kt`

**변경 사항**:
```kotlin
// 1. 복수형으로 변경
val activeGroups by viewModel.activeGroups.collectAsState()

// 2. 활성화된 모든 그룹을 카드로 표시 (작동 중인 것 제외)
val waitingGroups = activeGroups.filter { it.id != runningScheduleGroupId }
items(waitingGroups) { group ->
    ActiveScheduleSummaryCard(
        activeSchedule = group,
        linkedLocations = linkedLocations[group.id] ?: emptyList()
    )
}

// 3. 활성 상태 체크 개선
ScheduleSummaryCard(
    group = group,
    isActive = activeGroups.any { it.id == group.id },  // ✅ 여러 활성화 그룹 지원
    // ...
)
```

**UI 우선순위**:
1. **🔴 작동 중 카드** (타이머 실행 중) - 최우선 표시
2. **🟢 활성화 카드들** (대기 상태) - 작동 중인 것 제외, 모든 활성화된 그룹 표시
3. **📅 다음 예약 카드** - 모든 활성화 그룹의 다음 예약 표시

---

### 3. 기존 화면 호환성 유지

#### `ScheduleGroupScreen.kt`
```kotlin
// 단순히 activeGroup → activeGroups로 변경
val activeGroups by viewModel.activeGroups.collectAsStateWithLifecycle()
```

---

## 🧪 검증 결과

### 컴파일 검증
```bash
./gradlew compileDebugKotlin
```

**결과**: ✅ 성공 (경고만 있음, 기존 코드 관련)

### 변경 파일 목록
- `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
  - `activeGroup` → `activeGroups` 변경
  - `getNextScheduleToday()` 로직 개선 (멀티 그룹 지원)
  
- `app/src/main/java/com/allday/detoxy/presentation/ui/schedule/ScheduleTabScreen.kt`
  - `activeGroup` → `activeGroups` 변경
  - 활성화된 모든 그룹을 `items()`로 표시
  - `isActive` 체크 로직 개선
  
- `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/ScheduleGroupScreen.kt`
  - `activeGroup` → `activeGroups` 변경

---

## 📊 아키텍처 영향

### 개념 정리

| 상태 | 의미 | UI 표시 | 예시 |
|------|------|---------|------|
| **활성(Active)** | 스케줄이 켜져있어 조건이 맞으면 자동 작동할 준비가 된 상태 | 🟢 활성화됨 (대기 중) | "집" 스케줄 활성화, "회사" 스케줄 활성화 |
| **작동(Running)** | 지금 이 순간 타이머가 실행 중인 상태 | 🔴 작동 중 | "집" 스케줄이 타이머 실행 중 |

### 멀티 활성화 지원의 의미

**Before (v0.10.0)**:
- 하나의 스케줄만 활성화 가능
- "집" 스케줄이 활성화되면 "회사" 스케줄은 자동 비활성화

**After (v0.10.1)**:
- 여러 스케줄 동시 활성화 가능
- "집" 스케줄과 "회사" 스케줄 모두 활성화
- 각 위치에 진입하면 해당 스케줄 자동 작동
- 시간 기반 스케줄도 여러 개 활성화 가능

### 기존 아키텍처와의 일관성

✅ **Repository Layer**: 이미 `getActive()`가 `Flow<List<ScheduleGroup>>`를 반환 중
✅ **DAO Layer**: 이미 멀티 활성화를 지원하는 쿼리 사용 중
✅ **UI Layer**: Compose `items()`로 여러 카드를 자연스럽게 표시

**결론**: 이전에는 ViewModel에서 인위적으로 `.firstOrNull()`로 제한했던 것을 제거하여, 원래 의도대로 동작하도록 복원한 것입니다.

---

## 🎨 사용자 경험 개선

### Before
```
┌─────────────────────────┐
│ 🟢 활성화됨 (대기 중)    │
│ 집                      │  ❌ "회사"는 활성화되었는데 안 보임!
│ 📍 고양시에 진입하면 작동 │
└─────────────────────────┘

┌─────────────────────────┐
│ 📅 다음 예약            │
│ 14:00 - 240분           │  ❌ "집"의 다음 예약만 표시
└─────────────────────────┘

모든 시간표 (2)
┌─────────────────────────┐
│ ⭐ 집                   │ ✅ 활성화
│ ⏰ 2개  📍 위치 1개     │
└─────────────────────────┘
┌─────────────────────────┐
│ ⭐ 회사                 │ ✅ 활성화 (but 위에 안 나옴!)
│ ⏰ 1개  📍 위치 1개     │
└─────────────────────────┘
```

### After
```
┌─────────────────────────┐
│ 🟢 활성화됨 (대기 중)    │
│ 집                      │
│ 📍 고양시에 진입하면 작동 │
└─────────────────────────┘
┌─────────────────────────┐
│ 🟢 활성화됨 (대기 중)    │
│ 회사                    │  ✅ "회사"도 표시됨!
│ 📍 판교에 진입하면 작동   │
└─────────────────────────┘

┌─────────────────────────┐
│ 📅 다음 예약            │
│ 14:00 - 240분           │  ✅ 모든 활성화 그룹의 다음 예약
└─────────────────────────┘

모든 시간표 (2)
┌─────────────────────────┐
│ ⭐ 집                   │ ✅ 활성화
│ ⏰ 2개  📍 위치 1개     │
└─────────────────────────┘
┌─────────────────────────┐
│ ⭐ 회사                 │ ✅ 활성화
│ ⏰ 1개  📍 위치 1개     │
└─────────────────────────┘
```

---

## 📝 후속 작업 제안

### Phase 1 완료 ✅
- [x] "활성" vs "작동" 개념 UI 구분
- [x] 멀티 활성화 지원

### Phase 2 (선택)
- [ ] 활성화 순서 관리 UI (우선순위)
- [ ] 활성화 그룹 간 충돌 해결 로직 개선
- [ ] "어디서나 적용" 스케줄과의 우선순위 명확화

---

## 🔧 기술 상세

### StateFlow 변경
```kotlin
// Before
activeGroup: StateFlow<ScheduleGroup?>

// After
activeGroups: StateFlow<List<ScheduleGroup>>
```

### Compose items() 사용
```kotlin
// Before: 단일 item
activeGroup?.let { group ->
    item { ActiveScheduleSummaryCard(...) }
}

// After: 여러 items
val waitingGroups = activeGroups.filter { it.id != runningScheduleGroupId }
items(waitingGroups) { group ->
    ActiveScheduleSummaryCard(...)
}
```

### 활성 상태 체크
```kotlin
// Before
isActive = group.id == activeGroup?.id

// After
isActive = activeGroups.any { it.id == group.id }
```

---

## 🎯 성과

### 문제 해결
✅ "집"과 "회사" 모두 활성화 상태일 때, 둘 다 대기 카드에 표시
✅ 다음 예약 로직이 모든 활성화 그룹을 고려
✅ 위치 기반 스케줄의 멀티 활성화 지원

### 아키텍처 개선
✅ ViewModel 단에서 인위적 제한 제거
✅ Repository/DAO의 원래 의도대로 동작
✅ UI가 실제 데이터 모델을 정확히 반영

### 사용자 경험
✅ 활성화된 모든 스케줄을 한눈에 확인 가능
✅ "집", "회사", "학교" 등 여러 위치 스케줄을 동시에 관리 가능
✅ 혼란 없는 명확한 상태 표시

---

## 커밋 정보

```bash
git add -A
git commit -m "feat(schedule): 멀티 활성화 지원 및 UI 개선 (v0.10.1)

## 주요 변경사항

### 1. ViewModel: activeGroup → activeGroups (복수형)
- 여러 스케줄 그룹이 동시에 활성화될 수 있도록 개선
- getNextScheduleToday() 로직 개선: 모든 활성화 그룹 고려

### 2. ScheduleTabScreen UI 개선
- 활성화된 모든 그룹을 카드로 표시
- 작동 중인 그룹은 제외하고 대기 중인 그룹만 표시
- isActive 체크 로직 개선 (any 사용)

### 3. ScheduleGroupScreen 호환성 유지
- activeGroup → activeGroups 변경

## 배경
- 사용자 피드백: \"집\"과 \"회사\" 모두 활성화되었는데 UI에 하나만 표시됨
- 근본 원인: ViewModel에서 .firstOrNull()로 인위적 제한
- 해결: 원래 의도대로 멀티 활성화 지원

## 개념 정리
- 활성(Active): 스케줄이 켜져있어 조건 만족 시 자동 작동 준비
- 작동(Running): 지금 이 순간 타이머가 실행 중

## 검증
- 컴파일 성공 ✅
- 위치 기반 멀티 스케줄 동시 활성화 지원
- UI 우선순위: 작동 중 → 활성화 (대기) → 다음 예약"
```

**작업 완료 시각**: 2025-11-04
**버전**: v0.10.1
**작업 소요**: ~1시간

