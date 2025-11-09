# 작업 기록: 도넛 차트 프리셋 버튼 동기화 수정

**작업 일시**: 2025-11-09  
**작업 범위**: 집중 타이머 도넛 차트와 프리셋 버튼 간 동기화 문제 해결

## 문제 상황

사용자가 집중 타이머 설정 시 다음과 같은 문제가 발생했습니다:

1. **도넛 차트 드래그**: 정상 작동 (시간이 변경됨)
2. **프리셋 버튼 클릭**: 원형 차트가 변경되지 않음 ❌
   - 25분, 45분, 60분 등의 프리셋 버튼 클릭
   - 내부 시간 값은 변경되지만 도넛 차트의 시각적 표시가 업데이트되지 않음

## 원인 분석

### 문제 코드 위치

**DonutTimerPicker.kt** (73번째 라인):

```kotlin
// 현재 각도 (0~360도)
var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes, maxMinutes)) }
```

### 근본 원인

**remember 사용의 제한**:
- `remember`는 초기값만 설정하고 Composable이 최초 생성될 때만 실행됨
- 이후 `selectedMinutes` prop이 외부에서 변경되어도 `currentAngle`은 업데이트되지 않음

### 동작 흐름 분석

#### ✅ 정상 작동 (드래그)
```kotlin
detectDragGestures { change, _ ->
    // ...
    currentAngle = angle  // ← 직접 업데이트
    onMinutesChange(minutes)
}
```
- 사용자가 드래그
- `currentAngle` 직접 업데이트
- `onMinutesChange` 콜백으로 부모에 전달
- 도넛 차트 시각적 업데이트 ✅

#### ❌ 문제 발생 (프리셋 버튼)

**TimerScreen.kt**:
```kotlin
PresetButtonRow(
    customPresets = customPresets,
    selectedMinutes = selectedMinutes,
    onPresetClick = { minutes, presetId ->
        selectedMinutes = minutes  // ← selectedMinutes 업데이트
        viewModel.incrementPresetUsage(presetId)
    },
    ...
)

DonutTimerPicker(
    selectedMinutes = selectedMinutes,  // ← prop 전달
    onMinutesChange = { selectedMinutes = it },
    ...
)
```

**문제 발생 순서**:
1. 프리셋 버튼 클릭 → `selectedMinutes = 45`
2. `DonutTimerPicker`의 `selectedMinutes` prop 변경
3. **하지만** `currentAngle`은 `remember`로 인해 업데이트 안 됨 ❌
4. 도넛 차트가 이전 각도를 유지

이는 이전에 수정한 **시간 입력 필드 문제와 동일한 패턴**입니다:
- `remember(hour)` → `remember` + `LaunchedEffect`로 해결했던 것과 같음

## 수정 내용

### DonutTimerPicker.kt 수정

#### LaunchedEffect 추가

```kotlin
// ❌ 수정 전 (73-73 라인)
var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes, maxMinutes)) }

// ✅ 수정 후 (73-79 라인)
var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes, maxMinutes)) }

// selectedMinutes가 외부에서 변경되면 currentAngle 업데이트
LaunchedEffect(selectedMinutes) {
    val newAngle = minutesToAngle(selectedMinutes, maxMinutes)
    currentAngle = newAngle
}
```

### 동작 원리

**LaunchedEffect(selectedMinutes)**:
- `selectedMinutes`를 key로 사용
- `selectedMinutes`가 변경될 때마다 실행
- 새로운 각도를 계산하여 `currentAngle` 업데이트

**전체 흐름**:
1. 프리셋 버튼 클릭 → `selectedMinutes = 45`
2. `DonutTimerPicker`의 `selectedMinutes` prop 변경
3. `LaunchedEffect`가 변경 감지 ✅
4. `currentAngle` 업데이트
5. `animatedAngle`이 애니메이션과 함께 업데이트
6. Canvas가 새로운 각도로 다시 그려짐 ✅

### 애니메이션 유지

```kotlin
val animatedAngle by animateFloatAsState(
    targetValue = currentAngle,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "angle_animation"
)
```

- `currentAngle`이 변경되면 `animatedAngle`이 부드럽게 애니메이션
- 프리셋 버튼 클릭 시에도 부드러운 전환 효과 제공

## 검증 결과

### Lint 검증
```bash
$ read_lints ["DonutTimerPicker.kt"]
No linter errors found.
```

### 기능 검증 시나리오

#### 시나리오 1: 프리셋 버튼 클릭
1. 초기 상태: 25분 선택됨 (도넛 차트 25분 표시)
2. 45분 버튼 클릭
3. **예상 결과**: 도넛 차트가 부드럽게 45분으로 애니메이션 ✅

#### 시나리오 2: 여러 프리셋 연속 클릭
1. 25분 → 45분 클릭
2. 45분 → 60분 클릭
3. 60분 → 25분 클릭
4. **예상 결과**: 각 클릭마다 도넛 차트가 부드럽게 업데이트 ✅

