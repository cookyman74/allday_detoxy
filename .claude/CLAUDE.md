# Allday Detoxy - Project Context

**Last Updated**: 2025-10-06
**Current Phase**: Week 3 - 포인트/스트릭 시스템 완료
**Build Status**: ✅ BUILD SUCCESSFUL

---

## 🎯 Project Overview

**Allday Detoxy**: 스마트폰 습관 교정 코치 앱 - Android 네이티브 앱으로 사용자의 스마트폰 과다 사용을 방지하고 집중 시간을 증가시킵니다.

**MVP 목표**: 앱 차단 + 보상 시스템을 통해 사용자의 집중 시간 증가 검증

---

## 🛠 Tech Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (선언형 UI)
- **Architecture**: Clean Architecture (domain, data, presentation)
- **DI**: Hilt (Dagger 기반)
- **Database**: Room (SQLite)
- **Async**: Kotlin Coroutines + Flow
- **State Management**: StateFlow

### Android System APIs
- **AccessibilityService**: 앱 차단 감지 및 제어
- **Overlay Service**: 전체 화면 잠금 UI
- **NotificationManager**: 방해금지 모드(DND)
- **Foreground Service**: 백그라운드 서비스

### Build Configuration
- **minSdk**: 26 (Android 8.0 Oreo)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34
- **JDK**: 17
- **Gradle**: 8.7
- **Kotlin Compiler Extension**: 1.5.1

---

## 📁 Project Structure

```
app/src/main/java/com/allday/detoxy/
├── core/                      # 공통 모듈
│   ├── di/                    # Hilt DI 모듈
│   │   ├── DatabaseModule.kt  # Room DB 의존성 주입
│   │   └── RepositoryModule.kt # Repository 바인딩
│   ├── manager/               # 매니저 클래스
│   │   └── DndManager.kt      # 방해금지 모드 제어
│   └── utils/                 # 유틸리티
│       └── PermissionUtils.kt # 권한 확인 헬퍼
│
├── data/                      # 데이터 계층
│   ├── local/                 # 로컬 데이터 소스
│   │   ├── entity/            # Room 엔티티
│   │   │   ├── FocusSession.kt    # 집중 세션 데이터
│   │   │   └── UserSettings.kt    # 사용자 설정 데이터
│   │   ├── dao/               # Room DAO
│   │   │   ├── FocusSessionDao.kt # 세션 CRUD
│   │   │   └── UserSettingsDao.kt # 설정 CRUD
│   │   └── DetoxyDatabase.kt  # Room 데이터베이스
│   └── repository/            # Repository 구현체
│       └── FocusRepositoryImpl.kt # Repository 구현
│
├── domain/                    # 도메인 계층 (비즈니스 로직)
│   ├── model/                 # 도메인 모델
│   │   ├── FocusState.kt      # 타이머 상태 (IDLE, RUNNING, SUCCESS, FAILED)
│   │   └── FocusTimer.kt      # 타이머 비즈니스 로직
│   ├── manager/               # 도메인 매니저
│   │   └── GamificationManager.kt # 포인트/스트릭 계산
│   └── repository/            # Repository 인터페이스
│       └── FocusRepository.kt # 데이터 접근 추상화
│
├── presentation/              # UI 계층
│   ├── ui/                    # Compose UI
│   │   ├── theme/             # 테마 설정
│   │   │   ├── Color.kt
│   │   │   ├── Type.kt
│   │   │   └── Theme.kt
│   │   ├── timer/             # 타이머 화면
│   │   │   └── TimerScreen.kt
│   │   ├── overlay/           # 오버레이 화면
│   │   │   └── LockOverlayScreen.kt
│   │   └── permission/        # 권한 다이얼로그
│   │       └── PermissionDialog.kt
│   └── viewmodel/             # ViewModel
│       └── TimerViewModel.kt  # 타이머 상태 관리
│
├── service/                   # Android 서비스
│   ├── accessibility/         # 접근성 서비스
│   │   └── FocusAccessibilityService.kt # 앱 차단 서비스
│   └── overlay/               # 오버레이 서비스
│       └── LockOverlayService.kt # 잠금 화면 서비스
│
├── DetoxyApplication.kt       # Application 클래스 (@HiltAndroidApp)
└── MainActivity.kt            # 메인 액티비티 (Compose 진입점)
```

