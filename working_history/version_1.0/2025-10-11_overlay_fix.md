# 작업 기록 2025-10-11 - Overlay 화면 표시 문제 해결

## 작업 정보
- **작업 번호**: 오버레이 긴급 수정
- **작업명**: LockOverlayScreen 표시 문제 해결 및 권한 안내 기능 추가
- **작업 일시**: 2025-10-11
- **작업 상태**: ✅ 완료
- **작업 시간**: 약 1시간

## 작업 개요
Chrome 실행 시 오버레이 화면이 표시되지 않는 문제를 디버깅하고 해결했습니다. 문제는 크게 3가지였습니다:
1. MainActivity가 foreground로 와서 오버레이가 뒤에 숨겨짐
2. ComposeView에 Lifecycle이 설정되지 않음
3. 오버레이 권한 체크 필요

또한 사용자 편의를 위해 앱 설치 후 권한 안내 화면을 자동으로 제시하는 기능의 구현 계획을 수립했습니다.

## 완료된 작업

### 1. 오버레이 화면 표시 문제 해결

#### 1.1 Compose UI 통합 문제 (XML → Compose)
**문제**: LockOverlayService가 XML 레이아웃을 찾으려고 했으나 실제 UI는 Compose 기반

**해결**:
```kotlin
// Before (XML 방식 - 잘못됨)
val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
overlayView = inflater.inflate(R.layout.lock_overlay_layout, null)

// After (Compose 방식 - 올바름)
overlayView = ComposeView(this).apply {
    setContent {
        DetoxyTheme {
            LockOverlayScreen(...)
        }
    }
}
```

#### 1.2 ComposeView Lifecycle 누락
**문제**: ComposeView에 Lifecycle이 설정되지 않아 UI가 렌더링되지 않음

**해결**:
```kotlin
overlayView = ComposeView(this).apply {
    // CRITICAL: Lifecycle 설정 (필수!)
    setViewTreeLifecycleOwner(this@LockOverlayService)
    setViewTreeSavedStateRegistryOwner(this@LockOverlayService)
    
    // 명시적 크기 설정
    layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    
    setContent { /* ... */ }
}
```

**필수 import 추가**:
```kotlin
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
```

#### 1.3 홈 화면 이동 전략 변경
**문제**: MainActivity를 foreground로 가져오면 오버레이가 Activity 뒤에 숨겨짐

**해결**: 홈 화면으로 이동하는 원래 전략으로 복귀
```kotlin
// FocusAccessibilityService.kt
private fun navigateToHome() {
    // 1. 먼저 LockOverlayScreen 표시
    LockOverlayService.showOverlay(...)
    
    // 2. 홈 화면으로 이동 (차단된 앱 종료)
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(homeIntent)
}
```

#### 1.4 WindowManager 플래그 조정
**개선**:
```kotlin
// FLAG_NOT_FOCUSABLE 제거: 오버레이가 포커스를 받아 최상위에 표시
// FLAG_NOT_TOUCH_MODAL 제거: 오버레이 밖의 터치를 차단
// FLAG_FULLSCREEN 추가: 전체 화면 모드
WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
    WindowManager.LayoutParams.FLAG_FULLSCREEN or
    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
```

### 2. 권한 문제 발견 및 해결

#### 2.1 오버레이 권한 미부여 확인
**로그 분석**:
```
LockOverlayService: 🔐 Overlay permission check: ❌ DENIED
❌ Overlay permission not granted! Cannot show overlay.
```

**해결**: 사용자에게 권한 부여 안내
```bash
adb shell am start -a android.settings.action.MANAGE_OVERLAY_PERMISSION \
    -d package:com.allday.detoxy
```

#### 2.2 권한 체크 강화
```kotlin
// LockOverlayService.kt - showOverlay() 호출 전
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val hasPermission = android.provider.Settings.canDrawOverlays(context)
    Log.d(TAG, "🔐 Overlay permission check: ${if (hasPermission) "✅ GRANTED" else "❌ DENIED"}")
    if (!hasPermission) {
        Log.e(TAG, "Cannot show overlay - permission not granted!")
        return
    }
}
```

### 3. 권한 안내 기능 구현 계획 수립

#### 3.1 MVP 개발 계획 업데이트
**추가된 섹션**: Week 1.4 권한 안내 및 초기 설정 (Day 5-6)

