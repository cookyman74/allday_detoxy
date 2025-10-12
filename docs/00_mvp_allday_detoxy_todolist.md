# 안드로이드 Allday Detoxy MVP 개발 할 일 목록

## 📋 프로젝트 개요
**프로젝트명**: Allday Detoxy - 스마트폰 습관 교정 코치 앱 (MVP)
**기술 스택**: Kotlin 네이티브 + Jetpack Compose
**개발환경**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md) 참조
**PRD 문서**: [prd.md](./prd.md) 참조
**기술 설계**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) 참조

---

## 🎯 MVP 개발 전략

### MVP 목표
**상업적 가설 검증**: "사용자가 앱 차단 + 보상 시스템으로 집중 시간을 늘리는가?"

### 핵심 검증 지표
- **D1 유지율**: 60% 이상 (첫날 사용 후 다음날 재방문)
- **타이머 완주율**: 50% 이상 (설정한 시간을 끝까지 완료)
- **주간 활성 사용자**: 사용자의 40% 이상이 주 3회 이상 사용
- **평균 세션 시간**: 20분 이상

### MVP 범위 (4주 개발)
**포함**: 타이머, 앱 차단, 오버레이 잠금, DND, 기본 포인트/스릭, 일일 리포트, 온보딩
**제외**: SMS 자동응답, 화이트리스트, 주간 리포트, 퀘스트, 위치 기반, AI 코치, 보상 상점

---

## 📅 MVP 개발 일정 (4주)

### Week 1: 핵심 차단 기능 (타이머 + 앱 차단)
- **목표**: 사용자가 타이머를 시작하면 앱이 차단되는 기본 기능 구현
- **완료 기준**: 설정한 시간 동안 차단 앱 실행 시 홈 화면으로 이동

### Week 2: 잠금 화면 + DND + 데이터 저장
- **목표**: 전체 화면 잠금, DND 제어, 세션 데이터 저장
- **완료 기준**: 잠금 화면 표시, DND 활성화, 세션 기록 저장

### Week 3: 보상 시스템 + 리포트 + UI
- **목표**: 포인트/스릭 시스템, 일일 리포트, 기본 UI 완성
- **완료 기준**: 성공 시 포인트 지급, 연속 성공 추적, 일일 통계 표시

### Week 4: 온보딩 + 테스트 + 최적화
- **목표**: 온보딩 플로우, QA, 성능 최적화
- **완료 기준**: 베타 테스트 가능 상태

---

## 📅 Week 1: 핵심 차단 기능 (타이머 + 앱 차단)

### 1.1 프로젝트 기본 구조 설정 (Day 1)

#### 1.1.0 프로젝트 생성 및 Git/README 설정

- [x] **Android Studio 프로젝트 생성**
  1. Android Studio 실행 → "New Project" 선택
  2. 템플릿: "Empty Activity" 선택
  3. 프로젝트 설정:
     - Name: `Allday Detoxy`
     - Package name: `com.allday.detoxy`
     - Language: `Kotlin`
     - Minimum SDK: `API 26 (Android 8.0)`
     - Build configuration language: `Kotlin DSL (build.gradle.kts)`
  4. "Finish" 클릭하여 프로젝트 생성

- [x] **Git 저장소 초기화**
  ```bash
  cd /Users/junghojang/Developments/myProject/allday_detoxy
  git init
  git branch -M main
  ```

- [x] **.gitignore 설정**
  - Android Studio에서 자동 생성된 .gitignore 확인 및 보완
  ```gitignore
  # Android Studio
  *.iml
  .gradle
  .idea/
  local.properties

  # Build outputs
  /build
  /app/build
  /captures
  .externalNativeBuild
  .cxx

  # Keystore files
  *.jks
  *.keystore

  # Local configuration
  local.properties
  keystore.properties
  google-services.json
  ```

