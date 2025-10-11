# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Allday Detoxy** is an Android native app for smartphone habit correction, helping users maintain focus by blocking distracting apps during timer sessions. Built with Kotlin and Jetpack Compose following Clean Architecture principles.

**MVP Goal**: Validate user focus time increase through app blocking + reward system.

## Build & Development Commands

### Building
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "com.allday.detoxy.YourTestClass"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Code Quality
```bash
# Kotlin compilation check
./gradlew compileDebugKotlin

# Lint check
./gradlew lint
```

## Architecture Overview

### Clean Architecture Layers

**Domain Layer** (`domain/`): Business logic, framework-independent
- `model/`: Domain models (FocusTimer, FocusState enum)
- `usecase/`: Business use cases (Week 3+)

**Data Layer** (`data/`): Data sources and repositories
- `local/`: Room database entities, DAOs (Week 2+)
- `repository/`: Repository implementations (Week 2+)

**Presentation Layer** (`presentation/`): UI and ViewModels
- `ui/`: Jetpack Compose screens organized by feature
  - `timer/`: Timer screen with circular progress
  - `overlay/`: Lock overlay screen
  - `permission/`: Permission dialogs and guides
  - `theme/`: Material3 theme configuration
- `viewmodel/`: Hilt ViewModels with StateFlow

**Service Layer** (`service/`): Android system services
- `accessibility/`: FocusAccessibilityService for app blocking
- `overlay/`: LockOverlayService for full-screen lock

**Core Layer** (`core/`): Shared utilities
- `di/`: Hilt dependency injection modules
- `utils/`: Permission utilities, helpers

### Key Architecture Patterns

**State Management**:
- StateFlow for reactive state in ViewModels
- Jetpack Compose collectAsState() for UI observation
- Single source of truth in domain layer (FocusTimer)

**Dependency Injection**:
- Hilt for all ViewModels and services
- Application context injected where needed (e.g., TimerViewModel)
- `@AndroidEntryPoint` annotation for Android components

**Service Communication**:
- Static flags for MVP (e.g., `FocusAccessibilityService.isTimerRunning`)
- Intent-based actions for LockOverlayService
- TODO: Refactor to StateFlow in Week 2

## Critical Implementation Details

### Timer Lifecycle Integration

The timer integrates FOUR components that must be synchronized:

1. **FocusTimer** (domain model): Manages countdown with coroutines
2. **AccessibilityService**: Blocks apps when `isTimerRunning = true`
3. **LockOverlayService**: Shows full-screen overlay on blocked app access
4. **DndManager** (Week 2): Activates Do Not Disturb mode during focus sessions

**Startup sequence** (see `TimerViewModel.startTimer()`):
```kotlin
// In TimerViewModel.startTimer()
1. FocusAccessibilityService.isTimerRunning = true
2. LockOverlayService.showOverlay(context, remainingSeconds, totalSeconds)
3. dndManager.enableDnd() // Android 6.0+ only
4. focusTimer.start(duration) { success -> onTimerFinish(success) }
```

**Cleanup sequence** (applies to `giveUpTimer()`, `resetTimer()`, `onTimerFinish()`):
```kotlin
// In TimerViewModel (giveUp/reset/onFinish)
1. FocusAccessibilityService.isTimerRunning = false
2. LockOverlayService.hideOverlay(context)
3. dndManager.disableDnd() // Android 6.0+ only
4. focusTimer.giveUp() or reset()
```

**Critical**: All four components must be synchronized. If one fails, all must be cleaned up to avoid inconsistent state.

### AccessibilityService Implementation

**Purpose**: Detects when user opens blocked apps and triggers home navigation.

**Key mechanism**:
- Listens to `TYPE_WINDOW_STATE_CHANGED` events
- Checks package name against hardcoded blocked apps list
- Navigates to home using `Intent.ACTION_MAIN + CATEGORY_HOME`
- Only active when `isTimerRunning = true`

**Blocked apps** (hardcoded in MVP):
- Instagram: `com.instagram.android`
- TikTok: `com.zhiliaoapp.musically`
- YouTube: `com.google.android.youtube`
- Facebook: `com.facebook.katana`

**Permission required**: User must manually enable in Settings → Accessibility → Allday Detoxy

### Overlay Service Architecture

**LockOverlayService** is a foreground service that displays a ComposeView overlay:

**WindowManager configuration**:
- Type: `TYPE_APPLICATION_OVERLAY` (API 26+) or `TYPE_SYSTEM_ALERT` (older)
- Flags: `FLAG_LAYOUT_IN_SCREEN | FLAG_KEEP_SCREEN_ON`
  - Note: `FLAG_NOT_FOCUSABLE` removed to allow overlay to receive focus and stay on top
  - Note: `FLAG_NOT_TOUCH_MODAL` removed to block touches outside overlay
