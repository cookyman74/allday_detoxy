# 안드로이드 개발환경 구축 계획서

## 📋 프로젝트 개요

**프로젝트명**: Allday Detoxy - 스마트폰 습관 교정 코치 앱  
**기술 스택**: Kotlin 네이티브 + Jetpack Compose  
**목표**: 접근성 서비스 기반 앱 차단, SMS 자동응답, 게이미피케이션을 통한 습관 교정

---

## 🎯 기술 스택 선택 이유

### Flutter vs Kotlin 네이티브 비교 결과

| 핵심 기능 | Flutter | Kotlin 네이티브 | 선택 |
|-----------|---------|-----------------|------|
| 접근성 서비스 | ❌ 제한적 | ✅ 완전 제어 | **Kotlin** |
| 앱 차단/모니터링 | ❌ 불가능 | ✅ AccessibilityService | **Kotlin** |
| SMS 자동응답 | ❌ 매우 제한적 | ✅ 기본 SMS 앱 등록 | **Kotlin** |
| 오버레이 잠금 | ⚠️ 복잡 | ✅ 직접 구현 | **Kotlin** |
| DND 제어 | ⚠️ 플러그인 의존 | ✅ 직접 API | **Kotlin** |
| 배터리 최적화 | ❌ 제한적 | ✅ 완전 제어 | **Kotlin** |
| 성능 | ⚠️ 중간 | ✅ 최고 | **Kotlin** |

**결론**: PRD의 핵심 요구사항(앱 차단, SMS 자동응답)을 완전히 구현하려면 **Kotlin 네이티브가 필수**

---

## 🛠️ 개발환경 구축 계획

### 1단계: 기본 개발환경 설정 (1일)

#### 1.1 Android Studio 설치 및 설정
```bash
# Android Studio 최신 버전 설치
# SDK Manager에서 다음 설치:
- Android SDK 34 (API Level 34)
- Android SDK Build-Tools 34.0.0
- Android Emulator
- Android SDK Platform-Tools
```

#### 1.2 프로젝트 초기 설정
```bash
# 프로젝트 생성
mkdir allday_detoxy_android
cd allday_detoxy_android

# Git 초기화
git init
git remote add origin [repository_url]
```

#### 1.3 필수 권한 및 의존성 설정
```kotlin
// build.gradle.kts (Module: app)
dependencies {
    implementation "androidx.compose.ui:ui:$compose_version"
    implementation "androidx.compose.material3:material3:$material3_version"
    implementation "androidx.activity:activity-compose:$activity_compose_version"
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version"
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version"
}
```

### 2단계: 핵심 권한 및 서비스 구현 (3-4일)