- [x] **README.md 작성**
  - 프로젝트 루트에 README.md 파일 생성
  ```markdown
  # Allday Detoxy - 스마트폰 습관 교정 코치 앱

  ## 📱 프로젝트 소개
  Allday Detoxy는 사용자의 스마트폰 과다 사용을 방지하고 집중 시간을 늘리는 Android 네이티브 앱입니다.

  **MVP 목표**: 앱 차단 + 보상 시스템을 통해 사용자의 집중 시간 증가 검증

  ## 🛠 기술 스택

  ### 언어 & 프레임워크
  - **Kotlin**: Android 네이티브 개발
  - **Jetpack Compose**: 선언형 UI

  ### 아키텍처 & 라이브러리
  - **Clean Architecture**: domain, data, presentation 계층 분리
  - **Hilt**: 의존성 주입 (Dependency Injection)
  - **Room**: 로컬 데이터베이스 (SQLite)
  - **Kotlin Coroutines**: 비동기 처리
  - **StateFlow/Flow**: 반응형 데이터 스트림

  ### Android 시스템 기능
  - **AccessibilityService**: 앱 차단 감지 및 제어
  - **Overlay Service**: 전체 화면 잠금 UI
  - **NotificationManager**: 방해금지 모드(DND) 제어
  - **Foreground Service**: 백그라운드 차단 서비스

  ## 🏗 프로젝트 아키텍처

  ```
  app/
  ├── core/          # 공통 모듈
  │   ├── di/        # Hilt 의존성 주입 모듈
  │   └── utils/     # 유틸리티 함수
  ├── data/          # 데이터 계층
  │   ├── local/     # Room 데이터베이스
  │   └── repository/# Repository 구현
  ├── domain/        # 비즈니스 로직 계층
  │   ├── model/     # 도메인 모델
  │   └── usecase/   # UseCase
  ├── presentation/  # UI 계층
  │   ├── ui/        # Compose 화면
  │   └── viewmodel/ # ViewModel
  └── service/       # Android 서비스
      ├── accessibility/ # 앱 차단 서비스
      └── overlay/   # 오버레이 잠금 서비스
  ```

  ## 📋 주요 기능 (MVP)

  1. **타이머 기반 집중 모드**
     - 프리셋 타이머 (25분, 45분, 60분)
     - 타이머 실행 중 특정 앱 차단

  2. **앱 차단 기능**
     - Instagram, TikTok, YouTube, Facebook 차단
     - AccessibilityService를 통한 실시간 감지

  3. **전체 화면 잠금**
     - 오버레이를 통한 잠금 화면 표시
     - 남은 시간 표시 및 포기 옵션

  4. **방해금지 모드(DND)**
     - 타이머 실행 중 알림 차단

  5. **보상 시스템**
     - 포인트 지급 (1분 = 1포인트)
     - 연속 성공 스트릭(streak) 추적

  6. **일일 리포트**
     - 오늘 성공한 세션 수
     - 총 집중 시간
     - 현재 스트릭 및 총 포인트

  ## 🚀 빌드 및 실행

  ### 요구사항
  - Android Studio Hedgehog | 2023.1.1 이상
  - JDK 17 이상
  - Android SDK API 34
  - Android SDK Build-Tools 34.0.0
  - Gradle 8.2 이상

  ### 설치 방법

  1. **저장소 클론**
     ```bash
     git clone https://github.com/[username]/allday_detoxy.git
     cd allday_detoxy
     ```

  2. **Android Studio에서 프로젝트 열기**
     - File → Open → 프로젝트 폴더 선택

  3. **Gradle 동기화**
     - Android Studio에서 자동으로 Gradle 동기화 실행
     - 또는 File → Sync Project with Gradle Files

  4. **에뮬레이터 또는 실제 기기 연결**
     - API 26 이상 필요
     - 권장: API 34 (Android 14) 에뮬레이터

  5. **빌드 및 실행**
     - Run → Run 'app' (Shift + F10)

  ### 필수 권한 설정

  앱 실행 후 다음 권한을 수동으로 설정해야 합니다:

  1. **접근성 서비스 권한**
     - 설정 → 접근성 → Allday Detoxy 활성화

  2. **다른 앱 위에 표시 권한**
     - 설정 → 앱 → 특수 앱 액세스 → 다른 앱 위에 표시 → Allday Detoxy 허용

  3. **방해금지 모드 액세스 권한**
     - 설정 → 알림 → 방해 금지 모드 액세스 → Allday Detoxy 허용

  ## 📊 MVP 성공 지표

  - **D1 유지율**: 60% 이상
  - **타이머 완주율**: 50% 이상
  - **주간 활성 사용자**: 40% 이상 (주 3회 이상 사용)
  - **평균 세션 시간**: 20분 이상

  ## 📝 참조 문서

  - [PRD 문서](./docs/prd.md) - 제품 요구사항 정의서
  - [MVP 개발 계획](./docs/00_mvp_allday_detoxy_todolist.md) - 4주 개발 일정
  - [기술 설계 문서](./docs/00_android_allday_detoxy_plan.md) - 상세 기술 설계
  - [개발환경 설정](./docs/00_kotlin_environment_todolist.md) - macOS 환경 구성


[1.1 작업 이전 기록 참조](working_history/2025-10-05_1.1.md)


### 1.2 AccessibilityService 구현 (Day 2-3)

#### 1.2.1 AccessibilityService 기본 구조
- [x] **FocusAccessibilityService 클래스 생성**
  ```kotlin
  class FocusAccessibilityService : AccessibilityService() {
      override fun onAccessibilityEvent(event: AccessibilityEvent?) {
          // 현재 실행 앱 감지
          // 차단 앱이면 홈 화면으로 이동
      }
      override fun onInterrupt() {}
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.2 AccessibilityService

#### 1.2.2 앱 차단 로직 (MVP 간소화)
- [x] **하드코딩된 차단 앱 리스트**
  ```kotlin
  private val blockedApps = setOf(
      "com.instagram.android",
      "com.zhiliaoapp.musically",  // TikTok
      "com.google.android.youtube",
      "com.facebook.katana"
  )
  ```
  - **MVP 결정**: 사용자 커스텀 차단 목록은 1차 릴리스로 연기
  - 성능 측정: 차단 반응 시간 < 500ms

#### 1.2.3 접근성 서비스 설정
- [x] **accessibility_service_config.xml**
  ```xml
  <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
      android:accessibilityEventTypes="typeWindowStateChanged"
      android:accessibilityFeedbackType="feedbackGeneric"
      android:canRetrieveWindowContent="true"
      android:description="@string/accessibility_description" />
  ```

#### 1.2.4 권한 요청 UI (간소화)
- [x] **접근성 권한 안내 다이얼로그**
  - 설정 화면으로 이동 버튼
  - 권한 상태 확인 로직
  - **MVP**: 복잡한 튜토리얼 없이 간단 안내만

[1.2 작업 이전 기록 참조](working_history/2025-10-05_1.2.md)

### 1.3 기본 타이머 기능 (Day 4-5)

#### 1.3.1 타이머 상태 관리
- [x] **FocusTimer 클래스**
  ```kotlin
  enum class FocusState { IDLE, RUNNING, FINISHED, FAILED }

  class FocusTimer {
      private val _state = MutableStateFlow(FocusState.IDLE)
      val state: StateFlow<FocusState> = _state

      fun start(durationMinutes: Int)
      fun finish()
      fun giveUp()
  }
  ```

#### 1.3.2 타이머 UI (MVP 간소화)
- [x] **TimerScreen Compose**
  ```kotlin
  @Composable
  fun TimerScreen() {
      // 프리셋 버튼: 25분, 45분, 60분 (3개만)
      // 원형 프로그레스 바
      // 시작/포기 버튼
  }
  ```
  - **MVP 제외**: 커스텀 시간 입력, 일시정지 기능
  - **참조**: [prd.md](./prd.md) - 18) 샘플 코드 타이머 로직

