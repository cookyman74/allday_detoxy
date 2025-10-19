# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Allday Detoxy** is a smartphone habit correction coach app for Android. It helps users overcome digital addiction by blocking distracting apps during focus timer sessions, using a gamification system with points and streaks.

**Current Phase**: 1st Enhancement (v0.5) - Focus Mode Settings & Advanced Reporting
**Architecture**: Clean Architecture (domain, data, presentation) + MVVM + Jetpack Compose

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
│   ├── manager/     # GamificationManager (points/streak calculations)
│   └── repository/  # Repository interfaces
│
├── data/            # Data management
│   ├── local/       # Room database (entities, DAOs)
│   └── repository/  # Repository implementations
│
├── presentation/    # UI layer
│   ├── ui/          # Jetpack Compose screens
│   └── viewmodel/   # ViewModels with StateFlow
│
├── service/         # Android system services
│   ├── accessibility/  # App blocking via AccessibilityService
│   └── overlay/        # Full-screen lock overlay
│
└── core/            # Shared utilities
    ├── di/          # Hilt dependency injection
    ├── manager/     # DndManager (Do Not Disturb)
    └── utils/       # AppCategory, AppCategoryMapper, MonitoringPolicy
```

### Critical Service Integration

The app coordinates **four core services** that must stay synchronized:

1. **FocusTimer** (domain/model): Coroutine-based countdown timer with StateFlow
2. **FocusAccessibilityService**: Detects and blocks distracting apps (40+ apps in 5 categories)
3. **LockOverlayService**: Shows full-screen overlay when blocked app is accessed
4. **DndManager**: Controls Do Not Disturb mode during focus sessions

**Timer Lifecycle Coordination**:
```kotlin
// Start sequence (TimerViewModel.startTimer)
1. Create FocusSession in database
2. FocusAccessibilityService.isTimerRunning = true
3. LockOverlayService.showOverlay(context, seconds)
4. dndManager.enableDnd()
5. focusTimer.start(duration)

// Cleanup sequence (giveUp/reset/onFinish)
1. FocusAccessibilityService.isTimerRunning = false
2. LockOverlayService.hideOverlay(context)
3. dndManager.disableDnd()
4. focusTimer.stop()
5. Update FocusSession with results
6. Calculate points/streak if successful
```

### App Blocking System (v0.5 Enhancement)

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

### Database Schema (Room v3)

**Core Entities**:
- `FocusSession`: Timer sessions with success/failure tracking
- `UserSettings`: Points, streaks, recovery metrics
- `FocusInterruption`: Blocked app events during sessions (v2+)
- `FocusDistraction`: All app access events (v3+)
- `FocusSettings`: Category blocking preferences (v3+)
- `DetoxyRoutineLog`: Scheduled routine execution logs (v3+)

**Migration Strategy**: v1→v2→v3 (see `docs/01_advanced_room_migration_strategy.md`)

### Permission Management

The app requires three critical permissions that users must manually grant:

1. **Accessibility Service**: Settings → Accessibility → Allday Detoxy
2. **Display over other apps**: Settings → Apps → Special access
3. **Do Not Disturb**: Settings → Notifications → DND access

**Permission Utils**: `core/utils/PermissionUtils.kt` provides unified checking/navigation

## Current Development Focus (Week 1 of v0.5)

### Completed (2025-10-15)
- ✅ Task 2.1: Data & Domain layer (AppCategory, AppCategoryMapper, MonitoringPolicy)
- ✅ Task 2.2: UI/UX implementation (DetoxyControlSettingsScreen, FocusSettingsViewModel)
- ✅ Dynamic blocking in FocusAccessibilityService (40+ apps)
- ✅ DndManager permission state enhancements
- ✅ DataStore preferences integration

### In Progress (Week 1 Remaining)
- [ ] Task 2.3: State persistence & service synchronization
- [ ] FocusAccessibilityService real-time sync with settings
- [ ] Timer start settings application logic
- [ ] Immediate settings reflection mechanism

### Upcoming (Week 2)
- [ ] Room migrations v1→v2→v3
- [ ] Advanced reporting metrics (risk index, recovery rate)
- [ ] Weekly insights graphs with Compose Canvas

## Key Technical Decisions

### State Management
- **StateFlow** for reactive UI updates (not LiveData)
- **Single source of truth** in domain layer
- **Unidirectional data flow** in ViewModels

### Dependency Injection
- **Hilt** for all DI needs (@HiltAndroidApp, @HiltViewModel)
- **@Binds** for interface-implementation binding
- **@Singleton** scope for database and repositories

### Service Communication
- **Static flags** for MVP (e.g., `FocusAccessibilityService.isTimerRunning`)
- **Intent-based** actions for overlay service
- **DataStore** for settings persistence (v0.5)
- **TODO**: Migrate to StateFlow for proper reactive updates

### Analytics & Privacy
- **Package names**: Local DB only (privacy protection)
- **Analytics**: Category-level data only
- **Event batching**: Immediate for interruptions, batched for allowed apps

## Testing Strategy

### Unit Tests
- ViewModels: State transitions, timer logic
- Repositories: CRUD operations, data mapping
- Domain managers: Points/streak calculations

### Integration Tests
- Room migrations: v1→v2→v3 data preservation
- Service coordination: Timer lifecycle flow
- Analytics events: Parameter mapping

### UI Tests
- Compose screens: User interactions
- Permission dialogs: Grant/deny flows
- Timer states: Visual feedback

## Performance Targets

- **App blocking reaction**: < 500ms
- **Report loading**: < 1s with caching
- **Timer update frequency**: 1Hz (every second)
- **APK size**: < 15MB
- **Memory usage**: < 100MB runtime

## Documentation Structure

```
docs/
├── 01_advanced_prd.md              # Product requirements v0.5
├── 01_advanced_setting_report_todolist.md  # 3-week task breakdown
├── 01_advanced_wireframe_spec.md   # UI specifications
├── 01_advanced_app_category_mapping.md  # 40-app categorization
├── 01_advanced_room_migration_strategy.md  # Database migrations
├── 01_advanced_analytics_schema.md  # Event tracking specs
└── 01_advanced_qa_devices.md       # Test scenarios