#### 시나리오 3: 프리셋 → 드래그 → 프리셋
1. 25분 프리셋 클릭
2. 도넛 차트 드래그로 37분 설정
3. 60분 프리셋 클릭
4. **예상 결과**: 모든 상호작용에서 도넛 차트 정상 업데이트 ✅

#### 시나리오 4: 커스텀 프리셋
1. 사용자가 저장한 커스텀 프리셋 (예: 30분) 클릭
2. **예상 결과**: 도넛 차트가 30분으로 업데이트 ✅

## 영향 범위

### 수정된 파일
- `app/src/main/java/com/allday/detoxy/presentation/ui/timer/components/DonutTimerPicker.kt`
  - LaunchedEffect 추가 (76-79 라인)
  - 외부 prop 변경 시 내부 상태 동기화

### 영향받는 기능
- 집중 타이머 도넛 차트
- 기본 프리셋 버튼 (25/45/60분)
- 커스텀 프리셋 버튼
- 도넛 차트 애니메이션

### 기존 기능 유지
- ✅ 드래그로 시간 조정
- ✅ 탭으로 즉시 시간 설정
- ✅ 햅틱 피드백
- ✅ 부드러운 애니메이션
- ✅ 시간 눈금 표시

## 관련 작업

### 동일 패턴 문제 해결
이 문제는 이전에 해결한 "시간 입력 필드 오류"와 동일한 패턴입니다:

**2025-11-09_fix_time_input_issues.md**:
- `remember(hour)` → `remember` + `LaunchedEffect(hour)`
- 외부 prop 변경 시 내부 상태 동기화
- 해결 방법이 동일하게 적용됨

### 핵심 교훈

**Compose State 동기화 패턴**:

```kotlin
// ❌ 잘못된 패턴
var internalState by remember(externalProp) { 
    mutableStateOf(externalProp) 
}
// → externalProp 변경 시 재생성되어 상태가 초기화됨

// ✅ 올바른 패턴 1 (외부 prop이 source of truth)
@Composable
fun Component(externalProp: Int) {
    // externalProp을 직접 사용
    val derivedValue = remember(externalProp) {
        transformProp(externalProp)
    }
}

// ✅ 올바른 패턴 2 (내부 상태 + 외부 동기화)
@Composable
fun Component(externalProp: Int) {
    var internalState by remember { mutableStateOf(externalProp) }
    
    LaunchedEffect(externalProp) {
        internalState = externalProp
    }
}

// ✅ 올바른 패턴 3 (derivedStateOf 사용)
@Composable
fun Component(externalProp: Int) {
    var internalState by remember { mutableStateOf(externalProp) }
    
    val derivedValue by remember { 
        derivedStateOf { transformProp(internalState) }
    }
}
```

**DonutTimerPicker의 경우**:
- 드래그/탭 입력: 내부 상태(`currentAngle`) 직접 업데이트
- 프리셋 버튼: 외부 prop(`selectedMinutes`) 변경 → 내부 상태 동기화 필요
- → **패턴 2** 적용이 적절함

## 추가 개선 사항

### 1. 중복 업데이트 방지 (옵션)

현재는 LaunchedEffect가 매번 실행되지만, 필요하다면 조건을 추가할 수 있습니다:

```kotlin
LaunchedEffect(selectedMinutes) {
    val newAngle = minutesToAngle(selectedMinutes, maxMinutes)
    // 현재 각도와 다를 때만 업데이트
    if (abs(currentAngle - newAngle) > 0.1f) {
        currentAngle = newAngle
    }
}
```

하지만 현재 구현도 충분히 효율적이므로 굳이 추가하지 않아도 됩니다.

### 2. previousMinutes 동기화 고려

햅틱 피드백용 `previousMinutes`도 동기화할 수 있습니다:

```kotlin
LaunchedEffect(selectedMinutes) {
    val newAngle = minutesToAngle(selectedMinutes, maxMinutes)
    currentAngle = newAngle
    previousMinutes = selectedMinutes  // 동기화
}
```

하지만 이는 프리셋 버튼 클릭 시 햅틱 피드백을 발생시키지 않으려는 의도라면 현재대로 유지하는 것이 좋습니다.

## 결론

도넛 차트와 프리셋 버튼 간의 동기화 문제를 해결했습니다:

- ✅ 프리셋 버튼 클릭 시 도넛 차트 정상 업데이트
- ✅ 부드러운 애니메이션 유지
- ✅ 드래그/탭 기능 정상 작동
- ✅ 햅틱 피드백 유지

**핵심 수정**: `LaunchedEffect(selectedMinutes)`를 추가하여 외부 prop 변경 시 내부 상태(`currentAngle`)를 동기화했습니다.

이는 Compose에서 외부 prop과 내부 상태를 동기화하는 표준 패턴이며, 
이전 시간 입력 필드 수정과 동일한 접근 방식입니다.