**주요 구성요소**:
1. **PermissionCheckScreen**: 권한 상태 추적 및 자동 화면 전환
2. **PermissionCard**: 재사용 가능한 권한 안내 카드 컴포넌트
3. **권한별 상세 안내**: 접근성, 오버레이, DND
4. **권한 재확인 메커니즘**: onResume 시 자동 권한 상태 갱신
5. **PreferenceManager**: SharedPreferences로 초기 실행 여부 관리
6. **권한 미부여 시 대응**: 타이머 시작 전 권한 재확인
7. **설정 화면 통합**: 언제든 권한 관리 가능

#### 3.2 UX 플로우
```
앱 최초 실행
  ↓
isFirstLaunch() == true?
  ↓ Yes
PermissionCheckScreen 표시
  ├─ ❌ 접근성 서비스 → 설정 화면으로
  ├─ ❌ 오버레이 권한 → 설정 화면으로
  └─ (선택) DND 권한 → 설정 화면으로
  ↓
onResume 시 권한 재확인
  ↓
모든 필수 권한 허용됨?
  ↓ Yes
setFirstLaunchCompleted()
  ↓
TimerScreen으로 자동 이동
```

#### 3.3 주요 설계 결정
- **필수 권한**: 접근성 서비스, 오버레이 권한
- **선택 권한**: DND 권한 (사용자가 부담 없이 건너뛸 수 있음)
- **자동 전환**: 모든 필수 권한 부여 시 자동으로 메인 화면 이동
- **재확인**: 설정 화면에서 돌아올 때마다 권한 상태 갱신
- **UX 개선**: 사용자 친화적 용어 사용 ("접근성 서비스" → "앱 차단 기능")

## 기술적 세부사항

### LockOverlayService.kt 주요 변경

#### import 추가
```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.allday.detoxy.R
import com.allday.detoxy.presentation.ui.overlay.LockOverlayScreen
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
```

#### State 관리 변경
```kotlin
// Before
private var timerTextView: TextView? = null
private var progressBar: ProgressBar? = null
private var currentRemainingSeconds = 0

// After
private val timerState = mutableStateOf(0)  // Compose State
private var currentTotalSeconds = 0
```

#### ComposeView 생성 (최종 버전)
```kotlin
overlayView = ComposeView(this).apply {
    setViewTreeLifecycleOwner(this@LockOverlayService)
    setViewTreeSavedStateRegistryOwner(this@LockOverlayService)
    
    layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    
    setContent {
        DetoxyTheme {
            LockOverlayScreen(
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                timerState = timerState,  // mutableStateOf 전달
                onGiveUp = {
                    Log.d(TAG, "🎯 Give up button clicked from LockOverlayScreen")
                    val intent = Intent(this@LockOverlayService, LockOverlayService::class.java).apply {
                        action = ACTION_GIVE_UP
                    }
                    startService(intent)
                }
            )
        }
    }
}
```

#### 타이머 업데이트 (Reactive)
```kotlin
private fun startTimerUpdate() {
    timerJob?.cancel()
    timerJob = lifecycleScope.launch {
        while (isActive && timerState.value > 0) {
            delay(1000)
            withContext(Dispatchers.Main) {
                timerState.value--  // Compose가 자동 recompose
                
                if (timerState.value % 5 == 0 || timerState.value <= 5) {
                    Log.d(TAG, "🕒 Timer update: ${timerState.value} seconds remaining")
                }
            }
        }
    }
}
```

### FocusAccessibilityService.kt 변경

#### navigateToHome() 수정
```kotlin
private fun navigateToHome() {
    // 1. 먼저 LockOverlayScreen 표시
    LockOverlayService.showOverlay(
        context = applicationContext,
        remainingSeconds = remainingSeconds,
        totalSeconds = totalSeconds
    )
    Log.d(TAG, "🔒 Lock overlay display requested: $remainingSeconds / $totalSeconds seconds")

    // 2. 홈 화면으로 이동 (차단된 앱 종료)
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        startActivity(homeIntent)
        Log.d(TAG, "✅ Navigated to home screen - blocked app closed")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to navigate to home: ${e.message}", e)
    }
}
```

## 빌드 및 테스트

### 빌드 결과
```bash
./gradlew clean assembleDebug
BUILD SUCCESSFUL in 14s
40 actionable tasks: 40 executed

adb install -r app/build/outputs/apk/debug/app-debug.apk
Success
```

