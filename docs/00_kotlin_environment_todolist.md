# Kotlin 안드로이드 개발환경 구축 체크리스트 (macOS)

## 📋 프로젝트 개요
**프로젝트명**: Allday Detoxy - 스마트폰 습관 교정 코치 앱
**기술 스택**: Kotlin 네이티브 + Jetpack Compose
**개발 환경**: macOS (Apple Silicon 최적화)
**목표**: 개발환경 구축 및 기본 프로젝트 구조 설정

---

## 🎯 개발환경 구축 목표
- Android Studio 설치 및 macOS 환경 최적화
- Apple Silicon (M1/M2/M3) 최적화 설정
- 프로젝트 생성 및 기본 구조 설정
- 필수 권한 및 의존성 설정
- 기본 개발 환경 완성

## 📅 1단계: Android Studio 설치 및 macOS 환경 설정

### 1.1 Android Studio 설치
- [x] **Android Studio 다운로드 및 설치**
  - [x] [Android Studio 공식 사이트](https://developer.android.com/studio)에서 최신 버전 다운로드
  - [x] Apple Silicon용 버전 확인 (arm64 네이티브 지원)
  - [x] DMG 파일 실행 후 Applications 폴더로 드래그
  - [x] `/Applications/Android Studio.app` 경로 확인
  - [ ] 첫 실행 시 설정 마법사 완료

- [x] **SDK 구성 요소 설치**
  - [x] SDK Manager 실행 (sdkmanager CLI 사용)
  - [x] Android SDK 34 (API Level 34) 설치
  - [x] Android SDK Build-Tools 34.0.0 설치
  - [x] Android Emulator 설치 (v36.1.9)
  - [x] Android SDK Platform-Tools 설치 (v36.0.0)
  - [x] **Apple Silicon용 ARM64 시스템 이미지 설치** (google_apis;arm64-v8a)
  - [ ] Kotlin 플러그인 확인 및 업데이트 (최신 버전)

### 1.2 macOS 환경변수 설정
- [x] **ANDROID_HOME 환경변수 설정**
  ```bash
  # ~/.zshrc 파일에 추가 완료
  export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
  export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
  export PATH=$PATH:$ANDROID_HOME/platform-tools
  export PATH=$PATH:$ANDROID_HOME/emulator

  # 변경사항 적용
  source ~/.zshrc
  ```

- [x] **Java JDK 확인**
  - [x] Android Studio 번들 JDK 확인 (기본값: `/Applications/Android Studio.app/Contents/jbr`)
  - [x] 터미널에서 `java -version` 실행하여 JDK 17+ 확인 (현재 OpenJDK 20.0.2 설치됨)
  - [ ] 필요시 Homebrew로 별도 설치: `brew install openjdk@17`

### 1.3 Apple Silicon 최적화 설정
- [ ] **에뮬레이터 ARM64 이미지 설정**
  - [ ] AVD Manager에서 새 가상 디바이스 생성
  - [ ] System Image: **ARM64 (arm64-v8a)** 이미지 선택 (중요!)
  - [ ] 하드웨어 가속: Hypervisor.framework 사용 (HAXM 아님)
  - [ ] Graphics: Hardware - GLES 3.0 선택

- [ ] **성능 최적화**
  - [ ] Android Studio > Preferences > Appearance & Behavior > System Settings
  - [ ] Memory Settings: Heap size 4096 MB 이상 권장
  - [ ] Gradle 설정: `org.gradle.jvmargs=-Xmx4096m` 설정

### 1.4 개발 환경 검증
- [ ] **설치 확인**
  - [ ] Android Studio 정상 실행 확인
  - [ ] SDK 경로 설정 확인 (File → Project Structure → SDK Location)
  - [ ] 기본 경로: `~/Library/Android/sdk` 확인
  - [ ] ARM64 에뮬레이터 생성 및 실행 테스트
  - [ ] Kotlin 컴파일러 동작 확인
  - [ ] 터미널에서 `adb devices` 명령 실행 확인

## 📅 2단계: 프로젝트 생성 및 기본 구조 설정

### 2.1 새 프로젝트 생성
- [ ] **프로젝트 생성**
  - [ ] Android Studio에서 "New Project" 클릭
  - [ ] "Empty Activity" 템플릿 선택
  - [ ] 프로젝트명: `allday_detoxy_android`
  - [ ] 패키지명: `com.allday.detoxy`
  - [ ] 언어: Kotlin
  - [ ] 최소 SDK: **API 26 (Android 8.0)** (권장: API 24보다 안정적)
  - [ ] 타겟 SDK: API 34 (Android 14)
  - [ ] Build configuration language: **Kotlin DSL (build.gradle.kts)** 선택
  - [ ] "Finish" 클릭하여 프로젝트 생성

### 2.2 프로젝트 구조 설정
- [ ] **폴더 구조 생성**
  ```
  app/src/main/java/com/allday/detoxy/
  ├── core/
  │   ├── di/
  │   ├── network/
  │   └── utils/
  ├── data/
  │   ├── local/
  │   ├── remote/
  │   └── repository/
  ├── domain/
  │   ├── model/
  │   ├── repository/
  │   └── usecase/
  ├── presentation/
  │   ├── ui/
  │   ├── viewmodel/
  │   └── navigation/
  └── service/
      ├── accessibility/
      ├── overlay/
      └── sms/
  ```

### 2.3 Git 저장소 설정
- [ ] **Git 초기화**
  - [ ] 터미널에서 프로젝트 루트 디렉토리로 이동
  - [ ] `git init` 실행
  - [ ] `.gitignore` 파일 생성 (Android 템플릿 사용)
  - [ ] 첫 커밋 생성: `git add . && git commit -m "Initial commit"`
  - [ ] 원격 저장소 연결 (필요시)
  - [ ] 기본 브랜치 설정: `git branch -M main`

## 📅 3단계: 의존성 및 라이브러리 설정

### 3.1 버전 카탈로그 설정 (libs.versions.toml)
- [ ] **gradle/libs.versions.toml 파일 생성 및 설정**
  ```toml
  [versions]
  # Kotlin & Android
  kotlin = "1.9.22"
  androidGradlePlugin = "8.2.2"

  # Compose
  composeBom = "2024.02.00"
  activityCompose = "1.8.2"
  navigationCompose = "2.7.7"

  # Dependency Injection
  hilt = "2.50"
  hiltNavigationCompose = "1.1.0"

  # Architecture Components
  lifecycle = "2.7.0"
  room = "2.6.1"

  # Coroutines
  coroutines = "1.7.3"

  # Utilities
  timber = "5.0.1"
  datastore = "1.0.0"

  # Chart Library
  mpAndroidChart = "3.1.0"

  # Testing
  junit = "4.13.2"
  junitExt = "1.1.5"
  espresso = "3.5.1"

  [libraries]
  # Compose BOM
  androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
  androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
  androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
  androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
  androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
  androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
  androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

  # Activity & Navigation
  androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
  androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

  # Hilt
  hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
  hilt-android-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
  androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

  # Lifecycle
  androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
  androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

  # Room
  androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
  androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
  androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

  # Coroutines
  kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

  # Utilities
  timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
  androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

  # Chart
  mpandroidchart = { group = "com.github.PhilJay", name = "MPAndroidChart", version.ref = "mpAndroidChart" }

  # Testing
  junit = { group = "junit", name = "junit", version.ref = "junit" }
  androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitExt" }
  androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

  [plugins]
  android-application = { id = "com.android.application", version.ref = "androidGradlePlugin" }
  kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
  hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
  kotlin-ksp = { id = "com.google.devtools.ksp", version = "1.9.22-1.0.17" }
  kotlin-parcelize = { id = "org.jetbrains.kotlin.plugin.parcelize", version.ref = "kotlin" }
  ```

### 3.2 Project Level build.gradle.kts 설정
- [ ] **플러그인 설정**
  ```kotlin
  plugins {
      alias(libs.plugins.android.application) apply false
      alias(libs.plugins.kotlin.android) apply false
      alias(libs.plugins.hilt.android) apply false
      alias(libs.plugins.kotlin.ksp) apply false
  }
  ```

### 3.3 App Level build.gradle.kts 설정
- [ ] **플러그인 추가**
  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.hilt.android)
      alias(libs.plugins.kotlin.ksp)  // kapt 대신 ksp 사용 (성능 향상)
      alias(libs.plugins.kotlin.parcelize)
  }
  ```

- [ ] **의존성 라이브러리 추가**
  ```kotlin
  dependencies {
      // Compose BOM
      implementation(platform(libs.androidx.compose.bom))
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)
      implementation(libs.androidx.compose.ui.tooling.preview)
      implementation(libs.androidx.compose.material3)

      // Activity & Navigation
      implementation(libs.androidx.activity.compose)
      implementation(libs.androidx.navigation.compose)

      // Hilt
      implementation(libs.hilt.android)
      ksp(libs.hilt.android.compiler)
      implementation(libs.androidx.hilt.navigation.compose)

      // Lifecycle
      implementation(libs.androidx.lifecycle.runtime.ktx)
      implementation(libs.androidx.lifecycle.viewmodel.compose)

      // Room
      implementation(libs.androidx.room.runtime)
      implementation(libs.androidx.room.ktx)
      ksp(libs.androidx.room.compiler)

      // Coroutines
      implementation(libs.kotlinx.coroutines.android)

      // Utilities
      implementation(libs.timber)
      implementation(libs.androidx.datastore.preferences)

      // Chart
      implementation(libs.mpandroidchart)

      // Testing
      testImplementation(libs.junit)
      androidTestImplementation(libs.androidx.test.ext.junit)
      androidTestImplementation(libs.androidx.test.espresso.core)
      androidTestImplementation(platform(libs.androidx.compose.bom))

      // Debug
      debugImplementation(libs.androidx.compose.ui.tooling)
      debugImplementation(libs.androidx.compose.ui.test.manifest)
  }
  ```

### 3.4 Gradle 최적화 설정
- [ ] **gradle.properties 설정**
  ```properties
  # Gradle 성능 최적화 (macOS)
  org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
  org.gradle.parallel=true
  org.gradle.caching=true
  org.gradle.configureondemand=true

  # Kotlin 컴파일러 최적화
  kotlin.code.style=official
  kotlin.incremental=true
  kotlin.incremental.useClasspathSnapshot=true

  # Android 최적화
  android.useAndroidX=true
  android.enableJetifier=false
  ```

### 3.5 Gradle 동기화
- [ ] **동기화 완료**
  - [ ] "Sync Now" 클릭
  - [ ] 빌드 오류 없이 완료 확인
  - [ ] 의존성 다운로드 완료 확인 (최초 5-10분 소요 가능)
  - [ ] macOS Firewall 허용 확인 (필요시)

## 📅 4단계: AndroidManifest.xml 권한 및 서비스 설정

### 4.1 필수 권한 추가
- [ ] **AndroidManifest.xml 권한 추가**
  ```xml
  <!-- 시스템 오버레이 권한 -->
  <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

  <!-- 포그라운드 서비스 -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

  <!-- SMS 관련 권한 -->
  <uses-permission android:name="android.permission.SEND_SMS" />
  <uses-permission android:name="android.permission.READ_SMS" />
  <uses-permission android:name="android.permission.RECEIVE_SMS" />

  <!-- 전화 및 로그 -->
  <uses-permission android:name="android.permission.READ_PHONE_STATE" />
  <uses-permission android:name="android.permission.READ_CALL_LOG" />

  <!-- 앱 사용 통계 -->
  <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
      tools:ignore="ProtectedPermissions" />

  <!-- 알림 및 알람 -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
  ```

### 4.2 서비스 선언
- [ ] **서비스 등록**
  ```xml
  <application>
      <!-- AccessibilityService -->
      <service
          android:name=".service.accessibility.FocusAccessibilityService"
          android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
          android:exported="false">
          <intent-filter>
              <action android:name="android.accessibilityservice.AccessibilityService" />
          </intent-filter>
          <meta-data
              android:name="android.accessibilityservice"
              android:resource="@xml/accessibility_service_config" />
      </service>

      <!-- ForegroundService -->
      <service
          android:name=".service.overlay.LockOverlayService"
          android:enabled="true"
          android:exported="false"
          android:foregroundServiceType="mediaProjection" />

      <!-- SMS BroadcastReceiver -->
      <receiver
          android:name=".service.sms.SmsReceiver"
          android:enabled="true"
          android:exported="true">
          <intent-filter android:priority="999">
              <action android:name="android.provider.Telephony.SMS_RECEIVED" />
          </intent-filter>
      </receiver>
  </application>
  ```

### 4.3 macOS 권한 테스트 설정
- [ ] **macOS에서 권한 테스트 준비**
  - [ ] 에뮬레이터 Settings → Accessibility → 앱 권한 활성화 방법 확인
  - [ ] Settings → Special app access → Display over other apps 확인
  - [ ] Settings → Apps → Special app access → Usage access 확인
  - [ ] macOS System Preferences에서 Android Studio 접근성 권한 허용 (필요시)

---

## 📅 5단계: 기본 설정 완료

### 5.1 Hilt 설정
- [ ] **Application 클래스 생성**
  - [ ] `DetoxyApplication` 클래스 생성
  - [ ] `@HiltAndroidApp` 어노테이션 추가
  - [ ] AndroidManifest.xml에 Application 등록

- [ ] **MainActivity 설정**
  - [ ] `@AndroidEntryPoint` 어노테이션 추가
  - [ ] Compose 설정 확인

### 5.2 기본 UI 구조
- [ ] **Compose 설정**
  - [ ] MainActivity Compose 설정
  - [ ] 기본 테마 설정 (Material3)
  - [ ] 네비게이션 구조 설정

### 5.3 테스트 환경
- [ ] **테스트 설정**
  - [ ] 단위 테스트 설정
  - [ ] UI 테스트 설정
  - [ ] 테스트 실행 확인

---

## 📅 6단계: 개발환경 검증

### 6.1 빌드 테스트
- [ ] **빌드 확인**
  - [ ] Clean Build 실행
  - [ ] 빌드 오류 없이 완료 확인
  - [ ] APK 생성 확인

### 6.2 에뮬레이터 테스트
- [ ] **에뮬레이터 실행**
  - [ ] 에뮬레이터에서 앱 실행
  - [ ] 기본 UI 표시 확인
  - [ ] 로그 출력 확인

### 6.3 개발 도구 확인
- [ ] **도구 설정**
  - [ ] Logcat 설정 확인
  - [ ] 디버거 설정 확인
  - [ ] 프로파일러 설정 확인

---

## ✅ 개발환경 구축 완료 체크리스트

### 필수 확인 사항
- [x] Android Studio 정상 설치 및 실행 (설치 완료, SDK 설정 필요)
- [ ] 프로젝트 생성 및 빌드 성공
- [ ] 모든 의존성 라이브러리 추가 완료
- [ ] Git 저장소 설정 완료
- [ ] 기본 권한 설정 완료
- [ ] Hilt 의존성 주입 설정 완료
- [ ] Compose UI 기본 설정 완료
- [ ] 테스트 환경 설정 완료

### 다음 단계 준비
- [ ] 개발환경 구축 완료 확인
- [ ] 다음 개발 단계 계획 수립
- [ ] 팀원과 환경 공유 (필요시)

---

## 🚨 주의사항 및 문제해결

### macOS 환경 특화 주의사항
- [ ] **Apple Silicon (M1/M2/M3) 필수 확인사항**
  - [ ] ARM64 에뮬레이터 이미지만 사용 (x86 이미지는 성능 저하)
  - [ ] Rosetta 2 설치 확인: `softwareupdate --install-rosetta`
  - [ ] Homebrew ARM 버전 설치 확인: `/opt/homebrew/bin/brew`

- [ ] **SDK 및 Gradle 호환성**
  - [ ] Kotlin 1.9.22 + Gradle 8.2.2 조합 권장
  - [ ] KSP 버전은 Kotlin 버전과 동일한 메이저/마이너 버전 사용
  - [ ] Compose BOM 사용으로 버전 충돌 방지

- [ ] **빌드 성능 문제 해결**
  - [ ] 빌드 느림 → gradle.properties에서 메모리 증가 (4GB → 6GB)
  - [ ] 의존성 충돌 → `./gradlew dependencies` 실행하여 의존성 트리 확인
  - [ ] 빌드 캐시 삭제: `./gradlew clean` 또는 `~/.gradle/caches` 삭제

### 자주 발생하는 문제 해결

#### 문제 1: 에뮬레이터 실행 안됨
```bash
# 해결방법 1: AVD 재생성 (ARM64 이미지로)
# 해결방법 2: Hypervisor 권한 확인
sudo xcode-select --install

