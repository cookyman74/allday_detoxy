# 99. Allday Detoxy iOS 마이그레이션 기획/설계서

> **작성일**: 2025-12-15
> **상태**: Draft
> **작성자**: Antigravity
> **대상 플랫폼**: iOS 15.0+

---

## 1. 개요 (Executive Summary)

### 1.1 배경

Allday Detoxy는 현재 Android 전용 앱으로, 스마트폰 습관 교정을 위한 앱 차단 및 집중 타이머 기능을 제공합니다. iOS 사용자 확대를 위해 iOS 버전 개발이 필요합니다.

### 1.2 가능성 검토 결과

**iOS 15+ (WWDC 2021)** 부터 Apple이 **Screen Time API**를 공개하여, 서드파티 앱에서도 앱 차단 기능 구현이 가능해졌습니다.

| 검토 항목 | 결과 |
|----------|------|
| 앱 차단 기능 | ✅ 가능 (Screen Time API) |
| 위치 기반 자동 실행 | ✅ 가능 (Core Location) |
| 시간 기반 스케줄 | ✅ 가능 (DeviceActivity) |
| App Store 출시 | ✅ 가능 (entitlement 승인 필요) |

### 1.3 경쟁 앱 분석

| 앱 | 다운로드 | 가격 | 주요 기능 |
|----|---------|------|----------|
| **AppBlock** | 1000만+ | 무료+IAP | 앱 차단, 포모도로, 위젯 |
| **Opal** | 100만+ | $100/년 | 앱 차단, 세션, 분석 |
| **ScreenZen** | 50만+ | 무료 (기부) | 딜레이 차단, 앱 숨기기 |
| **One Sec** | 100만+ | 유료 | 딜레이 기반 습관 교정 |
| **Freedom** | 50만+ | 구독 | 크로스 플랫폼 차단 |

**차별화 포인트**: 위치 기반 자동 실행 + 시간표 연동 + 게임화 시스템

---

## 2. iOS Screen Time API 아키텍처

### 2.1 핵심 프레임워크

```
┌─────────────────────────────────────────────────────────────────┐
│                    Screen Time API (iOS 15+)                    │
├─────────────────────┬─────────────────────┬─────────────────────┤
│   FamilyControls    │   ManagedSettings   │   DeviceActivity    │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ • 권한 요청          │ • 앱 차단 설정       │ • 스케줄 관리        │
│ • AuthorizationCenter│ • Shield 표시       │ • 사용 시간 추적     │
│ • FamilyActivityPicker│ • 웹사이트 차단    │ • 모니터링 콜백      │
│ • 앱/카테고리 선택   │ • 설정 잠금         │ • 백그라운드 실행    │
└─────────────────────┴─────────────────────┴─────────────────────┘
```

### 2.2 Android vs iOS 기능 매핑

| Android 구성요소 | iOS 대응 | 비고 |
|-----------------|----------|------|
| `AccessibilityService` | `ManagedSettings.shield` | Shield 방식 차단 |
| `LockOverlayService` | `ShieldConfigurationDataSource` | 커스텀 Shield UI |
| `FocusTimer` (Coroutine) | `Timer` + `Combine` | 타이머 로직 |
| `GeofenceManager` | `CLLocationManager` + `CLCircularRegion` | 위치 기반 |
| `AutoRunAlarmManager` | `DeviceActivitySchedule` | 시간 기반 스케줄 |
| `DndManager` | `INFocusStatusCenter` (제한적) | Focus 상태 읽기만 |
| `Room Database` | `SwiftData` / `Core Data` | 로컬 저장소 |
| `Hilt DI` | `Swift Dependencies` / 수동 DI | 의존성 주입 |
| `StateFlow` | `@Published` + `Combine` | 반응형 상태 |
| `Jetpack Compose` | `SwiftUI` | 선언형 UI |

### 2.3 제약사항 및 차이점