#### 1.3.3 타이머-서비스 연동
- [x] **타이머 시작 시**
  - AccessibilityService 상태 확인
  - FocusAccessibilityService.isTimerRunning 상태 업데이트
  - TODO: 세션 시작 기록 (Room DB, Week 2)
  - TODO: DND 활성화 (Week 2)

[1.3 작업 이전 기록 참조](working_history/2025-10-05_1.3.md)

### 1.4 권한 안내 및 초기 설정 (Day 5-6)

#### 1.4.1 권한 체크 로직
- [x] **PermissionCheckScreen 생성**
  ```kotlin
  @Composable
  fun PermissionCheckScreen(
      onAllPermissionsGranted: () -> Unit
  ) {
      val context = LocalContext.current
      
      // 권한 상태 추적
      var accessibilityGranted by remember { 
          mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context))
      }
      var overlayGranted by remember {
          mutableStateOf(PermissionUtils.canDrawOverlays(context))
      }
      var dndGranted by remember {
          mutableStateOf(PermissionUtils.hasNotificationPolicyAccess(context))
      }
      
      // 모든 권한이 부여되면 자동으로 다음 화면
      LaunchedEffect(accessibilityGranted, overlayGranted, dndGranted) {
          if (accessibilityGranted && overlayGranted) {
              onAllPermissionsGranted()
          }
      }
  }
  ```
  - **참조**: [PermissionUtils.kt](app/src/main/java/com/allday/detoxy/core/utils/PermissionUtils.kt)
  - **필수 권한**: 접근성 서비스, 오버레이 권한
  - **선택 권한**: DND 권한 (알림 차단을 원하지 않는 사용자 고려)

#### 1.4.2 권한 안내 UI 구현
- [x] **PermissionCard 컴포넌트**
  ```kotlin
  @Composable
  fun PermissionCard(
      title: String,
      description: String,
      icon: ImageVector,
      isGranted: Boolean,
      isRequired: Boolean = true,
      onRequestClick: () -> Unit
  ) {
      Card(
          colors = CardDefaults.cardColors(
              containerColor = if (isGranted) {
                  Color(0xFF4CAF50).copy(alpha = 0.1f)
              } else {
                  MaterialTheme.colorScheme.surfaceVariant
              }
          )
      ) {
          Column {
              Row {
                  Icon(icon, contentDescription = null)
                  Column {
                      Row {
                          Text(title)
                          if (isRequired) {
                              Text("*", color = Color.Red)
                          }
                      }
                      Text(description, style = MaterialTheme.typography.bodySmall)
                  }
              }
              
              // 상태 표시
              if (isGranted) {
                  Row {
                      Icon(Icons.Default.CheckCircle, tint = Color(0xFF4CAF50))
                      Text("권한 허용됨", color = Color(0xFF4CAF50))
                  }
              } else {
                  Button(onClick = onRequestClick) {
                      Text("권한 설정하기")
                  }
              }
          }
      }
  }
  ```
  - Material3 디자인 적용
  - 권한 상태에 따른 시각적 피드백
  - 필수/선택 권한 구분 표시