---

## 🏗 Architecture Details

### Clean Architecture Implementation

#### 1. Domain Layer (비즈니스 로직)
**위치**: `domain/`

**역할**:
- 비즈니스 로직의 핵심
- 외부 의존성 없음 (가장 안정적인 계층)
- 인터페이스를 통한 추상화

**주요 컴포넌트**:
- `FocusRepository` (인터페이스): 데이터 접근 추상화
- `GamificationManager`: 포인트/스트릭 계산 로직
- `FocusTimer`: 타이머 비즈니스 로직
- `FocusState`: 타이머 상태 모델

#### 2. Data Layer (데이터 관리)
**위치**: `data/`

**역할**:
- 데이터 소스 관리 (Room DB)
- Repository 인터페이스 구현
- DAO를 통한 실제 데이터 작업

**주요 컴포넌트**:
- `FocusRepositoryImpl`: Repository 구현체
- `DetoxyDatabase`: Room 데이터베이스
- `FocusSessionDao`, `UserSettingsDao`: 데이터 접근 객체
- `FocusSession`, `UserSettings`: Room 엔티티

#### 3. Presentation Layer (UI)
**위치**: `presentation/`

**역할**:
- UI 렌더링 (Jetpack Compose)
- 사용자 입력 처리
- ViewModel을 통한 상태 관리

**주요 컴포넌트**:
- `TimerViewModel`: 타이머 상태 및 비즈니스 로직 연동
- `TimerScreen`: 타이머 UI
- `LockOverlayScreen`: 잠금 화면 UI

#### 4. Service Layer (시스템 통합)
**위치**: `service/`

**역할**:
- Android 시스템 서비스 관리
- 백그라운드 작업 처리

**주요 컴포넌트**:
- `FocusAccessibilityService`: 앱 차단 감지
- `LockOverlayService`: 전체 화면 잠금

---

## 🔌 Dependency Injection (Hilt)

### DatabaseModule
**파일**: `core/di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDetoxyDatabase(@ApplicationContext context: Context): DetoxyDatabase

    @Provides
    @Singleton
    fun provideFocusSessionDao(database: DetoxyDatabase): FocusSessionDao

    @Provides
    @Singleton
    fun provideUserSettingsDao(database: DetoxyDatabase): UserSettingsDao
}
```

**제공 항목**:
- `DetoxyDatabase`: 앱 전체 DB 인스턴스
- `FocusSessionDao`: 세션 데이터 접근
- `UserSettingsDao`: 설정 데이터 접근

### RepositoryModule
**파일**: `core/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository
}
```

**특징**:
- `@Binds`로 인터페이스-구현체 바인딩
- Clean Architecture의 의존성 역전 원칙(DIP) 구현
- `@Provides`보다 효율적

---

## 📊 Data Layer Details

### Room Database

#### FocusSession Entity
**파일**: `data/local/entity/FocusSession.kt`

```kotlin
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long,                    // Unix timestamp (milliseconds)
    val endTime: Long? = null,              // nullable: 진행 중 세션
    val durationMinutes: Int,               // 목표 시간
    val success: Boolean = false            // 성공 여부
)
```

**특징**:
- UUID 기반 고유 ID
- Unix timestamp로 시간대 문제 해결
- nullable `endTime`으로 진행 중 세션 표현

#### UserSettings Entity
**파일**: `data/local/entity/UserSettings.kt`

```kotlin
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,            // 단일 레코드
    val totalPoints: Int = 0,               // 총 포인트
    val currentStreak: Int = 0,             // 현재 스트릭
    val lastSuccessDate: String? = null     // 마지막 성공 날짜 (YYYY-MM-DD)
)
```

**특징**:
- 단일 레코드 (ID=1)
- 게임화 정보 저장

#### DAOs

**FocusSessionDao**:
- `insert()`, `update()`: 세션 CRUD
- `getTodaySessions()`: 오늘 세션 조회 (Flow)
- `getAllSessions()`: 모든 세션 조회 (최신순)
- `getSessionById()`: ID로 세션 조회
- `getSuccessfulSessions()`: 성공 세션만 조회

**UserSettingsDao**:
- `getSettings()`: 설정 조회 (Flow)
- `insert()`, `update()`: 설정 CRUD
- `addPoints(points)`: 포인트 증가
- `updateStreak(streak, date)`: 스트릭 업데이트
- `resetStreak()`: 스트릭 초기화