### 테스트 결과
**로그 출력** (정상 작동):
```
FocusAccessibilityService: ⚠️ BLOCKED APP DETECTED: com.android.chrome
FocusAccessibilityService: 🔒 Lock overlay display requested: 1496 / 1500 seconds
FocusAccessibilityService: ✅ Navigated to home screen - blocked app closed
LockOverlayService: 📞 showOverlay() called - remainingSeconds: 1496, totalSeconds: 1500
LockOverlayService: 🔐 Overlay permission check: ✅ GRANTED
LockOverlayService: 🚀 Starting LockOverlayService with ACTION_SHOW_OVERLAY
LockOverlayService: 📋 Creating ComposeView with LockOverlayScreen...
LockOverlayService: ✅ ComposeView created with Lifecycle: overlayView = true
LockOverlayService: 🪟 WindowManager adding view...
LockOverlayService: ✅ Overlay shown successfully: 1496 / 1500 seconds
LockOverlayService: 🕒 Timer update: 1495 seconds remaining
LockOverlayService: 🎯 Give up button clicked from LockOverlayScreen
LockOverlayService: 📍 ACTION_GIVE_UP received
```

**확인 사항**:
- ✅ Chrome 실행 → 즉시 닫힘
- ✅ 홈 화면으로 이동
- ✅ **LockOverlayScreen이 홈 화면 위에 전체 화면으로 표시**
- ✅ 타이머 실시간 카운트다운 (매 1초)
- ✅ 포기 버튼 정상 작동

## 생성/수정된 파일 목록

**수정된 파일**:
1. `app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt`
   - XML → Compose 변경
   - Lifecycle 설정 추가
   - mutableStateOf 기반 타이머 업데이트
   - WindowManager 플래그 조정

2. `app/src/main/java/com/allday/detoxy/service/accessibility/FocusAccessibilityService.kt`
   - MainActivity 대신 홈 화면 이동
   - 오버레이 표시 순서 조정

3. `OVERLAY_DEBUG.md`
   - 로그 순서 업데이트
   - ComposeView 관련 디버깅 정보 추가
   - Lifecycle 설정 문제 해결 방법 추가

4. `CLAUDE.md`
   - Overlay Service Architecture 섹션 업데이트
   - Lifecycle 설정 필수 항목 명시
   - WindowManager 플래그 설명 업데이트

5. `docs/00_mvp_allday_detoxy_todolist.md`
   - Week 1.4 "권한 안내 및 초기 설정" 섹션 추가
   - 7개 하위 작업 항목 정의
   - Week 4.1.2 권한 안내 화면과 연결

## 향후 작업

### 즉시 구현 예정 (Week 1.4)
- [ ] PermissionCheckScreen Compose UI 구현
- [ ] PermissionCard 재사용 컴포넌트 구현
- [ ] PreferenceManager 유틸리티 클래스 생성
- [ ] MainActivity에서 초기 화면 분기 로직 추가
- [ ] TimerViewModel에 권한 체크 로직 추가

### MVP 후속 작업 (Week 3)
- [ ] 설정 화면에 권한 관리 섹션 추가
- [ ] 권한 미부여 시 스낵바/다이얼로그 안내
- [ ] 배터리 최적화 제외 요청

## 주요 교훈 및 문서화

### 1. ComposeView in Service
**핵심**: ComposeView를 Service에서 사용할 때는 반드시 Lifecycle 설정 필요
```kotlin
composeView.setViewTreeLifecycleOwner(lifecycleOwner)
composeView.setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
```

### 2. WindowManager Overlay
**전략**: 같은 앱의 Activity 위에 표시하기보다 홈 화면 위에 표시하는 것이 안전
- TYPE_APPLICATION_OVERLAY는 다른 앱 위에는 잘 표시되지만 같은 앱의 Activity 위에서는 불안정

### 3. 권한 UX
**원칙**: 
- 필수/선택 권한 명확히 구분
- 사용자 친화적 용어 사용
- 자동 화면 전환으로 부드러운 경험 제공
- 설정 화면에서 돌아올 때 자동으로 권한 재확인

## 참조 문서
- [OVERLAY_DEBUG.md](../OVERLAY_DEBUG.md) - 오버레이 디버깅 가이드
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 아키텍처 문서
- [00_mvp_allday_detoxy_todolist.md](../docs/00_mvp_allday_detoxy_todolist.md) - MVP 개발 계획
- [2025-10-06_2.1.md](./2025-10-06_2.1.md) - 오버레이 잠금 화면 초기 구현

---

**작업 완료자**: Claude (AI Assistant)
**작업 승인자**: Jung Ho Jang
**마지막 업데이트**: 2025-10-11
**커밋 해시** : ffd32fa6d53b2bb13491097ab4e1f129533baec3