| 항목 | Android | iOS |
|------|---------|-----|
| **차단 방식** | 완전 오버레이 (강제) | Shield (우회 가능) |
| **권한** | 접근성 서비스 | Screen Time 권한 |
| **DND 제어** | 직접 ON/OFF | 읽기만 가능 |
| **백그라운드** | Foreground Service 무제한 | 제한적 (위치/오디오만) |
| **Geofence 개수** | 무제한 | 20개 제한 |
| **정확한 알람** | AlarmManager (정확) | 불가 (DeviceActivity 사용) |
| **심사** | 상대적 자유 | 엄격한 가이드라인 |

---

## 3. 기술 스택

### 3.1 권장 기술 스택

```yaml
Language: Swift 5.9+
UI Framework: SwiftUI
Minimum iOS: 15.0
Target iOS: 17.0+
Architecture: MVVM + Clean Architecture

Core Frameworks:
  - FamilyControls      # 권한 및 앱 선택
  - ManagedSettings     # 앱 차단
  - DeviceActivity      # 스케줄 및 모니터링
  - CoreLocation        # 위치 기반
  - SwiftData           # 로컬 데이터베이스 (iOS 17+)
  - Core Data           # 로컬 데이터베이스 (iOS 15-16 폴백)
  - Combine             # 반응형 프로그래밍
  - WidgetKit           # 홈 화면 위젯

Testing:
  - XCTest              # 유닛/UI 테스트
  - Quick/Nimble        # BDD 스타일 테스트
```

### 3.2 프로젝트 구조

```
AlldayDetoxy-iOS/
├── App/
│   ├── AlldayDetoxyApp.swift        # @main 진입점
│   └── AppDelegate.swift            # 앱 생명주기
│
├── Core/
│   ├── DI/                          # 의존성 주입
│   ├── Extensions/                  # Swift 확장
│   └── Utils/                       # 유틸리티
│
├── Domain/
│   ├── Models/                      # 도메인 모델
│   │   ├── FocusSession.swift
│   │   ├── FocusState.swift
│   │   ├── UserSettings.swift
│   │   ├── LocationAutoRun.swift
│   │   └── ScheduleGroup.swift
│   ├── Repositories/                # Repository 프로토콜
│   └── UseCases/                    # 비즈니스 로직
│
├── Data/
│   ├── Local/
│   │   ├── SwiftData/               # SwiftData 모델
│   │   └── CoreData/                # Core Data 폴백
│   ├── Repositories/                # Repository 구현체
│   └── Managers/
│       ├── ShieldManager.swift      # 앱 차단 관리
│       ├── ScheduleManager.swift    # DeviceActivity 스케줄
│       └── LocationManager.swift    # 위치 기반 관리
│
├── Presentation/
│   ├── Views/
│   │   ├── Timer/                   # 타이머 화면
│   │   ├── Settings/                # 설정 화면
│   │   ├── AutoRun/                 # 자동 실행 설정
│   │   ├── Report/                  # 리포트/통계
│   │   └── Components/              # 공통 컴포넌트
│   ├── ViewModels/                  # ViewModel
│   └── Theme/                       # 디자인 시스템
│
├── Shield/                          # Shield Extension (별도 타겟)
│   ├── ShieldConfigurationExtension.swift
│   └── ShieldActionExtension.swift
│
├── DeviceActivityMonitor/           # Monitor Extension (별도 타겟)
│   └── DeviceActivityMonitorExtension.swift
│
├── Widget/                          # Widget Extension
│   └── TimerWidget.swift
│
└── Resources/
    ├── Assets.xcassets
    ├── Localizable.strings
    └── Info.plist
```

### 3.3 App Extensions 구성

Screen Time API 사용을 위해 **3개의 App Extension**이 필요합니다:

| Extension | 역할 | 필수 여부 |
|-----------|------|----------|
| **Shield Configuration** | 차단 화면 커스터마이징 | 선택 |
| **Shield Action** | 차단 화면 버튼 액션 | 선택 |
| **Device Activity Monitor** | 스케줄 모니터링, 백그라운드 실행 | 필수 |

---

## 4. 기능 요구사항

### 4.1 핵심 기능 (MVP)

#### 4.1.1 앱 차단 시스템

```swift
// 앱 차단 흐름
1. FamilyControls 권한 요청
2. FamilyActivityPicker로 차단할 앱 선택
3. ManagedSettingsStore에 Shield 설정
4. 사용자가 차단된 앱 실행 시 Shield 화면 표시
```

**UI 흐름**:
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  권한 요청   │ → │  앱 선택     │ → │  차단 활성화 │
│  AlertDialog │    │ActivityPicker│    │  완료 화면  │
└─────────────┘    └─────────────┘    └─────────────┘
```

#### 4.1.2 집중 타이머

```swift
// 타이머 상태
enum FocusState {
    case idle           // 대기 중
    case running        // 타이머 실행 중
    case paused         // 일시정지 (iOS 미지원 고려)
    case success        // 성공 완료
    case failed         // 포기/실패
}
```

**기능**:
- 25/45/60분 타이머 선택
- 타이머 실행 시 앱 차단 자동 활성화
- 타이머 종료 시 차단 해제
- 포기(Give Up) 기능

#### 4.1.3 게임화 시스템

- **포인트**: 집중 시간 1분 = 1포인트
- **스트릭**: 연속 성공 일수
- **회복**: 실패 후 회복 메트릭

#### 4.1.4 시간 기반 자동 실행

```swift
// DeviceActivitySchedule 활용
let schedule = DeviceActivitySchedule(
    intervalStart: DateComponents(hour: 9, minute: 0),
    intervalEnd: DateComponents(hour: 18, minute: 0),
    repeats: true
)

let center = DeviceActivityCenter()
try center.startMonitoring(.workHours, during: schedule)
```

**기능**:
- 요일별 시간대 설정
- 반복 스케줄
- 스케줄 시작 시 자동 차단 활성화

#### 4.1.5 위치 기반 자동 실행

```swift
// Core Location Geofencing
let region = CLCircularRegion(
    center: CLLocationCoordinate2D(latitude: 37.5, longitude: 127.0),
    radius: 100,
    identifier: "office"
)
region.notifyOnEntry = true
region.notifyOnExit = true