---

## 🎮 Gamification System

### GamificationManager
**파일**: `domain/manager/GamificationManager.kt`

```kotlin
@Singleton
class GamificationManager @Inject constructor() {

    // 포인트 계산 (MVP: 1분 = 1포인트)
    fun calculatePoints(durationMinutes: Int): Int {
        return durationMinutes
    }

    // 스트릭 업데이트
    fun updateStreak(settings: UserSettings, success: Boolean): UserSettings {
        val today = LocalDate.now().toString()

        return if (success) {
            val yesterday = LocalDate.now().minusDays(1).toString()

            if (settings.lastSuccessDate == yesterday) {
                // 어제 성공 → 스트릭 +1
                settings.copy(currentStreak = settings.currentStreak + 1, lastSuccessDate = today)
            } else {
                // 처음 또는 건너뜀 → 스트릭 1
                settings.copy(currentStreak = 1, lastSuccessDate = today)
            }
        } else {
            // 실패 → 스트릭 0
            settings.copy(currentStreak = 0)
        }
    }
}
```

**특징**:
- MVP 간소화: 1분 = 1포인트
- LocalDate로 날짜 비교 정확성
- Immutable 데이터 클래스 활용

---

## 🔄 Timer Workflow

### TimerViewModel
**파일**: `presentation/viewmodel/TimerViewModel.kt`

#### 의존성
```kotlin
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application,
    private val repository: FocusRepository,
    private val gamificationManager: GamificationManager
) : ViewModel()
```

#### 상태 관리
- `timerState: StateFlow<FocusState>`: 타이머 상태 (IDLE, RUNNING, SUCCESS, FAILED)
- `remainingSeconds: StateFlow<Int>`: 남은 시간 (초)
- `totalSeconds: StateFlow<Int>`: 전체 시간 (초)
- `currentSessionId: String?`: 현재 세션 ID

#### 타이머 시작 플로우
```kotlin
fun startTimer(durationMinutes: Int) {
    // 1. 세션 생성 및 저장
    val sessionId = UUID.randomUUID().toString()
    currentSessionId = sessionId
    viewModelScope.launch {
        repository.startSession(
            FocusSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                endTime = null,
                durationMinutes = durationMinutes,
                success = false
            )
        )
    }

    // 2. AccessibilityService 활성화
    FocusAccessibilityService.isTimerRunning = true

    // 3. LockOverlayService 시작 (포그라운드 서비스)
    LockOverlayService.showOverlay(application, remainingSeconds, totalSeconds)

    // 4. DND 모드 활성화
    dndManager.enableDnd()

    // 5. 타이머 시작
    focusTimer.start(durationMinutes) { success -> onTimerFinish(success) }
}
```

#### 타이머 완료 플로우
```kotlin
private fun onTimerFinish(success: Boolean) {
    // 1. 서비스 정리
    FocusAccessibilityService.isTimerRunning = false
    LockOverlayService.hideOverlay(application)
    dndManager.disableDnd()

    // 2. 세션 종료 및 보상 지급
    currentSessionId?.let { sessionId ->
        viewModelScope.launch {
            // 세션 종료
            repository.endSession(sessionId, success, System.currentTimeMillis())

            // 성공 시 포인트 지급 및 스트릭 업데이트
            if (success) {
                val settings = repository.getSettings().first()
                settings?.let {
                    val session = repository.getSession(sessionId).first()
                    session?.let { focusSession ->
                        // 포인트 계산 및 추가
                        val points = gamificationManager.calculatePoints(focusSession.durationMinutes)
                        repository.addPoints(points)

                        // 스트릭 업데이트
                        val updatedSettings = gamificationManager.updateStreak(it, success)
                        repository.updateStreak(updatedSettings.currentStreak, updatedSettings.lastSuccessDate ?: "")
                    }
                }
            }

            currentSessionId = null
        }
    }
}
```

---

## 🚀 Current Implementation Status

### ✅ Completed (Week 1-3)

#### Week 1: 기본 타이머 + 앱 차단
- [x] 타이머 UI (Jetpack Compose)
- [x] FocusTimer 도메인 모델
- [x] AccessibilityService 앱 차단
- [x] LockOverlayService 잠금 화면

