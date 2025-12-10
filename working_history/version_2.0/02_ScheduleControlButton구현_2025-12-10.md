# 작업 기록: ScheduleControlButton 컴포넌트 구현

**작업 일시**: 2025-12-10  
**작업 범위**: 스케줄 그룹 통합 제어 버튼 UI 컴포넌트 구현

## 작업 목적

스케줄 그룹의 활성/일시중지/비활성 상태를 표시하고 제어하는 통합 버튼 컴포넌트를 구현합니다.

### 해결하고자 하는 문제
- 기존 Switch 컴포넌트는 활성/비활성 2가지 상태만 표현
- 일시중지 기능을 위해 별도 화면 진입 필요
- 사용자가 직관적으로 3가지 상태를 제어할 수 없음

### 관련 PRD
- [04_버튼역할변경_prd.md](../../docs/04_버튼역할변경_prd.md)

## 변경 사항

### 1. ScheduleGroupControlState enum 생성

**파일**: `app/src/main/java/com/allday/detoxy/domain/model/ScheduleGroupControlState.kt`

```kotlin
enum class ScheduleGroupControlState {
    ACTIVE,    // 활성 (위치 감지 + 알람 트리거)
    PAUSED,    // 일시중지 (위치 감지 O, 알람 건너뛰기)
    INACTIVE;  // 비활성 (위치 감지 X, 알람 건너뛰기)

    companion object {
        fun fromEntity(manualOverrideState: String?, pauseUntil: Long?): ScheduleGroupControlState
    }
}
```

### 2. PauseDuration enum 생성

**파일**: `app/src/main/java/com/allday/detoxy/domain/model/PauseDuration.kt`

```kotlin
enum class PauseDuration(val displayName: String, private val durationMillis: Long) {
    ONE_HOUR("1시간", 3600_000L),
    TWO_HOURS("2시간", 7200_000L),
    TODAY("오늘 하루", -1L),
    TOMORROW("내일까지", -2L);

    fun calculatePauseUntil(): Long
}
```

### 3. ScheduleControlButton 컴포넌트 구현

**파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/autorun/components/ScheduleControlButton.kt`

#### 주요 기능
- **탭**: 상태 토글 (ACTIVE ↔ INACTIVE, PAUSED → ACTIVE)
- **롱프레스 또는 ▾ 탭**: 드롭다운 메뉴 표시
- **드롭다운 메뉴**: 활성화/일시중지 옵션(1시간, 2시간, 오늘하루, 내일까지)/비활성화

#### 상태별 UI
| 상태 | 배경색 | 아이콘 | 레이블 |
|------|-------|--------|--------|
| ACTIVE | primaryContainer | Check ✓ | 활성 |
| PAUSED | tertiaryContainer | DateRange | 일시중지 + 남은시간 |
| INACTIVE | surfaceVariant | Close ✗ | 비활성 |

#### 사용법
```kotlin
ScheduleControlButton(
    controlState = ScheduleGroupControlState.ACTIVE,
    pauseUntil = null,  // PAUSED일 때만 사용
    onStateChange = { newState -> },
    onPause = { duration -> }
)
```

### 4. formatRemainingTime 유틸 함수

```kotlin
fun formatRemainingTime(pauseUntil: Long): String {
    // "1시간 30분", "45분", "곧 해제" 등 반환
}
```

## 검증 방법

### 1. 컴파일 테스트

```bash
./gradlew compileDebugKotlin
```

**결과**: BUILD SUCCESSFUL ✅

### 2. Preview 확인

- `ScheduleControlButtonActivePreview`
- `ScheduleControlButtonPausedPreview`
- `ScheduleControlButtonInactivePreview`
- `ScheduleControlButtonAllStatesPreview`

## 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `ScheduleGroupControlState.kt` | 신규 생성 - 제어 상태 enum |
| `PauseDuration.kt` | 신규 생성 - 일시중지 기간 enum |
| `ScheduleControlButton.kt` | 신규 생성 - 통합 제어 버튼 컴포넌트 |

## 다음 단계 주의사항

### 단계 3 (ScheduleGroupCard 수정) 시 참고

1. **기존 Switch 제거 및 ScheduleControlButton 교체**
   - 헤더 영역의 Switch → ScheduleControlButton
   - 파라미터 변경: `isActive`, `onActivate` → `controlState`, `onStateChange`, `onPause`

2. **ScheduleGroupControlState 계산**
   - `ScheduleGroupControlState.fromEntity(group.manualOverrideState, group.pauseUntil)`
   - ViewModel에서 계산하여 전달 권장

3. **카드 배경색 연동**
   - ACTIVE: primaryContainer (기존과 동일)
   - PAUSED: tertiaryContainer
   - INACTIVE: surfaceVariant (기존 isActive=false와 동일)

## 교훈 및 참고사항

1. **Material Icons 제한**
   - `Icons.Default.Pause`, `Icons.Default.Schedule`, `Icons.Default.AccessTime` 등은 기본 아이콘 세트에 없음
   - 사용 가능한 아이콘: `Check`, `Close`, `DateRange`, `Edit`, `Delete`, `Star`, `Home`, `Place` 등
   - 와일드카드 import(`import androidx.compose.material.icons.filled.*`) 사용 권장

2. **remember 블록에서 Composable 호출 금지**
   - `remember` 계산 블록 내에서는 `@Composable` 함수 호출 불가
   - 해결: Composable 함수를 remember 외부에서 호출

3. **combinedClickable 사용법**
   - `@OptIn(ExperimentalFoundationApi::class)` 필요
   - `onClick`, `onLongClick` 파라미터로 탭/롱프레스 구분
   - 햅틱 피드백: `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`

---

**작업 완료일**: 2025-12-10  
**커밋 ID**: de0d549

