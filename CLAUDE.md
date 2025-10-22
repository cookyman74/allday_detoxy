# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Allday Detoxy** is a smartphone habit correction coach app for Android. It helps users overcome digital addiction by blocking distracting apps during focus timer sessions, using a gamification system with points and streaks.

**Current Phase**: 2nd Enhancement (v0.6/v0.8) - Automatic Timer Execution
**Architecture**: Clean Architecture (domain, data, presentation) + MVVM + Jetpack Compose
**Development Branch**: `feat/v0.8` (targeting `develop` branch for PRs)

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

# Generate lint report
./gradlew lintDebug
```

## High-Level Architecture

### Clean Architecture Layers

```
app/src/main/java/com/allday/detoxy/
├── domain/          # Business logic (no Android dependencies)
│   ├── model/       # FocusTimer, FocusState
│   ├── manager/     # GamificationManager, Risk/Recovery calculators
│   └── repository/  # Repository interfaces
│
├── data/            # Data management
│   ├── local/       # Room database v4 (entities, DAOs)
│   └── repository/  # Repository implementations
│
├── presentation/    # UI layer
│   ├── ui/          # Jetpack Compose screens
│   └── viewmodel/   # ViewModels with StateFlow
│
├── service/         # Android system services
│   ├── accessibility/  # App blocking via AccessibilityService
│   ├── overlay/        # Full-screen lock overlay
│   └── timer/          # FocusTimerService for background execution
│
└── core/            # Shared utilities
    ├── di/          # Hilt dependency injection
    ├── manager/     # DndManager (Do Not Disturb)
    └── utils/       # AppCategory, AppCategoryMapper, MonitoringPolicy
```

### Critical Service Integration

The app coordinates **five core services** that must stay synchronized:

1. **FocusTimer** (domain/model): Coroutine-based countdown timer with StateFlow
2. **FocusAccessibilityService**: Detects and blocks distracting apps (40+ apps in 5 categories)
3. **LockOverlayService**: Shows full-screen overlay when blocked app is accessed
4. **DndManager**: Controls Do Not Disturb mode during focus sessions
5. **FocusTimerService**: Background service for timer lifecycle management (v0.6+)

**Timer Lifecycle Coordination**:
```kotlin
// Start sequence (TimerViewModel.startTimer)
1. Create FocusSession in database
2. Start FocusTimerService (foreground service)
3. FocusAccessibilityService.isTimerRunning = true
4. LockOverlayService.showOverlay(context, seconds)
5. dndManager.enableDnd()
6. focusTimer.start(duration)

// Cleanup sequence (giveUp/reset/onFinish)
1. FocusAccessibilityService.isTimerRunning = false
2. LockOverlayService.hideOverlay(context)
3. dndManager.disableDnd()
4. focusTimer.stop()
5. Update FocusSession with results
6. Calculate points/streak if successful
7. Stop FocusTimerService
```

### App Blocking System

**Dynamic Category-Based Blocking**:
- 5 categories: SNS, MESSENGER, WEB, VIDEO_SHORTS, OTHER
- 40+ predefined apps mapped to categories
- 3 recovery presets: COMPLETE_BLOCK, STANDARD_DETOXY, RELAXED

**Key Components**:
- `AppCategory`: Enum with recovery-focused descriptions
- `AppCategoryMapper`: Package mapping & blocking logic
- `MonitoringPolicy`: Event logging & Analytics integration
- `FocusAccessibilityService`: Real-time app detection & blocking
- `DetoxyControlSettingsScreen`: Settings UI with presets & category toggles
- `FocusSettingsViewModel`: State management with DataStore persistence

### Database Schema (Room v4)

**Core Entities**:
- `FocusSession`: Timer sessions with success/failure tracking
- `UserSettings`: Points, streaks, recovery metrics
- `FocusInterruption`: Blocked app events during sessions (v2+)
- `FocusDistraction`: All app access events (v3+)
- `FocusSettings`: Category blocking preferences (v3+)
- `DetoxyRoutineLog`: Scheduled routine execution logs (v3+)
- `TimeBasedAutoRun`: Time-based auto execution settings (v4+)
- `LocationBasedAutoRun`: Location-based auto execution settings (v4+)
- `AutoRunLog`: Auto execution history (v4+)
- `CustomTimerPreset`: User's custom timer presets (v4+)

**Migration Strategy**: v1→v2→v3→v4 (see `data/local/migrations/`)

### Permission Management

The app requires critical permissions that users must manually grant:

**Required Permissions**:
1. **Accessibility Service**: Settings → Accessibility → Allday Detoxy
2. **Display over other apps**: Settings → Apps → Special access
3. **Do Not Disturb**: Settings → Notifications → DND access

**Optional Permissions (v0.6+)**:
4. **Exact Alarm**: Android 12+ for time-based auto execution
5. **Location (Background)**: For location-based auto execution
6. **Battery Optimization Exemption**: For reliable background execution

**Permission Utils**: `core/utils/PermissionUtils.kt` provides unified checking/navigation

## Current Development Focus (v0.6/v0.8)

### Active Development Areas
- Time-based automatic timer execution (AlarmManager)
- Location-based automatic execution (Geofencing)
- Custom timer UI with drag gestures
- Auto-run dashboard and statistics
- Room database migration v3→v4

### Key Technical Decisions

#### State Management
- **StateFlow** for reactive UI updates (not LiveData)
- **Single source of truth** in domain layer
- **Unidirectional data flow** in ViewModels

#### Dependency Injection
- **Hilt** for all DI needs (@HiltAndroidApp, @HiltViewModel)
- **@Binds** for interface-implementation binding
- **@Singleton** scope for database and repositories

#### Service Communication
- **FocusTimerService** manages timer lifecycle
- **Intent-based** actions for overlay service
- **DataStore** for settings persistence
- **Static flags** for MVP (migrating to StateFlow)

#### Analytics & Privacy
- **Firebase Analytics**: Category-level events only
- **Firebase Crashlytics**: Crash reporting
- **Package names**: Never sent to analytics (privacy)
- **Event batching**: Immediate for interruptions, batched for distractions

## Testing Strategy

### Unit Tests
- ViewModels: State transitions, timer logic
- Repositories: CRUD operations, data mapping
- Domain managers: Points/streak/risk calculations

### Integration Tests
- Room migrations: v1→v2→v3→v4 data preservation
- Service coordination: Timer lifecycle flow
- Permission flows: Grant/deny scenarios

### UI Tests
- Compose screens: User interactions
- Custom timer gestures: Drag/tap behavior
- Auto-run dashboard: State updates

## Performance Targets

- **App blocking reaction**: < 500ms
- **Report loading**: < 1s with caching
- **Timer update frequency**: 1Hz (every second)
- **APK size**: < 15MB
- **Memory usage**: < 100MB runtime
- **Battery impact**: < 2% per hour during timer

## Documentation Structure

```
docs/
├── 02_advanced_autosetting_prd.md          # Current PRD (v0.6)
├── 02_advanced_autosetting_todolist.md     # 6-week task breakdown
├── 01_advanced_*.md                        # v0.5 documentation (completed)
└── 00_*.md                                 # MVP documentation