#### 1.4.3 권한별 상세 안내
- [x] **접근성 서비스 안내**
  ```kotlin
  PermissionCard(
      title = "앱 차단 기능",
      description = "Instagram, TikTok, YouTube, Facebook 등의 앱을 차단하기 위해 필요합니다. " +
                   "설정에서 'Allday Detoxy'를 찾아 활성화해주세요.",
      icon = Icons.Default.Block,
      isGranted = accessibilityGranted,
      isRequired = true,
      onRequestClick = {
          PermissionUtils.openAccessibilitySettings(context)
      }
  )
  ```
  - 사용자가 이해하기 쉬운 설명
  - "접근성 서비스" 대신 "앱 차단 기능"으로 표현
  - 설정 화면에서 찾는 방법 안내

- [x] **오버레이 권한 안내**
  ```kotlin
  PermissionCard(
      title = "잠금 화면 표시",
      description = "차단된 앱 실행 시 집중 모드 잠금 화면을 표시하기 위해 필요합니다. " +
                   "'다른 앱 위에 표시' 권한을 허용해주세요.",
      icon = Icons.Default.Lock,
      isGranted = overlayGranted,
      isRequired = true,
      onRequestClick = {
          PermissionUtils.openOverlaySettings(context)
      }
  )
  ```

- [x] **DND 권한 안내 (선택)**
  ```kotlin
  PermissionCard(
      title = "알림 차단 (선택)",
      description = "집중 모드 중 알림을 자동으로 차단합니다. " +
                   "필수는 아니지만, 더 나은 집중 환경을 위해 권장됩니다.",
      icon = Icons.Default.NotificationsOff,
      isGranted = dndGranted,
      isRequired = false,  // 선택 권한
      onRequestClick = {
          PermissionUtils.openNotificationPolicySettings(context)
      }
  )
  ```
  - 선택 권한임을 명확히 표시
  - 사용자가 부담 없이 건너뛸 수 있도록

#### 1.4.4 권한 재확인 메커니즘
- [x] **onResume 시 권한 재확인**
  ```kotlin
  @Composable
  fun PermissionCheckScreen(
      onAllPermissionsGranted: () -> Unit
  ) {
      val context = LocalContext.current
      val lifecycleOwner = LocalLifecycleOwner.current
      
      // 화면 재진입 시 권한 상태 재확인
      DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
              if (event == Lifecycle.Event.ON_RESUME) {
                  // 권한 상태 업데이트
                  accessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
                  overlayGranted = PermissionUtils.canDrawOverlays(context)
                  dndGranted = PermissionUtils.hasNotificationPolicyAccess(context)
              }
          }
          
          lifecycleOwner.lifecycle.addObserver(observer)
          
          onDispose {
              lifecycleOwner.lifecycle.removeObserver(observer)
          }
      }
  }
  ```
  - 사용자가 설정 화면에서 돌아왔을 때 자동으로 권한 상태 확인
  - 모든 필수 권한이 부여되면 자동으로 메인 화면으로 이동
  - 부드러운 UX 제공

#### 1.4.5 초기 실행 플래그 관리
- [x] **SharedPreferences로 초기 실행 여부 저장**
  ```kotlin
  class PreferenceManager(context: Context) {
      private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
      
      fun isFirstLaunch(): Boolean {
          return prefs.getBoolean("is_first_launch", true)
      }
      
      fun setFirstLaunchCompleted() {
          prefs.edit().putBoolean("is_first_launch", false).apply()
      }
  }
  ```

- [x] **MainActivity에서 초기 화면 분기**
  ```kotlin
  @Composable
  fun MainScreen() {
      val context = LocalContext.current
      val preferenceManager = remember { PreferenceManager(context) }
      var showPermissionCheck by remember { mutableStateOf(preferenceManager.isFirstLaunch()) }
      
      if (showPermissionCheck) {
          PermissionCheckScreen(
              onAllPermissionsGranted = {
                  preferenceManager.setFirstLaunchCompleted()
                  showPermissionCheck = false
              }
          )
      } else {
          // 메인 타이머 화면
          TimerScreen()
      }
  }
  ```
  - 최초 실행 시에만 권한 안내 화면 표시
  - 이후 실행 시 바로 타이머 화면으로 이동
  - 사용자가 권한 설정을 건너뛰면 타이머 시작 시 다시 안내