# 해결방법 3: QEMU 재설치
brew reinstall qemu
```

#### 문제 2: Gradle Sync 실패
```bash
# 해결방법 1: Gradle 캐시 삭제
rm -rf ~/.gradle/caches/

# 해결방법 2: Android Studio 캐시 삭제
# File → Invalidate Caches → Invalidate and Restart
```

#### 문제 3: KSP 빌드 오류
```kotlin
// build.gradle.kts에서 ksp 버전 확인
// Kotlin 1.9.22 → KSP 1.9.22-1.0.17 사용
```

#### 문제 4: macOS 권한 거부
```bash
# macOS System Preferences 확인
# Security & Privacy → Privacy → Accessibility
# Android Studio 및 에뮬레이터 앱 허용
```

### 성능 최적화 체크리스트
- [ ] **Gradle 빌드 최적화**
  - [ ] gradle.properties 최적화 설정 완료
  - [ ] Build cache 활성화 확인
  - [ ] Parallel execution 활성화

- [ ] **메모리 설정 최적화**
  - [ ] Android Studio Heap: 4GB 이상
  - [ ] Gradle JVM: 4GB 이상
  - [ ] 에뮬레이터 RAM: 2GB-4GB (디바이스 사양에 따라)

- [ ] **에뮬레이터 성능 최적화**
  - [ ] ARM64 이미지 사용 (필수)
  - [ ] Hardware acceleration 활성화
  - [ ] Multi-Core CPU 할당 (2-4 코어)
  - [ ] Graphics: Hardware - GLES 3.0

### 추가 리소스
- **Android Studio 공식 문서**: https://developer.android.com/studio
- **Jetpack Compose 가이드**: https://developer.android.com/jetpack/compose
- **Hilt 의존성 주입**: https://developer.android.com/training/dependency-injection/hilt-android
- **macOS 개발 환경 가이드**: https://developer.android.com/studio/install#mac

---

## 📝 다음 단계

### 개발환경 구축 완료 후
1. **AccessibilityService 구현** (00_android_environment.md 참고)
2. **기본 Compose UI 구조 설정**
3. **Hilt 의존성 주입 구조 구축**
4. **Room 데이터베이스 설계**
5. **실제 앱 기능 개발 시작**

### 검증 완료 체크리스트
- [x] Android Studio 정상 실행 (Apple Silicon 네이티브) - 설치 확인 완료
- [ ] ARM64 에뮬레이터 정상 실행
- [ ] 프로젝트 빌드 성공 (에러 없음)
- [ ] Git 저장소 설정 완료
- [ ] 모든 의존성 다운로드 완료
- [ ] 기본 Compose UI 표시 확인
- [ ] macOS 환경변수 설정 완료

> **참고**: 이 체크리스트는 macOS 환경에 최적화된 Kotlin 안드로이드 개발환경 구축 가이드입니다. Apple Silicon Mac 사용자는 반드시 ARM64 에뮬레이터를 사용해야 최적의 성능을 얻을 수 있습니다.