locationManager.startMonitoring(for: region)
```

**기능**:
- 위치 등록 (최대 20개)
- 진입 시 자동 차단 활성화
- 이탈 시 자동 비활성화
- 시간표(ScheduleGroup) 연동

### 4.2 확장 기능 (Phase 2)

| 기능 | 설명 | 우선순위 |
|------|------|----------|
| 홈 화면 위젯 | 타이머 상태, 오늘 집중 시간 | 높음 |
| Apple Watch 연동 | 타이머 제어, 알림 | 중간 |
| Siri Shortcuts | "집중 모드 시작" 음성 명령 | 중간 |
| iCloud 동기화 | 설정 및 통계 동기화 | 낮음 |
| Focus Mode 연동 | iOS 집중 모드와 연계 | 높음 |

---

## 5. UI/UX 설계

### 5.1 화면 구성

```
┌─────────────────────────────────────┐
│           Tab Navigation            │
├─────────┬─────────┬─────────┬───────┤
│  타이머  │ 자동실행 │  리포트  │ 설정  │
│  (Home) │(AutoRun)│(Report) │(More) │
└─────────┴─────────┴─────────┴───────┘
```

### 5.2 타이머 화면

```
┌─────────────────────────────────┐
│           Detoxy Timer          │
├─────────────────────────────────┤
│                                 │
│         ╭─────────────╮         │
│         │             │         │
│         │    45:00    │         │  ← 원형 타이머
│         │             │         │
│         ╰─────────────╯         │
│                                 │
│     🔥 12일 연속 집중 중         │  ← 스트릭
│                                 │
├─────────────────────────────────┤
│  [ 25분 ]  [ 45분 ]  [ 60분 ]   │  ← 시간 선택
├─────────────────────────────────┤
│                                 │
│      [    시작하기    ]         │  ← 메인 버튼
│                                 │
│    📊 오늘 2시간 15분 집중       │  ← 오늘 통계
└─────────────────────────────────┘
```

### 5.3 Shield 화면 (차단 화면)

```
┌─────────────────────────────────┐
│                                 │
│            🔒                   │
│                                 │
│      지금은 집중 시간입니다       │
│                                 │
│         남은 시간: 32:15         │
│                                 │
│    ┌─────────────────────┐     │
│    │      포기하기        │     │  ← Shield Action
│    └─────────────────────┘     │
│                                 │
│    💡 조금만 더 집중해보세요     │
│                                 │
└─────────────────────────────────┘
```

### 5.4 자동 실행 설정 화면

```
┌─────────────────────────────────┐
│         자동 실행 설정           │
├─────────────────────────────────┤
│  📅 시간 기반                    │
│  ├─ 📌 출근 시간 (월-금 9-18시)  │
│  ├─ 📌 공부 시간 (매일 20-22시)  │
│  └─ [+ 새 스케줄 추가]           │
├─────────────────────────────────┤
│  📍 위치 기반                    │
│  ├─ 🏢 회사 (반경 100m)         │
│  ├─ 📚 도서관 (반경 50m)        │
│  └─ [+ 새 위치 추가]            │
├─────────────────────────────────┤
│  ⚙️ 전체 설정                   │
│  ├─ 🔔 사전 알림 (5분 전)       │
│  └─ 🔄 자동 시작                │
└─────────────────────────────────┘
```

---

## 6. 데이터 모델

### 6.1 SwiftData 모델

```swift
import SwiftData

@Model
final class FocusSession {
    @Attribute(.unique) var id: UUID
    var startTime: Date
    var endTime: Date?
    var durationMinutes: Int
    var success: Bool
    var createdAt: Date

    init(durationMinutes: Int) {
        self.id = UUID()
        self.startTime = Date()
        self.durationMinutes = durationMinutes
        self.success = false
        self.createdAt = Date()
    }
}

@Model
final class UserSettings {
    @Attribute(.unique) var id: Int = 1
    var totalPoints: Int = 0
    var currentStreak: Int = 0
    var lastSuccessDate: Date?
    var recoveryAttempts: Int = 0
    var recoverySuccesses: Int = 0

    // 차단 설정
    var selectedApps: Data?  // FamilyActivitySelection 인코딩
    var selectedCategories: Data?
}

@Model
final class LocationAutoRun {
    @Attribute(.unique) var id: UUID
    var label: String
    var address: String?
    var latitude: Double
    var longitude: Double
    var radiusMeters: Int
    var durationMinutes: Int
    var isEnabled: Bool
    var activateOnEnter: Bool
    var deactivateOnExit: Bool
    var linkedScheduleGroupId: UUID?
    var createdAt: Date
    var updatedAt: Date
}

@Model
final class TimeAutoRun {
    @Attribute(.unique) var id: UUID
    var label: String
    var startHour: Int
    var startMinute: Int
    var endHour: Int
    var endMinute: Int
    var enabledDays: [Int]  // 0=일, 1=월, ..., 6=토
    var durationMinutes: Int
    var isEnabled: Bool
    var linkedScheduleGroupId: UUID?
}

@Model
final class ScheduleGroup {
    @Attribute(.unique) var id: UUID
    var name: String
    var isEnabled: Bool
    var isLocationBased: Bool
    var createdAt: Date