#### 1.4.6 권한 미부여 시 대응
- [x] **타이머 시작 시 권한 재확인**
  ```kotlin
  fun startTimer(durationMinutes: Int) {
      // 접근성 서비스 확인
      if (!PermissionUtils.isAccessibilityServiceEnabled(application)) {
          _timerState.value = FocusState.IDLE
          // TODO: 스낵바 또는 다이얼로그로 안내
          Log.e(TAG, "접근성 서비스가 비활성화되어 있습니다")
          return
      }
      
      // 오버레이 권한 확인
      if (!PermissionUtils.canDrawOverlays(application)) {
          _timerState.value = FocusState.IDLE
          Log.e(TAG, "오버레이 권한이 없습니다")
          return
      }
      
      // 정상 진행
      startTimerInternal(durationMinutes)
  }
  ```
  - 타이머 시작 전 필수 권한 확인
  - 권한이 없으면 타이머 시작 중단
  - 사용자에게 권한 설정 필요성 안내

#### 1.4.7 설정 화면에서 권한 관리
- [ ] **설정 화면에 권한 관리 섹션 추가 (MVP 이후)**
  ```kotlin
  @Composable
  fun SettingsScreen() {
      Column {
          // 기타 설정...
          
          Section(title = "권한 관리") {
              SettingsItem(
                  title = "앱 차단 기능",
                  subtitle = if (accessibilityGranted) "활성화됨" else "비활성화됨",
                  onClick = { PermissionUtils.openAccessibilitySettings(context) }
              )
              
              SettingsItem(
                  title = "잠금 화면 표시",
                  subtitle = if (overlayGranted) "허용됨" else "거부됨",
                  onClick = { PermissionUtils.openOverlaySettings(context) }
              )
              
              SettingsItem(
                  title = "알림 차단",
                  subtitle = if (dndGranted) "허용됨" else "거부됨",
                  onClick = { PermissionUtils.openNotificationPolicySettings(context) }
              )
          }
      }
  }
  ```
  - 사용자가 언제든 권한 설정 확인 및 변경 가능
  - Week 3 UI 완성 단계에서 구현

---

## 📅 Week 2: 잠금 화면 + DND + 데이터 저장

### 2.1 오버레이 잠금 화면 (Day 6-7)

#### 2.1.1 LockOverlayService 구현
- [x] **포그라운드 서비스**
  ```kotlin
  class LockOverlayService : Service() {
      override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
          showOverlay()
          return START_STICKY
      }

      private fun showOverlay() {
          // WindowManager로 전체 화면 오버레이
      }
  }
  ```
  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.4 오버레이
  - ComposeView를 사용한 오버레이 구현
  - 포그라운드 서비스 알림 표시
  - TimerViewModel과 연동

#### 2.1.2 오버레이 UI (MVP 간소화)
- [x] **잠금 화면 최소 UI**
  ```kotlin
  // 반투명 배경
  // 남은 시간 텍스트
  // 작은 "포기" 버튼 (하단)
  ```
  - **MVP 제외**: 미션 해제, 퍼즐 해제 → 1차 릴리스
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 진행 중
  - Material3 디자인, 진행률 표시, 반투명 검정 배경

#### 2.1.3 오버레이 권한 관리
- [x] **SYSTEM_ALERT_WINDOW 권한**
  - 권한 요청 및 설정 화면 이동 (PermissionUtils 사용)
  - **MVP**: 간단한 안내 메시지만
  - AndroidManifest에 권한 및 서비스 등록 완료

### 2.2 DND 제어 (Day 8)

#### 2.2.1 DND 모드 제어
- [x] **DndManager 구현**
  ```kotlin
  class DndManager(private val context: Context) {
      fun enableDnd() {
          val notificationManager = context.getSystemService(NotificationManager::class.java)
          notificationManager?.setInterruptionFilter(
              NotificationManager.INTERRUPTION_FILTER_ALARMS
          )
      }

      fun disableDnd() {
          // DND 해제
      }
  }
  ```
  - **MVP**: 모든 알림 차단 (화이트리스트 없음)
  - **참조**: [prd.md](./prd.md) - 3) 핵심 기능 B. 연락·알림 정책

#### 2.2.2 DND 권한 요청
- [x] **알림 정책 접근 권한**
  - 설정 화면으로 이동
  - 권한 상태 확인

### 2.3 Room 데이터베이스 (Day 9-10)

#### 2.3.1 핵심 엔티티 (MVP 최소)
- [x] **FocusSession 엔티티**
  ```kotlin
  @Entity(tableName = "focus_sessions")
  data class FocusSession(
      @PrimaryKey val id: String = UUID.randomUUID().toString(),
      val startTime: Long,
      val endTime: Long? = null,
      val durationMinutes: Int,
      val success: Boolean = false
  )
  ```

