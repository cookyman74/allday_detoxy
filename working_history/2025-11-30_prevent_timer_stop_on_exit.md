# 작업 기록: 앱 종료 시 집중 모드 유지 및 백버튼 처리

**작업 일시**: 2025-11-30
**작업 범위**: `MainActivity`, `TimerViewModel`

## 문제 상황

**증상**:
- 타이머 실행 중 뒤로가기 버튼을 누르거나 앱을 스와이프하여 종료하면 타이머가 함께 종료됨.
- 사용자는 타이머가 백그라운드에서 계속 실행되기를 원함.

**원인**:
1.  `TimerViewModel.onCleared()`에서 `resetTimer()`를 호출하여 ViewModel 파괴 시 타이머를 강제로 중지하고 있었음.
2.  `MainActivity`에서 뒤로가기 버튼에 대한 별도 처리가 없어, 기본 동작(Activity 종료)이 수행됨.

## 수정 내용

### 1. `TimerViewModel` 수정: 백그라운드 실행 유지
- **파일**: `app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt`
- **변경**: `onCleared()` 메서드 내의 `resetTimer()` 호출 제거.
- **효과**: 앱(Activity)이 종료되어 ViewModel이 파괴되더라도, Foreground Service로 실행 중인 `FocusTimerService`는 영향을 받지 않고 계속 실행됨.

```kotlin
override fun onCleared() {
    super.onCleared()
    
    // ViewModel 종료 시 타이머도 중지 -> 🔥 제거: 백그라운드 실행 유지를 위해 주석 처리
    // if (timerState.value == FocusState.RUNNING) {
    //     resetTimer()
    // }
}
```

### 2. `MainActivity` 수정: 종료 확인 대화상자 추가
- **파일**: `app/src/main/java/com/allday/detoxy/MainActivity.kt`
- **변경**:
    - `BackHandler`를 사용하여 타이머 실행 중 뒤로가기 이벤트를 가로챔.
    - "집중 모드 실행 중" 대화상자 표시.
    - "백그라운드 실행" 선택 시 `moveTaskToBack(true)`를 호출하여 앱을 종료하지 않고 백그라운드로 전환.

```kotlin
// 타이머가 실행 중일 때만 백버튼 가로채기
BackHandler(enabled = timerState == FocusState.RUNNING) {
    showExitDialog = true
}

// ... AlertDialog 구현 ...
// 확인 버튼 클릭 시: (context as? Activity)?.moveTaskToBack(true)
```

## 검증 결과

### 빌드 검증
```bash
./gradlew compileDebugKotlin
```
- **결과**: `BUILD SUCCESSFUL`

### 기대 효과
- 실수로 뒤로가기를 눌러 타이머가 꺼지는 현상 방지.
- 앱을 닫아도 타이머가 유지되므로 사용자 경험 향상.
- 명시적인 종료("포기하기" 버튼) 외에는 타이머가 계속 돌아가도록 보장.
