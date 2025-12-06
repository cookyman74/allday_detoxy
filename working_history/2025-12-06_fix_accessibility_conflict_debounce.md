# 작업 기록: 접근성 서비스 충돌 해결 (쿨다운 로직 적용)

**작업 일시**: 2025-12-06
**작업 범위**: `FocusAccessibilityService` 안정화

## 문제 분석
- **증상**: 접근성 서비스(`FocusAccessibilityService`)가 빈번하게 충돌하여 시스템에 의해 비활성화됨.
- **원인 (로그 및 동작 분석)**:
    - 차단된 앱 실행 시, `AccessibilityEvent`가 연속적으로 발생(스크롤, 윈도우 변경 등).
    - 이로 인해 `handleBlockedApp`이 수십 밀리초 단위로 중복 호출됨.
    - 결과적으로 `LockOverlayService` 시작(`startForegroundService`)과 홈 화면 이동(`startActivity`) 인텐트가 폭주.
    - Android 시스템은 이를 "오동작(Malfunctioning)" 또는 "배터리 과다 소모"로 판단하여 서비스를 강제 종료 또는 비활성화.

## 수정 내용
### 1. `FocusAccessibilityService`에 쿨다운(Debounce) 로직 추가
- **파일**: `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`
- **변경**:
    - `BLOCK_COOLDOWN_MS = 1500L` (1.5초) 상수 추가.
    - `lastBlockTime` 변수를 통해 마지막 차단 시각 기록.
    - `handleBlockedApp` 진입 시 1.5초 이내 재호출이면 무시(`return`).

### 2. `LockOverlayService` 안정화 (이전 작업 확인)
- **파일**: `app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt`
- **확인**: 불필요한 `@AndroidEntryPoint` 제거 및 `ACTION_SHOW_SUCCESS` 관련 주석 처리가 정상적으로 적용되어 있음을 확인.

## 검증 계획
1. 타이머 실행 중 차단 앱(예: 인스타그램) 접속.
2. 오버레이가 뜨고 홈으로 튕겨 나가는지 확인.
3. 앱을 빠르게 다시 실행하여 "따닥"하고 진입 시도 시, 1.5초 내에는 추가 오버레이/인텐트가 발생하지 않는지(로그 확인) 검증.
4. 장시간(10분 이상) 반복 테스트 시 접근성 서비스가 유지되는지 확인.

## 참고
- 이번 수정은 "시스템 리소스 폭주 방지"에 초점을 맞추었습니다. 
- Xiaomi/Samsung 등 제조사별 백그라운드 제약(절전 모드 등)은 별도 시스템 설정 안내가 필요할 수 있습니다.

