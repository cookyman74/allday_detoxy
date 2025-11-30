# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Allday Detoxy (ScreenSence)** is an Android smartphone habit correction coach app that helps users overcome digital addiction by blocking distracting apps during focus timer sessions with gamification.

**Current State**: v1.0.7 (version/0.9 branch)
**Architecture**: Clean Architecture (domain, data, presentation) + MVVM + Jetpack Compose
**Primary Branch**: `develop` (PRs target this branch)

## Build & Development Commands

### Building
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Clean build (when facing build issues)
./gradlew clean assembleDebug

# Kotlin compilation check (use before commits)
./gradlew compileDebugKotlin
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.allday.detoxy.data.entity.ScheduleGroupTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific migration test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.allday.detoxy.data.local.migration.MigrationTest_6_7
```

### Code Quality
```bash
# Lint check
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

## Critical Architecture Details

### Seven Core Services Coordination

The app's core functionality depends on **synchronization between seven services**:

1. **FocusTimer** (domain/model): Coroutine-based countdown timer with StateFlow
2. **FocusAccessibilityService**: Detects and blocks 40+ distracting apps across 5 categories
3. **LockOverlayService**: Shows full-screen overlay when blocked app is accessed
4. **DndManager**: Controls Do Not Disturb mode during focus sessions
5. **FocusTimerService**: Foreground service for timer lifecycle management
6. **AutoRunAlarmManager**: Schedules time-based auto-runs using AlarmManager/WorkManager
7. **GeofenceManager**: Manages location-based auto-runs via Geofencing API (max 5 locations)

**Critical Timer Lifecycle Flow**:

```kotlin
// START sequence (TimerViewModel.startTimer or auto-triggered)
1. Create FocusSession in Room database
2. Start FocusTimerService (foreground service)
3. FocusAccessibilityService.isTimerRunning = true
4. LockOverlayService.showOverlay(context, seconds)
5. dndManager.enableDnd()
6. focusTimer.start(duration)

// CLEANUP sequence (giveUp/reset/onFinish)
1. FocusAccessibilityService.isTimerRunning = false
2. LockOverlayService.hideOverlay(context)
3. dndManager.disableDnd()
4. focusTimer.stop()
5. Update FocusSession with results
6. Calculate points/streak if successful
7. Stop FocusTimerService
8. Record AutoRunLog if auto-triggered
```

**⚠️ Breaking this sequence causes service desync bugs** - always follow the full lifecycle.

### BroadcastReceiver Dependency Injection Pattern

**Critical**: BroadcastReceivers use `EntryPointAccessors` pattern instead of `@AndroidEntryPoint`:

```kotlin
// ❌ WRONG - causes ASM transformation errors
@AndroidEntryPoint
class AutoRunAlarmReceiver : BroadcastReceiver()

// ✅ CORRECT
class AutoRunAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AutoRunAlarmReceiverEntryPoint::class.java
        )
        val repository = entryPoint.repository()
        // ...
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoRunAlarmReceiverEntryPoint {
    fun repository(): FocusRepository
}
```

System services (AlarmManager, NotificationManager, etc.) are provided in `AlarmModule` and `GeofenceModule`.

### Room Database Versioning

**Current Version**: 7 (see `DetoxyDatabase`)
**Migration Path**: v1 → v2 → v3 → v4 → v5 → v6 → v7

**Key Entities**:
- `FocusSession`: Timer sessions with success/failure tracking
- `UserSettings`: Points, streaks, recovery metrics, auto-run master toggle
- `TimeBasedAutoRun`: Time-based schedules (AlarmManager)
- `LocationBasedAutoRun`: Geofence-based triggers
- `AutoRunLog`: Auto-run execution history
- `CustomTimerPreset`: User's custom timer configurations
- `ScheduleGroup`: Grouped schedules with collective enable/disable
- `FocusInterruption`, `FocusDistraction`: App blocking analytics
- `FocusSettings`: Category-based blocking preferences

**When modifying schema**:
1. Increment database version in `DetoxyDatabase`
2. Create migration in `data/local/migrations/Migration_X_Y.kt`
3. Add migration to `DatabaseModule.MIGRATIONS` array
4. Write instrumented test in `data/local/migration/MigrationTest_X_Y.kt`
5. Document in `docs/0X_advanced_room_migration_strategy.md`

### State Management Patterns

**Use StateFlow, NOT LiveData**:
```kotlin
// ✅ CORRECT
private val _timerState = MutableStateFlow<FocusState>(FocusState.IDLE)
val timerState: StateFlow<FocusState> = _timerState.asStateFlow()

// Flow to StateFlow conversion in ViewModels
val settings: StateFlow<UserSettings?> = repository.getSettings()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
```

**Unidirectional data flow** in all Compose screens:
- ViewModels expose StateFlow
- UI observes with `collectAsStateWithLifecycle()`
- UI sends events to ViewModel functions
- ViewModel updates StateFlow

### Auto-Run Implementation Details

**Time-based Auto-Run**:
- **Primary**: AlarmManager with `setExactAndAllowWhileIdle()` for precise timing
- **Fallback**: WorkManager for delayed actions and recovery
- **Permission**: Exact alarm permission required on Android 12+ (API 31+)
- **Doze Mode**: Uses `AlarmManager.RTC_WAKEUP` to wake device from Doze

**Location-based Auto-Run**:
- **API**: Geofencing API (max 5 active geofences)
- **Permissions**: Fine location + Background location
- **Radius**: 50m ~ 500m (user configurable)
- **Privacy**: Location data stored locally only, never sent to servers
- **Battery**: Uses `GEOFENCE_TRANSITION_DWELL` with responsiveness optimization