- [x] **UserSettings 엔티티**
  ```kotlin
  @Entity(tableName = "user_settings")
  data class UserSettings(
      @PrimaryKey val id: Int = 1,
      val totalPoints: Int = 0,
      val currentStreak: Int = 0,
      val lastSuccessDate: String? = null
  )
  ```

#### 2.3.2 DAO 인터페이스
- [x] **FocusSessionDao**
  ```kotlin
  @Dao
  interface FocusSessionDao {
      @Insert
      suspend fun insert(session: FocusSession)

      @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch') = DATE('now')")
      fun getTodaySessions(): Flow<List<FocusSession>>
  }
  ```

- [x] **UserSettingsDao**
  ```kotlin
  @Dao
  interface UserSettingsDao {
      @Query("SELECT * FROM user_settings WHERE id = 1")
      fun getSettings(): Flow<UserSettings>

      @Update
      suspend fun update(settings: UserSettings)
  }
  ```

#### 2.3.3 데이터베이스 구성
- [x] **DetoxyDatabase**
  ```kotlin
  @Database(
      entities = [FocusSession::class, UserSettings::class],
      version = 1
  )
  abstract class DetoxyDatabase : RoomDatabase() {
      abstract fun sessionDao(): FocusSessionDao
      abstract fun settingsDao(): UserSettingsDao
  }
  ```

---

## 📅 Week 3: 보상 시스템 + 리포트 + UI

### 3.1 포인트/스릭 시스템 (Day 11-12)

#### 3.1.1 GamificationManager (MVP 간소화)
- [x] **간단한 포인트 계산**
  ```kotlin
  class GamificationManager {
      fun calculatePoints(durationMinutes: Int): Int {
          // 간단 공식: 1분 = 1포인트
          return durationMinutes
      }

      fun updateStreak(settings: UserSettings, success: Boolean): UserSettings {
          val today = LocalDate.now().toString()

          if (success) {
              if (settings.lastSuccessDate == LocalDate.now().minusDays(1).toString()) {
                  return settings.copy(currentStreak = settings.currentStreak + 1)
              } else {
                  return settings.copy(currentStreak = 1)
              }
          } else {
              return settings.copy(currentStreak = 0)
          }
      }
  }
  ```
  - **MVP 제외**: 복잡한 보너스, 가중치, 뱃지 → 1차 릴리스
  - **참조**: [prd.md](./prd.md) - 11) 알고리즘

#### 3.1.2 세션 완료 시 보상 지급
- [x] **EndFocusUseCase**
  ```kotlin
  class EndFocusUseCase(
      private val repository: FocusRepository,
      private val gamificationManager: GamificationManager
  ) {
      suspend operator fun invoke(sessionId: String, success: Boolean) {
          repository.endSession(sessionId, success)

          if (success) {
              val session = repository.getSession(sessionId)
              val points = gamificationManager.calculatePoints(session.durationMinutes)
              repository.addPoints(points)
              repository.updateStreak(success)
          }
      }
  }
  ```

### 3.2 일일 리포트 (Day 13)

#### 3.2.1 일일 통계 UI (MVP 간소화)
- [x] **ReportScreen Compose**
  ```kotlin
  @Composable
  fun DailyReportScreen(viewModel: ReportViewModel) {
      val todaySessions by viewModel.todaySessions.collectAsState()
      val settings by viewModel.settings.collectAsState()

      Column {
          // 오늘 성공한 세션 수
          Text("성공: ${todaySessions.count { it.success }}회")

          // 오늘 총 집중 시간
          Text("총 집중: ${todaySessions.filter { it.success }.sumOf { it.durationMinutes }}분")

          // 현재 스릭
          Text("연속 성공: ${settings.currentStreak}일")

          // 총 포인트
          Text("총 포인트: ${settings.totalPoints}")
      }
  }
  ```
  - **MVP 제외**: 차트, 주간 리포트, AI 코치 메시지 → 1차 릴리스

#### 3.2.2 세션 리스트
- [x] **오늘 세션 목록 표시**
  ```kotlin
  LazyColumn {
      items(todaySessions) { session ->
          SessionCard(session)
      }
  }
  ```

### 3.3 기본 UI 완성 (Day 14-15)

#### 3.3.1 네비게이션 구조
- [x] **간단한 탭 네비게이션**
  ```kotlin
  @Composable
  fun MainScreen() {
      var selectedTab by remember { mutableStateOf(0) }

      Scaffold(
          bottomBar = {
              NavigationBar {
                  NavigationBarItem(
                      selected = selectedTab == 0,
                      onClick = { selectedTab = 0 },
                      icon = { Icon(Icons.Default.Timer, null) },
                      label = { Text("타이머") }
                  )
                  NavigationBarItem(
                      selected = selectedTab == 1,
                      onClick = { selectedTab = 1 },
                      icon = { Icon(Icons.Default.Assessment, null) },
                      label = { Text("리포트") }
                  )
              }
          }
      ) {
          when (selectedTab) {
              0 -> TimerScreen()
              1 -> DailyReportScreen()
          }
      }
  }
  ```
  - **MVP**: 2개 탭만 (타이머, 리포트)
  - **제외**: 설정, 상점, 친구 → 1차 릴리스