- PixelFormat: `TRANSLUCENT` for semi-transparent background

**Compose Integration**:
```kotlin
ComposeView(context).apply {
    // CRITICAL: Set Lifecycle for Compose to work properly
    setViewTreeLifecycleOwner(this@LockOverlayService)
    setViewTreeSavedStateRegistryOwner(this@LockOverlayService)
    
    setContent {
        DetoxyTheme {
            LockOverlayScreen(
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                timerState = timerState, // mutableStateOf for reactive updates
                onGiveUp = { /* handle give up */ }
            )
        }
    }
}
```

**Timer Updates**: Uses Compose `mutableStateOf` for reactive UI updates. Service updates `timerState.value` every second, and Compose automatically recomposes the UI.

**Permission required**: `SYSTEM_ALERT_WINDOW` - User must grant "Display over other apps"

### Hilt Setup

**Application class**: `DetoxyApplication` with `@HiltAndroidApp`

**ViewModel injection**:
```kotlin
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application
) : ViewModel()
```

**Service injection**: Use `@AndroidEntryPoint` on services for field injection

## Work History & Documentation Pattern

All development work is documented in `working_history/` with the pattern:
- Filename: `YYYY-MM-DD_[task-number].md` (e.g., `2025-10-06_2.1.md`)
- Must include: task overview, completed items, code snippets, build verification, commit ID
- Checklist updates in `docs/00_mvp_allday_detoxy_todolist.md`

**When completing tasks**:
1. Implement features
2. Build and verify: `./gradlew assembleDebug`
3. Update checklist with `[x]`
4. Create work history markdown
5. Commit with detailed message
6. Add commit ID to work history
7. Commit work history update

### DND (Do Not Disturb) Manager

**DndManager** (`core/manager/DndManager.kt`) controls notification blocking during focus sessions:

**Key features**:
- Blocks all notifications except alarms (`INTERRUPTION_FILTER_ALARMS`)
- Requires Android 6.0+ (API 23) and manual permission grant
- Permission check: `hasNotificationPolicyAccess()`
- Graceful degradation: Timer works even without DND permission

**Integration**:
```kotlin
// TimerViewModel creates instance
private val dndManager = DndManager(application)

// Enabled on timer start (if permission granted)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    dndManager.enableDnd()
}

// Disabled on timer stop/finish/give-up
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    dndManager.disableDnd()
}
```

**Permission utilities** (in `PermissionUtils`):
- `hasNotificationPolicyAccess(context)`: Check DND permission
- `openNotificationPolicySettings(context)`: Navigate to DND settings

### Room Database (Week 2.3)

**DetoxyDatabase** (`data/local/DetoxyDatabase.kt`) provides local data persistence:

**Entities**:
- **FocusSession**: Stores focus session records
  - UUID-based unique ID
  - Unix timestamp for start/end times
  - Duration (minutes) and success status
  - File: `data/local/entity/FocusSession.kt`

- **UserSettings**: Stores user gamification data
  - Single record (ID=1) for app-wide settings
  - Total points, current streak, last success date
  - File: `data/local/entity/UserSettings.kt`

**DAOs**:
- **FocusSessionDao** (`data/local/dao/FocusSessionDao.kt`):
  - `getTodaySessions()`: Flow of today's sessions
  - `getAllSessions()`: Flow of all sessions (newest first)
  - `getSuccessfulSessions()`: Flow of successful sessions only
  - `insert()`, `update()`: Session CRUD operations

- **UserSettingsDao** (`data/local/dao/UserSettingsDao.kt`):
  - `getSettings()`: Flow of user settings
  - `addPoints(points)`: Increment total points
  - `updateStreak(streak, date)`: Update streak and last success date
  - `resetStreak()`: Reset streak to 0

**Hilt Integration** (`core/di/DatabaseModule.kt`):
```kotlin
@Provides
@Singleton
fun provideDetoxyDatabase(@ApplicationContext context: Context): DetoxyDatabase {
    return Room.databaseBuilder(context, DetoxyDatabase::class.java, "detoxy_database").build()
}
```

**Key SQL Query**:
```sql
-- Get today's sessions (converts Unix timestamp to date)
SELECT * FROM focus_sessions
WHERE DATE(startTime/1000, 'unixepoch') = DATE('now')
ORDER BY startTime DESC
```

**Usage Pattern** (Week 3 integration):
1. Timer starts → Create FocusSession with startTime
2. Timer completes → Update session with endTime and success=true
3. Calculate points → Update UserSettings via DAO
4. Update streak → Check lastSuccessDate and update accordingly