**autoStartDelayMinutes**:
- Controls automatic timer start after pre-notification
- 0 = immediate start, >0 = user has X minutes to prepare
- Implemented via WorkManager `OneTimeWorkRequest` with delay

## Common Development Workflows

### Working on Enhancement Tasks

1. Check current task in `docs/0X_advanced_*_todolist.md`
2. Review recent commits: `git log --oneline -10`
3. Implement following Clean Architecture layers:
   - domain/ first (business logic, no Android deps)
   - data/ second (Room, repositories)
   - presentation/ last (ViewModels, Compose UI)
4. Verify compilation: `./gradlew compileDebugKotlin`
5. Update todolist with ✅
6. Create work history: `working_history/YYYY-MM-DD_description.md`
7. Commit with Korean message, record commit ID in work history

### Adding Auto-Run Features

**Time-based**:
1. Update `TimeBasedAutoRunDao` for data layer
2. Modify `AutoRunAlarmManager` for scheduling logic
3. Update `AutoRunAlarmReceiver` for trigger handling
4. Create/update UI in `presentation/ui/autorun/`

**Location-based**:
1. Update `LocationBasedAutoRunDao` for data layer
2. Modify `GeofenceManager` for geofence registration
3. Handle permissions in `PermissionUtils`
4. Create/update UI in `presentation/ui/autorun/`

**Both**:
- Record all triggers in `AutoRunLog` via `AutoRunLogDao`
- Use `AutoRunNotificationManager` for pre/start notifications
- Update `UserSettings.autoRunMasterEnabled` for master toggle

### Debugging Auto-Run Issues

**AlarmManager not triggering**:
```bash
# Check scheduled alarms
adb shell dumpsys alarm | grep allday.detoxy

# Monitor receiver
adb logcat | grep AutoRunAlarmReceiver

# Verify exact alarm permission (Android 12+)
adb shell dumpsys package com.allday.detoxy | grep SCHEDULE_EXACT_ALARM
```

**WorkManager not running**:
```bash
# Check work status
adb shell dumpsys jobscheduler | grep allday.detoxy

# Monitor worker
adb logcat | grep AutoStartTimerWorker
```

**Geofence not triggering**:
```bash
# Check geofence registration
adb shell dumpsys activity service GeofencingService

# Monitor location updates
adb logcat | grep GeofenceManager

# Test with mock location (requires developer options)
adb shell setprop debug.location.provider network
```

Common issues:
- Battery optimization not disabled
- Doze mode active (test with `adb shell dumpsys deviceidle force-idle`)
- Background location permission not set to "Always allow"
- GPS disabled or poor accuracy
- PendingIntent flags missing `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`

### Testing Service Coordination

Use the test script for manual verification:
```bash
./test_dnd_and_session.sh
```

This validates:
- DND mode activation/deactivation
- FocusSession creation and completion
- Point/streak calculation
- Service lifecycle coordination

## Tech Stack Reference

- **Kotlin**: 1.9.0
- **Compose BOM**: 2024.04.01
- **Hilt**: 2.48
- **Room**: 2.6.1
- **Coroutines**: 1.7.3
- **DataStore**: 1.0.0
- **WorkManager**: 2.9.0
- **Play Services Location**: 21.0.1
- **Firebase**: Crashlytics + Analytics (production)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Java**: 17
- **Gradle**: 8.7.0

## Required Permissions

**Runtime Permissions**:
1. `ACCESS_FINE_LOCATION` - for location-based auto-run
2. `ACCESS_BACKGROUND_LOCATION` - for geofence monitoring
3. `SCHEDULE_EXACT_ALARM` - for precise time-based triggers (Android 12+)
4. `USE_EXACT_ALARM` - fallback for exact alarms (Android 14+)
5. `POST_NOTIFICATIONS` - for auto-run notifications (Android 13+)

**System Settings Permissions** (requires user action):
1. Accessibility Service - for app blocking
2. Display over other apps - for lock overlay
3. Do Not Disturb access - for DND mode
4. Battery optimization exclusion - for reliable background execution

## Git Workflow

**Branch Strategy**:
- `develop` - main development branch (PR target)
- `version/X.X` - version branches (current: `version/0.9`)
- `feat/*` - feature branches
- `fix/*` - bugfix branches

**Commit Message Format**: Korean messages with descriptive context
```
feat: 위치 기반 스케줄 그룹 활성화 로직 개선
fix(location): Geofence 실패 시 isEnabled 값 유지
docs: 버그 수정 작업 기록 추가
```

**Work History**: Create `working_history/YYYY-MM-DD_description.md` for each significant task completion with commit IDs.

## Documentation Structure

```
docs/
├── 0X_advanced_*_prd.md           # Product requirement documents
├── 0X_advanced_*_todolist.md      # Development task breakdowns
├── 0X_advanced_wireframe_spec.md  # UI/UX specifications
├── 0X_advanced_analytics_schema.md # Analytics event definitions
├── 0X_advanced_room_migration_strategy.md # Database migrations
├── 0X_advanced_qa_devices.md      # QA test scenarios
└── RELEASE_NOTES_vX.X.md         # Release notes per version

working_history/
└── YYYY-MM-DD_description.md      # Implementation logs with commit IDs
```

**Current active docs**: Check `docs/02_advanced_autosetting_todolist.md` and `docs/03_complex_time&location_todolist.md` for latest development plans.