working_history/
└── YYYY-MM-DD_task.md  # Daily implementation logs with commit IDs
```

## Git Workflow

- **Branch**: `feat/v0.5` (current enhancement)
- **Commit format**: Korean messages with task references
- **Co-author**: Add Claude Code attribution when applicable
- **Work logs**: Create `working_history/` entry for each task completion

## Common Development Tasks

### Working on Enhancement Tasks
1. Review task requirements in `docs/01_advanced_setting_report_todolist.md`
2. Check previous work in `working_history/` folder
3. Update todolist with completion marks after implementation
4. Create working history document with format: `YYYY-MM-DD_1st_advanced_X.X.md`
5. Commit with Korean message and record commit ID

### Adding a New Blocked App
1. Add package name to `AppCategoryMapper.categoryMap`
2. Verify with `./gradlew compileDebugKotlin`
3. Test with real device (Accessibility Service required)

### Modifying Room Schema
1. Increment database version in `DetoxyDatabase`
2. Write migration in `DatabaseModule`
3. Test with `./gradlew connectedAndroidTest`
4. Document in migration strategy

### Adding Analytics Event
1. Define event in `MonitoringPolicy`
2. Create parameter data class with `toAnalyticsParams()`
3. Log event in appropriate service/viewmodel
4. Verify in Firebase console debug view

### Creating New Compose Screen
1. Create screen composable in `presentation/ui/`
2. Add preview with `@Preview` annotation
3. Create ViewModel with `@HiltViewModel`
4. Add navigation in `MainActivity`
5. Test on multiple screen sizes

## Debugging Tips

### AccessibilityService Not Working
- Check permission in Settings → Accessibility
- Verify `accessibility_service_config.xml` configuration
- Monitor logcat: `adb logcat | grep FocusAccessibilityService`

### Overlay Not Showing
- Verify "Display over other apps" permission
- Check foreground service notification
- Ensure MainActivity is in foreground when starting

### Timer Sync Issues
- Check all 4 components are coordinated (Timer, Accessibility, Overlay, DND)
- Verify cleanup on all exit paths (complete, giveUp, reset)
- Monitor StateFlow emissions in ViewModel

## Tech Stack Reference

- **Kotlin**: 1.9.0
- **Compose BOM**: 2024.04.01
- **Hilt**: 2.48
- **Room**: 2.6.1
- **Coroutines**: 1.7.3
- **DataStore**: 1.0.0
- **Lifecycle**: 2.6.2
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Java**: 17
- **Gradle**: 8.6.0