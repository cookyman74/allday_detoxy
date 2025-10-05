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

- [ ] **Android Studio 프로젝트 생성**
  1. Android Studio 실행 → "New Project" 선택
  2. 템플릿: "Empty Activity" 선택
  3. 프로젝트 설정:
     - Name: `Allday Detoxy`
     - Package name: `com.allday.detoxy`
     - Language: `Kotlin`
     - Minimum SDK: `API 26 (Android 8.0)`
     - Build configuration language: `Kotlin DSL (build.gradle.kts)`
  4. "Finish" 클릭하여 프로젝트 생성

- [ ] **Git 저장소 초기화**
  ```bash
  cd /Users/junghojang/Developments/myProject/allday_detoxy
  git init
  git branch -M main
  ```

- [ ] **.gitignore 설정**
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

- [ ] **README.md 작성**
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

  ## 📄 라이선스

  MIT License (예정)

  ## 👨‍💻 개발자

  Jung Ho Jang - [@junghojang](https://github.com/junghojang)
  ```

- [ ] **초기 커밋**
  ```bash
  git add .
  git commit -m "Initial commit: Android project setup with README"
  ```

#### 1.1.1 MVP 최소 폴더 구조 생성

- [ ] **Clean Architecture 폴더 구조 생성**

  Android Studio에서 다음 패키지를 순서대로 생성:

  1. **core 패키지 생성**
     - `app/src/main/java/com/allday/detoxy` 우클릭 → New → Package
     - `core` 입력하여 생성
     - `core` 내부에 `di`, `utils` 패키지 생성
     - **목적**: 전역 의존성 주입 모듈과 공통 유틸리티 함수 관리

  2. **data 계층 패키지 생성**
     - `com.allday.detoxy` 하위에 `data` 패키지 생성
     - `data` 내부에 `local`, `repository` 패키지 생성
     - **목적**: Room 데이터베이스 엔티티/DAO와 Repository 구현체 관리
     - **예시 파일**:
       - `data/local/entity/FocusSessionEntity.kt`
       - `data/local/dao/FocusSessionDao.kt`
       - `data/local/DetoxyDatabase.kt`
       - `data/repository/FocusRepositoryImpl.kt`

  3. **domain 계층 패키지 생성**
     - `com.allday.detoxy` 하위에 `domain` 패키지 생성
     - `domain` 내부에 `model`, `usecase` 패키지 생성
     - **목적**: 비즈니스 로직과 도메인 모델 정의
     - **예시 파일**:
       - `domain/model/FocusSession.kt` (데이터 클래스)
       - `domain/model/UserSettings.kt` (데이터 클래스)
       - `domain/usecase/StartFocusUseCase.kt`
       - `domain/usecase/EndFocusUseCase.kt`

  4. **presentation 계층 패키지 생성**
     - `com.allday.detoxy` 하위에 `presentation` 패키지 생성
     - `presentation` 내부에 `ui`, `viewmodel` 패키지 생성
     - `ui` 내부에 `timer`, `report`, `onboarding` 패키지 생성
     - **목적**: Jetpack Compose UI와 ViewModel 관리
     - **예시 파일**:
       - `presentation/ui/timer/TimerScreen.kt`
       - `presentation/ui/report/ReportScreen.kt`
       - `presentation/viewmodel/TimerViewModel.kt`

  5. **service 패키지 생성**
     - `com.allday.detoxy` 하위에 `service` 패키지 생성
     - `service` 내부에 `accessibility`, `overlay` 패키지 생성
     - **목적**: Android 시스템 서비스 구현
     - **예시 파일**:
       - `service/accessibility/FocusAccessibilityService.kt`
       - `service/overlay/LockOverlayService.kt`

  **최종 구조**:
  ```
  app/src/main/java/com/allday/detoxy/
  ├── core/
  │   ├── di/           # AppModule.kt, DatabaseModule.kt
  │   └── utils/        # DateUtils.kt, TimeFormatter.kt
  ├── data/
  │   ├── local/        # DetoxyDatabase.kt, FocusSessionDao.kt
  │   └── repository/   # FocusRepositoryImpl.kt
  ├── domain/
  │   ├── model/        # FocusSession.kt, UserSettings.kt
  │   └── usecase/      # StartFocusUseCase.kt, EndFocusUseCase.kt
  ├── presentation/
  │   ├── ui/
  │   │   ├── timer/    # TimerScreen.kt
  │   │   ├── report/   # ReportScreen.kt
  │   │   └── onboarding/ # OnboardingScreen.kt
  │   └── viewmodel/    # TimerViewModel.kt, ReportViewModel.kt
  └── service/
      ├── accessibility/ # FocusAccessibilityService.kt
      └── overlay/      # LockOverlayService.kt
  ```

  - **참조**: [00_android_allday_detoxy_plan.md](./00_android_allday_detoxy_plan.md) - 2.2 프로젝트 구조
  - **확인 방법**: Project 뷰에서 폴더 트리가 위 구조와 일치하는지 확인

#### 1.1.2 Hilt 의존성 주입 설정

- [ ] **build.gradle.kts 의존성 추가**

  1. **프로젝트 레벨 build.gradle.kts** (`/build.gradle.kts`)
     ```kotlin
     plugins {
         id("com.android.application") version "8.2.0" apply false
         id("org.jetbrains.kotlin.android") version "1.9.20" apply false
         id("com.google.dagger.hilt.android") version "2.48" apply false
     }
     ```

  2. **앱 레벨 build.gradle.kts** (`/app/build.gradle.kts`)
     ```kotlin
     plugins {
         id("com.android.application")
         id("org.jetbrains.kotlin.android")
         id("kotlin-kapt")
         id("com.google.dagger.hilt.android")
     }

     android {
         namespace = "com.allday.detoxy"
         compileSdk = 34

         defaultConfig {
             applicationId = "com.allday.detoxy"
             minSdk = 26
             targetSdk = 34
             versionCode = 1
             versionName = "1.0.0"
         }

         compileOptions {
             sourceCompatibility = JavaVersion.VERSION_17
             targetCompatibility = JavaVersion.VERSION_17
         }

         kotlinOptions {
             jvmTarget = "17"
         }

         buildFeatures {
             compose = true
         }

         composeOptions {
             kotlinCompilerExtensionVersion = "1.5.4"
         }
     }

     dependencies {
         // Hilt
         implementation("com.google.dagger:hilt-android:2.48")
         kapt("com.google.dagger:hilt-compiler:2.48")

         // Jetpack Compose
         implementation(platform("androidx.compose:compose-bom:2023.10.01"))
         implementation("androidx.compose.ui:ui")
         implementation("androidx.compose.material3:material3")
         implementation("androidx.compose.ui:ui-tooling-preview")
         implementation("androidx.activity:activity-compose:1.8.1")

         // Lifecycle
         implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
         implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

         // Room
         implementation("androidx.room:room-runtime:2.6.1")
         implementation("androidx.room:room-ktx:2.6.1")
         kapt("androidx.room:room-compiler:2.6.1")

         // Coroutines
         implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
     }
     ```

  3. **Gradle 동기화**
     - File → Sync Project with Gradle Files
     - 또는 Gradle 동기화 알림 팝업에서 "Sync Now" 클릭

- [ ] **Application 클래스 생성**

  **파일**: `app/src/main/java/com/allday/detoxy/DetoxyApplication.kt`

  ```kotlin
  package com.allday.detoxy

  import android.app.Application
  import dagger.hilt.android.HiltAndroidApp

  /**
   * Hilt 의존성 주입을 위한 Application 클래스
   *
   * @HiltAndroidApp 어노테이션으로 Hilt의 코드 생성 트리거
   * 앱의 전체 생명주기 동안 DI 컨테이너 유지
   */
  @HiltAndroidApp
  class DetoxyApplication : Application() {
      override fun onCreate() {
          super.onCreate()
          // TODO: Timber 로그 초기화 (추후 추가)
          // TODO: Crashlytics 초기화 (Week 4)
      }
  }
  ```

  **AndroidManifest.xml 수정**

  `app/src/main/AndroidManifest.xml` 파일에서 `<application>` 태그에 `android:name` 추가:

  ```xml
  <application
      android:name=".DetoxyApplication"
      android:allowBackup="true"
      android:icon="@mipmap/ic_launcher"
      android:label="@string/app_name"
      android:theme="@style/Theme.AlldayDetoxy">

      <activity
          android:name=".MainActivity"
          android:exported="true">
          <intent-filter>
              <action android:name="android.intent.action.MAIN" />
              <category android:name="android.intent.category.LAUNCHER" />
          </intent-filter>
      </activity>
  </application>
  ```

- [ ] **MainActivity Hilt 설정**

  **파일**: `app/src/main/java/com/allday/detoxy/MainActivity.kt`

  ```kotlin
  package com.allday.detoxy

  import android.os.Bundle
  import androidx.activity.ComponentActivity
  import androidx.activity.compose.setContent
  import dagger.hilt.android.AndroidEntryPoint

  /**
   * 앱의 메인 액티비티
   *
   * @AndroidEntryPoint 어노테이션으로 Hilt가 의존성 주입 가능
   * Jetpack Compose를 사용하여 UI 렌더링
   */
  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)

          setContent {
              // TODO: DetoxyTheme 적용 (Week 3)
              // TODO: MainScreen 구현 (Week 3)
          }
      }
  }
  ```

- [ ] **기본 DI 모듈 생성**

  1. **DatabaseModule** - Room 데이터베이스 제공

     **파일**: `app/src/main/java/com/allday/detoxy/core/di/DatabaseModule.kt`

     ```kotlin
     package com.allday.detoxy.core.di

     import android.content.Context
     import androidx.room.Room
     import com.allday.detoxy.data.local.DetoxyDatabase
     import dagger.Module
     import dagger.Provides
     import dagger.hilt.InstallIn
     import dagger.hilt.android.qualifiers.ApplicationContext
     import dagger.hilt.components.SingletonComponent
     import javax.inject.Singleton

     /**
      * Room 데이터베이스 의존성 제공 모듈
      *
      * @InstallIn(SingletonComponent::class)로 앱 전체 생명주기 동안 싱글톤 유지
      */
     @Module
     @InstallIn(SingletonComponent::class)
     object DatabaseModule {

         @Provides
         @Singleton
         fun provideDetoxyDatabase(
             @ApplicationContext context: Context
         ): DetoxyDatabase {
             return Room.databaseBuilder(
                 context,
                 DetoxyDatabase::class.java,
                 "detoxy_database"
             ).build()
         }

         @Provides
         fun provideFocusSessionDao(database: DetoxyDatabase) =
             database.sessionDao()

         @Provides
         fun provideUserSettingsDao(database: DetoxyDatabase) =
             database.settingsDao()
     }
     ```

  2. **RepositoryModule** - Repository 구현체 제공

     **파일**: `app/src/main/java/com/allday/detoxy/core/di/RepositoryModule.kt`

     ```kotlin
     package com.allday.detoxy.core.di

     import com.allday.detoxy.data.repository.FocusRepositoryImpl
     import com.allday.detoxy.domain.repository.FocusRepository
     import dagger.Binds
     import dagger.Module
     import dagger.hilt.InstallIn
     import dagger.hilt.components.SingletonComponent
     import javax.inject.Singleton

     /**
      * Repository 인터페이스와 구현체 바인딩 모듈
      *
      * @Binds를 사용하여 인터페이스를 구현체로 매핑
      * Clean Architecture의 의존성 역전 원칙(DIP) 구현
      */
     @Module
     @InstallIn(SingletonComponent::class)
     abstract class RepositoryModule {

         @Binds
         @Singleton
         abstract fun bindFocusRepository(
             impl: FocusRepositoryImpl
         ): FocusRepository
     }
     ```

  - **DatabaseModule 설명**:
    - `@Module`: Hilt에게 이 클래스가 의존성 제공 모듈임을 알림
    - `@InstallIn(SingletonComponent::class)`: 앱 전체 생명주기 동안 싱글톤으로 유지
    - `@Provides`: 의존성 제공 메서드 표시
    - `@Singleton`: 인스턴스를 하나만 생성
    - Room 데이터베이스와 DAO를 제공

  - **RepositoryModule 설명**:
    - `@Binds`: 인터페이스와 구현체를 연결 (추상 메서드 사용)
    - Clean Architecture 원칙에 따라 domain 계층은 data 계층을 직접 의존하지 않음
    - Repository 인터페이스(domain)를 구현체(data)로 매핑

- [ ] **Hilt 설정 확인**

  1. **빌드 성공 확인**
     - Build → Rebuild Project
     - "BUILD SUCCESSFUL" 메시지 확인

  2. **Hilt 코드 생성 확인**
     - `app/build/generated/hilt` 폴더에 Hilt 생성 코드 존재 확인
     - `Hilt_DetoxyApplication.java` 파일 생성 확인

  3. **에러 해결**
     - kapt 오류 발생 시: File → Invalidate Caches → Restart
     - 의존성 오류 시: Gradle 버전 및 Kotlin 버전 확인

### 1.2 AccessibilityService 구현 (Day 2-3)

#### 1.2.1 AccessibilityService 기본 구조
- [ ] **FocusAccessibilityService 클래스 생성**
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
- [ ] **하드코딩된 차단 앱 리스트**
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
- [ ] **accessibility_service_config.xml**
  ```xml
  <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
      android:accessibilityEventTypes="typeWindowStateChanged"
      android:accessibilityFeedbackType="feedbackGeneric"
      android:canRetrieveWindowContent="true"
      android:description="@string/accessibility_description" />
  ```

#### 1.2.4 권한 요청 UI (간소화)
- [ ] **접근성 권한 안내 다이얼로그**
  - 설정 화면으로 이동 버튼
  - 권한 상태 확인 로직
  - **MVP**: 복잡한 튜토리얼 없이 간단 안내만

### 1.3 기본 타이머 기능 (Day 4-5)

#### 1.3.1 타이머 상태 관리
- [ ] **FocusTimer 클래스**
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
- [ ] **TimerScreen Compose**
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
- [ ] **타이머 시작 시**
  - AccessibilityService 상태 확인
  - 세션 시작 기록 (Room DB)
  - DND 활성화 (다음 주)

---

## 📅 Week 2: 잠금 화면 + DND + 데이터 저장

### 2.1 오버레이 잠금 화면 (Day 6-7)

#### 2.1.1 LockOverlayService 구현
- [ ] **포그라운드 서비스**
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

#### 2.1.2 오버레이 UI (MVP 간소화)
- [ ] **잠금 화면 최소 UI**
  ```kotlin
  // 반투명 배경
  // 남은 시간 텍스트
  // 작은 "포기" 버튼 (하단)
  ```
  - **MVP 제외**: 미션 해제, 퍼즐 해제 → 1차 릴리스
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 진행 중

#### 2.1.3 오버레이 권한 관리
- [ ] **SYSTEM_ALERT_WINDOW 권한**
  - 권한 요청 및 설정 화면 이동
  - **MVP**: 간단한 안내 메시지만

### 2.2 DND 제어 (Day 8)

#### 2.2.1 DND 모드 제어
- [ ] **DndManager 구현**
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
- [ ] **알림 정책 접근 권한**
  - 설정 화면으로 이동
  - 권한 상태 확인

### 2.3 Room 데이터베이스 (Day 9-10)

#### 2.3.1 핵심 엔티티 (MVP 최소)
- [ ] **FocusSession 엔티티**
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

- [ ] **UserSettings 엔티티**
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
- [ ] **FocusSessionDao**
  ```kotlin
  @Dao
  interface FocusSessionDao {
      @Insert
      suspend fun insert(session: FocusSession)

      @Query("SELECT * FROM focus_sessions WHERE DATE(startTime/1000, 'unixepoch') = DATE('now')")
      fun getTodaySessions(): Flow<List<FocusSession>>
  }
  ```

- [ ] **UserSettingsDao**
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
- [ ] **DetoxyDatabase**
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
- [ ] **간단한 포인트 계산**
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
- [ ] **EndFocusUseCase**
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
- [ ] **ReportScreen Compose**
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
- [ ] **오늘 세션 목록 표시**
  ```kotlin
  LazyColumn {
      items(todaySessions) { session ->
          SessionCard(session)
      }
  }
  ```

### 3.3 기본 UI 완성 (Day 14-15)

#### 3.3.1 네비게이션 구조
- [ ] **간단한 탭 네비게이션**
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
- [ ] **기본 테마 설정**
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
- [ ] **3단계 온보딩**
  ```kotlin
  @Composable
  fun OnboardingScreen() {
      var step by remember { mutableStateOf(0) }

      when (step) {
          0 -> WelcomeScreen { step++ }
          1 -> PermissionGuideScreen { step++ }
          2 -> CompleteScreen()
      }
  }
  ```

#### 4.1.2 권한 안내 화면
- [ ] **통합 권한 가이드**
  ```kotlin
  @Composable
  fun PermissionGuideScreen() {
      Column {
          PermissionCard(
              title = "접근성 서비스",
              description = "앱 차단을 위해 필요합니다",
              onRequestClick = { /* 설정 화면 이동 */ }
          )

          PermissionCard(
              title = "오버레이 권한",
              description = "잠금 화면 표시를 위해 필요합니다",
              onRequestClick = { /* 설정 화면 이동 */ }
          )

          PermissionCard(
              title = "방해금지 모드",
              description = "알림 차단을 위해 필요합니다",
              onRequestClick = { /* 설정 화면 이동 */ }
          )
      }
  }
  ```
  - **참조**: [prd.md](./prd.md) - 5) UX 플로우 1. 온보딩

### 4.2 테스트 (Day 18)

#### 4.2.1 핵심 기능 테스트
- [ ] **수동 테스트 체크리스트**
  - ✅ 타이머 시작 → 앱 차단 동작 확인
  - ✅ 타이머 완료 → 포인트 지급 확인
  - ✅ 타이머 포기 → 스릭 초기화 확인
  - ✅ 오버레이 잠금 화면 표시 확인
  - ✅ DND 활성화/비활성화 확인
  - ✅ 세션 데이터 저장 확인

#### 4.2.2 권한 플로우 테스트
- [ ] **권한 시나리오 테스트**
  - 접근성 권한 거부 시 앱 차단 안내
  - 오버레이 권한 거부 시 잠금 화면 안내
  - DND 권한 거부 시 알림 차단 안내

### 4.3 성능 최적화 (Day 19)

#### 4.3.1 배터리 최적화
- [ ] **포그라운드 서비스 최적화**
  - 불필요한 백그라운드 작업 제거
  - 배터리 최적화 예외 설정 안내
  - **목표**: 일일 배터리 사용량 5% 이하

#### 4.3.2 메모리 최적화
- [ ] **메모리 누수 점검**
  - 오버레이 뷰 해제 확인
  - 서비스 생명주기 관리

### 4.4 베타 준비 (Day 20)

#### 4.4.1 크래시 리포팅
- [ ] **Firebase Crashlytics 설정**
  ```kotlin
  dependencies {
      implementation("com.google.firebase:firebase-crashlytics-ktx")
  }
  ```

#### 4.4.2 분석 이벤트 (MVP 최소)
- [ ] **핵심 이벤트만 기록**
  ```kotlin
  // focus_start
  // focus_finish (success: true/false)
  // give_up
  ```
  - **참조**: [prd.md](./prd.md) - 14) 측정 지표

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
