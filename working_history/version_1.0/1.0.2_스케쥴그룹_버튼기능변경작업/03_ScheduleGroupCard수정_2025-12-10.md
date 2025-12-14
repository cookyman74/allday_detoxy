# 작업 기록: ScheduleGroupCard 수정

**작업 일시**: 2025-12-10  
**작업 범위**: ScheduleGroupCard 컴포넌트 및 관련 코드 수정

## 작업 목적

기존 Switch 컴포넌트를 ScheduleControlButton으로 교체하고, 상태별 카드 배경색을 적용합니다.

### 해결하고자 하는 문제
- 기존 Switch는 활성/비활성 2가지 상태만 표현
- 일시중지 상태 표시 및 제어 불가
- 카드 배경색이 2가지 상태만 지원

### 관련 PRD
- [04_버튼역할변경_prd.md](../../docs/04_버튼역할변경_prd.md)

## 변경 사항

### 1. ScheduleGroupCard.kt 수정

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleGroupCard.kt`

#### 파라미터 변경

| 변경 전 | 변경 후 |
|--------|--------|
| `isActive: Boolean` | `controlState: ScheduleGroupControlState` |
| `onActivate: () -> Unit` | `pauseUntil: Long? = null` |
| - | `onStateChange: (ScheduleGroupControlState) -> Unit` |
| - | `onPause: (PauseDuration) -> Unit` |

#### 카드 배경색 상태별 적용

```kotlin
val cardBackgroundColor = when (controlState) {
    ScheduleGroupControlState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    ScheduleGroupControlState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer
    ScheduleGroupControlState.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant
}
```

#### Switch → ScheduleControlButton 교체

```kotlin
// 통합 제어 버튼 (v8)
ScheduleControlButton(
    controlState = controlState,
    pauseUntil = pauseUntil,
    onStateChange = onStateChange,
    onPause = onPause
)
```

### 2. ScheduleGroupScreen.kt 수정

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/ScheduleGroupScreen.kt`

#### controlState 계산 추가

```kotlin
val controlState = ScheduleGroupControlState.fromEntity(
    manualOverrideState = group.manualOverrideState,
    pauseUntil = group.pauseUntil
)
```

#### ScheduleGroupCard 호출 변경

```kotlin
ScheduleGroupCard(
    group = group,
    controlState = controlState,
    pauseUntil = group.pauseUntil,
    timeBasedAutoRuns = timeBasedAutoRunsList,
    linkedLocations = linkedLocationsList,
    onStateChange = { newState ->
        viewModel.changeControlState(group.id, newState)
    },
    onPause = { duration ->
        viewModel.pauseScheduleGroup(group.id, duration)
    },
    // ... 기존 콜백들
)
```

### 3. ScheduleGroupRepository 인터페이스 수정

**파일**: `app/src/main/java/com/allday/detoxy/domain/repository/ScheduleGroupRepository.kt`

```kotlin
// v8: 통합 제어
suspend fun updateManualOverride(groupId: String, overrideState: String?, pauseUntil: Long?)
```

### 4. ScheduleGroupRepositoryImpl 수정

**파일**: `app/src/main/java/com/allday/detoxy/data/repository/ScheduleGroupRepositoryImpl.kt`

```kotlin
override suspend fun updateManualOverride(groupId: String, overrideState: String?, pauseUntil: Long?) {
    scheduleGroupDao.updateManualOverride(groupId, overrideState, pauseUntil)
}
```

### 5. ScheduleGroupViewModel 수정

**파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/ScheduleGroupViewModel.kt`

#### 새 메서드 추가

```kotlin
// 상태 변경
fun changeControlState(groupId: String, newState: ScheduleGroupControlState)

// 일시중지
fun pauseScheduleGroup(groupId: String, duration: PauseDuration)
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
| `ScheduleGroupCard.kt` | 파라미터 변경, Switch→ScheduleControlButton, 배경색 상태별 적용 |
| `ScheduleGroupScreen.kt` | controlState 계산, 새 콜백 연결 |
| `ScheduleGroupRepository.kt` | `updateManualOverride` 메서드 추가 |
| `ScheduleGroupRepositoryImpl.kt` | `updateManualOverride` 구현 |
| `ScheduleGroupViewModel.kt` | `changeControlState`, `pauseScheduleGroup` 추가 |

## 다음 단계 주의사항

### 단계 4 (ViewModel/Repository 수정) 시 참고

단계 3에서 이미 ViewModel/Repository의 기본 메서드들을 추가했습니다.
단계 4에서는 추가 로직 검토 및 일시중지 만료 체크 로직을 구현해야 합니다.

1. **일시중지 만료 자동 해제**
   - 앱 시작 시 또는 백그라운드에서 `clearExpiredPauses()` 호출 권장
   - DAO에 이미 쿼리가 추가되어 있음

2. **Geofence 연동**
   - INACTIVE 상태에서 Geofence 해제 로직 (단계 5에서 구현)
   - PAUSED 상태에서 알람 건너뛰기 로직 (단계 5, 6에서 구현)

## 교훈 및 참고사항

1. **파라미터 변경 시 호출부 동시 수정 필수**
   - ScheduleGroupCard 파라미터 변경 시 ScheduleGroupScreen도 함께 수정
   - 컴파일 에러로 미리 발견 가능

2. **Repository 계층 구조**
   - Interface(domain) → Impl(data) → DAO(data/local)
   - 3개 파일 모두 수정 필요

3. **ViewModel에서 상태 계산 vs UI에서 계산**
   - 현재는 UI(Screen)에서 `fromEntity()` 호출
   - 향후 StateFlow로 변환하여 ViewModel에서 제공하는 것이 더 효율적

---

**작업 완료일**: 2025-12-10  
**커밋 ID**: be6de89

