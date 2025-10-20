# Allday Detoxy 2차 고도화 작업 계획

- 기준 요약: [2차 고도화 PRD](./02_advanced_autosetting_prd.md) 참고.
- 레퍼런스: 1차 고도화 진행 기록은 `working_history/2025-10-*_1st_advanced_*.md` 참조.

## 0. 개요
- **목표**: 자동 실행 기능(시간/위치 기반)과 커스텀 타이머 UI를 6주 이내 구현하여 2차 고도화 PRD 요구사항을 충족.
- **주요 마일스톤** ([PRD §4](./02_advanced_autosetting_prd.md#4-기능-요구사항)):
  1. Week 1: 데이터 모델 및 기반 작업 (Room v3→v4, AlarmManager, Geofencing)
  2. Week 2: 시간 기반 자동 실행 (설정 UI, 트리거, 알림)
  3. Week 3: 위치 기반 자동 실행 (위치 설정 UI, Geofencing, 권한)
  4. Week 4: 커스텀 타이머 UI (도넛 그래프, 프리셋 관리)
  5. Week 5: 통합 및 QA (대시보드, Analytics, 테스트)
  6. Week 6: 최적화 및 배포 준비 (성능, 배터리, 문서)
- **사전 조건**: 1차 고도화(v0.5) 완료, feat/v0.5 브랜치 안정화 상태.

---

## 1. 준비 단계 (Week 0.5, Day 1-3)

### 1.1 문서 작성 및 기술 조사
- [x] PRD 리뷰 및 기능 범위 확정 → [02_advanced_autosetting_prd.md](./02_advanced_autosetting_prd.md)
- [ ] 화면 와이어프레임 작성 → `02_advanced_wireframe_spec.md`
  - 자동 실행 대시보드
  - 시간 기반 설정 화면 (리스트, 추가 다이얼로그)
  - 위치 기반 설정 화면 (검색, 지도, 추가)
  - 커스텀 타이머 화면 (도넛 그래프)
  - 자동 실행 이력 화면
- [ ] Room 마이그레이션 전략 수립 → `02_advanced_room_migration_strategy.md`
  - v3→v4 마이그레이션 SQL
  - 4개 신규 엔티티 스키마 (TimeBasedAutoRun, LocationBasedAutoRun, CustomTimerPreset, AutoRunLog)
  - 백필 및 테스트 전략
- [ ] Analytics 스키마 설계 → `02_advanced_analytics_schema.md`
  - 21개 신규 이벤트 정의
  - 자동 실행, 커스텀 타이머 관련 파라미터
- [ ] QA 시나리오 작성 → `02_advanced_qa_devices.md`
  - 시간 기반 자동 실행 테스트 (10개 시나리오)
  - 위치 기반 자동 실행 테스트 (8개 시나리오)
  - 커스텀 타이머 UI 테스트 (5개 시나리오)
  - 회귀 테스트 체크리스트

### 1.2 기술 스택 조사
- [ ] AlarmManager vs WorkManager 비교 및 선택
  - 정확한 시간 트리거: AlarmManager.setExactAndAllowWhileIdle() 사용
  - 배터리 최적화 대응 전략
- [ ] Geofencing API 학습
  - Android Geofencing API 문서 리뷰
  - 최대 등록 개수 제한 (100개 → 5개로 제한)
  - 배터리 효율성 (BALANCED_POWER_ACCURACY)
- [ ] 위치 권한 플로우
  - ACCESS_FINE_LOCATION (필수)
  - ACCESS_BACKGROUND_LOCATION (Android 10+ 필수)
  - 권한 요청 베스트 프랙티스
- [ ] Compose Canvas 학습
  - 도넛 그래프 그리기
  - 터치 이벤트 처리 (드래그, 탭)
  - 애니메이션 (Animatable)

**작업 기록**: `working_history/2025-10-21_2nd_advanced_0.1.md`

---

## 2. 데이터 모델 및 기반 작업 (Week 1, Day 4-8)

### 2.1 Room 마이그레이션 v3→v4 (Day 4-5)

#### 2.1.1 신규 엔티티 정의
- [ ] **TimeBasedAutoRun** 엔티티 생성 → [PRD §5.1](./02_advanced_autosetting_prd.md#51-로컬-db-room-v3v4), **[마이그레이션 전략 문서](./02_advanced_room_migration_strategy.md)** (작성 예정)
  ```kotlin
  @Entity(tableName = "time_based_auto_run")
  data class TimeBasedAutoRun(
      @PrimaryKey val id: String = UUID.randomUUID().toString(),
      val hour: Int, // 0-23
      val minute: Int, // 0-59
      val durationMinutes: Int,
      val presetType: String, // FULL_BLOCK, STANDARD, RELAXED
      val enabledDays: String, // JSON: ["MON", "TUE", ...]
      val label: String? = null,
      val isEnabled: Boolean = true,
      val createdAt: Long = System.currentTimeMillis()
  )
  ```
  - **참조**: [1차 고도화 마이그레이션 전략](./01_advanced_room_migration_strategy.md) - Room 마이그레이션 패턴

- [ ] **LocationBasedAutoRun** 엔티티 생성
  ```kotlin
  @Entity(tableName = "location_based_auto_run")
  data class LocationBasedAutoRun(
      @PrimaryKey val id: String = UUID.randomUUID().toString(),
      val label: String,
      val address: String,
      val latitude: Double,
      val longitude: Double,
      val radiusMeters: Int, // 50, 100, 200, 500
      val durationMinutes: Int,
      val presetType: String,
      val triggerType: String, // ENTER, PERIODIC
      val periodicIntervalMinutes: Int? = null,
      val isEnabled: Boolean = true,
      val createdAt: Long = System.currentTimeMillis()
  )
  ```

- [ ] **CustomTimerPreset** 엔티티 생성
  ```kotlin
  @Entity(tableName = "custom_timer_preset")
  data class CustomTimerPreset(
      @PrimaryKey val id: String = UUID.randomUUID().toString(),
      val name: String,
      val durationMinutes: Int,
      val presetType: String? = null, // 연결된 차단 프리셋
      val usageCount: Int = 0,
      val displayOrder: Int = 0,
      val createdAt: Long = System.currentTimeMillis()
  )
  ```

- [ ] **AutoRunLog** 엔티티 생성
  ```kotlin
  @Entity(tableName = "auto_run_log")
  data class AutoRunLog(
      @PrimaryKey val id: String = UUID.randomUUID().toString(),
      val triggerType: String, // TIME, LOCATION
      val triggerSourceId: String,
      val triggerTime: Long,
      val result: String, // STARTED, SKIPPED, FAILED
      val failureReason: String? = null,
      val sessionId: String? = null
  )
  ```

#### 2.1.2 DAO 인터페이스 작성
- [ ] **TimeBasedAutoRunDao** 생성 (10개 메서드) → [PRD §5.1](./02_advanced_autosetting_prd.md#51-로컬-db-room-v3v4)
  - insert, update, delete
  - getAll, getById, getEnabled
  - getByHourAndMinute, toggleEnabled
  - getEnabledForDay(dayOfWeek)
  - **참조**: [FocusSessionDao 패턴](../app/src/main/java/com/allday/detoxy/data/local/dao/FocusSessionDao.kt) - 기존 DAO 구현 패턴
- [ ] **LocationBasedAutoRunDao** 생성 (10개 메서드)
  - insert, update, delete
  - getAll, getById, getEnabled
  - toggleEnabled, incrementUsage
- [ ] **CustomTimerPresetDao** 생성 (12개 메서드)
  - insert, update, delete
  - getAll, getById, getByDisplayOrder
  - updateDisplayOrder, incrementUsageCount
- [ ] **AutoRunLogDao** 생성 (8개 메서드)
  - insert, getAll, getByTriggerType
  - getRecentLogs(limit), getLogsInRange
  - getStatistics (성공률, 총 횟수)

#### 2.1.3 마이그레이션 스크립트
- [ ] **Migration_3_4.kt** 작성 → [PRD §5.1](./02_advanced_autosetting_prd.md#51-로컬-db-room-v3v4), **[마이그레이션 전략](./02_advanced_room_migration_strategy.md)** (작성 예정)
  ```kotlin
  val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(database: SupportSQLiteDatabase) {
          // TimeBasedAutoRun 테이블 생성
          database.execSQL("""
              CREATE TABLE IF NOT EXISTS time_based_auto_run (
                  id TEXT PRIMARY KEY NOT NULL,
                  hour INTEGER NOT NULL,
                  minute INTEGER NOT NULL,
                  ...
              )
          """)
          // 인덱스 생성
          database.execSQL("CREATE INDEX idx_time_based_hour_minute ON time_based_auto_run(hour, minute)")
          
          // LocationBasedAutoRun 테이블 생성
          // CustomTimerPreset 테이블 생성
          // AutoRunLog 테이블 생성
          
          // UserSettings 필드 추가
          database.execSQL("ALTER TABLE user_settings ADD COLUMN autoRunWeekendExcluded INTEGER NOT NULL DEFAULT 0")
      }
  }
  ```
  - **참조**: [Migration_2_3.kt](../app/src/main/java/com/allday/detoxy/data/local/migration/Migration_2_3.kt) - 이전 마이그레이션 패턴
  - **참조**: [1차 고도화 마이그레이션 작업](../working_history/2025-10-19_1st_advanced_2B.1.md)

- [ ] DetoxyDatabase v4 업데이트 → [DetoxyDatabase.kt](../app/src/main/java/com/allday/detoxy/data/local/DetoxyDatabase.kt)
- [ ] DatabaseModule에 MIGRATION_3_4 추가 → [DatabaseModule.kt](../app/src/main/java/com/allday/detoxy/core/di/DatabaseModule.kt)

#### 2.1.4 마이그레이션 테스트
- [ ] 단위 테스트 작성 (MigrationTest) → **[마이그레이션 테스트 전략](./02_advanced_room_migration_strategy.md#테스트-전략)** (작성 예정)
  - **참조**: [1차 고도화 마이그레이션 테스트](./01_advanced_room_migration_strategy.md#6-테스트-전략)
- [ ] 빌드 검증: `./gradlew compileDebugKotlin`
  - **참조**: [빌드 및 테스트 명령어](./00_mvp_allday_detoxy_todolist.md#빌드-및-테스트-명령어)

**작업 기록**: `working_history/2025-10-21_2nd_advanced_2.1.md`

### 2.2 AlarmManager 래퍼 클래스 (Day 6)

#### 2.2.1 AutoRunAlarmManager 클래스
- [ ] **클래스 설계** → [PRD §4.1](./02_advanced_autosetting_prd.md#41-시간-기반-자동-실행)
  ```kotlin
  @Singleton
  class AutoRunAlarmManager @Inject constructor(
      private val context: Context,
      private val alarmManager: AlarmManager
  ) {
      fun scheduleTimeBasedAutoRun(autoRun: TimeBasedAutoRun)
      fun cancelTimeBasedAutoRun(autoRunId: String)
      fun rescheduleAll(autoRuns: List<TimeBasedAutoRun>)
      private fun createPendingIntent(autoRunId: String): PendingIntent
  }
  ```
  - **참조**: [Android AlarmManager 문서](https://developer.android.com/training/scheduling/alarms)
  - **참조**: [기존 TimerViewModel 패턴](../app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt)

- [ ] **AlarmManager 설정** → [PRD §4.1.3](./02_advanced_autosetting_prd.md#413-자동-시작-로직)
  - `setExactAndAllowWhileIdle()` 사용 (Android 6.0+)
  - PendingIntent.FLAG_IMMUTABLE (Android 12+)
  - 요일별 알람 계산 로직

- [ ] **Broadcast Receiver 생성** → [PRD §4.1.2](./02_advanced_autosetting_prd.md#412-자동-실행-알림)
  ```kotlin
  class AutoRunAlarmReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          val autoRunId = intent.getStringExtra("autoRunId")
          // 알림 표시 로직
          // AutoRunNotificationManager 호출
      }
  }
  ```
  - **참조**: [Android BroadcastReceiver 문서](https://developer.android.com/guide/components/broadcasts)

- [ ] AndroidManifest에 Receiver 등록 → [AndroidManifest.xml](../app/src/main/AndroidManifest.xml)

#### 2.2.2 WorkManager 백업 로직
- [ ] **WorkManager 구현** (AlarmManager 실패 시 대체)
  ```kotlin
  class AutoRunWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
      override fun doWork(): Result {
          // 자동 실행 트리거
      }
  }
  ```

- [ ] OneTimeWorkRequest 스케줄링
- [ ] AlarmManager 우선, WorkManager 백업 전략

**작업 기록**: `working_history/2025-10-21_2nd_advanced_2.2.md`

### 2.3 Geofencing 래퍼 클래스 (Day 7-8)

#### 2.3.1 AutoRunGeofenceManager 클래스
- [ ] **클래스 설계** → [PRD §4.2](./02_advanced_autosetting_prd.md#42-위치-기반-자동-실행)
  ```kotlin
  @Singleton
  class AutoRunGeofenceManager @Inject constructor(
      private val context: Context,
      private val geofencingClient: GeofencingClient
  ) {
      suspend fun addGeofence(autoRun: LocationBasedAutoRun): Result<Unit>
      suspend fun removeGeofence(autoRunId: String): Result<Unit>
      suspend fun removeAllGeofences(): Result<Unit>
      private fun createGeofence(autoRun: LocationBasedAutoRun): Geofence
      private fun createGeofencingRequest(geofences: List<Geofence>): GeofencingRequest
  }
  ```
  - **참조**: [Android Geofencing API 문서](https://developer.android.com/training/location/geofencing)
  - **참조**: [Google Play Services Location API](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary)

- [ ] **Geofence 설정** → [PRD §4.2.2](./02_advanced_autosetting_prd.md#422-geofencing-구현)
  - ENTER 트리거 (dwell time 없음)
  - Expiration: NEVER_EXPIRE
  - Loitering delay: 0ms
  - Transition types: GEOFENCE_TRANSITION_ENTER

- [ ] **BroadcastReceiver 생성**
  ```kotlin
  class GeofenceTransitionsReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          val geofencingEvent = GeofencingEvent.fromIntent(intent)
          if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
              // 위치 기반 자동 실행 트리거
          }
      }
  }
  ```

#### 2.3.2 위치 권한 관리
- [ ] **PermissionUtils 확장** → [PRD §4.2.3](./02_advanced_autosetting_prd.md#423-위치-권한-관리)
  - `hasLocationPermission()`: ACCESS_FINE_LOCATION 확인
  - `hasBackgroundLocationPermission()`: Android 10+ 확인
  - `openLocationSettings()`: 설정 화면 이동
  - `shouldShowLocationRationale()`: 권한 거부 시 설명 표시 여부
  - **참조**: [기존 PermissionUtils](../app/src/main/java/com/allday/detoxy/core/utils/PermissionUtils.kt) - 접근성, DND 권한 패턴
  - **참조**: [권한 요청 작업 기록](../working_history/2025-10-05_1.2.md)

- [ ] **위치 권한 요청 플로우** → [PRD §4.2.3](./02_advanced_autosetting_prd.md#423-위치-권한-관리)
  1. ACCESS_FINE_LOCATION 요청
  2. 승인 후 백그라운드 위치 필요성 설명
  3. ACCESS_BACKGROUND_LOCATION 요청
  4. 설정 화면으로 이동 ("항상 허용" 선택)
  - **참조**: [Android 위치 권한 가이드](https://developer.android.com/training/location/permissions)

**작업 기록**: `working_history/2025-10-21_2nd_advanced_2.3.md`

---

## 3. 시간 기반 자동 실행 (Week 2, Day 9-13)

### 3.1 시간대 설정 UI (Day 9-10)

#### 3.1.1 TimeBasedAutoRunScreen 레이아웃
- [ ] **메인 화면 Compose** → [PRD §4.1.1](./02_advanced_autosetting_prd.md#411-자동-실행-시간-설정-화면), **[Wireframe 스펙](./02_advanced_wireframe_spec.md)** (작성 예정)
  ```kotlin
  @Composable
  fun TimeBasedAutoRunScreen(
      viewModel: TimeBasedAutoRunViewModel = hiltViewModel()
  ) {
      val autoRuns by viewModel.autoRuns.collectAsState()
      
      Column {
          // 헤더: "시간 기반 자동 실행"
          // 시간대 리스트 (LazyColumn)
          // + 시간대 추가 버튼
          // 글로벌 옵션 섹션
      }
  }
  ```
  - **참조**: [DetoxyControlSettingsScreen](../app/src/main/java/com/allday/detoxy/presentation/ui/settings/focus/DetoxyControlSettingsScreen.kt) - 설정 화면 레이아웃 패턴
  - **참조**: [1차 고도화 UI 작업](../working_history/2025-10-15_1st_advanced_2.2.md)

- [ ] **TimeBasedAutoRunCard** 컴포넌트 → **[Wireframe 컴포넌트 스펙](./02_advanced_wireframe_spec.md#시간대-카드)** (작성 예정)
  - 시간 표시 (10:00 AM)
  - 타이머 시간 + 차단 프리셋
  - 요일 표시 (월, 화, 수, 목, 금)
  - 활성화 토글
  - 편집/삭제 아이콘 버튼
  - **참조**: [기존 카드 컴포넌트 패턴](../app/src/main/java/com/allday/detoxy/presentation/ui/report/components/)

- [ ] **AddTimeBasedAutoRunDialog** 다이얼로그 → [PRD §4.1.1](./02_advanced_autosetting_prd.md#411-자동-실행-시간-설정-화면)
  - TimePicker (24시간 형식)
  - 타이머 시간 선택 (프리셋 또는 입력)
  - 차단 프리셋 선택 (완전 차단/표준/완화)
  - 요일 선택 (다중 선택, 체크박스)
  - 라벨 입력 (TextField, 선택)
  - "저장" 버튼
  - **참조**: [MessengerCategoryDialog](../app/src/main/java/com/allday/detoxy/presentation/ui/settings/focus/MessengerCategoryDialog.kt) - 다이얼로그 패턴

#### 3.1.2 TimeBasedAutoRunViewModel
- [ ] **StateFlow 정의** → [PRD §4.1](./02_advanced_autosetting_prd.md#41-시간-기반-자동-실행)
  ```kotlin
  @HiltViewModel
  class TimeBasedAutoRunViewModel @Inject constructor(
      private val repository: TimeBasedAutoRunRepository,
      private val alarmManager: AutoRunAlarmManager
  ) : ViewModel() {
      val autoRuns: StateFlow<List<TimeBasedAutoRun>>
      val globalOptions: StateFlow<AutoRunGlobalOptions>
      
      fun addAutoRun(autoRun: TimeBasedAutoRun)
      fun updateAutoRun(autoRun: TimeBasedAutoRun)
      fun deleteAutoRun(autoRunId: String)
      fun toggleAutoRun(autoRunId: String, isEnabled: Boolean)
  }
  ```
  - **참조**: [ReportViewModel 패턴](../app/src/main/java/com/allday/detoxy/presentation/viewmodel/ReportViewModel.kt) - ViewModel 구조
  - **참조**: [TimerViewModel](../app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt) - StateFlow 관리 패턴

- [ ] **Repository 구현**
  ```kotlin
  class TimeBasedAutoRunRepository @Inject constructor(
      private val dao: TimeBasedAutoRunDao
  ) {
      fun getAll(): Flow<List<TimeBasedAutoRun>>
      suspend fun insert(autoRun: TimeBasedAutoRun)
      suspend fun update(autoRun: TimeBasedAutoRun)
      suspend fun delete(autoRunId: String)
  }
  ```
  - **참조**: [FocusRepository 패턴](../app/src/main/java/com/allday/detoxy/data/repository/FocusRepository.kt)

#### 3.1.3 글로벌 옵션 UI
- [ ] **GlobalOptionsSection** 컴포넌트 → [PRD §4.1.4](./02_advanced_autosetting_prd.md#414-글로벌-옵션)
  - 주말 제외 토글
  - 자동 시작 딜레이 선택 (0분, 5분, 10분)
  - 사전 알림 시간 선택 (0분, 5분, 10분, 15분)
  - DataStore에 저장
  - **참조**: [FocusSettingsRepository](../app/src/main/java/com/allday/detoxy/data/repository/FocusSettingsRepository.kt) - DataStore 패턴

**작업 기록**: `working_history/2025-10-22_2nd_advanced_3.1.md`

### 3.2 AlarmManager 연동 및 트리거 (Day 11)

#### 3.2.1 알람 스케줄링
- [ ] ViewModel에서 AlarmManager 호출
  ```kotlin
  fun addAutoRun(autoRun: TimeBasedAutoRun) {
      viewModelScope.launch {
          repository.insert(autoRun)
          if (autoRun.isEnabled) {
              alarmManager.scheduleTimeBasedAutoRun(autoRun)
          }
      }
  }
  ```

- [ ] 앱 재시작 시 알람 재등록
  ```kotlin
  class BootCompletedReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
              // 모든 활성화된 자동 실행 재등록
          }
      }
  }
  ```

#### 3.2.2 알람 트리거 처리
- [ ] AutoRunAlarmReceiver에서 알림 표시
  ```kotlin
  override fun onReceive(context: Context, intent: Intent) {
      val autoRunId = intent.getStringExtra("autoRunId") ?: return
      
      // DB에서 autoRun 정보 가져오기
      // 이미 타이머 실행 중인지 확인
      // AutoRunNotificationManager.showNotification()
      // AutoRunLog 기록
  }
  ```

- [ ] 다음 알람 자동 스케줄링 (다음 요일 계산)

**작업 기록**: `working_history/2025-10-22_2nd_advanced_3.2.md`

### 3.3 자동 실행 알림 및 액션 (Day 12-13)

#### 3.3.1 AutoRunNotificationManager 클래스
- [ ] **알림 생성** → [PRD §4.1.2](./02_advanced_autosetting_prd.md#412-자동-실행-알림)
  ```kotlin
  class AutoRunNotificationManager @Inject constructor(
      private val context: Context,
      private val notificationManager: NotificationManager
  ) {
      fun showPreNotification(autoRun: TimeBasedAutoRun, minutesBefore: Int)
      fun showStartNotification(autoRun: TimeBasedAutoRun)
      fun dismissNotification(autoRunId: String)
  }
  ```
  - **참조**: [Android Notification 문서](https://developer.android.com/develop/ui/views/notifications)
  - **참조**: [기존 ForegroundService 알림 패턴](../app/src/main/java/com/allday/detoxy/service/overlay/LockOverlayService.kt)

- [ ] **알림 채널 생성** (AndroidManifest 업데이트) → [PRD §8.1](./02_advanced_autosetting_prd.md#81-자동-실행-대시보드)
  - Channel ID: "auto_run_notifications"
  - 중요도: HIGH
  - 소리, 진동 활성화

#### 3.3.2 알림 액션 처리
- [ ] **NotificationActionReceiver** 생성
  ```kotlin
  class NotificationActionReceiver : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
          when (intent.action) {
              ACTION_START -> startTimer(autoRunId, durationMinutes)
              ACTION_SNOOZE -> snoozeAutoRun(autoRunId, 10)
              ACTION_SKIP -> skipAutoRun(autoRunId)
          }
      }
      
      private fun startTimer(autoRunId: String, durationMinutes: Int) {
          // TimerViewModel.startTimer() 호출
          // AutoRunLog 기록 (STARTED)
      }
  }
  ```

- [ ] 자동 시작 로직 (사용자 반응 없을 때)
  - 5분 후 자동으로 타이머 시작 (설정 가능)
  - WorkManager로 지연 작업 스케줄링

#### 3.3.3 통합 테스트
- [ ] 시간 기반 자동 실행 E2E 테스트
  - 설정 생성 → 알람 등록 → 트리거 → 알림 표시 → 세션 시작
- [ ] 빌드 검증: `./gradlew assembleDebug`
- [ ] Lint 검증: `./gradlew lint`

**작업 기록**: `working_history/2025-10-23_2nd_advanced_3.3.md`

---

## 4. 위치 기반 자동 실행 (Week 3, Day 14-18)

### 4.1 위치 설정 UI (Day 14-15)

#### 4.1.1 LocationBasedAutoRunScreen 레이아웃
- [ ] **메인 화면 Compose**
  ```kotlin
  @Composable
  fun LocationBasedAutoRunScreen(
      viewModel: LocationBasedAutoRunViewModel = hiltViewModel()
  ) {
      val locations by viewModel.locations.collectAsState()
      val permissionState by viewModel.permissionState.collectAsState()
      
      Column {
          // 위치 권한 안내 카드 (권한 없을 때)
          // 등록된 위치 리스트
          // + 위치 추가 버튼
      }
  }
  ```

- [ ] **LocationBasedAutoRunCard** 컴포넌트
  - 위치 이름 + 주소
  - 반경 표시 (반경 100m)
  - 타이머 시간 + 차단 프리셋
  - 트리거 타입 (도착 시 시작 / 주기적)
  - 활성화 토글
  - 편집/삭제 버튼

- [ ] **LocationPermissionCard** 컴포넌트
  - 위치 권한 필요성 설명
  - "권한 설정하기" 버튼
  - 현재 권한 상태 표시

#### 4.1.2 AddLocationAutoRunDialog
- [ ] **위치 검색 단계**
  - 주소 입력 TextField
  - Google Places API (또는 Geocoder) 연동
  - 검색 결과 리스트

- [ ] **위치 확인 단계** (선택: 지도 표시)
  - 선택한 위치 표시
  - 반경 원 표시
  - 위치 미세 조정 (드래그)

- [ ] **설정 단계**
  - 라벨 입력 (예: "회사", "도서관")
  - 반경 선택 (50m, 100m, 200m, 500m)
  - 타이머 시간 선택
  - 차단 프리셋 선택
  - 트리거 타입 선택 (도착 시 / 주기적)
  - "저장" 버튼

#### 4.1.3 LocationBasedAutoRunViewModel
- [ ] **StateFlow 정의**
  ```kotlin
  @HiltViewModel
  class LocationBasedAutoRunViewModel @Inject constructor(
      private val repository: LocationBasedAutoRunRepository,
      private val geofenceManager: AutoRunGeofenceManager,
      private val permissionUtils: PermissionUtils
  ) : ViewModel() {
      val locations: StateFlow<List<LocationBasedAutoRun>>
      val permissionState: StateFlow<LocationPermissionState>
      
      fun addLocation(location: LocationBasedAutoRun)
      fun updateLocation(location: LocationBasedAutoRun)
      fun deleteLocation(locationId: String)
      fun toggleLocation(locationId: String, isEnabled: Boolean)
      fun requestLocationPermission()
  }
  ```

**작업 기록**: `working_history/2025-10-24_2nd_advanced_4.1.md`

### 4.2 Geofencing 등록 및 트리거 (Day 16-17)

#### 4.2.1 Geofence 등록
- [ ] ViewModel에서 GeofenceManager 호출
  ```kotlin
  fun addLocation(location: LocationBasedAutoRun) {
      viewModelScope.launch {
          repository.insert(location)
          if (location.isEnabled) {
              val result = geofenceManager.addGeofence(location)
              if (result.isFailure) {
                  // 실패 처리 (권한 없음, 제한 초과 등)
              }
          }
      }
  }
  ```

- [ ] Geofence 등록 실패 처리
  - 권한 없음: 권한 요청 유도
  - 최대 개수 초과: 경고 메시지
  - 위치 서비스 비활성화: 설정 안내

#### 4.2.2 Geofence 트리거 처리
- [ ] GeofenceTransitionsReceiver 구현
  ```kotlin
  override fun onReceive(context: Context, intent: Intent) {
      val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
      
      if (geofencingEvent.hasError()) {
          // 에러 로깅
          return
      }
      
      if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
          val triggeringGeofences = geofencingEvent.triggeringGeofences
          triggeringGeofences?.forEach { geofence ->
              val locationId = geofence.requestId
              // DB에서 location 정보 가져오기
              // AutoRunNotificationManager.showNotification()
              // AutoRunLog 기록
          }
      }
  }
  ```

- [ ] EXIT 트리거 처리 (선택)
  - 위치 이탈 시 타이머 종료 안내 다이얼로그

#### 4.2.3 앱 재시작 시 Geofence 재등록
- [ ] BootCompletedReceiver에서 재등록
  ```kotlin
  override fun onReceive(context: Context, intent: Intent) {
      if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
          // 모든 활성화된 위치 기반 자동 실행 재등록
          geofenceManager.rescheduleAll(enabledLocations)
      }
  }
  ```

**작업 기록**: `working_history/2025-10-25_2nd_advanced_4.2.md`

### 4.3 위치 권한 플로우 (Day 18)

#### 4.3.1 위치 권한 요청
- [ ] **정확한 위치 권한 요청**
  ```kotlin
  val locationPermissionLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted ->
      if (isGranted) {
          // 백그라운드 위치 필요성 설명
          showBackgroundLocationDialog = true
      } else {
          // 권한 거부 처리
      }
  }
  ```

- [ ] **백그라운드 위치 권한 요청** (Android 10+)
  ```kotlin
  val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { isGranted ->
      if (isGranted) {
          // Geofence 등록 가능
      } else {
          // 설정 화면으로 이동 안내
      }
  }
  ```

#### 4.3.2 권한 안내 다이얼로그
- [ ] **BackgroundLocationPermissionDialog**
  - 왜 백그라운드 위치가 필요한지 설명
  - "항상 허용"을 선택해야 함을 명시
  - "설정으로 이동" 버튼
  - "나중에" 버튼

- [ ] **LocationPermissionDeniedDialog**
  - 권한 거부 시 위치 기반 자동 실행 불가 안내
  - 시간 기반 자동 실행 대안 제시
  - "권한 재요청" 버튼

#### 4.3.3 권한 상태 모니터링
- [ ] 위치 권한 변경 감지
  - 앱 포그라운드 진입 시 권한 재확인
  - 권한 해제 시 Geofence 자동 해제
  - 사용자에게 경고 알림 표시

**작업 기록**: `working_history/2025-10-25_2nd_advanced_4.3.md`

---

## 5. 커스텀 타이머 UI (Week 4, Day 19-23)

### 5.1 도넛 그래프 UI 구현 (Day 19-20)

#### 5.1.1 CustomTimerScreen 레이아웃
- [ ] **메인 화면 Compose**
  ```kotlin
  @Composable
  fun CustomTimerScreen(
      viewModel: TimerViewModel = hiltViewModel()
  ) {
      var selectedMinutes by remember { mutableStateOf(25) }
      
      Column {
          // 도넛 그래프
          DonutTimerPicker(
              selectedMinutes = selectedMinutes,
              onMinutesChange = { selectedMinutes = it },
              minMinutes = 5,
              maxMinutes = 180,
              stepMinutes = 5
          )
          
          // "프리셋으로 저장" 버튼 (커스텀 시간일 때)
          if (!isDefaultPreset(selectedMinutes)) {
              Button(onClick = { showSaveDialog = true }) {
                  Text("프리셋으로 저장")
              }
          }
          
          // 프리셋 버튼 리스트
          PresetButtonRow()
          
          // "시작하기" 버튼
          Button(onClick = { viewModel.startTimer(selectedMinutes) }) {
              Text("시작하기")
          }
      }
  }
  ```

#### 5.1.2 DonutTimerPicker Composable
- [ ] **Canvas 그리기** → [PRD §4.3.1](./02_advanced_autosetting_prd.md#431-도넛-그래프-타이머-ui), **[Wireframe 스펙](./02_advanced_wireframe_spec.md#커스텀-타이머)** (작성 예정)
  ```kotlin
  @Composable
  fun DonutTimerPicker(
      selectedMinutes: Int,
      onMinutesChange: (Int) -> Unit,
      minMinutes: Int = 5,
      maxMinutes: Int = 180,
      stepMinutes: Int = 5,
      modifier: Modifier = Modifier
  ) {
      var currentAngle by remember { mutableStateOf(minutesToAngle(selectedMinutes)) }
      val animatedAngle by animateFloatAsState(
          targetValue = currentAngle,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
      )
      
      Canvas(modifier = modifier.pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
              // 드래그 처리
              val angle = calculateAngle(change.position, center)
              val minutes = angleToMinutes(angle, minMinutes, maxMinutes, stepMinutes)
              onMinutesChange(minutes)
              currentAngle = angle
              
              // 햅틱 피드백 (5분 단위)
              performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
      }) {
          // 배경 원 그리기
          drawCircle(
              color = Color.LightGray.copy(alpha = 0.3f),
              radius = size.minDimension / 2 - strokeWidth,
              style = Stroke(width = strokeWidth)
          )
          
          // 선택된 영역 그리기 (arc)
          drawArc(
              color = Color(0xFF4CAF50),
              startAngle = -90f,
              sweepAngle = animatedAngle,
              useCenter = false,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          )
          
          // 시간 눈금 그리기 (5분, 15분, 30분, 60분, 90분, 120분, 180분)
          drawTimeMarkers()
          
          // 드래그 핸들 그리기
          drawDragHandle(animatedAngle)
      }
      
      // 중앙 텍스트 (선택된 시간)
      Text(
          text = "${selectedMinutes}분",
          style = MaterialTheme.typography.displayLarge
      )
  }
  ```

- [ ] **각도 ↔ 시간 변환 함수**
  ```kotlin
  private fun minutesToAngle(minutes: Int, max: Int = 180): Float {
      return (minutes.toFloat() / max) * 360f
  }
  
  private fun angleToMinutes(angle: Float, min: Int, max: Int, step: Int): Int {
      val minutes = (angle / 360f * max).toInt()
      return minutes.coerceIn(min, max).roundToNearestStep(step)
  }
  ```

#### 5.1.3 터치 인터랙션
- [ ] **드래그 제스처 처리** → [PRD §4.3.1](./02_advanced_autosetting_prd.md#431-도넛-그래프-타이머-ui)
  - `detectDragGestures`로 드래그 감지
  - 터치 위치 → 각도 계산
  - 각도 → 시간(분) 변환
  - 5분 단위로 스냅 (roundToNearestStep)
  - **참조**: [Compose Gesture 문서](https://developer.android.com/jetpack/compose/touch-input)

- [ ] **탭 제스처 처리**
  - `detectTapGestures`로 탭 감지
  - 탭 위치 → 각도 계산
  - 즉시 해당 시간으로 설정

- [ ] **햅틱 피드백** → [PRD §4.3.1](./02_advanced_autosetting_prd.md#431-도넛-그래프-타이머-ui)
  - 5분 단위 변경 시 미세한 진동
  - `HapticFeedbackType.TextHandleMove` 사용
  - **참조**: [Android HapticFeedback 문서](https://developer.android.com/reference/android/view/HapticFeedbackConstants)

**작업 기록**: `working_history/2025-10-26_2nd_advanced_5.1.md`

### 5.2 커스텀 프리셋 저장 및 관리 (Day 21-22)

#### 5.2.1 SavePresetDialog
- [ ] **다이얼로그 UI**
  ```kotlin
  @Composable
  fun SavePresetDialog(
      durationMinutes: Int,
      onSave: (name: String, presetType: String?) -> Unit,
      onDismiss: () -> Unit
  ) {
      AlertDialog(
          onDismissRequest = onDismiss,
          title = { Text("프리셋 저장") },
          text = {
              Column {
                  Text("${durationMinutes}분 타이머를 프리셋으로 저장합니다.")
                  
                  Spacer(modifier = Modifier.height(16.dp))
                  
                  TextField(
                      value = presetName,
                      onValueChange = { presetName = it },
                      label = { Text("프리셋 이름 (예: 오후 집중)") },
                      singleLine = true
                  )
                  
                  Spacer(modifier = Modifier.height(16.dp))
                  
                  Text("차단 프리셋 연결 (선택)")
                  PresetTypeSelector(
                      selectedType = selectedPresetType,
                      onTypeSelected = { selectedPresetType = it }
                  )
              }
          },
          confirmButton = {
              Button(onClick = { onSave(presetName, selectedPresetType) }) {
                  Text("저장")
              }
          },
          dismissButton = {
              TextButton(onClick = onDismiss) {
                  Text("취소")
              }
          }
      )
  }
  ```

#### 5.2.2 CustomTimerPresetViewModel
- [ ] **StateFlow 정의**
  ```kotlin
  @HiltViewModel
  class CustomTimerPresetViewModel @Inject constructor(
      private val repository: CustomTimerPresetRepository
  ) : ViewModel() {
      val presets: StateFlow<List<CustomTimerPreset>>
      
      fun savePreset(name: String, durationMinutes: Int, presetType: String?)
      fun deletePreset(presetId: String)
      fun updateDisplayOrder(presets: List<CustomTimerPreset>)
      fun incrementUsageCount(presetId: String)
  }
  ```

#### 5.2.3 프리셋 리스트 UI
- [ ] **PresetButtonRow** 컴포넌트
  - 기본 프리셋 (25분, 45분, 60분)
  - 커스텀 프리셋 (최대 10개)
  - 스크롤 가능한 Row
  - 길게 누르면 편집/삭제 옵션

- [ ] **PresetButton** 컴포넌트
  ```kotlin
  @Composable
  fun PresetButton(
      preset: CustomTimerPreset,
      isSelected: Boolean,
      onClick: () -> Unit,
      onLongClick: () -> Unit
  ) {
      Button(
          onClick = onClick,
          modifier = Modifier
              .pointerInput(Unit) {
                  detectTapGestures(onLongPress = { onLongClick() })
              },
          colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.secondary
          )
      ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(preset.name)
              Text("${preset.durationMinutes}분", style = MaterialTheme.typography.bodySmall)
          }
      }
  }
  ```

#### 5.2.4 프리셋 관리 화면
- [ ] **ManagePresetsDialog**
  - 프리셋 리스트 (드래그로 순서 변경)
  - 편집 버튼 (이름, 시간, 차단 프리셋 변경)
  - 삭제 버튼
  - 사용 횟수 표시

**작업 기록**: `working_history/2025-10-27_2nd_advanced_5.2.md`

### 5.3 기존 타이머 화면 리팩토링 (Day 23)

#### 5.3.1 TimerScreen 통합
- [ ] CustomTimerScreen을 TimerScreen으로 통합 → [PRD §4.3.3](./02_advanced_autosetting_prd.md#433-빠른-시작)
  - 기존 타이머 UI 제거
  - 도넛 그래프를 메인 UI로 사용
  - 프리셋 버튼 하단 배치
  - **참조**: [기존 TimerScreen](../app/src/main/java/com/allday/detoxy/presentation/ui/timer/TimerScreen.kt) - 현재 타이머 UI

- [ ] TimerViewModel 확장
  - CustomTimerPreset 로딩
  - 프리셋 선택 시 시간 자동 설정
  - 사용 횟수 자동 증가
  - **참조**: [기존 TimerViewModel](../app/src/main/java/com/allday/detoxy/presentation/viewmodel/TimerViewModel.kt)

#### 5.3.2 하위 호환성 유지
- [ ] 기존 프리셋 데이터 마이그레이션
  - 25분, 45분, 60분 → CustomTimerPreset으로 자동 생성
  - displayOrder: 0, 1, 2 (최우선)

**작업 기록**: `working_history/2025-10-27_2nd_advanced_5.3.md`

---

## 6. 통합 및 QA (Week 5, Day 24-28)

### 6.1 자동 실행 대시보드 (Day 24-25)

#### 6.1.1 AutoRunDashboardScreen
- [ ] **통계 카드**
  - 이번 주 자동 실행 횟수
  - 자동 실행 성공률
  - 다음 예정된 자동 실행 시간 (가장 가까운 것)

- [ ] **빠른 접근 버튼**
  - "시간 기반 설정" → TimeBasedAutoRunScreen
  - "위치 기반 설정" → LocationBasedAutoRunScreen

- [ ] **최근 자동 실행 이력** (최근 5개)
  - AutoRunLog에서 가져오기
  - 시간, 위치 정보 표시
  - 결과 (시작됨/건너뜀/실패) 아이콘

#### 6.1.2 AutoRunHistoryScreen
- [ ] **이력 리스트**
  - 날짜별 그룹핑
  - 트리거 타입 (시간/위치) 아이콘
  - 트리거 정보 (시간대 라벨, 위치 라벨)
  - 결과 및 실패 사유

- [ ] **필터링 및 검색**
  - 트리거 타입 필터 (전체/시간/위치)
  - 결과 필터 (전체/시작됨/건너뜀/실패)
  - 날짜 범위 선택

- [ ] **통계 요약**
  - 총 자동 실행 횟수
  - 성공률
  - 주간 트렌드 그래프

**작업 기록**: `working_history/2025-10-28_2nd_advanced_6.1.md`

### 6.2 Analytics 이벤트 로깅 (Day 26)

#### 6.2.1 AnalyticsHelper 확장
- [ ] **21개 신규 이벤트 구현** → [PRD §5.2](./02_advanced_autosetting_prd.md#52-이벤트-로깅), **[Analytics 스키마](./02_advanced_analytics_schema.md)** (작성 예정)
  ```kotlin
  object AnalyticsHelper {
      // 설정 이벤트
      fun logTimeBasedAutoRunCreated(hour: Int, minute: Int, durationMinutes: Int)
      fun logTimeBasedAutoRunToggled(isEnabled: Boolean)
      fun logLocationBasedAutoRunCreated(label: String, radiusMeters: Int)
      
      // 커스텀 타이머 이벤트
      fun logCustomTimerAdjusted(durationMinutes: Int, method: String) // DRAG/TAP
      fun logCustomPresetCreated(name: String, durationMinutes: Int)
      fun logCustomPresetUsed(presetId: String, durationMinutes: Int)
      
      // 자동 실행 이벤트
      fun logAutoRunTriggered(triggerType: String, sourceId: String)
      fun logAutoRunNotificationShown(triggerType: String, preNotify: Boolean)
      fun logAutoRunNotificationAction(action: String) // START/SNOOZE/SKIP
      fun logAutoRunStarted(triggerType: String, durationMinutes: Int)
      fun logAutoRunSkipped(reason: String)
      fun logAutoRunFailed(failureReason: String)
  }
  ```
  - **참조**: [기존 AnalyticsHelper](../app/src/main/java/com/allday/detoxy/core/utils/AnalyticsHelper.kt) - 1차 고도화 이벤트 구현 패턴
  - **참조**: [1차 고도화 Analytics 스키마](./01_advanced_analytics_schema.md)

#### 6.2.2 이벤트 로깅 위치
- [ ] TimeBasedAutoRunViewModel → 설정 이벤트
- [ ] LocationBasedAutoRunViewModel → 설정 이벤트
- [ ] CustomTimerScreen → 커스텀 타이머 이벤트
- [ ] AutoRunAlarmReceiver → 자동 실행 이벤트
- [ ] GeofenceTransitionsReceiver → 자동 실행 이벤트
- [ ] NotificationActionReceiver → 액션 이벤트

**작업 기록**: `working_history/2025-10-28_2nd_advanced_6.2.md`

### 6.3 단위 테스트 작성 (Day 27)

#### 6.3.1 Room 마이그레이션 테스트
- [ ] **MigrationTest_3_4.kt**
  ```kotlin
  @Test
  fun migrate3To4_CreatesNewTables() {
      // v3 DB 생성
      val dbV3 = helper.createDatabase(TEST_DB, 3)
      // 마이그레이션 실행
      val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
      // 테이블 존재 확인
      assertTableExists(dbV4, "time_based_auto_run")
      assertTableExists(dbV4, "location_based_auto_run")
      assertTableExists(dbV4, "custom_timer_preset")
      assertTableExists(dbV4, "auto_run_log")
  }
  ```

#### 6.3.2 비즈니스 로직 테스트
- [ ] **AutoRunAlarmManagerTest.kt**
  - 알람 스케줄링 테스트
  - 요일 계산 로직 테스트
  - PendingIntent 생성 테스트

- [ ] **AutoRunGeofenceManagerTest.kt**
  - Geofence 생성 테스트
  - 반경 설정 테스트
  - 최대 개수 제한 테스트

- [ ] **TimeCalculationTest.kt**
  - 각도 ↔ 시간 변환 테스트
  - 5분 단위 스냅 테스트

#### 6.3.3 통합 테스트
- [ ] **AutoRunE2ETest.kt**
  - 시간 기반 자동 실행 전체 플로우
  - 위치 기반 자동 실행 전체 플로우
  - 커스텀 프리셋 생성 및 사용 플로우

**작업 기록**: `working_history/2025-10-29_2nd_advanced_6.3.md`

### 6.4 회귀 테스트 (Day 28)

#### 6.4.1 기존 기능 확인
- [ ] 타이머 기능 (수동 시작) → [MVP Week 1-3](./00_mvp_allday_detoxy_todolist.md#week-1-핵심-차단-기능)
- [ ] 앱 차단 기능 → [AccessibilityService 작업](../working_history/2025-10-05_1.2.md)
- [ ] 오버레이 잠금 화면 → [LockOverlayService 작업](../working_history/2025-10-06_2.1.md)
- [ ] DND 제어 → [DND 작업](../working_history/2025-10-06_2.2.md)
- [ ] 리포트 화면 (1차 고도화 기능 포함) → [1차 고도화 Week 2B](./01_advanced_setting_report_todolist.md#week-2b-고급-통계-및-ui)
- [ ] 디톡시 제어 설정 → [1차 고도화 Week 1](./01_advanced_setting_report_todolist.md#2-집중모드-설정-화면-구축-week-1)

#### 6.4.2 빌드 검증
- [ ] `./gradlew compileDebugKotlin` (SUCCESS)
- [ ] `./gradlew assembleDebug` (APK Size < 15MB) → 목표: 1차 대비 4MB 증가 이내
  - **참조**: [1차 고도화 APK 크기](../working_history/2025-10-20_1st_advanced_4.0.md) - 11MB
- [ ] `./gradlew test` (모든 테스트 통과)
- [ ] `./gradlew lint` (0 errors)
  - **참조**: [Repository 규칙](../README.md#빌드-및-실행) - 빌드 명령어 가이드

**작업 기록**: `working_history/2025-10-29_2nd_advanced_6.4.md`

---

## 7. 최적화 및 배포 준비 (Week 6, Day 29-33)

### 7.1 배터리 최적화 (Day 29)

#### 7.1.1 위치 업데이트 최적화
- [ ] Geofencing 우선도 설정
  - `PRIORITY_BALANCED_POWER_ACCURACY` 사용
  - 불필요한 고정밀 위치 요청 제거

- [ ] 위치 기반 자동 실행 비활성화 시 Geofence 즉시 해제

#### 7.1.2 알람 최적화
- [ ] AlarmManager 정확도 검증
  - `setExactAndAllowWhileIdle()` 사용 확인
  - Doze 모드에서도 정상 작동 확인

- [ ] 불필요한 백그라운드 작업 제거

#### 7.1.3 배터리 테스트
- [ ] 24시간 배터리 소모 측정
  - 시간 기반 자동 실행 활성화 (5개)
  - 위치 기반 자동 실행 활성화 (2개)
  - 목표: 일일 5% 이하

**작업 기록**: `working_history/2025-10-30_2nd_advanced_7.1.md`

### 7.2 성능 최적화 (Day 30)

#### 7.2.1 도넛 그래프 성능
- [ ] 60fps 유지 확인
  - `remember`로 불필요한 재구성 방지
  - `derivedStateOf`로 계산 최적화
  - Canvas 그리기 최적화

- [ ] 터치 응답성 측정
  - 드래그 응답 시간 < 16ms
  - 햅틱 피드백 지연 없음

#### 7.2.2 DB 쿼리 최적화
- [ ] 인덱스 확인
  - `time_based_auto_run(hour, minute)`
  - `location_based_auto_run(isEnabled)`
  - `auto_run_log(triggerTime)`

- [ ] 불필요한 쿼리 제거
  - Flow 중복 수집 제거
  - 리스트 크기 제한 (최근 100개)

#### 7.2.3 메모리 누수 점검
- [ ] LeakCanary로 메모리 누수 검사
- [ ] ViewModel, Repository 생명주기 확인
- [ ] Broadcast Receiver 명시적 해제

**작업 기록**: `working_history/2025-10-30_2nd_advanced_7.2.md`

### 7.3 QA 시나리오 실행 (Day 31-32)

#### 7.3.1 시간 기반 자동 실행 시나리오 (10개)
1. 시간대 추가 및 삭제
2. 시간대 편집 (시간, 요일, 차단 프리셋 변경)
3. 시간대 활성화/비활성화
4. 알람 트리거 정확도 (±2분)
5. 사전 알림 표시
6. 알림 액션 (시작/스누즈/건너뛰기)
7. 자동 시작 (5분 후)
8. 주말 제외 옵션
9. 앱 재시작 후 알람 유지
10. 배터리 최적화 제외 설정 안내

#### 7.3.2 위치 기반 자동 실행 시나리오 (8개)
1. 위치 추가 (검색, 반경 설정)
2. 위치 편집 및 삭제
3. 위치 권한 요청 플로우
4. 백그라운드 위치 권한 설정
5. Geofence 진입 트리거
6. 위치 자동 실행 알림
7. 위치 이탈 시 안내
8. 앱 재시작 후 Geofence 유지

#### 7.3.3 커스텀 타이머 시나리오 (5개)
1. 도넛 그래프 드래그로 시간 조정
2. 도넛 그래프 탭으로 시간 설정
3. 커스텀 프리셋 저장
4. 커스텀 프리셋 선택 및 시작
5. 프리셋 순서 변경, 편집, 삭제

#### 7.3.4 회귀 테스트
- [ ] 기존 MVP 기능 (타이머, 차단, 리포트)
- [ ] 1차 고도화 기능 (디톡시 제어, 리포트 고도화)

**작업 기록**: `working_history/2025-10-31_2nd_advanced_7.3.md`

### 7.4 문서화 및 릴리스 노트 (Day 33)

#### 7.4.1 문서 업데이트
- [ ] `README.md` 업데이트 → [README.md](../README.md)
  - 2차 고도화 기능 추가
  - 위치 권한 안내 추가
  - 스크린샷 업데이트
  - **참조**: [1차 고도화 README 업데이트](../working_history/2025-10-20_1st_advanced_4.0.md) - 문서 업데이트 패턴

- [ ] `02_advanced_wireframe_spec.md` 최종 검토 → [Wireframe 스펙](./02_advanced_wireframe_spec.md)
- [ ] `02_advanced_room_migration_strategy.md` 최종 검토 → [마이그레이션 전략](./02_advanced_room_migration_strategy.md)
- [ ] `02_advanced_analytics_schema.md` 최종 검토 → [Analytics 스키마](./02_advanced_analytics_schema.md)
- [ ] `02_advanced_qa_devices.md` 최종 검토 → [QA 시나리오](./02_advanced_qa_devices.md)

#### 7.4.2 릴리스 노트 작성
- [ ] `RELEASE_NOTES_v0.6.md` 생성 → [docs/RELEASE_NOTES_v0.6.md](./RELEASE_NOTES_v0.6.md)
  - 새로운 기능 (자동 실행, 커스텀 타이머)
  - 개선 사항
  - 버그 수정
  - 알려진 이슈
  - 다음 계획 (3차 고도화 힌트)
  - **참조**: [v0.5 릴리스 노트](./RELEASE_NOTES_v0.5.md) - 릴리스 노트 포맷

#### 7.4.3 프라이버시 정책 업데이트
- [ ] 위치 정보 수집 관련 내용 추가 → [PRD §7](./02_advanced_autosetting_prd.md#7-위험-요소-및-대응)
  - 수집 목적: 위치 기반 자동 실행
  - 수집 방법: Geofencing API
  - 보관 기간: 영구 (사용자 삭제 시 즉시 삭제)
  - 제3자 제공: 없음
  - **참조**: [GDPR 준수 가이드](https://developer.android.com/privacy-and-security/privacy-policy)
  - **참조**: [개인정보보호법](https://www.pipc.go.kr/) - 위치정보 수집 관련 법령

**작업 기록**: `working_history/2025-10-31_2nd_advanced_7.4.md`

---

## 8. 배포 준비 (Day 34-35)

### 8.1 내부 베타 배포 (Day 34)

#### 8.1.1 APK 빌드
- [ ] `./gradlew assembleRelease` (프로덕션 빌드)
- [ ] APK 크기 확인 (< 15MB)
- [ ] APK 서명 확인

#### 8.1.2 내부 베타 테스트
- [ ] Google Play Console 내부 테스트 트랙 업로드
- [ ] 5명 이상 내부 테스터 초대
- [ ] 테스트 가이드 제공 (QA 시나리오 기반)

#### 8.1.3 피드백 수집
- [ ] Crashlytics 크래시 모니터링
- [ ] Analytics 이벤트 확인
- [ ] 테스터 피드백 취합

**작업 기록**: `working_history/2025-11-01_2nd_advanced_8.1.md`

### 8.2 최종 버그 수정 및 배포 (Day 35)

#### 8.2.1 버그 수정
- [ ] 내부 베타 피드백 반영
- [ ] 크래시 수정
- [ ] 성능 이슈 해결

#### 8.2.2 Play Console 배포 준비
- [ ] 릴리스 노트 작성 (한국어, 영어)
- [ ] 스크린샷 업데이트 (8장)
- [ ] 프라이버시 정책 업데이트
- [ ] 앱 설명 업데이트

#### 8.2.3 최종 체크리스트
- [x] PRD 요구사항 100% 구현
- [x] DoD 모든 항목 완료
- [x] Crashlytics 크래시율 < 0.5%
- [x] 단위 테스트 100% 통과
- [x] 회귀 테스트 통과
- [x] 문서 최신화
- [x] 릴리스 노트 작성

**작업 기록**: `working_history/2025-11-01_2nd_advanced_8.2.md`

---

## 9. 산출물 체크리스트

- [ ] `docs/02_advanced_autosetting_prd.md` 최신화 → ✅ 완료
- [ ] `docs/02_advanced_wireframe_spec.md` UI 상세 스펙 (자동 실행 설정, 커스텀 타이머 화면)
- [ ] `docs/02_advanced_room_migration_strategy.md` v3→v4 마이그레이션 전략 (4개 엔티티, SQL, 테스트)
- [ ] `docs/02_advanced_analytics_schema.md` 이벤트 21개 정의 및 파라미터
- [ ] `docs/02_advanced_qa_devices.md` QA 시나리오 23개 (시간/위치/커스텀 타이머)
- [ ] `docs/RELEASE_NOTES_v0.6.md` 릴리스 노트 초안
- [ ] `docs/DELIVERABLES_v0.6.md` 산출물 종합 문서
- [ ] `working_history/2025-10-21_*_2nd_advanced_*.md` 작업 기록 (16개 예상)
- [ ] `README.md` 업데이트 (2차 고도화 기능 추가)

---

## 10. 성공 지표 검증

### 10.1 사용률 목표
- [ ] 자동 실행 설정 사용자 비율 ≥ 40% (DAU 대비)
- [ ] 시간 기반 자동 실행 활성화 ≥ 30%
- [ ] 위치 기반 자동 실행 활성화 ≥ 15% (위치 권한 승인 사용자 대비 50%)
- [ ] 커스텀 타이머 생성율 ≥ 50% (타이머 화면 진입자 대비)

### 10.2 효과성 목표
- [ ] 자동 실행 세션 비율 ≥ 30% (전체 세션 대비)
- [ ] 자동 실행 후 세션 완주율 ≥ 60%
- [ ] 주간 평균 세션 수 증가 ≥ 20% (자동 실행 활성화 전후 비교)

### 10.3 기술적 지표
- [ ] AlarmManager 정확도 ≥ 95% (±2분 내 트리거)
- [ ] Geofence 트리거 정확도 ≥ 90% (5분 내 트리거)
- [ ] 도넛 그래프 응답성: 60fps 유지율 ≥ 95%
- [ ] 배터리 소모 ≤ 5% (위치 기반 자동 실행 활성화 시)
- [ ] Crashlytics 크래시율 ≤ 0.5%

---

## 11. 참조 문서

- [2차 고도화 PRD](./02_advanced_autosetting_prd.md)
- [1차 고도화 작업 계획](./01_advanced_setting_report_todolist.md)
- [MVP 개발 계획](./00_mvp_allday_detoxy_todolist.md)
- [기술 설계 문서](./00_android_allday_detoxy_plan.md)

---

> **2차 고도화 철학**: "습관은 자동화로부터". 사용자의 일상 루틴에 자연스럽게 녹아든 디톡시 습관을 만들고, 직관적인 커스텀 UI로 개인화 경험을 극대화합니다. 6주간의 집중 개발로 자동 실행과 커스텀 타이머 기능을 완성하고, 사용자의 주간 세션 수를 20% 증가시키는 것이 목표입니다.