    @Relationship(deleteRule: .cascade)
    var timeSlots: [TimeSlot]
}
```

### 6.2 Android ↔ iOS 데이터 호환성

| Android (Room) | iOS (SwiftData) | 변환 |
|----------------|-----------------|------|
| `String` (UUID) | `UUID` | 직접 매핑 |
| `Long` (timestamp) | `Date` | `Date(timeIntervalSince1970:)` |
| `Int` | `Int` | 직접 매핑 |
| `Boolean` | `Bool` | 직접 매핑 |
| `String?` | `String?` | 직접 매핑 |

---

## 7. 핵심 구현 상세

### 7.1 앱 차단 Manager

```swift
import FamilyControls
import ManagedSettings

@MainActor
final class ShieldManager: ObservableObject {
    static let shared = ShieldManager()

    @Published var isAuthorized = false
    @Published var selection = FamilyActivitySelection()

    private let store = ManagedSettingsStore()
    private let center = AuthorizationCenter.shared

    // MARK: - 권한 요청
    func requestAuthorization() async throws {
        try await center.requestAuthorization(for: .individual)
        isAuthorized = true
    }

    // MARK: - 앱 차단 활성화
    func enableShield() {
        store.shield.applications = selection.applicationTokens
        store.shield.applicationCategories = .specific(selection.categoryTokens)
        store.shield.webDomains = selection.webDomainTokens
    }

    // MARK: - 앱 차단 비활성화
    func disableShield() {
        store.shield.applications = nil
        store.shield.applicationCategories = nil
        store.shield.webDomains = nil
    }

    // MARK: - 선택 저장/로드
    func saveSelection() {
        // UserDefaults 또는 SwiftData에 인코딩하여 저장
    }
}
```

### 7.2 타이머 ViewModel

```swift
import SwiftUI
import Combine

@MainActor
final class TimerViewModel: ObservableObject {
    // MARK: - Published State
    @Published var state: FocusState = .idle
    @Published var remainingSeconds: Int = 0
    @Published var totalSeconds: Int = 0
    @Published var currentStreak: Int = 0
    @Published var totalPoints: Int = 0

    // MARK: - Dependencies
    private let shieldManager: ShieldManager
    private let repository: FocusRepository
    private let gamificationManager: GamificationManager

    private var timerCancellable: AnyCancellable?
    private var currentSessionId: UUID?

    // MARK: - Timer Control
    func startTimer(durationMinutes: Int) {
        totalSeconds = durationMinutes * 60
        remainingSeconds = totalSeconds
        state = .running

        // 1. 세션 생성
        let session = FocusSession(durationMinutes: durationMinutes)
        currentSessionId = session.id
        repository.save(session)

        // 2. 앱 차단 활성화
        shieldManager.enableShield()

        // 3. 타이머 시작
        timerCancellable = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.tick()
            }
    }

    func giveUp() {
        finishTimer(success: false)
    }

    private func tick() {
        guard remainingSeconds > 0 else {
            finishTimer(success: true)
            return
        }
        remainingSeconds -= 1
    }

    private func finishTimer(success: Bool) {
        timerCancellable?.cancel()
        shieldManager.disableShield()

        state = success ? .success : .failed

        // 세션 종료 및 보상 처리
        if let sessionId = currentSessionId {
            repository.endSession(sessionId, success: success)

            if success {
                let points = gamificationManager.calculatePoints(totalSeconds / 60)
                repository.addPoints(points)
                // 스트릭 업데이트
            }
        }
    }
}
```

### 7.3 위치 기반 Manager

```swift
import CoreLocation

final class LocationAutoRunManager: NSObject, ObservableObject {
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var monitoredRegions: [CLCircularRegion] = []

    private let locationManager = CLLocationManager()
    private let shieldManager: ShieldManager
    private let repository: LocationAutoRunRepository

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func requestAuthorization() {
        locationManager.requestAlwaysAuthorization()
    }