#### 3.3.2 Material3 테마 적용
- [x] **기본 테마 설정**
  ```kotlin
  @Composable
  fun DetoxyTheme(content: @Composable () -> Unit) {
      MaterialTheme(
          colorScheme = lightColorScheme(),
          content = content
      )
  }
  ```

---

## 📅 Week 4: 온보딩 + 테스트 + 최적화

### 4.1 온보딩 플로우 (Day 16-17)

#### 4.1.1 온보딩 화면 (MVP 간소화)
- [x] **3단계 온보딩**
  ```kotlin
  @Composable
  fun WelcomeScreen(onNextClick: () -> Unit) {
      // 앱 소개 및 주요 기능 안내
      // - 타이머 기반 집중 모드
      // - 앱 차단 기능
      // - 보상 시스템
      // "시작하기" 버튼 클릭 시 권한 안내 화면으로 이동
  }
  ```
  - WelcomeScreen: 앱 소개 및 주요 기능 안내
  - PermissionCheckScreen: 권한 안내 및 설정 (Week 1.4 구현 완료)
  - MainScreen: 모든 권한 설정 완료 후 자동 이동
  - **구현 파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/onboarding/WelcomeScreen.kt`
  - **작업 기록**: [working_history/2025-10-12_4.1.md](../working_history/2025-10-12_4.1.md)

#### 4.1.2 권한 안내 화면
- [x] **통합 권한 가이드 (Week 1.4 구현 완료)**
  - **참조**: [Week 1.4 권한 안내 및 초기 설정](#14-권한-안내-및-초기-설정-day-5-6)
  - PermissionCheckScreen 구현 완료 (Week 1.4)
  - 온보딩 화면과 권한 안내 화면을 자연스럽게 연결
  - **플로우 구성**: 웰컴 화면 → 권한 안내 → 메인 화면 (자동 이동)
  - **PreferenceManager 플래그**:
    - `isOnboardingCompleted()`: 환영 화면 완료 여부
    - `isFirstLaunch()`: 권한 안내 화면 완료 여부
  - **MainActivity 통합**: 상태 기반 화면 전환 로직 구현
  - **작업 기록**: [working_history/2025-10-12_4.1.md](../working_history/2025-10-12_4.1.md)

### 4.2 테스트 (Day 18)

#### 4.2.1 핵심 기능 테스트
- [x] **수동 테스트 체크리스트**
  - ✅ 타이머 시작 → 앱 차단 동작 확인
  - ✅ 타이머 완료 → 포인트 지급 확인
  - ✅ 타이머 포기 → 스릭 초기화 확인
  - ✅ 오버레이 잠금 화면 표시 확인
  - ✅ DND 활성화/비활성화 확인
  - ✅ 세션 데이터 저장 확인
  
- **테스트 결과**: [working_history/2025-10-12_4.2.1_test_results.md](../working_history/2025-10-12_4.2.1_test_results.md)
- **DND 기능**: DndManager 구현 완료, TimerViewModel 통합 완료, 권한 처리 정상
- **세션 데이터**: Room 데이터베이스 정상 작동, CRUD 작업 정상, ReportScreen 통합 완료

#### 4.2.2 권한 플로우 테스트
- [x] **권한 시나리오 테스트**
  - ✅ 접근성 권한 거부 시 다이얼로그 표시
  - ✅ 오버레이 권한 거부 시 다이얼로그 표시
  - ✅ DND 권한 거부 시 정상 작동 (선택 권한)
  
- **테스트 결과**: [working_history/2025-10-12_4.2.2_permission_flow_test.md](../working_history/2025-10-12_4.2.2_permission_flow_test.md)
- **구현 사항**:
  - TimerViewModel에 PermissionError StateFlow 추가
  - PermissionErrorDialog 컴포넌트 구현
  - "설정으로 이동" 버튼으로 즉시 해결 가능
- **개선 효과**: 권한 없을 때 사용자 혼란 제거, 즉각적인 해결 방법 제공

### 4.3 성능 최적화 (Day 19)

#### 4.3.1 배터리 최적화
- [x] **포그라운드 서비스 최적화**
  - ✅ 불필요한 백그라운드 작업 없음
  - ✅ IMPORTANCE_LOW 알림 사용
  - ✅ 타이머 업데이트 주기 최소화 (1초)
  - ✅ 로그 출력 최소화
  - ✅ **목표 달성**: 예상 일일 배터리 사용량 2-4% (목표: 5% 이하)

#### 4.3.2 메모리 최적화
- [x] **메모리 누수 점검**
  - ✅ 오버레이 뷰 명시적 해제
  - ✅ windowManager 참조 해제
  - ✅ timerJob 명시적 취소
  - ✅ lifecycleScope 자동 정리
  - ✅ onDestroy()에서 모든 리소스 정리

- **작업 기록**: [working_history/2025-10-12_4.3.md](../working_history/2025-10-12_4.3.md)
- **주요 개선사항**:
  - LockOverlayService.onDestroy() 리소스 정리 강화
  - 명시적 null 설정으로 GC 힌트 제공
  - 메모리 누수 없음 확인

### 4.4 베타 준비 (Day 20)

#### 4.4.1 크래시 리포팅
- [x] **Firebase Crashlytics 설정**
  - [x] Gradle 의존성 추가 (Firebase BOM 32.7.0)
  - [x] Google Services, Crashlytics 플러그인 적용
  - [x] DetoxyApplication.kt 주석 업데이트
  - [ ] Firebase 프로젝트 생성 (수동 작업 필요)
  - [ ] google-services.json 다운로드 및 배치 (수동 작업 필요)
  - [ ] 빌드 및 Crashlytics 연결 확인 (수동 작업 필요)
  
- **작업 기록**: [working_history/2025-10-12_4.4.md](../working_history/2025-10-12_4.4.md)
- **설정 가이드**: [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

#### 4.4.2 분석 이벤트 (MVP 최소)
- [ ] **핵심 이벤트만 기록**
  ```kotlin
  // focus_start
  // focus_finish (success: true/false)
  // give_up
  ```
  - **참조**: [prd.md](./prd.md) - 14) 측정 지표
  - **TODO**: Firebase Analytics 이벤트 로깅 구현 (1차 고도화 착수 후)

---

## 🎯 MVP 성공 지표

### 기술적 지표
- [ ] **앱 차단 성공률 95% 이상**
  - 차단 앱 실행 시 홈 화면 이동 확인

- [ ] **배터리 사용량 일일 5% 이하**
  - 배터리 통계 모니터링

- [ ] **크래시 발생률 1% 이하**
  - Crashlytics 모니터링

### 사용자 지표 (베타 테스트)
- [ ] **D1 유지율 60% 이상**
  - 첫 사용 후 다음날 재방문

- [ ] **타이머 완주율 50% 이상**
  - 시작한 타이머를 끝까지 완료

- [ ] **주간 활성률 40% 이상**
  - 주 3회 이상 사용

---

## 🚀 1차 릴리스 계획 (MVP 이후 4주)

### 추가 기능
- **SMS 자동응답** (2일)
  - 기본 SMS 앱 등록
  - SmsReceiver 구현
  - 자동응답 메시지 설정

- **화이트리스트 연락처** (2일)
  - 연락처 선택 UI
  - 긴급 연락처 처리

- **주간 리포트 + 차트** (3일)
  - MPAndroidChart 활용
  - 주간 통계 UI

- **퀘스트 시스템** (2일)
  - 데일리 퀘스트
  - 보상 지급

- **뱃지 시스템** (2일)
  - 뱃지 정의 및 획득 로직
  - 뱃지 UI

- **미션 해제** (2일)
  - 타이핑 미션
  - 15초 지연 해제

---

## 🎨 2차 릴리스 계획 (1차 이후 6주)

### 고도화 기능
- **위치 기반 자동화** (3일)
- **AI 코치 기능** (2일)
- **보상 상점** (2일)
- **협력 타이머/친구 랭킹** (3일)
- **보호자 모드** (2일, Device Owner)
- **아바타/세계관** (4일)

---

## 📝 MVP 즉시 실행 액션

### Day 1 (오늘)
1. [ ] 프로젝트 폴더 구조 생성
2. [ ] Hilt 의존성 주입 설정
3. [ ] Room 데이터베이스 엔티티 정의

### Day 2-3
1. [ ] AccessibilityService 구현
2. [ ] 하드코딩된 차단 앱 리스트
3. [ ] 앱 차단 로직 구현

### Day 4-5
1. [ ] 타이머 UI (25/45/60분 프리셋)
2. [ ] 타이머 상태 관리
3. [ ] 타이머-서비스 연동

---

## 🔗 참조 문서

- **PRD 문서**: [prd.md](./prd.md) - 전체 요구사항
- **개발환경**: [00_kotlin_environment_todolist.md](./00_kotlin_environment_todolist.md)
- **기술 설계**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md)

---

> **MVP 철학**: "완벽한 앱보다 빠른 가설 검증". 사용자가 앱 차단 + 간단한 보상으로 집중 시간을 늘리는지 4주 내에 확인합니다.
