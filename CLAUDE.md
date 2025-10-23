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

# Clean build (when facing build issues)
./gradlew clean assembleDebug
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.allday.detoxy.YourTestClass"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Code Quality
```bash
# Kotlin compilation check (use before commits)
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
│   ├── model/       # FocusTimer, FocusState, AutoRun models
│   ├── manager/     # GamificationManager, Risk/Recovery calculators
│   └── repository/  # Repository interfaces
│
├── data/            # Data management
│   ├── local/       # Room database v4 (entities, DAOs, migrations)
│   ├── repository/  # Repository implementations
│   └── datastore/   # DataStore preferences
│
├── presentation/    # UI layer
│   ├── ui/          # Jetpack Compose screens
│   │   ├── timer/   # Timer and custom timer screens
│   │   ├── autorun/ # Auto-run settings screens
│   │   └── report/  # Reports and dashboard
│   └── viewmodel/   # ViewModels with StateFlow
│
├── service/         # Android system services
│   ├── accessibility/  # App blocking via AccessibilityService
│   ├── overlay/        # Full-screen lock overlay
│   └── timer/          # FocusTimerService for background execution
│
├── receiver/        # BroadcastReceivers
│   ├── AutoRunAlarmReceiver      # AlarmManager triggers
│   ├── NotificationActionReceiver # Notification actions
│   └── BootCompletedReceiver     # Reboot recovery
│
├── worker/          # WorkManager workers
│   └── AutoStartTimerWorker      # Auto-start timer execution
│
└── core/            # Shared utilities
    ├── di/          # Hilt dependency injection modules
    ├── manager/     # DndManager, AutoRunNotificationManager, AutoRunAlarmManager
    └── utils/       # AppCategory, AppCategoryMapper, MonitoringPolicy, Permissions
```

### Critical Service Integration

The app coordinates **seven core services** that must stay synchronized:

1. **FocusTimer** (domain/model): Coroutine-based countdown timer with StateFlow
2. **FocusAccessibilityService**: Detects and blocks distracting apps (40+ apps in 5 categories)
3. **LockOverlayService**: Shows full-screen overlay when blocked app is accessed
4. **DndManager**: Controls Do Not Disturb mode during focus sessions
5. **FocusTimerService**: Background service for timer lifecycle management
6. **AutoRunAlarmManager**: Schedules time-based auto-runs with AlarmManager/WorkManager
7. **GeofenceManager**: Manages location-based auto-runs with Geofencing API

**Timer Lifecycle Coordination**:
```kotlin
// Start sequence (TimerViewModel.startTimer or AutoStart)
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
8. Record AutoRunLog if auto-triggered
```

### Database Schema (Room v4)

**Core Entities**:
- `FocusSession`: Timer sessions with success/failure tracking
- `UserSettings`: Points, streaks, recovery metrics, auto-run settings
- `FocusInterruption`: Blocked app events during sessions
- `FocusDistraction`: All app access events
- `FocusSettings`: Category blocking preferences
- `DetoxyRoutineLog`: Scheduled routine execution logs
- `TimeBasedAutoRun`: Time-based auto execution settings (v4+)
- `LocationBasedAutoRun`: Location-based auto execution settings (v4+)
- `AutoRunLog`: Auto execution history (v4+)
- `CustomTimerPreset`: User's custom timer presets (v4+)

**Migration Strategy**: v1→v2→v3→v4 (see `data/local/migrations/`)

### Dependency Injection (Hilt)

**Critical Notes**:
- BroadcastReceivers use `EntryPointAccessors` pattern (NOT `@AndroidEntryPoint`) to avoid ASM transformation errors
- Services/Activities/Fragments use `@AndroidEntryPoint` normally
- System services (AlarmManager, NotificationManager, etc.) provided in `AlarmModule`

### Permission Management

**Required Permissions**:
1. **Accessibility Service**: Settings → Accessibility → Allday Detoxy
2. **Display over other apps**: Settings → Apps → Special access
3. **Do Not Disturb**: Settings → Notifications → DND access

**Optional Permissions (v0.6+)**:
4. **Exact Alarm**: Android 12+ for time-based auto execution
5. **Location (Background)**: For location-based auto execution
6. **Battery Optimization Exemption**: For reliable background execution

## Current Development Focus

### Active Features (v0.6/v0.8)
- Time-based automatic timer execution (AlarmManager with WorkManager fallback)
- Location-based automatic execution (Geofencing API)
- Custom timer UI with drag gestures (Compose Canvas)
- Auto-run dashboard and statistics
- Notification system with actions (Start/Snooze/Skip)