working_history/
└── YYYY-MM-DD_2nd_advanced_X.X.md         # Implementation logs with commit IDs

AGENTS.md                                    # Repository guidelines and conventions
```

## Git Workflow

- **Current Branch**: `feat/v0.8` (2nd enhancement)
- **Target Branch**: `develop` (for PRs)
- **Commit format**: Korean messages with task references
- **Co-author**: Add Claude Code attribution when applicable
- **Work logs**: Create `working_history/` entry for each task completion

## Common Development Tasks

### Working on Enhancement Tasks
1. Review current task in `docs/02_advanced_autosetting_todolist.md`
2. Check git status and recent commits for context
3. Implement the task following Clean Architecture principles
4. Update todolist with completion marks (✅)
5. Create working history: `YYYY-MM-DD_2nd_advanced_X.X.md`
6. Commit with Korean message and record commit ID

### Adding a New Blocked App
1. Add package name to `AppCategoryMapper.categoryMap`
2. Assign appropriate category (SNS, MESSENGER, etc.)
3. Verify with `./gradlew compileDebugKotlin`
4. Test with real device (Accessibility Service required)

### Modifying Room Schema
1. Increment database version in `DetoxyDatabase`
2. Create migration in `data/local/migrations/Migration_X_Y.kt`
3. Update `DatabaseModule` to include migration
4. Test with `./gradlew connectedAndroidTest`
5. Document migration strategy

### Adding Analytics Event
1. Define event in `MonitoringPolicy`
2. Create parameter data class with `toAnalyticsParams()`
3. Log event via `AnalyticsHelper` in appropriate service/viewmodel
4. Verify in Firebase console debug view

### Creating New Compose Screen
1. Create screen composable in `presentation/ui/feature/`
2. Add preview with `@Preview` annotation
3. Create ViewModel with `@HiltViewModel`
4. Add navigation in `MainActivity`
5. Test on multiple screen sizes (phone/tablet)

## Debugging Tips

### AccessibilityService Not Working
- Check permission in Settings → Accessibility
- Verify `accessibility_service_config.xml` configuration
- Monitor logcat: `adb logcat | grep FocusAccessibilityService`
- Ensure service is bound and `isTimerRunning` flag is set

### Overlay Not Showing
- Verify "Display over other apps" permission
- Check foreground service notification exists
- Ensure `TYPE_APPLICATION_OVERLAY` is used
- Confirm MainActivity context is available

### Timer Sync Issues
- Check all 5 components are coordinated (Timer, TimerService, Accessibility, Overlay, DND)
- Verify cleanup on all exit paths (complete, giveUp, reset)
- Monitor StateFlow emissions in ViewModel
- Check FocusTimerService lifecycle (onCreate, onStartCommand, onDestroy)

### Database Migration Failures
- Test migrations with `MigrationTestHelper`
- Verify column types match between versions
- Check for NOT NULL constraints on new columns
- Use fallback to destructive migration for development only

## Tech Stack Reference

- **Kotlin**: 1.9.0
- **Compose BOM**: 2024.04.01
- **Hilt**: 2.48
- **Room**: 2.6.1
- **Coroutines**: 1.7.3
- **DataStore**: 1.0.0
- **Lifecycle**: 2.6.2
- **Firebase BOM**: Latest
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Java**: 17
- **Gradle**: 8.6.0