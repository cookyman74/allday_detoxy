# Allday Detoxy 3차 고도화 작업 계획
## 위치 기반 컨텍스트 + 시간 기반 스케줄 복합 시나리오

> **⚠️ 2.5차 고도화 리뷰 반영 완료 (2025-10-31)**
> 
> **주요 변경 사항**:
> - ✅ 마이그레이션 버전 변경: v4→v5 → **v5→v6** (Migration_5_6)
> - ✅ 2.5차 완료 항목 명시: ScheduleGroup, scheduleGroupId, linkedScheduleGroupId 등
> - ✅ Soft Reference 전략 유지 (FK 제거, 애플리케이션 레벨 참조 관리)
> - ✅ Clean Architecture 패턴 명시 (domain 인터페이스 + data 구현체)
> - ✅ 선택적 필드 표시: iconType, colorHex, groupPriority (MVP 제외 가능)
> - ✅ 작업 기록 파일명 통일: `2025-11-XX_3rd_advanced_*.md`

- **기준 문서**: [2차 고도화 PRD §4.4.6](./02_advanced_autosetting_prd.md#446-위치-기반-컨텍스트--시간-기반-스케줄-25차-고도화-)
- **선행 작업**: **2.5차 고도화 (v0.7, Room v5) 완료 필수** ✅
- **버전**: v0.7 (Room v5) → v0.8 (Room v6)
- **예상 기간**: 2주 (Day 1-14)

---

## 0. 개요

### 0.1 배경 및 목표

**배경**:
- 2차 고도화(v0.6)에서 시간 기반, 위치 기반 자동 실행을 각각 독립적으로 구현
- 사용자 리뷰를 통해 핵심 문제점 발견:
  1. 위치 기반 자동 실행은 1회성 트리거로 반복 사용 시나리오 부족
  2. 특정 위치(회사, 학교)에서 반복적인 시간 스케줄 필요
  3. 매번 수동으로 설정해야 하는 불편함

**목표**:
- 위치 진입 → 해당 위치의 시간표 자동 활성화
- 위치 이탈 → 시간표 자동 비활성화
- 반복 진입 시 동일 시간표 자동 재적용
- 위치별로 다른 시간 스케줄 자동 전환

**성공 지표**:
- 위치 기반 자동 실행 재사용성 100% 향상
- 사용자 설정 시간 50% 단축
- 직장인/학생 사용자 만족도 80% 이상
- 위치-시간 복합 시나리오 활성화율 40% 이상

### 0.2 핵심 사용자 시나리오

**시나리오 1: 직장인 A**
```
1. 초기 설정 (1회)
   - "회사" 위치 등록 (서울시 강남구 테헤란로)
   - "업무 시간표" 생성
     - 오전 10시: 45분 (완전 차단)
     - 오후 2시: 30분 (표준)
     - 오후 4시: 45분 (완전 차단)
   - 위치와 시간표 연결

2. 일상 사용 (자동)
   - 월요일 오전 8:30 회사 도착
     → "업무 시간표 활성화" 알림
     → 오전 10시, 오후 2시, 오후 4시 자동 실행 예약
   - 월요일 오후 6:00 회사 퇴근
     → "업무 시간표 비활성화" 알림
   - 화요일 오전 8:45 회사 도착
     → 동일 시간표 자동 재활성화 ✅
```

**시나리오 2: 학생 B**
```
1. 초기 설정
   - "도서관" 위치 등록
   - "공부 시간표" 생성
     - 오전 9시: 90분
     - 오후 2시: 90분
   - 위치와 시간표 연결

2. 일상 사용
   - 토요일 오전 8:50 도서관 도착
     → "공부 시간표 활성화"
     → 오전 9시, 오후 2시 자동 실행 예약
   - 도서관 퇴실 → 시간표 비활성화
```

### 0.3 기술 스택

- **Room Database**: v6 (v5에서 마이그레이션)
- **Geofencing API**: EXIT 트리거 추가
- **Hilt**: 의존성 주입
- **Jetpack Compose**: 기존 ScheduleGroupScreen 확장
- **Kotlin Coroutines & Flow**: 비동기 처리

### 0.4 2.5차 고도화에서 완료된 선행 작업

**이미 구현 완료** (2025-10-30):
- ✅ ScheduleGroup 엔티티 생성 (Room v5)
- ✅ Migration_4_5 구현 (v4→v5)
- ✅ TimeBasedAutoRun.scheduleGroupId, isIndependent 필드 추가
- ✅ LocationBasedAutoRun.linkedScheduleGroupId 필드 추가
- ✅ ScheduleGroupDao, ScheduleGroupRepository (domain + data 레이어)
- ✅ ScheduleGroupScreen, ScheduleGroupViewModel 기본 UI
- ✅ ScheduleGroup CRUD 기능
- ✅ GeofenceTransitionsReceiver EXIT 이벤트 처리 (TODO 주석으로 설계 완료)

**3차 고도화에서 추가할 내용**:
- 🆕 위치-시간표 연동 세부 옵션 (activateScheduleOnEnter, deactivateScheduleOnExit)
- 🆕 ScheduleGroupManager 구현 (알람 등록/취소 로직)
- 🆕 GeofenceTransitionsReceiver TODO 구현 (실제 동작)
- 🆕 AlarmManager 연동 (시간표 활성화 시 알람 등록)
- 🆕 UI 확장 (시간표 연결 설정 단계)

---

## 1. 데이터 모델 확장 (Day 1-2)

### 1.1 Room 마이그레이션 v5→v6

#### 1.1.1 선행 완료 사항 (2.5차 고도화)

**이미 존재하는 엔티티 및 필드** (Room v5):
- ✅ ScheduleGroup 엔티티 (id, name, description, isActive, createdAt)
- ✅ TimeBasedAutoRun.scheduleGroupId, isIndependent
- ✅ LocationBasedAutoRun.linkedScheduleGroupId

**Note**: Soft Reference 전략 유지 - FK 없이 애플리케이션 레벨에서 참조 무결성 관리

#### 1.1.2 새로 추가할 필드 (v5→v6)

- [x] **ScheduleGroup.kt** 선택적 UI 필드 추가 (v6)
  ```kotlin
  // 기존 필드는 모두 유지, 아래 필드만 추가
  val iconType: String = "WORK",       // 🆕 WORK, STUDY, GYM, HOME, etc.
  val colorHex: String = "#4CAF50",    // 🆕 UI 표시용 색상
  val lastActivatedAt: Long? = null    // 🆕 마지막 활성화 시각 (통계용)
  ```
  - **Note**: 이 필드들은 선택적이며, 3차 고도화 MVP에서 제외 가능
  - ✅ **완료**: 2025-10-30

- [x] **LocationBasedAutoRun.kt** 활성화 옵션 필드 추가 (v6)
  ```kotlin
  // linkedScheduleGroupId는 이미 v5에 존재, 아래 필드만 추가
  val activateScheduleOnEnter: Boolean = false,   // 🆕 진입 시 시간표 활성화
  val deactivateScheduleOnExit: Boolean = false,  // 🆕 이탈 시 시간표 비활성화
  val exitActionType: String = "DEACTIVATE"       // 🆕 DEACTIVATE, ASK_USER, DO_NOTHING
  ```
  - **참조**: [기존 LocationBasedAutoRun.kt](../app/src/main/java/com/allday/detoxy/data/local/entity/LocationBasedAutoRun.kt)
  - ✅ **완료**: 2025-10-30

- [x] **TimeBasedAutoRun.kt** 우선순위 필드 추가 (v6, 선택적)
  ```kotlin
  // scheduleGroupId, isIndependent는 이미 v5에 존재
  val groupPriority: Int = 0    // 🆕 그룹 내 우선순위 (정렬용, 선택적)
  ```
  - **Note**: 우선순위는 3차 고도화 MVP에서 제외 가능
  - ✅ **완료**: 2025-10-30

#### 1.1.3 마이그레이션 스크립트 (v5→v6)
- [x] **Migration_5_6.kt** 작성 (새로 추가할 필드만) ✅ **완료**: 2025-10-30
  ```kotlin
  val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
          // 1. ScheduleGroup 테이블 확장 (선택적 UI 필드)
          db.execSQL("""
              ALTER TABLE schedule_group 
              ADD COLUMN iconType TEXT NOT NULL DEFAULT 'WORK'
          """)
          
          db.execSQL("""
              ALTER TABLE schedule_group 
              ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#4CAF50'
          """)
          
          db.execSQL("""
              ALTER TABLE schedule_group 
              ADD COLUMN lastActivatedAt INTEGER
          """)
          
          // 2. LocationBasedAutoRun 확장 (활성화 옵션)
          db.execSQL("""
              ALTER TABLE location_based_auto_run 
              ADD COLUMN activateScheduleOnEnter INTEGER NOT NULL DEFAULT 0
          """)
          
          db.execSQL("""
              ALTER TABLE location_based_auto_run 
              ADD COLUMN deactivateScheduleOnExit INTEGER NOT NULL DEFAULT 0
          """)
          
          db.execSQL("""
              ALTER TABLE location_based_auto_run 
              ADD COLUMN exitActionType TEXT NOT NULL DEFAULT 'DEACTIVATE'
          """)
          
          // 3. TimeBasedAutoRun 확장 (우선순위, 선택적)
          db.execSQL("""
              ALTER TABLE time_based_auto_run 
              ADD COLUMN groupPriority INTEGER NOT NULL DEFAULT 0
          """)
      }
  }
  ```
  - **참조**: [Migration_4_5.kt](../app/src/main/java/com/allday/detoxy/data/local/migrations/Migration_4_5.kt) - 2.5차에서 완료
  - **Note**: v5에서 이미 추가된 scheduleGroupId, linkedScheduleGroupId, isIndependent는 제외

- [x] **DetoxyDatabase.kt** 버전 업데이트 ✅ **완료**: 2025-10-30
  ```kotlin
  @Database(
      entities = [
          // ... 기존 엔티티들 (ScheduleGroup 포함, 이미 v5에 존재)
      ],
      version = 6,  // 5 → 6
      exportSchema = true
  )
  ```

- [x] **DatabaseModule.kt**에 MIGRATION_5_6 추가 ✅ **완료**: 2025-10-30
  ```kotlin
  addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
  ```

#### 1.1.4 DAO 인터페이스 확장

**선행 완료 사항** (2.5차 고도화):
- ✅ ScheduleGroupDao 기본 CRUD (insert, update, delete, getAll, getById, getActive)
- ✅ TimeBasedAutoRunDao.getByScheduleGroup, unlinkFromGroup, setGroupActive
- ✅ LocationBasedAutoRunDao.getByLinkedGroup, unlinkFromGroup

- [x] **ScheduleGroupDao.kt** 메서드 추가 ✅ **완료**: 2025-10-30
  ```kotlin
  // lastActivatedAt 업데이트용 (v6 필드)
  @Query("UPDATE schedule_group SET isActive = :isActive, lastActivatedAt = :timestamp WHERE id = :groupId")
  suspend fun setActiveWithTimestamp(groupId: String, isActive: Boolean, timestamp: Long? = null)
  ```
  - **참조**: [기존 ScheduleGroupDao.kt](../app/src/main/java/com/allday/detoxy/data/local/dao/ScheduleGroupDao.kt)
  - **Note**: 기존 setActive 메서드와 병행 사용

- [x] **LocationBasedAutoRunDao.kt** 메서드 추가 ✅ **완료**: 2025-10-30
  ```kotlin
  // 활성화 옵션이 설정된 위치 조회 (v6 필드)
  @Query("SELECT * FROM location_based_auto_run WHERE activateScheduleOnEnter = 1 AND isEnabled = 1")
  fun getAutoActivateLocations(): Flow<List<LocationBasedAutoRun>>
  
  @Query("SELECT * FROM location_based_auto_run WHERE deactivateScheduleOnExit = 1 AND isEnabled = 1")
  fun getAutoDeactivateLocations(): Flow<List<LocationBasedAutoRun>>
  ```
  - **참조**: [기존 LocationBasedAutoRunDao.kt](../app/src/main/java/com/allday/detoxy/data/local/dao/LocationBasedAutoRunDao.kt)

#### 1.1.5 Repository 확장

**선행 완료 사항** (2.5차 고도화):
- ✅ ScheduleGroupRepository 도메인 인터페이스 (`domain/repository/ScheduleGroupRepository.kt`)
- ✅ ScheduleGroupRepositoryImpl 데이터 구현체 (`data/repository/ScheduleGroupRepositoryImpl.kt`)
- ✅ 기본 CRUD 메서드 (getAll, getActive, getById, insert, update, delete)
- ✅ toggleActive, getLinkedTimeBasedAutoRuns, getLinkedLocations
- ✅ unlinkAllAutoRuns, getLinkedTimeBasedAutoRunCount, getLinkedLocationCount
- ✅ Hilt 바인딩 완료 (RepositoryModule)

**Note**: 기존 Clean Architecture 패턴 유지 - domain 인터페이스 + data 구현체 분리

- [x] **ScheduleGroupRepository.kt** (domain) 메서드 추가 ✅ **완료**: 2025-10-30
  ```kotlin
  interface ScheduleGroupRepository {
      // ... 기존 메서드들
      
      // 🆕 v6: lastActivatedAt 업데이트용
      suspend fun toggleActiveWithTimestamp(scheduleGroupId: String, isActive: Boolean, timestamp: Long? = null)
  }
  ```

- [x] **ScheduleGroupRepositoryImpl.kt** (data) 구현 추가 ✅ **완료**: 2025-10-30
  ```kotlin
  @Singleton
  class ScheduleGroupRepositoryImpl @Inject constructor(
      private val scheduleGroupDao: ScheduleGroupDao,
      private val timeBasedAutoRunDao: TimeBasedAutoRunDao,
      private val locationBasedAutoRunDao: LocationBasedAutoRunDao
  ) : ScheduleGroupRepository {
      // ... 기존 메서드들
      
      override suspend fun toggleActiveWithTimestamp(scheduleGroupId: String, isActive: Boolean, timestamp: Long?) {
          scheduleGroupDao.setActiveWithTimestamp(scheduleGroupId, isActive, timestamp)
          timeBasedAutoRunDao.setGroupActive(scheduleGroupId, isActive)
      }
  }
  ```
  - **참조**: [기존 ScheduleGroupRepositoryImpl.kt](../app/src/main/java/com/allday/detoxy/data/repository/ScheduleGroupRepositoryImpl.kt)
  - **Note**: 기존 toggleActive와 병행 사용

#### 1.1.6 마이그레이션 테스트 (v5→v6)
- [x] **MigrationTest_5_6.kt** 작성 ✅ **완료**: 2025-10-30
  ```kotlin
  @RunWith(AndroidJUnit4::class)
  class MigrationTest_5_6 {
      @get:Rule
      val helper: MigrationTestHelper = MigrationTestHelper(
          InstrumentationRegistry.getInstrumentation(),
          DetoxyDatabase::class.java
      )
      
      @Test
      fun migrate5To6_AddsNewFields() {
          // v5 DB 생성 (샘플 데이터 삽입)
          val dbV5 = helper.createDatabase(TEST_DB, 5).apply {
              execSQL("INSERT INTO schedule_group (id, name, description, isActive, createdAt) VALUES ('test_group', '테스트', NULL, 0, ${System.currentTimeMillis()})")
              close()
          }
          
          // v5 → v6 마이그레이션 실행
          val dbV6 = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
          
          // 1. ScheduleGroup 새 컬럼 확인
          dbV6.query("PRAGMA table_info(schedule_group)").use { cursor ->
              val columns = mutableListOf<String>()
              while (cursor.moveToNext()) {
                  columns.add(cursor.getString(cursor.getColumnIndex("name")))
              }
              assertTrue(columns.contains("iconType"))
              assertTrue(columns.contains("colorHex"))
              assertTrue(columns.contains("lastActivatedAt"))
          }
          
          // 2. LocationBasedAutoRun 새 컬럼 확인
          dbV6.query("PRAGMA table_info(location_based_auto_run)").use { cursor ->
              val columns = mutableListOf<String>()
              while (cursor.moveToNext()) {
                  columns.add(cursor.getString(cursor.getColumnIndex("name")))
              }
              assertTrue(columns.contains("activateScheduleOnEnter"))
              assertTrue(columns.contains("deactivateScheduleOnExit"))
              assertTrue(columns.contains("exitActionType"))
          }
          
          // 3. 기본값 확인
          dbV6.query("SELECT iconType, colorHex FROM schedule_group WHERE id = 'test_group'").use { cursor ->
              assertTrue(cursor.moveToFirst())
              assertEquals("WORK", cursor.getString(0))
              assertEquals("#4CAF50", cursor.getString(1))
          }
      }
  }
  ```
  - **참조**: [MigrationTest_4_5.kt](../app/src/androidTest/java/com/allday/detoxy/data/local/migrations/MigrationTest_4_5.kt) - 2.5차에서 완료

**작업 기록**: `working_history/2025-10-30_3rd_advanced_1.0.md` ✅ **완료**: 2025-10-30

---

## 2. 핵심 로직 구현 (Day 3-5)

### 2.1 ScheduleGroup 관리 로직

#### 2.1.1 ScheduleGroupManager 클래스
- [x] **ScheduleGroupManager.kt** 생성 (~300줄) ✅ **완료**: 2025-10-31
  ```kotlin
  @Singleton
  class ScheduleGroupManager @Inject constructor(
      private val repository: ScheduleGroupRepository,
      private val alarmManager: AutoRunAlarmManager,
      private val context: Context
  ) {
      private val TAG = "ScheduleGroupManager"
      
      /**
       * 시간표 그룹 활성화
       * - 그룹 내 모든 시간대의 알람 등록
       * - 다른 모든 그룹 비활성화 (단일 활성화 원칙)
       */
      suspend fun activateGroup(groupId: String): Result<Unit> = runCatching {
          Log.i(TAG, "🔄 Activating schedule group: $groupId")
          
          // 1. 다른 모든 그룹 비활성화 (개별 순회 방식)
          val activeGroups = repository.getActive().first()
          activeGroups.forEach { group ->
              if (group.id != groupId && group.isActive) {
                  deactivateGroup(group.id).getOrThrow()
              }
          }
          
          // 2. 해당 그룹 활성화 (lastActivatedAt 업데이트)
          val timestamp = System.currentTimeMillis()
          repository.toggleActiveWithTimestamp(groupId, true, timestamp)
          
          // 3. 그룹 내 활성화된 시간대 알람 등록
          val timeBasedAutoRuns = repository.getLinkedTimeBasedAutoRuns(groupId)
          val enabledAutoRuns = timeBasedAutoRuns.filter { it.isEnabled }
          
          Log.d(TAG, "📅 Scheduling ${enabledAutoRuns.size} time-based auto-runs")
          
          var successCount = 0
          var failCount = 0
          
          enabledAutoRuns.forEach { autoRun ->
              val success = alarmManager.scheduleTimeBasedAutoRun(autoRun)
              if (success) successCount++ else failCount++
          }
          
          Log.i(TAG, "✅ Schedule group activated: $groupId (Success: $successCount, Failed: $failCount)")
      }
      
      /**
       * 시간표 그룹 비활성화
       * - 그룹 내 모든 시간대의 알람 취소
       */
      suspend fun deactivateGroup(groupId: String): Result<Unit> = runCatching {
          Log.i(TAG, "🔄 Deactivating schedule group: $groupId")
          
          // 1. 그룹 비활성화 (lastActivatedAt은 유지)
          repository.toggleActive(groupId, false)
          
          // 2. 그룹 내 모든 시간대 알람 취소
          val timeBasedAutoRuns = repository.getLinkedTimeBasedAutoRuns(groupId)
          
          Log.d(TAG, "📅 Cancelling ${timeBasedAutoRuns.size} time-based auto-runs")
          
          timeBasedAutoRuns.forEach { autoRun ->
              alarmManager.cancelTimeBasedAutoRun(autoRun.id)
          }
          
          Log.i(TAG, "✅ Schedule group deactivated: $groupId (${timeBasedAutoRuns.size} alarms cancelled)")
      }
      
      /**
       * 시간표 그룹에 시간대 추가
       */
      suspend fun addTimeBasedAutoRunToGroup(
          groupId: String,
          autoRun: TimeBasedAutoRun
      ): Result<Unit> = runCatching {
          val group = repository.getById(groupId) 
              ?: throw IllegalArgumentException("Schedule group not found: $groupId")
          
          // scheduleGroupId 설정
          val autoRunWithGroup = autoRun.copy(
              scheduleGroupId = groupId,
              isIndependent = false,
              isEnabled = group.isActive  // 그룹 활성화 상태 따름
          )
          
          // TimeBasedAutoRunRepository를 통해 insert
          // (실제 구현 시 TimeBasedAutoRunRepository 주입 필요)
          
          // 그룹이 활성화 상태면 알람 등록
          if (group.isActive) {
              alarmManager.scheduleTimeBasedAutoRun(autoRunWithGroup)
          }
      }
      
      /**
       * 현재 활성화된 시간표 그룹 조회
       */
      suspend fun getActiveGroup(): ScheduleGroup? {
          return repository.getActive().first().firstOrNull()
      }
  }
  ```
  - **참조**: [AutoRunAlarmManager.kt](../app/src/main/java/com/allday/detoxy/core/manager/AutoRunAlarmManager.kt)

#### 2.1.2 Hilt 의존성 주입
- [x] **Hilt 자동 주입** ✅ **완료**: 2025-10-31
  - `@Singleton` + `@Inject constructor` 사용으로 별도 모듈 불필요
  - Hilt가 자동으로 ScheduleGroupManager를 SingletonComponent에 제공
  - 의존성: ScheduleGroupRepository, AutoRunAlarmManager, Context
  
  **실제 구현**:
  ```kotlin
  @Singleton
  class ScheduleGroupManager @Inject constructor(
      @ApplicationContext private val context: Context,
      private val repository: ScheduleGroupRepository,
      private val alarmManager: AutoRunAlarmManager
  ) { /* ... */ }
  ```
  
  **Note**: Repository와 AlarmManager도 이미 Hilt로 관리되므로 추가 모듈 설정 없이 자동 주입됩니다.

### 2.2 위치 진입/이탈 시 시간표 활성화 로직

#### 2.2.1 GeofenceTransitionsReceiver 확장
- [x] **EXIT 트리거 추가** ✅ **완료**: 2025-10-31
  ```kotlin
  override fun onReceive(context: Context, intent: Intent) {
      val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
      
      if (geofencingEvent.hasError()) {
          handleGeofenceError(context, geofencingEvent.errorCode)
          return
      }
      
      val transition = geofencingEvent.geofenceTransition
      
      when (transition) {
          Geofence.GEOFENCE_TRANSITION_ENTER, 
          Geofence.GEOFENCE_TRANSITION_DWELL -> {
              handleGeofenceEnter(context, intent, geofencingEvent)
          }
          Geofence.GEOFENCE_TRANSITION_EXIT -> {
              // 🆕 EXIT 트리거 처리
              handleGeofenceExit(context, intent, geofencingEvent)
          }
      }
  }
  
  /**
   * 위치 이탈 처리 (🆕)
   */
  private fun handleGeofenceExit(
      context: Context,
      intent: Intent,
      geofencingEvent: GeofencingEvent
  ) {
      val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
      
      triggeringGeofences.forEach { geofence ->
          val locationId = geofence.requestId
          
          Log.i(TAG, "📍 Geofence EXIT: $locationId")
          
          // EntryPoint를 통해 필요한 의존성 가져오기
          val entryPoint = EntryPointAccessors.fromApplication(
              context.applicationContext,
              GeofenceReceiverEntryPoint::class.java
          )
          
          val locationDao = entryPoint.locationBasedAutoRunDao()
          val scheduleManager = entryPoint.scheduleGroupManager()
          
          // 비동기 작업 (goAsync)
          val pendingResult = goAsync()
          
          CoroutineScope(Dispatchers.IO).launch {
              try {
                  // 위치 정보 조회
                  val location = locationDao.getById(locationId)
                  
                  if (location != null && location.deactivateScheduleOnExit) {
                      val scheduleGroupId = location.linkedScheduleGroupId
                      
                      if (scheduleGroupId != null) {
                          // 시간표 비활성화
                          scheduleManager.deactivateGroup(scheduleGroupId)
                          
                          // 사용자 알림
                          showScheduleDeactivatedNotification(
                              context,
                              location.label,
                              scheduleGroupId
                          )
                          
                          Log.i(TAG, "✅ Schedule group deactivated on exit: $scheduleGroupId")
                      }
                  }
              } catch (e: Exception) {
                  Log.e(TAG, "⚠️ Error handling geofence exit", e)
              } finally {
                  pendingResult.finish()
              }
          }
      }
  }
  
  /**
   * 시간표 비활성화 알림 표시
   */
  private fun showScheduleDeactivatedNotification(
      context: Context,
      locationLabel: String,
      scheduleGroupId: String
  ) {
      // AutoRunNotificationManager 활용
      // "OO 시간표가 비활성화되었습니다" 알림
  }
  ```

- [x] **EntryPoint 확장** ✅ **완료**: 2025-10-31
  ```kotlin
  @InstallIn(SingletonComponent::class)
  @EntryPoint
  interface GeofenceReceiverEntryPoint {
      fun locationBasedAutoRunDao(): LocationBasedAutoRunDao
      fun scheduleGroupManager(): ScheduleGroupManager  // 🆕 추가
  }
  ```

#### 2.2.2 GeofenceEnter 처리 확장
- [x] **handleGeofenceEnter 수정** ✅ **완료**: 2025-10-31
  ```kotlin
  private fun handleGeofenceEnter(
      context: Context,
      intent: Intent,
      geofencingEvent: GeofencingEvent
  ) {
      val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
      
      triggeringGeofences.forEach { geofence ->
          val locationId = geofence.requestId
          
          // ... 기존 코드
          
          val entryPoint = EntryPointAccessors.fromApplication(
              context.applicationContext,
              GeofenceReceiverEntryPoint::class.java
          )
          
          val locationDao = entryPoint.locationBasedAutoRunDao()
          val scheduleManager = entryPoint.scheduleGroupManager()  // 🆕
          
          val pendingResult = goAsync()
          
          CoroutineScope(Dispatchers.IO).launch {
              try {
                  val location = locationDao.getById(locationId)
                  
                  if (location != null) {
                      // 🆕 시간표 활성화 체크
                      if (location.activateScheduleOnEnter && 
                          location.linkedScheduleGroupId != null) {
                          
                          // 시간표 활성화
                          val result = scheduleManager.activateGroup(
                              location.linkedScheduleGroupId
                          )
                          
                          if (result.isSuccess) {
                              // 시간표 활성화 알림
                              showScheduleActivatedNotification(
                                  context,
                                  location.label,
                                  location.linkedScheduleGroupId
                              )
                              
                              Log.i(TAG, "✅ Schedule group activated on enter: ${location.linkedScheduleGroupId}")
                          } else {
                              Log.e(TAG, "⚠️ Failed to activate schedule group", result.exceptionOrNull())
                          }
                      } else {
                          // 기존 로직: 위치 기반 1회성 자동 실행
                          if (!location.requiresUserConfirmation) {
                              // AutoRunNotificationManager 호출
                              // ...
                          }
                      }
                  }
              } catch (e: Exception) {
                  Log.e(TAG, "⚠️ Error handling geofence enter", e)
              } finally {
                  pendingResult.finish()
              }
          }
      }
  }
  ```

#### 2.2.3 AutoRunGeofenceManager EXIT 등록
- [x] **addGeofence 메서드 수정** ✅ **완료**: 2025-10-31
  ```kotlin
  suspend fun addGeofence(location: LocationBasedAutoRun): Result<Unit> = withContext(Dispatchers.IO) {
      // ... 기존 코드
      
      // Transition 타입 설정
      val transitionTypes = if (location.deactivateScheduleOnExit) {
          // EXIT 트리거 추가
          Geofence.GEOFENCE_TRANSITION_ENTER or 
          Geofence.GEOFENCE_TRANSITION_DWELL or
          Geofence.GEOFENCE_TRANSITION_EXIT  // 🆕
      } else {
          Geofence.GEOFENCE_TRANSITION_ENTER or 
          Geofence.GEOFENCE_TRANSITION_DWELL
      }
      
      val geofence = Geofence.Builder()
          .setRequestId(location.id)
          .setCircularRegion(location.latitude, location.longitude, location.radiusMeters.toFloat())
          .setExpirationDuration(Geofence.NEVER_EXPIRE)
          .setLoiteringDelay(location.dwellTimeMinutes * 60 * 1000)
          .setTransitionTypes(transitionTypes)  // 🆕 동적 설정
          .build()
      
      // ... 나머지 코드
  }
  ```

### 2.3 시간 기반 트리거 로직 수정

#### 2.3.1 AutoRunAlarmReceiver 수정
- [x] **scheduleGroupId 체크 추가** ✅ **완료**: 2025-10-31
  ```kotlin
  private suspend fun handleAutoRunAlarm(
      context: Context,
      autoRunId: String,
      isPreNotification: Boolean
  ) {
      // ... 기존 코드
      
      val autoRun = timeBasedAutoRunDao.getById(autoRunId)
      
      if (autoRun != null) {
          // 🆕 시간표 그룹 소속 확인
          if (!autoRun.isIndependent && autoRun.scheduleGroupId != null) {
              val scheduleGroupDao = entryPoint.scheduleGroupDao()
              val group = scheduleGroupDao.getById(autoRun.scheduleGroupId)
              
              // 그룹이 비활성화 상태면 실행 건너뛰기
              if (group == null || !group.isActive) {
                  Log.d(TAG, "⏭️ Skipping auto-run: schedule group not active")
                  
                  // AutoRunLog 기록
                  autoRunLogDao.insert(
                      AutoRunLog(
                          triggerType = "TIME",
                          triggerSourceId = autoRunId,
                          triggerTime = System.currentTimeMillis(),
                          result = "SKIPPED",
                          failureReason = "SCHEDULE_GROUP_NOT_ACTIVE"
                      )
                  )
                  
                  rescheduleNextAlarm(context, autoRun)
                  return
              }
          }
          
          // ... 기존 알림/자동 시작 로직
      }
  }
  ```

**작업 기록**: `working_history/2025-10-31_3rd_advanced_2.0.md` ✅ **완료**: 2025-10-31

---

## 3. UI 구현 (Day 6-9)

### 3.1 ScheduleGroupScreen (신규 화면)

#### 3.1.1 ScheduleGroupViewModel
- [x] **ScheduleGroupViewModel.kt** 확장 ✅ **완료**: 2025-10-31
  ```kotlin
  @HiltViewModel
  class ScheduleGroupViewModel @Inject constructor(
      private val repository: ScheduleGroupRepository,
      private val scheduleManager: ScheduleGroupManager
  ) : ViewModel() {
      
      val scheduleGroups: StateFlow<List<ScheduleGroup>> = 
          repository.getAll()
              .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
      
      val activeGroup: StateFlow<ScheduleGroup?> = 
          repository.getActive()
              .map { it.firstOrNull() }
              .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
      
      private val _errorState = MutableStateFlow<String?>(null)
      val errorState: StateFlow<String?> = _errorState.asStateFlow()
      
      fun getTimeBasedAutoRuns(groupId: String): Flow<List<TimeBasedAutoRun>> =
          repository.getTimeBasedAutoRuns(groupId)
      
      fun createGroup(group: ScheduleGroup) {
          viewModelScope.launch {
              try {
                  repository.insert(group)
              } catch (e: Exception) {
                  _errorState.value = "시간표 생성 실패: ${e.message}"
              }
          }
      }
      
      fun updateGroup(group: ScheduleGroup) {
          viewModelScope.launch {
              try {
                  repository.update(group)
              } catch (e: Exception) {
                  _errorState.value = "시간표 수정 실패: ${e.message}"
              }
          }
      }
      
      fun deleteGroup(groupId: String) {
          viewModelScope.launch {
              try {
                  repository.delete(groupId)
              } catch (e: Exception) {
                  _errorState.value = "시간표 삭제 실패: ${e.message}"
              }
          }
      }
      
      fun activateGroup(groupId: String) {
          viewModelScope.launch {
              val result = scheduleManager.activateGroup(groupId)
              if (result.isFailure) {
                  _errorState.value = "시간표 활성화 실패: ${result.exceptionOrNull()?.message}"
              }
          }
      }
      
      fun deactivateGroup(groupId: String) {
          viewModelScope.launch {
              val result = scheduleManager.deactivateGroup(groupId)
              if (result.isFailure) {
                  _errorState.value = "시간표 비활성화 실패: ${result.exceptionOrNull()?.message}"
              }
          }
      }
      
      fun clearError() {
          _errorState.value = null
      }
  }
  ```

#### 3.1.2 ScheduleGroupScreen 레이아웃
- [x] **ScheduleGroupScreen.kt** 확장 ✅ **완료**: 2025-10-31
  ```kotlin
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun ScheduleGroupScreen(
      onBack: () -> Unit = {},
      onEditGroup: (String) -> Unit = {},
      viewModel: ScheduleGroupViewModel = hiltViewModel()
  ) {
      val scheduleGroups by viewModel.scheduleGroups.collectAsState()
      val activeGroup by viewModel.activeGroup.collectAsState()
      val errorState by viewModel.errorState.collectAsState()
      
      val snackbarHostState = remember { SnackbarHostState() }
      var showCreateDialog by remember { mutableStateOf(false) }
      var groupToDelete by remember { mutableStateOf<String?>(null) }
      
      // 에러 표시
      LaunchedEffect(errorState) {
          errorState?.let { error ->
              snackbarHostState.showSnackbar(error)
              viewModel.clearError()
          }
      }
      
      Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
              TopAppBar(
                  title = { Text("시간표 관리") },
                  navigationIcon = {
                      IconButton(onClick = onBack) {
                          Icon(Icons.Default.ArrowBack, "뒤로가기")
                      }
                  }
              )
          },
          floatingActionButton = {
              FloatingActionButton(onClick = { showCreateDialog = true }) {
                  Icon(Icons.Default.Add, "시간표 추가")
              }
          }
      ) { paddingValues ->
          Column(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(paddingValues)
                  .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
              // 현재 활성화된 시간표 카드
              if (activeGroup != null) {
                  ActiveScheduleGroupCard(
                      group = activeGroup!!,
                      onDeactivate = { viewModel.deactivateGroup(activeGroup!!.id) }
                  )
              }
              
              // 안내 텍스트
              Text(
                  text = "시간표를 만들고 위치와 연결하세요",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
              )
              
              // 시간표 리스트
              if (scheduleGroups.isEmpty()) {
                  EmptyScheduleGroupState(
                      onCreateClick = { showCreateDialog = true }
                  )
              } else {
                  LazyColumn(
                      verticalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                      items(scheduleGroups) { group ->
                          ScheduleGroupCard(
                              group = group,
                              isActive = group.id == activeGroup?.id,
                              onActivate = { viewModel.activateGroup(group.id) },
                              onEdit = { onEditGroup(group.id) },
                              onDelete = { groupToDelete = group.id },
                              viewModel = viewModel
                          )
                      }
                  }
              }
          }
      }
      
      // 시간표 생성 다이얼로그
      if (showCreateDialog) {
          CreateScheduleGroupDialog(
              onDismiss = { showCreateDialog = false },
              onCreate = { group ->
                  viewModel.createGroup(group)
                  showCreateDialog = false
              }
          )
      }
      
      // 삭제 확인 다이얼로그
      groupToDelete?.let { groupId ->
          DeleteConfirmDialog(
              onConfirm = {
                  viewModel.deleteGroup(groupId)
                  groupToDelete = null
              },
              onDismiss = { groupToDelete = null }
          )
      }
  }
  ```

#### 3.1.3 ScheduleGroupCard 컴포넌트
- [x] **ScheduleGroupCard.kt** 확장 ✅ **완료**: 2025-10-31

**작업 기록**: `working_history/2025-10-31_3rd_advanced_3.1.md` ✅ **완료**: 2025-10-31

  ```kotlin
  @Composable
  fun ScheduleGroupCard(
      group: ScheduleGroup,
      isActive: Boolean,
      onActivate: () -> Unit,
      onEdit: () -> Unit,
      onDelete: () -> Unit,
      viewModel: ScheduleGroupViewModel
  ) {
      val timeBasedAutoRuns by viewModel.getTimeBasedAutoRuns(group.id).collectAsState(initial = emptyList())
      var expanded by remember { mutableStateOf(false) }
      
      Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
              containerColor = if (isActive) {
                  MaterialTheme.colorScheme.primaryContainer
              } else {
                  MaterialTheme.colorScheme.surfaceVariant
              }
          )
      ) {
          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
          ) {
              // 헤더: 아이콘, 이름, 활성화 상태
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      // 아이콘
                      Icon(
                          imageVector = getIconForType(group.iconType),
                          contentDescription = null,
                          tint = Color(android.graphics.Color.parseColor(group.colorHex))
                      )
                      
                      Column {
                          Text(
                              text = group.name,
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Bold
                          )
                          
                          if (group.description != null) {
                              Text(
                                  text = group.description,
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                          }
                      }
                  }
                  
                  // 활성화 배지
                  if (isActive) {
                      Badge {
                          Text("활성화")
                      }
                  }
              }
              
              Spacer(modifier = Modifier.height(12.dp))
              
              // 시간대 요약
              if (timeBasedAutoRuns.isNotEmpty()) {
                  Text(
                      text = "${timeBasedAutoRuns.size}개 시간대",
                      style = MaterialTheme.typography.bodyMedium
                  )
                  
                  if (expanded) {
                      // 시간대 리스트
                      Spacer(modifier = Modifier.height(8.dp))
                      timeBasedAutoRuns.forEach { autoRun ->
                          Text(
                              text = "• ${formatTime(autoRun.hour, autoRun.minute)} - ${autoRun.durationMinutes}분",
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                      }
                  }
              } else {
                  Text(
                      text = "시간대 없음",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.error
                  )
              }
              
              Spacer(modifier = Modifier.height(12.dp))
              
              // 액션 버튼
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
              ) {
                  // 활성화/비활성화 버튼
                  if (isActive) {
                      OutlinedButton(onClick = onActivate) {
                          Text("비활성화")
                      }
                  } else {
                      Button(onClick = onActivate) {
                          Text("활성화")
                      }
                  }
                  
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      IconButton(onClick = { expanded = !expanded }) {
                          Icon(
                              imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                              contentDescription = if (expanded) "접기" else "펼치기"
                          )
                      }
                      
                      IconButton(onClick = onEdit) {
                          Icon(Icons.Default.Edit, "편집")
                      }
                      
                      IconButton(onClick = onDelete) {
                          Icon(Icons.Default.Delete, "삭제")
                      }
                  }
              }
          }
      }
  }
  
  @Composable
  private fun getIconForType(iconType: String): ImageVector {
      return when (iconType) {
          "WORK" -> Icons.Default.Work
          "STUDY" -> Icons.Default.School
          "GYM" -> Icons.Default.FitnessCenter
          "HOME" -> Icons.Default.Home
          else -> Icons.Default.Schedule
      }
  }
  ```

### 3.2 AddLocationAutoRunDialog 확장

#### 3.2.1 단계 3 추가: 시간표 연결 설정
- [x] **AddLocationAutoRunDialog.kt** 수정 ✅ **완료**: 2025-10-31
  ```kotlin
  enum class LocationDialogStep {
      SEARCH,      // 위치 검색
      CONFIRM,     // 위치 확인
      SETTINGS,    // 설정 (기존)
      SCHEDULE     // 🆕 시간표 연결 (새로운 단계)
  }
  
  @Composable
  fun AddLocationAutoRunDialog(
      existingLocation: LocationBasedAutoRun? = null,
      onDismiss: () -> Unit,
      onSave: (LocationBasedAutoRun) -> Unit,
      viewModel: LocationBasedAutoRunViewModel = hiltViewModel(),
      scheduleViewModel: ScheduleGroupViewModel = hiltViewModel()  // 🆕
  ) {
      // ... 기존 상태들
      
      // 🆕 시간표 연결 상태
      var enableScheduleLink by remember { mutableStateOf(existingLocation?.linkedScheduleGroupId != null) }
      var selectedScheduleGroupId by remember { mutableStateOf(existingLocation?.linkedScheduleGroupId) }
      var activateOnEnter by remember { mutableStateOf(existingLocation?.activateScheduleOnEnter ?: false) }
      var deactivateOnExit by remember { mutableStateOf(existingLocation?.deactivateScheduleOnExit ?: false) }
      
      val scheduleGroups by scheduleViewModel.scheduleGroups.collectAsState()
      
      // ... 기존 코드
      
      // 저장 로직 수정
      val handleSave = {
          val location = LocationBasedAutoRun(
              id = existingLocation?.id ?: UUID.randomUUID().toString(),
              // ... 기존 필드들
              
              // 🆕 시간표 연결 필드
              linkedScheduleGroupId = if (enableScheduleLink) selectedScheduleGroupId else null,
              activateScheduleOnEnter = enableScheduleLink && activateOnEnter,
              deactivateScheduleOnExit = enableScheduleLink && deactivateOnExit,
              exitActionType = if (deactivateOnExit) "DEACTIVATE" else "DO_NOTHING"
          )
          onSave(location)
          onDismiss()
      }
      
      AlertDialog(
          // ... 기존 코드
          
          text = {
              AnimatedContent(targetState = currentStep) { step ->
                  when (step) {
                      LocationDialogStep.SEARCH -> {
                          // ... 기존 위치 검색 UI
                      }
                      LocationDialogStep.CONFIRM -> {
                          // ... 기존 위치 확인 UI
                      }
                      LocationDialogStep.SETTINGS -> {
                          // ... 기존 설정 UI
                      }
                      LocationDialogStep.SCHEDULE -> {
                          // 🆕 시간표 연결 설정 UI
                          ScheduleLinkSettingsStep(
                              enableScheduleLink = enableScheduleLink,
                              onEnableScheduleLinkChange = { enableScheduleLink = it },
                              selectedScheduleGroupId = selectedScheduleGroupId,
                              onScheduleGroupIdChange = { selectedScheduleGroupId = it },
                              activateOnEnter = activateOnEnter,
                              onActivateOnEnterChange = { activateOnEnter = it },
                              deactivateOnExit = deactivateOnExit,
                              onDeactivateOnExitChange = { deactivateOnExit = it },
                              scheduleGroups = scheduleGroups
                          )
                      }
                  }
              }
          },
          
          confirmButton = {
              Button(
                  onClick = {
                      when (currentStep) {
                          LocationDialogStep.SEARCH -> {
                              if (selectedLocation != null) {
                                  currentStep = LocationDialogStep.CONFIRM
                              }
                          }
                          LocationDialogStep.CONFIRM -> {
                              currentStep = LocationDialogStep.SETTINGS
                          }
                          LocationDialogStep.SETTINGS -> {
                              currentStep = LocationDialogStep.SCHEDULE  // 🆕
                          }
                          LocationDialogStep.SCHEDULE -> {
                              handleSave()
                          }
                      }
                  },
                  enabled = when (currentStep) {
                      LocationDialogStep.SEARCH -> selectedLocation != null
                      LocationDialogStep.CONFIRM -> true
                      LocationDialogStep.SETTINGS -> label.isNotBlank()
                      LocationDialogStep.SCHEDULE -> !enableScheduleLink || selectedScheduleGroupId != null
                  }
              ) {
                  Text(
                      when (currentStep) {
                          LocationDialogStep.SCHEDULE -> "저장"
                          else -> "다음"
                      }
                  )
              }
          }
      )
  }
  ```

#### 3.2.2 ScheduleLinkSettingsStep 컴포넌트
- [x] **ScheduleLinkSettingsStep.kt** 생성 ✅ **완료**: 2025-10-31

**작업 기록**: `working_history/2025-10-31_3rd_advanced_3.2.md` ✅ **완료**: 2025-10-31
  ```kotlin
  @Composable
  fun ScheduleLinkSettingsStep(
      enableScheduleLink: Boolean,
      onEnableScheduleLinkChange: (Boolean) -> Unit,
      selectedScheduleGroupId: String?,
      onScheduleGroupIdChange: (String?) -> Unit,
      activateOnEnter: Boolean,
      onActivateOnEnterChange: (Boolean) -> Unit,
      deactivateOnExit: Boolean,
      onDeactivateOnExitChange: (Boolean) -> Unit,
      scheduleGroups: List<ScheduleGroup>
  ) {
      Column(
          modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
          // 안내 텍스트
          Text(
              text = "시간표 연결 설정",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
          )
          
          Text(
              text = "이 위치에 도착하면 특정 시간표를 자동으로 활성화할 수 있습니다.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          HorizontalDivider()
          
          // 시간표 연결 활성화 토글
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
          ) {
              Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = "시간표 자동 활성화",
                      style = MaterialTheme.typography.titleSmall
                  )
                  Text(
                      text = "도착 시 선택한 시간표가 활성화됩니다",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
              }
              Switch(
                  checked = enableScheduleLink,
                  onCheckedChange = onEnableScheduleLinkChange
              )
          }
          
          if (enableScheduleLink) {
              // 시간표 선택
              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                  Text(
                      text = "활성화할 시간표",
                      style = MaterialTheme.typography.titleSmall
                  )
                  
                  if (scheduleGroups.isEmpty()) {
                      Text(
                          text = "아직 생성된 시간표가 없습니다. 먼저 시간표를 만들어주세요.",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.error
                      )
                  } else {
                      scheduleGroups.forEach { group ->
                          ScheduleGroupSelectCard(
                              group = group,
                              isSelected = group.id == selectedScheduleGroupId,
                              onClick = { onScheduleGroupIdChange(group.id) }
                          )
                      }
                  }
              }
              
              HorizontalDivider()
              
              // 진입 시 활성화 옵션
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = "도착 시 활성화",
                          style = MaterialTheme.typography.titleSmall
                      )
                      Text(
                          text = "위치 진입 시 시간표 활성화",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                  }
                  Switch(
                      checked = activateOnEnter,
                      onCheckedChange = onActivateOnEnterChange
                  )
              }
              
              // 이탈 시 비활성화 옵션
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
              ) {
                  Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = "떠날 때 비활성화",
                          style = MaterialTheme.typography.titleSmall
                      )
                      Text(
                          text = "위치 이탈 시 시간표 비활성화",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                  }
                  Switch(
                      checked = deactivateOnExit,
                      onCheckedChange = onDeactivateOnExitChange
                  )
              }
              
              // 주의사항
              Card(
                  modifier = Modifier.fillMaxWidth(),
                  colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer
                  )
              ) {
                  Column(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                      Text(
                          text = "ℹ️ 참고사항",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold
                      )
                      Text(
                          text = "• 한 번에 하나의 시간표만 활성화됩니다",
                          style = MaterialTheme.typography.bodySmall
                      )
                      Text(
                          text = "• 위치를 떠나도 진행 중인 타이머는 계속됩니다",
                          style = MaterialTheme.typography.bodySmall
                      )
                      Text(
                          text = "• 시간표의 시간대는 위치에 있을 때만 작동합니다",
                          style = MaterialTheme.typography.bodySmall
                      )
                  }
              }
          }
      }
  }
  
  @Composable
  fun ScheduleGroupSelectCard(
      group: ScheduleGroup,
      isSelected: Boolean,
      onClick: () -> Unit
  ) {
      Card(
          modifier = Modifier.fillMaxWidth(),
          onClick = onClick,
          colors = CardDefaults.cardColors(
              containerColor = if (isSelected) {
                  MaterialTheme.colorScheme.primaryContainer
              } else {
                  MaterialTheme.colorScheme.surface
              }
          ),
          border = if (isSelected) {
              BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
          } else {
              null
          }
      ) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Icon(
                  imageVector = getIconForType(group.iconType),
                  contentDescription = null,
                  tint = Color(android.graphics.Color.parseColor(group.colorHex))
              )
              
              Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = group.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold
                  )
                  if (group.description != null) {
                      Text(
                          text = group.description,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                  }
              }
              
              if (isSelected) {
                  Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "선택됨",
                      tint = MaterialTheme.colorScheme.primary
                  )
              }
          }
      }
  }
  ```

### 3.3 TimeBasedAutoRunScreen 확장

#### 3.3.1 그룹 표시 추가
- [x] **TimeBasedAutoRunCard.kt** 수정
  ```kotlin
  @Composable
  fun TimeBasedAutoRunCard(
      autoRun: TimeBasedAutoRun,
      scheduleGroup: ScheduleGroup? = null,  // 🆕 추가
      onToggle: (Boolean) -> Unit,
      onEdit: () -> Unit,
      onDelete: () -> Unit
  ) {
      Card(/* ... */) {
          Column(/* ... */) {
              // ... 기존 UI
              
              // 🆕 그룹 배지
              if (scheduleGroup != null) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                      Icon(
                          imageVector = Icons.Default.Folder,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(16.dp)
                      )
                      Text(
                          text = scheduleGroup.name,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary
                      )
                      
                      if (!scheduleGroup.isActive) {
                          Badge {
                              Text("비활성", style = MaterialTheme.typography.labelSmall)
                          }
                      }
                  }
              }
              
              if (!autoRun.isIndependent) {
                  Text(
                      text = "이 시간대는 시간표가 활성화되었을 때만 실행됩니다",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontStyle = FontStyle.Italic
                  )
              }
              
              // ... 나머지 UI
          }
      }
  }
  ```

### 3.4 네비게이션 통합

#### 3.4.1 MainActivity 네비게이션 수정
- [x] **AutoRunScreenType enum 확장**
  ```kotlin
  enum class AutoRunScreenType {
      NONE,
      TIME_BASED,
      LOCATION_BASED,
      SCHEDULE_GROUP     // 🆕 추가
  }
  ```

- [x] **네비게이션 분기 추가**
  ```kotlin
  when (showAutoRunScreen) {
      AutoRunScreenType.TIME_BASED -> {
          TimeBasedAutoRunScreen(
              onBack = { showAutoRunScreen = AutoRunScreenType.NONE },
              onNavigateToLocationBased = { showAutoRunScreen = AutoRunScreenType.LOCATION_BASED },
              onNavigateToScheduleGroup = { showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP }  // 🆕
          )
      }
      AutoRunScreenType.LOCATION_BASED -> {
          LocationBasedAutoRunScreen(
              onBack = { showAutoRunScreen = AutoRunScreenType.NONE },
              onNavigateToTimeBased = { showAutoRunScreen = AutoRunScreenType.TIME_BASED },
              onNavigateToScheduleGroup = { showAutoRunScreen = AutoRunScreenType.SCHEDULE_GROUP }  // 🆕
          )
      }
      AutoRunScreenType.SCHEDULE_GROUP -> {  // 🆕
          ScheduleGroupScreen(
              onBack = { showAutoRunScreen = AutoRunScreenType.NONE },
              onEditGroup = { groupId ->
                  // 그룹 편집 화면으로 이동 (추후 구현)
              }
          )
      }
      AutoRunScreenType.NONE -> {
          // 탭별 화면
      }
  }
  ```

#### 3.4.2 화면 간 이동 버튼 추가
- [x] **TimeBasedAutoRunScreen에 시간표 관리 버튼 추가**
  ```kotlin
  // TopAppBar에 actions 추가
  TopAppBar(
      title = { Text("시간 기반 자동 실행") },
      navigationIcon = { /* ... */ },
      actions = {
          IconButton(onClick = onNavigateToScheduleGroup) {
              Icon(Icons.Default.Folder, "시간표 관리")
          }
      }
  )
  ```

- [x] **LocationBasedAutoRunScreen에 시간표 관리 버튼 추가**
  ```kotlin
  TopAppBar(
      title = { Text("위치 기반 자동 실행") },
      navigationIcon = { /* ... */ },
      actions = {
          IconButton(onClick = onNavigateToScheduleGroup) {
              Icon(Icons.Default.Folder, "시간표 관리")
          }
      }
  )
  ```

**작업 기록**: `working_history/2025-10-31_3rd_advanced_3.4.md`