#### Week 2: 시스템 통합 + 권한 + 데이터베이스
- [x] 권한 UI (PermissionDialog)
- [x] DND 제어 (DndManager)
- [x] Room 데이터베이스 (FocusSession, UserSettings)
- [x] DAO 구현 (FocusSessionDao, UserSettingsDao)
- [x] DatabaseModule (Hilt 의존성 주입)

#### Week 3: 보상 시스템 (진행 중)
- [x] 3.1 포인트/스트릭 시스템
  - [x] GamificationManager (포인트 계산, 스트릭 업데이트)
  - [x] FocusRepository 인터페이스/구현
  - [x] RepositoryModule (의존성 주입)
  - [x] TimerViewModel 통합 (세션 저장, 보상 지급)
- [ ] 3.2 일일 리포트 (Day 13)
- [ ] 3.3 UI 개선 (Day 14)

### 📋 Next Steps

#### Week 3: 보상 시스템 + 리포트 + UI
- [ ] 3.2 일일 리포트 (Day 13)
  - [ ] ReportScreen Compose UI
  - [ ] ReportViewModel
  - [ ] 오늘 성공 세션 수, 총 집중 시간
  - [ ] 현재 스트릭, 총 포인트 표시
  - [ ] 세션 리스트 (간단한 Card 리스트)

- [ ] 3.3 UI 개선 (Day 14)
  - [ ] 잠금 화면 디자인 개선
  - [ ] 타이머 화면 애니메이션
  - [ ] 성공/실패 피드백 화면

#### Week 4: 테스트 + 최적화 + 배포 준비
- [ ] 통합 테스트
- [ ] UI/UX 최적화
- [ ] 성능 최적화
- [ ] APK 빌드 및 배포 준비

---

## 📝 Development Guidelines

### Coding Conventions
- **Language**: Kotlin
- **Formatting**: Android Studio default (ktlint)
- **Architecture**: Clean Architecture 패턴 준수
- **DI**: Hilt를 통한 의존성 주입
- **Async**: Coroutines + Flow 사용
- **Comments**: KDoc 스타일 문서화

### Git Workflow
- **Commit Message**: 한글 작성
- **Format**: `[Week X] X.X 작업명` 또는 `feat/fix/docs: 설명`
- **Co-Author**: Claude Code 표시
- **Work History**: `working_history/YYYY-MM-DD_X.X.md` 형식

### Testing Strategy
- Unit Tests: ViewModel, UseCase, Repository 로직
- Integration Tests: Room DAO, Service 통합
- UI Tests: Compose UI 테스트 (Espresso)

---

## 🔧 Common Tasks

### Build Commands
```bash
# Debug 빌드
./gradlew assembleDebug

# Release 빌드
./gradlew assembleRelease

# Clean 빌드
./gradlew clean build

# 테스트 실행
./gradlew test
```

### Database Schema Migration
Room 데이터베이스 스키마 변경 시:
1. `DetoxyDatabase` version 증가
2. Migration 전략 정의
3. 테스트 데이터 백업

### Adding New Features
1. Domain Layer: 인터페이스 및 모델 정의
2. Data Layer: Repository 구현
3. Presentation Layer: ViewModel 및 UI
4. DI: Hilt 모듈 업데이트
5. Test: 단위 테스트 작성

---

## 📚 References

### Documentation
- [PRD](../docs/prd.md) - 제품 요구사항 정의서
- [MVP 개발 계획](../docs/00_mvp_allday_detoxy_todolist.md) - 4주 개발 일정
- [기술 설계 문서](../docs/00_android_allday_detoxy_plan.md) - 상세 기술 설계
- [개발환경 설정](../docs/00_kotlin_environment_todolist.md) - macOS 환경 구성

### Work History
- [2025-10-06_2.1](../working_history/2025-10-06_2.1.md) - 권한 UI/로직 (Week 2.1)
- [2025-10-06_2.2](../working_history/2025-10-06_2.2.md) - DND 제어 (Week 2.2)
- [2025-10-06_2.3](../working_history/2025-10-06_2.3.md) - Room 데이터베이스 (Week 2.3)
- [2025-10-06_3.1](../working_history/2025-10-06_3.1.md) - 포인트/스트릭 시스템 (Week 3.1)

### External Resources
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**End of Project Context**