## Required Permissions & User Setup

After app installation, users must manually enable three permissions:

1. **Accessibility Service**: Settings → Accessibility → Allday Detoxy → Enable
   - Required for app blocking functionality
   - Cannot be granted programmatically (Android security restriction)

2. **Display over other apps**: Settings → Apps → Special app access → Display over other apps → Allday Detoxy → Allow
   - Required for lock overlay screen
   - Check with `Settings.canDrawOverlays(context)`

3. **Do Not Disturb**: Settings → Notifications → Do Not Disturb access → Allday Detoxy → Allow
   - Required for notification blocking during focus sessions
   - Android 6.0+ only
   - Check with `NotificationManager.isNotificationPolicyAccessGranted`

**Note**: All three permissions use `PermissionUtils` for checking and settings navigation.

## MVP Development Phases

**Week 1 (Completed)**:
- Project setup with Hilt + Compose
- AccessibilityService for app blocking
- Basic timer with preset durations (25/45/60 min)
- Lock overlay screen

**Week 2 (Completed)**:
- ✅ Lock overlay service (2.1)
- ✅ DND (Do Not Disturb) mode control (2.2)
- ✅ Room database for session storage (2.3)

**Week 3**:
- Points and streak system
- Daily reports UI
- Battery optimization handling

**Week 4**:
- Final testing and optimization
- MVP release preparation

## Key Constraints & Limitations

**AccessibilityService**:
- Cannot be programmatically enabled (user must do it manually)
- May be killed by aggressive battery optimization on some devices
- Performance target: Block reaction time < 500ms
- Hardcoded blocked apps in MVP (Instagram, TikTok, YouTube, Facebook)

**Overlay Service**:
- Requires foreground notification (cannot be hidden on Android 8+)
- Uses Compose `mutableStateOf` for reactive real-time timer updates
- Some OEMs may restrict overlay permissions (Samsung, Xiaomi, Huawei)
- MainActivity must be in foreground for overlay to display properly

**DND Manager**:
- Requires Android 6.0+ (API 23), gracefully degrades on older versions
- Permission must be granted manually, cannot be programmatically requested
- Only blocks notifications, not alarms (using `INTERRUPTION_FILTER_ALARMS`)
- Timer works without DND but notifications won't be blocked

**Timer Communication**:
- Current MVP uses static flags (`isTimerRunning`) - suboptimal but functional
- Week 2 refactor planned: Migrate to StateFlow for proper reactive updates
- Week 2 refactor planned: Bidirectional ViewModel ↔ Service communication

## Tech Stack

- **Language**: Kotlin 1.9.0
- **UI**: Jetpack Compose + Material3
- **DI**: Hilt (Dagger)
- **Database**: Room (Week 2+)
- **Async**: Kotlin Coroutines + Flow/StateFlow
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.7 with Kotlin DSL

## Key Files for Understanding System

**Core Domain & State**:
- `domain/model/FocusTimer.kt`: Timer logic with StateFlow, coroutine-based countdown
- `domain/model/FocusState.kt`: Timer state enum (IDLE, RUNNING, FINISHED, FAILED)

**Data Layer**:
- `data/local/DetoxyDatabase.kt`: Room database with 2 entities
- `data/local/entity/FocusSession.kt`: Focus session records entity
- `data/local/entity/UserSettings.kt`: User gamification settings entity
- `data/local/dao/FocusSessionDao.kt`: Focus session data access
- `data/local/dao/UserSettingsDao.kt`: User settings data access

**ViewModels**:
- `presentation/viewmodel/TimerViewModel.kt`: Orchestrates all services and timer lifecycle

**Services**:
- `service/accessibility/FocusAccessibilityService.kt`: App blocking via AccessibilityEvent
- `service/overlay/LockOverlayService.kt`: Full-screen overlay with Compose integration

**Managers**:
- `core/manager/DndManager.kt`: Do Not Disturb mode control

**Utilities**:
- `core/utils/PermissionUtils.kt`: Centralized permission checks and settings navigation

**Dependency Injection**:
- `core/di/DatabaseModule.kt`: Room database and DAO providers
- `core/di/RepositoryModule.kt`: Repository providers (placeholder for Week 3)

**Configuration**:
- `app/src/main/res/xml/accessibility_service_config.xml`: AccessibilityService configuration
- `app/src/main/AndroidManifest.xml`: Service declarations and permissions

## Reference Documentation

- `docs/prd.md`: Product requirements and feature specifications
- `docs/00_mvp_allday_detoxy_todolist.md`: 4-week development plan with checklist
- `docs/00_android_allday_detoxy_plan.md`: Technical architecture details
- `working_history/`: Day-by-day implementation logs with commit IDs and code examples