    func registerGeofence(for autoRun: LocationAutoRun) {
        let region = CLCircularRegion(
            center: CLLocationCoordinate2D(
                latitude: autoRun.latitude,
                longitude: autoRun.longitude
            ),
            radius: CLLocationDistance(autoRun.radiusMeters),
            identifier: autoRun.id.uuidString
        )
        region.notifyOnEntry = autoRun.activateOnEnter
        region.notifyOnExit = autoRun.deactivateOnExit

        locationManager.startMonitoring(for: region)
    }

    func removeGeofence(id: UUID) {
        if let region = monitoredRegions.first(where: { $0.identifier == id.uuidString }) {
            locationManager.stopMonitoring(for: region)
        }
    }
}

extension LocationAutoRunManager: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        guard let autoRun = repository.get(id: UUID(uuidString: region.identifier)!) else { return }

        if autoRun.activateOnEnter {
            shieldManager.enableShield()
            // 시간표 활성화
        }
    }

    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        guard let autoRun = repository.get(id: UUID(uuidString: region.identifier)!) else { return }

        if autoRun.deactivateOnExit {
            shieldManager.disableShield()
            // 시간표 비활성화
        }
    }
}
```

### 7.4 DeviceActivity Monitor Extension

```swift
// DeviceActivityMonitorExtension.swift
import DeviceActivity
import ManagedSettings

class DeviceActivityMonitorExtension: DeviceActivityMonitor {
    let store = ManagedSettingsStore()

    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)

        // 스케줄 시작 시 앱 차단 활성화
        // 앱 그룹을 통해 선택된 앱 정보 로드
        if let selection = loadSelection() {
            store.shield.applications = selection.applicationTokens
            store.shield.applicationCategories = .specific(selection.categoryTokens)
        }
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)

        // 스케줄 종료 시 앱 차단 비활성화
        store.shield.applications = nil
        store.shield.applicationCategories = nil
    }

    private func loadSelection() -> FamilyActivitySelection? {
        // App Group UserDefaults에서 로드
        guard let data = UserDefaults(suiteName: "group.com.allday.detoxy")?
            .data(forKey: "selectedApps") else { return nil }
        return try? JSONDecoder().decode(FamilyActivitySelection.self, from: data)
    }
}
```

---

## 8. 개발 로드맵

### 8.1 Phase 1: 기반 구축 (2주)

| 주차 | 작업 | 산출물 |
|------|------|--------|
| 1주 | 프로젝트 설정, entitlement 신청 | Xcode 프로젝트, Apple 승인 |
| 1주 | SwiftData 모델, Repository 구현 | Data Layer |
| 2주 | Screen Time API 통합 | ShieldManager |
| 2주 | 기본 타이머 UI | 타이머 화면 |

### 8.2 Phase 2: 핵심 기능 (3주)

| 주차 | 작업 | 산출물 |
|------|------|--------|
| 3주 | 타이머 ViewModel, 비즈니스 로직 | 완성된 타이머 |
| 3주 | Shield Extension 커스터마이징 | 차단 화면 |
| 4주 | 시간 기반 자동 실행 | DeviceActivity 스케줄 |
| 4주 | 위치 기반 자동 실행 | Core Location Geofencing |
| 5주 | 설정/리포트 화면 | 전체 UI |

### 8.3 Phase 3: 고도화 (2주)

| 주차 | 작업 | 산출물 |
|------|------|--------|
| 6주 | 위젯, 알림 | WidgetKit Extension |
| 6주 | 테스트, 버그 수정 | 안정화 |
| 7주 | App Store 제출 준비 | 스크린샷, 설명문 |
| 7주 | TestFlight 배포, 피드백 | 베타 테스트 |

### 8.4 총 예상 일정

```
┌──────────────────────────────────────────────────────────┐
│  Phase 1 (2주)  │  Phase 2 (3주)  │  Phase 3 (2주)  │ 출시 │
│  기반 구축       │  핵심 기능       │  고도화/테스트   │      │
└──────────────────────────────────────────────────────────┘
       Week 1-2         Week 3-5          Week 6-7      Week 8
