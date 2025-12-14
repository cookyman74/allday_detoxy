# LockOverlayService 디버깅 가이드

## 🔍 오버레이가 표시되지 않을 때 확인 사항

### 1. 권한 확인
오버레이 권한이 부여되었는지 확인:
- 설정 → 앱 → Allday Detoxy → 특수 앱 접근권 → 다른 앱 위에 표시
- 또는 설정 → 앱 → 특별한 앱 접근 권한 → 다른 앱 위에 표시 → Allday Detoxy

### 2. 로그 모니터링
```bash
# 오버레이 관련 로그만 필터링
adb logcat | grep -E "LockOverlayService|FocusAccessibilityService"

# 또는 전체 앱 로그 확인
adb logcat | grep "com.allday.detoxy"
```

### 3. 로그 이모지 가이드
로그에서 다음 이모지들을 확인하여 문제를 진단:

| 이모지 | 의미 | 설명 |
|--------|------|------|
| 📞 | showOverlay() 호출 | 정적 메소드가 호출됨 |
| 🔐 | 권한 체크 | 오버레이 권한 상태 |
| ✅ | 성공 | 권한 확인 또는 작업 성공 |
| ❌ | 실패 | 권한 거부 또는 작업 실패 |
| 🚀 | 서비스 시작 | LockOverlayService 시작 |
| 🔔 | onStartCommand | 서비스 커맨드 수신 |
| 📍 | 액션 수신 | 특정 액션 처리 시작 |
| 📋 | 레이아웃 inflate | XML 레이아웃 로딩 |
| 📱 | View 찾기 | UI 컴포넌트 초기화 |
| 🕒 | 타이머 업데이트 | 시간 표시 업데이트 |
| 🪟 | WindowManager | 오버레이 추가 시도 |
| ⚠️ | 경고 | 비정상적인 상황 |

### 4. 예상 로그 순서 (정상 작동 시)

```
1. FocusAccessibilityService: ⚠️ BLOCKED APP DETECTED: com.android.chrome
2. FocusAccessibilityService: 🔒 Lock overlay display requested: X / Y seconds
3. FocusAccessibilityService: ✅ Navigated to home screen - blocked app closed
4. LockOverlayService: 📞 showOverlay() called - remainingSeconds: X, totalSeconds: Y
5. LockOverlayService: 🔐 Overlay permission check: ✅ GRANTED
6. LockOverlayService: 🚀 Starting LockOverlayService with ACTION_SHOW_OVERLAY
7. LockOverlayService: 🔔 onStartCommand() - action: ACTION_SHOW_OVERLAY
8. LockOverlayService: 📍 ACTION_SHOW_OVERLAY received
9. LockOverlayService: ✅ Overlay permission granted
10. LockOverlayService: 📋 Creating ComposeView with LockOverlayScreen...
11. LockOverlayService: ✅ ComposeView created with Lifecycle: overlayView = true
12. LockOverlayService: 🪟 WindowManager adding view...
13. LockOverlayService: ✅ Overlay shown successfully
14. LockOverlayService: 🕒 Timer update: X seconds remaining (매 5초마다)

### 5. 일반적인 문제와 해결 방법

#### 문제 1: 권한 거부
**로그**:
```
LockOverlayService: 🔐 Overlay permission check: ❌ DENIED
LockOverlayService: Cannot show overlay - permission not granted!
```
**해결**: 설정에서 "다른 앱 위에 표시" 권한 부여

#### 문제 2: ComposeView 생성 실패
**로그**:
```
LockOverlayService: ✅ ComposeView created: overlayView = false
```
**해결**:
- Jetpack Compose 의존성 확인
- 빌드 클린 후 재빌드: `./gradlew clean assembleDebug`

#### 문제 3: WindowManager null
**로그**:
```
LockOverlayService: 🪟 WindowManager adding view...
LockOverlayService:    - WindowManager: false
```
**해결**: 서비스 초기화 문제, 앱 재시작 필요

#### 문제 4: 서비스가 시작되지 않음
**로그 없음** 또는 `📞 showOverlay() called` 이후 아무 로그 없음
**해결**:
- AndroidManifest.xml에 서비스 선언 확인
- 포그라운드 서비스 권한 확인

#### 문제 5: 오버레이가 표시되지만 내용이 보이지 않음
**증상**: TimerScreen이 보이고 LockOverlayScreen이 표시되지 않음
**원인**: ComposeView에 Lifecycle이 설정되지 않음
**해결**:
```kotlin
ComposeView(context).apply {
    setViewTreeLifecycleOwner(this@LockOverlayService)  // 필수!
    setViewTreeSavedStateRegistryOwner(this@LockOverlayService)  // 필수!
    setContent { /* ... */ }
}
```

### 6. 테스트 시나리오

1. **타이머 시작 후 Chrome 실행**:
   - 타이머 시작 (25분/45분/60분)
   - Chrome 앱 실행
   - 오버레이가 표시되는지 확인

2. **권한 테스트**:
   - 오버레이 권한 비활성화
   - 타이머 시작 후 Chrome 실행
   - 로그에 권한 오류 메시지 확인
   - 권한 활성화 후 재시도

3. **포기 버튼 테스트**:
   - 오버레이 표시된 상태에서 포기 버튼 클릭
   - 타이머가 중지되고 메인 화면으로 이동하는지 확인

### 7. 추가 디버깅 명령어

```bash
# 실시간 로그 (오버레이 관련만)
adb logcat -c && adb logcat | grep -E "📞|🔐|✅|❌|🚀|🔔|📍|📋|📱|🕒|🪟|⚠️"

# 권한 상태 확인
adb shell dumpsys package com.allday.detoxy | grep -i permission

# 서비스 상태 확인
adb shell dumpsys activity services com.allday.detoxy

# 현재 윈도우 상태 확인
adb shell dumpsys window | grep -i overlay
```

### 8. 체크리스트

- [ ] 접근성 서비스 활성화됨
- [ ] 오버레이 권한 부여됨
- [ ] DND 권한 부여됨 (선택)
- [ ] 타이머가 실행 중임
- [ ] Chrome 패키지명이 차단 목록에 있음
- [ ] LockOverlayScreen.kt Compose UI가 존재함
- [ ] LockOverlayService가 AndroidManifest.xml에 선언됨
- [ ] MainActivity가 SINGLE_TOP 또는 SINGLE_TASK 런치 모드임

## 🛠 문제 해결 순서

1. 모든 권한이 부여되었는지 확인
2. 앱 완전히 종료 후 재시작
3. 로그 모니터링하며 타이머 시작 → Chrome 실행
4. 로그의 이모지를 따라가며 어느 단계에서 문제가 발생하는지 확인
5. 해당 단계의 해결 방법 적용

## 📝 버그 리포트 작성 시 포함할 정보

1. 문제 발생 단계 (타이머 시작 → Chrome 실행 → ?)
2. 권한 설정 상태 스크린샷
3. `adb logcat` 출력 (이모지 포함)
4. Android 버전 및 디바이스 모델
5. 빌드 시간 및 커밋 ID