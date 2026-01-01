# Phase 2 ViewModel 통합 - 작업 완료 보고서

> **작업일**: 2026-01-02
> **Phase**: Phase 2 - ViewModel 통합
> **상태**: ✅ 완료

---

## 📋 작업 요약

| 항목 | 내용 |
|------|------|
| 목표 | ScheduleGroupViewModel에 히트맵 StateFlow 추가 및 갱신 트리거 구현 |
| 결과 | ✅ 빌드 성공 |
| 소요 시간 | 약 20분 |

---

## 📁 수정된 파일

### 1. ScheduleGroupViewModel.kt
- **경로**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`
- **추가된 기능**:
  - `_selectedGroupIdForHeatmap: MutableStateFlow<String?>` - 선택된 그룹 ID
  - `selectedGroupHeatmap: StateFlow<WeeklyHeatmapUiModel>` - 히트맵 데이터 (기존 `_linkedTimeBasedAutoRuns` 활용)
  - `selectGroupForHeatmap(groupId)` - 그룹 선택 시 히트맵 계산
  - `refreshHeatmapData(groupId)` - 시간대 변경 후 히트맵 갱신
  - `clearHeatmapSelection()` - 히트맵 선택 해제

### 2. TimeBasedAutoRunViewModel.kt
- **경로**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimeBasedAutoRunViewModel.kt`
- **추가된 기능**:
  - `timeSlotUpdated: SharedFlow<String>` - 시간대 CRUD 후 groupId 이벤트 발행
  - `addAutoRun()`, `updateAutoRun()`, `deleteAutoRun()`, `toggleAutoRun()` 메서드에서 이벤트 emit

---

## 🔧 구현 세부사항

### ScheduleGroupViewModel 히트맵 통합

```kotlin
// v1.1: 히트맵 지원
private val _selectedGroupIdForHeatmap = MutableStateFlow<String?>(null)

val selectedGroupHeatmap: StateFlow<WeeklyHeatmapUiModel> = combine(
    _selectedGroupIdForHeatmap,
    _linkedTimeBasedAutoRuns  // 기존 StateFlow 활용
) { groupId, autoRunsMap ->
    if (groupId == null) {
        WeeklyHeatmapUiModel.EMPTY
    } else {
        val autoRuns = autoRunsMap[groupId] ?: emptyList()
        WeeklyHeatmapCalculator.calculate(autoRuns)
    }
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Lazily,
    initialValue = WeeklyHeatmapUiModel.EMPTY
)
```

### TimeBasedAutoRunViewModel 갱신 트리거

**방식 선택: SharedFlow 이벤트 (Option A)**

```kotlin
// v1.1: 히트맵 갱신 이벤트
private val _timeSlotUpdated = MutableSharedFlow<String>()
val timeSlotUpdated: SharedFlow<String> = _timeSlotUpdated.asSharedFlow()

// 각 CRUD 메서드에서 emit
autoRun.scheduleGroupId?.let { groupId ->
    _timeSlotUpdated.emit(groupId)
}
```

**UI에서 구독 방법** (Phase 3에서 구현):
```kotlin
// ScheduleGroupScreen.kt
LaunchedEffect(Unit) {
    timeBasedViewModel.timeSlotUpdated.collect { groupId ->
        scheduleGroupViewModel.refreshHeatmapData(groupId)
    }
}
```

---

## ✅ PRD 구현 검증

| todolist 항목 | 구현 상태 |
|---------------|----------|
| TASK-001: ScheduleGroupViewModel 확장 | ✅ 완료 |
| TASK-002: 히트맵 갱신 트리거 (Option A) | ✅ 완료 |

---

## 📊 빌드 결과

```
./gradlew :app:compileDebugKotlin

BUILD SUCCESSFUL in 3s
```

---

## 🔜 다음 단계

- **Phase 3**: UI 컴포넌트 구현
  - `HeatmapCell.kt` - 히트맵 셀 Composable
  - `HeatmapRow.kt` - 히트맵 행 Composable (Row 단위 탭 가능)
  - `WeeklyHeatmap.kt` - 히트맵 전체 Composable
  - `HeatmapDetailSheet.kt` - 행 탭 시 상세 정보 BottomSheet

---

## 📝 리뷰 피드백 반영 (2026-01-02)

### 발견된 이슈

| 우선순위 | 이슈 | 원인 |
|----------|------|------|
| Medium | emit() 무한 대기 가능 | `MutableSharedFlow()`에 버퍼 없음 |

### 수정 내용

**backpressure 방지 설정 추가**
```kotlin
private val _timeSlotUpdated = MutableSharedFlow<String>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
```

- `extraBufferCapacity = 1`: collector 없어도 1개 이벤트 버퍼링
- `DROP_OLDEST`: 버퍼 초과 시 오래된 이벤트 삭제