```

**총 개발 기간: 약 7-8주**

---

## 9. 리스크 및 대응 방안

### 9.1 기술적 리스크

| 리스크 | 영향 | 대응 방안 |
|--------|------|----------|
| Entitlement 승인 지연 | 개발 지연 | 조기 신청 (1주차) |
| Shield 우회 가능 | 차단 효과 감소 | 게임화로 동기부여 보완 |
| Geofence 20개 제한 | 기능 제한 | 우선순위 기반 자동 관리 |
| App Store 심사 거부 | 출시 지연 | 가이드라인 철저 준수 |

### 9.2 비즈니스 리스크

| 리스크 | 영향 | 대응 방안 |
|--------|------|----------|
| 경쟁 앱 다수 존재 | 차별화 어려움 | 위치 기반 + 시간표 연동 강조 |
| 유료화 저항 | 수익 감소 | Freemium 모델 적용 |

---

## 10. 필요 리소스

### 10.1 개발 환경

- macOS 14+ (Sonoma)
- Xcode 15+
- iOS 15+ 테스트 기기 (iPhone)
- Apple Developer Program 계정

### 10.2 필수 승인

| 항목 | 신청처 | 예상 소요 |
|------|--------|----------|
| `com.apple.developer.family-controls` | Apple Developer Portal | 1-2주 |
| App Store 심사 | App Store Connect | 1-3일 |

### 10.3 인력

| 역할 | 필요 스킬 |
|------|----------|
| iOS 개발자 | Swift, SwiftUI, Screen Time API |
| UI/UX 디자이너 | iOS HIG, Figma |
| QA | iOS 테스트, TestFlight |

---

## 11. 참고 자료

### 11.1 공식 문서

- [Apple WWDC21 - Meet the Screen Time API](https://developer.apple.com/videos/play/wwdc2021/10123/)
- [Apple WWDC22 - What's new in Screen Time API](https://developer.apple.com/videos/play/wwdc2022/110336/)
- [FamilyControls Documentation](https://developer.apple.com/documentation/familycontrols)
- [ManagedSettings Documentation](https://developer.apple.com/documentation/managedsettings)
- [DeviceActivity Documentation](https://developer.apple.com/documentation/deviceactivity)

### 11.2 샘플 프로젝트

- [GitHub - ScreenBreak](https://github.com/christianp-622/ScreenBreak)
- [GitHub - react-native-device-activity](https://github.com/kingstinct/react-native-device-activity)

### 11.3 튜토리얼

- [A Developer's Guide to Apple's Screen Time APIs](https://medium.com/@juliusbrussee/a-developers-guide-to-apple-s-screen-time-apis-familycontrols-managedsettings-deviceactivity-e660147367d7)
- [SwiftUI Tutorial: iOS App Blocker](https://medium.com/@jc_builds/building-a-powerful-ios-app-blocker-with-screen-time-apis-the-complete-guide-f6272bd00fc4)

---

## 12. 결론

iOS Screen Time API (iOS 15+)를 활용하면 Allday Detoxy의 핵심 기능인 **앱 차단**, **시간 기반 스케줄**, **위치 기반 자동 실행**을 iOS에서도 구현할 수 있습니다.

Android 버전과 100% 동일한 기능은 불가능하지만 (DND 직접 제어, 완전한 오버레이 등), 핵심 가치인 "디지털 디톡스"를 제공하는 앱 개발은 **충분히 가능**합니다.

**권장 사항**:
1. Phase 1에서 entitlement 조기 확보
2. Android 앱과 동일한 UX 유지 (브랜드 일관성)
3. iOS 특화 기능 추가 (위젯, Shortcuts, Focus Mode 연동)
4. Freemium 모델로 시장 진입

---

**End of Document**