### Key Technical Decisions

#### Auto-Run Implementation
- **AlarmManager** for time-based triggers (primary)
- **WorkManager** as fallback and for delayed actions
- **Geofencing API** for location-based triggers (max 5 locations)
- **autoStartDelayMinutes**: Controls automatic timer start after notification

#### State Management
- **StateFlow** for reactive UI updates (not LiveData)
- **Flow → StateFlow** conversion in ViewModels with `stateIn()`
- **Unidirectional data flow** in all screens

#### Service Communication
- **FocusTimerService** manages timer lifecycle
- **Intent-based** actions for overlay service
- **DataStore** for settings persistence
- **NotificationManager** for auto-run notifications

## Testing Strategy

### Unit Tests
- ViewModels: State transitions, timer logic, auto-run scheduling
- Repositories: CRUD operations, data mapping
- Domain managers: Points/streak/risk calculations

### Integration Tests
- Room migrations: v1→v2→v3→v4 data preservation
- Service coordination: Timer lifecycle flow
- AlarmManager: Trigger and reschedule logic
- WorkManager: Delayed actions and recovery

### UI Tests
- Compose screens: User interactions
- Custom timer gestures: Drag/tap behavior
- Auto-run dashboard: State updates

## Documentation Structure

```
docs/
├── 02_advanced_autosetting_prd.md          # Current PRD (v0.6)
├── 02_advanced_autosetting_todolist.md     # 6-week task breakdown
├── 02_advanced_wireframe_spec.md           # UI/UX specifications
├── 02_advanced_analytics_schema.md         # Analytics events
├── 02_advanced_room_migration_strategy.md  # DB migration v3→v4
└── 02_advanced_qa_devices.md              # QA test scenarios

working_history/
└── YYYY-MM-DD_2nd_advanced_X.X.md         # Implementation logs with commit IDs
```

## Git Workflow

- **Current Branch**: `feat/v0.8` (2nd enhancement)
- **Target Branch**: `develop` (for PRs)
- **Commit format**: Korean messages with task references
- **Work logs**: Create `working_history/` entry for each task completion

## Common Development Tasks

### Working on Enhancement Tasks
1. Review current task in `docs/02_advanced_autosetting_todolist.md`
2. Check git status and recent commits for context
3. Implement following Clean Architecture principles
4. Run `./gradlew compileDebugKotlin` to verify
5. Update todolist with completion marks (✅)
6. Create working history: `YYYY-MM-DD_2nd_advanced_X.X.md`
7. Commit with Korean message and record commit ID

### Adding Auto-Run Features
1. **Time-based**: Update `TimeBasedAutoRunDao`, create UI in `presentation/ui/autorun/`
2. **Location-based**: Implement in `GeofenceManager`, handle permissions
3. **Notifications**: Use `AutoRunNotificationManager` for pre/start notifications
4. **Logging**: Record all triggers in `AutoRunLog` table

### Modifying Room Schema
1. Increment database version in `DetoxyDatabase`
2. Create migration in `data/local/migrations/Migration_X_Y.kt`
3. Update `DatabaseModule` to include migration
4. Test with `./gradlew connectedAndroidTest`
5. Document in migration strategy file

### Debugging Auto-Run Issues

#### AlarmManager Not Triggering
- Check exact alarm permission (Android 12+)
- Verify `PendingIntent` flags include `FLAG_UPDATE_CURRENT`
- Monitor logcat: `adb logcat | grep AutoRunAlarmReceiver`
- Check battery optimization exemption

#### WorkManager Tasks Not Running
- Verify constraints (network, battery, etc.)
- Check work tags for cancellation
- Monitor WorkManager database: `adb shell dumpsys jobscheduler`

#### Geofence Not Triggering
- Verify background location permission
- Check GPS enabled and location accuracy
- Monitor Play Services availability
- Test with larger radius (>100m) first

#### Notification Actions Not Working
- Ensure `NotificationActionReceiver` registered in manifest
- Check PendingIntent request codes are unique
- Verify WorkManager tasks cancelled properly on user action

## Tech Stack Reference

- **Kotlin**: 1.9.0
- **Compose BOM**: 2024.04.01
- **Hilt**: 2.48
- **Room**: 2.6.1
- **Coroutines**: 1.7.3
- **DataStore**: 1.0.0
- **WorkManager**: 2.9.0
- **Play Services Location**: 21.0.1
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Java**: 17
- **Gradle**: 8.6.0