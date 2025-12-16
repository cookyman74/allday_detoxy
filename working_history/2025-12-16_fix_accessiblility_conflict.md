# 작업 기록: 접근성 서비스 오류 수정 (LockOverlayService 최적화)

**작업 일시**: 2025-12-16
**작업 범위**: `LockOverlayService` 최적화

## 문제 분석
- **증상**: 접근성 서비스(`FocusAccessibilityService`)가 간헐적으로 오류 발생하여 시스템에 의해 비활성화됨.
- **원인 (로그 및 코드 분석)**:
    - `LockOverlayService.showOverlay()` 호출 시 기존 오버레이를 **매번 제거 후 재생성** (Window Churning)
    - `WindowManager`에 View를 반복적으로 추가/제거하여 시스템 부하 발생
    - `AccessibilityWindowsPopulator` 및 `AccessibilityManagerService`에서 `wait for adding window timeout` 경고 다수 발생
    - 시스템이 이를 "오동작(Malfunctioning)"으로 판단하여 서비스 비활성화

## 수정 내용
### 1. `LockOverlayService`에 View 재사용 로직 추가 (v0.10.3)
- **파일**: `app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt`
- **변경**:
    - `showOverlay()` 호출 시 기존 오버레이가 표시 중이면 **View를 재생성하지 않고 재사용**
    - 타이머 텍스트와 진행률 UI만 업데이트하여 Window Churning 방지

```kotlin
// 변경 전
if (isOverlayShowing) {
    hideOverlay()  // ❌ View 제거
}
// ... View 재생성

// 변경 후
if (isOverlayShowing && overlayView != null) {
    Log.d(TAG, "♻️ Reusing existing overlay")
    currentRemainingSeconds = remainingSeconds
    updateTimerDisplay()  // ✅ UI만 업데이트
    return
}
```

### 2. 타이머 동기화 로직 개선 (v0.10.3.1)
- **변경**:
    - 외부에서 시간이 전달될 때마다 기존 타이머를 **취소 후 재시작**
    - 외부 소스와 내부 카운트다운 간 불일치(드리프트) 방지

```kotlin
// 변경 전: 조건부 재시작
if (timerJob == null || timerJob?.isActive == false) {
    startTimerUpdate()
}

// 변경 후: 항상 동기화
timerJob?.cancel()
startTimerUpdate()
Log.d(TAG, "🔄 Timer restarted with synced time: $remainingSeconds seconds")
```

## 검증 계획
1. 타이머 실행 중 차단 앱(예: 인스타그램) 접속.
2. 오버레이가 뜨고 홈으로 튕겨 나가는지 확인.
3. 앱을 빠르게 다시 실행 시 오버레이가 깜빡이지 않고 부드럽게 업데이트되는지 확인.
4. 로그에서 "♻️ Reusing existing overlay" 및 "🔄 Timer restarted with synced time" 확인.
5. 장시간(10분 이상) 반복 테스트 시 접근성 서비스가 비활성화되지 않는지 확인.

## 참고
- 이전 수정(`2025-12-06_fix_accessibility_conflict_debounce.md`)에서는 `handleBlockedApp`에 쿨다운 로직을 적용하여 중복 호출 방지함.
- 이번 수정은 쿨다운을 통과한 호출에서도 발생할 수 있는 **Window Churning 문제**의 근본 원인을 해결함.