#### 2.1 AndroidManifest.xml 권한 설정
```xml
<!-- 필수 권한들 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

#### 2.2 AccessibilityService 구현
```kotlin
// FocusAccessibilityService.kt
class FocusAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 앱 사용 모니터링 및 차단 로직
    }
    
    override fun onInterrupt() {
        // 서비스 중단 처리
    }
    
    fun blockApp(packageName: String) {
        // 특정 앱 차단 로직
    }
}
```

#### 2.3 SMS 자동응답 서비스
```kotlin
// SmsAutoReplyService.kt
class SmsAutoReplyService : Service() {
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // SMS 수신 시 자동응답 전송
        }
    }
}
```

#### 2.4 오버레이 잠금 화면
```kotlin
// LockOverlayService.kt
class LockOverlayService : Service() {
    private var overlayView: View? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }
    
    private fun showOverlay() {
        // 전체 화면 오버레이 표시
    }
}
```

### 3단계: 데이터베이스 및 비즈니스 로직 (2-3일)

#### 3.1 Room 데이터베이스 설정
```kotlin
// 데이터 모델들
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long?,
    val duration: Int,
    val success: Boolean,
    val breakReason: String?
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val baseAllowedMin: Int,
    val whitelistContacts: String, // JSON
    val blockCategories: String, // JSON
    val bonusPer60Min: Int
)
```

#### 3.2 Repository 패턴 구현
```kotlin
// FocusRepository.kt
class FocusRepository(
    private val focusDao: FocusDao,
    private val settingsDao: SettingsDao
) {
    suspend fun startFocusSession(duration: Int): Result<FocusSession>
    suspend fun endFocusSession(sessionId: String, success: Boolean): Result<Unit>
    suspend fun getUserSettings(): UserSettings
}
```

### 4단계: UI 구현 (Jetpack Compose) (3-4일)

#### 4.1 메인 화면 구성
```kotlin
// MainActivity.kt
@Composable
fun MainScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState.screen) {
        ScreenType.ONBOARDING -> OnboardingScreen()
        ScreenType.TIMER -> TimerScreen()
        ScreenType.REPORT -> ReportScreen()
        ScreenType.REWARDS -> RewardsScreen()
    }
}
```

#### 4.2 타이머 화면
```kotlin
@Composable
fun TimerScreen(
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit
) {
    // 원형 타이머, 프리셋 버튼들, 미션 해제 버튼
}
```

### 5단계: 게이미피케이션 시스템 (2-3일)

#### 5.1 포인트 및 스릭 시스템
```kotlin
// GamificationManager.kt
class GamificationManager {
    fun calculatePoints(session: FocusSession): Int
    fun updateStreak(success: Boolean): Int
    fun checkBadges(session: FocusSession): List<Badge>
    fun generateDailyQuest(): Quest
}
```

#### 5.2 보상 상점
```kotlin
@Composable
fun RewardsScreen() {
    // 포인트 잔액, 상품 목록, 구매 기능
}
```

### 6단계: 테스트 및 최적화 (2-3일)

#### 6.1 단위 테스트
```kotlin
// FocusRepositoryTest.kt
@Test
fun `포커스 세션 시작 시 올바른 데이터 저장`() {
    // 테스트 로직
}
```

#### 6.2 UI 테스트
```kotlin
// TimerScreenTest.kt
@Test
fun `타이머 시작 버튼 클릭 시 타이머 시작`() {
    // UI 테스트 로직
}
```

---

## 📱 필수 기능 구현 우선순위

### Phase 1 (MVP - 2주)
1. ✅ 기본 타이머 기능
2. ✅ AccessibilityService 기반 앱 모니터링
3. ✅ 오버레이 잠금 화면
4. ✅ DND 제어
5. ✅ 기본 리포트

### Phase 2 (핵심 기능 - 1주)
1. ✅ SMS 자동응답 (기본 SMS 앱 등록)
2. ✅ 포인트/스릭 시스템
3. ✅ 퀘스트 시스템
4. ✅ 보상 상점

### Phase 3 (고도화 - 1주)
1. ✅ 위치 기반 자동화
2. ✅ AI 코치 기능
3. ✅ 보호자 모드
4. ✅ 실물 리워드 연동

---

## 🔧 개발 도구 및 라이브러리

### 필수 라이브러리
```kotlin
// build.gradle.kts
dependencies {
    // UI
    implementation "androidx.compose.ui:ui:$compose_version"
    implementation "androidx.compose.material3:material3:$material3_version"
    implementation "androidx.activity:activity-compose:$activity_compose_version"
    
    // 아키텍처
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version"
    implementation "androidx.navigation:navigation-compose:$nav_version"
    implementation "com.google.dagger:hilt-android:$hilt_version"
    
    // 데이터
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version"
    
    // 유틸리티
    implementation "com.jakewharton.timber:timber:$timber_version"
    implementation "androidx.datastore:datastore-preferences:$datastore_version"
    
    // 차트
    implementation "com.github.PhilJay:MPAndroidChart:$mpchart_version"
}
```

### 개발 도구
- **Android Studio**: 최신 버전
- **Kotlin**: 1.9.0+
- **Gradle**: 8.0+
- **Git**: 버전 관리
- **Firebase**: 분석 및 크래시 리포팅

---

## 🚨 주의사항 및 제약사항

### Android 정책 준수
1. **접근성 서비스**: 사용자에게 명확한 목적 설명 필요
2. **오버레이 권한**: 사용자 수동 승인 필수
3. **SMS 권한**: 기본 SMS 앱 등록 시 개인정보 고지
4. **배터리 최적화**: 사용자에게 예외 설정 안내

### 성능 최적화
1. **포그라운드 서비스**: 백그라운드에서 지속적 실행
2. **배터리 사용량**: 최소화를 위한 효율적 구현
3. **메모리 관리**: 오버레이 뷰의 메모리 누수 방지

---

## 📅 개발 일정 (총 6주)

| 주차 | 주요 작업 | 완료 기준 |
|------|-----------|-----------|
| **1주** | 개발환경 구축, 기본 프로젝트 구조 | 프로젝트 생성, 권한 설정 완료 |
| **2주** | AccessibilityService, 오버레이, DND | 앱 차단 기능 동작 확인 |
| **3주** | SMS 자동응답, 타이머 UI | 자동응답 전송 테스트 완료 |
| **4주** | 데이터베이스, 비즈니스 로직 | 세션 저장/조회 기능 완료 |
| **5주** | 게이미피케이션, 리포트 | 포인트/스릭 시스템 동작 |
| **6주** | 테스트, 최적화, 베타 배포 | QA 완료, Play Console 업로드 |

---

## 🎯 성공 지표

### 기술적 지표
- ✅ 앱 차단 성공률 95% 이상
- ✅ SMS 자동응답 전송률 100%
- ✅ 배터리 사용량 최적화 (일일 5% 이하)
- ✅ 크래시율 0.1% 이하

### 사용자 지표
- ✅ D1 유지율 80% 이상
- ✅ 주간 성공 세션 3회 이상
- ✅ 스릭 평균 7일 이상
- ✅ NPS 점수 50 이상

---

## 📝 다음 액션 아이템

1. **즉시 실행**
   - [ ] Android Studio 설치 및 프로젝트 생성
   - [ ] Git 저장소 설정
   - [ ] 기본 권한 및 의존성 설정

2. **1주차 목표**
   - [ ] AccessibilityService 기본 구조 구현
   - [ ] 오버레이 서비스 구현
   - [ ] DND 제어 기능 구현

3. **2주차 목표**
   - [ ] SMS 자동응답 서비스 구현
   - [ ] 기본 타이머 UI 구현
   - [ ] 앱 차단 로직 테스트

---

> **참고**: 이 계획서는 PRD의 요구사항을 바탕으로 Kotlin 네이티브의 장점을 최대한 활용한 현실적인 개발 계획입니다. 각 단계별로 철저한 테스트와 사용자 피드백을 반영하여 안정적인 앱을 구축할 예정입니다.
