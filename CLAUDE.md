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

The timer integrates three components that must be synchronized:

1. **FocusTimer** (domain model): Manages countdown with coroutines
2. **AccessibilityService**: Blocks apps when `isTimerRunning = true`
3. **LockOverlayService**: Shows full-screen overlay on blocked app access

**Startup sequence**:
```kotlin
// In TimerViewModel.startTimer()
1. FocusAccessibilityService.isTimerRunning = true
2. LockOverlayService.showOverlay(context, seconds, total)
3. focusTimer.start(duration) { onFinish() }
```

**Cleanup sequence**:
```kotlin
// In TimerViewModel (giveUp/reset/onFinish)
1. FocusAccessibilityService.isTimerRunning = false
2. LockOverlayService.hideOverlay(context)
3. focusTimer.reset() or giveUp()
```

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
- Flags: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN | FLAG_KEEP_SCREEN_ON`
- PixelFormat: `TRANSLUCENT` for semi-transparent background

**Compose Integration**:
```kotlin
ComposeView(context).apply {
    setContent {
        DetoxyTheme {
            LockOverlayScreen(remainingSeconds, totalSeconds, onGiveUp)
        }
    }
}
```

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

## Required Permissions & User Setup

After app installation, users must manually enable:

1. **Accessibility Service**: Settings → Accessibility → Allday Detoxy → Enable
2. **Display over other apps**: Settings → Apps → Special app access → Display over other apps → Allday Detoxy → Allow
3. **Do Not Disturb** (Week 2+): Settings → Notifications → Do Not Disturb access → Allday Detoxy → Allow

## MVP Development Phases

**Week 1 (Completed)**:
- Project setup with Hilt + Compose
- AccessibilityService for app blocking
- Basic timer with preset durations (25/45/60 min)
- Lock overlay screen

**Week 2 (In Progress)**:
- DND (Do Not Disturb) mode control
- Room database for session storage
- Real-time timer updates in overlay

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

**Overlay Service**:
- Requires foreground notification (cannot be hidden on Android 8+)
- Static timer display in MVP (real-time update in Week 2)
- Some OEMs may restrict overlay permissions

**Timer Communication**:
- Current MVP uses static flags (suboptimal)
- Week 2 refactor: Migrate to StateFlow for proper reactive updates
- Week 2 refactor: Bidirectional ViewModel ↔ Service communication

## Tech Stack

- **Language**: Kotlin 1.9.0
- **UI**: Jetpack Compose + Material3
- **DI**: Hilt (Dagger)
- **Database**: Room (Week 2+)
- **Async**: Kotlin Coroutines + Flow/StateFlow
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Build Tool**: Gradle 8.7 with Kotlin DSL

## Reference Documentation

- `docs/prd.md`: Product requirements and feature specifications
- `docs/00_mvp_allday_detoxy_todolist.md`: 4-week development plan with checklist
- `docs/00_android_allday_detoxy_plan.md`: Technical architecture details
- `working_history/`: Day-by-day implementation logs with commit IDs
