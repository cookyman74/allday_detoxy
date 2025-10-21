# 2차 고도화 Room 마이그레이션 전략 (v3 → v4)

**버전**: Database v4  
**작성일**: 2025-10-20  
**기준 문서**: [2차 고도화 PRD](./02_advanced_autosetting_prd.md)  
**참조**: [1차 고도화 마이그레이션](./01_advanced_room_migration_strategy.md)

---

## 1. 개요

### 1.1 목표
- **Database 버전**: v3 → v4
- **신규 엔티티**: 4개 (TimeBasedAutoRun, LocationBasedAutoRun, CustomTimerPreset, AutoRunLog)
- **기존 엔티티 확장**: UserSettings (2개 필드 추가)
- **마이그레이션 전략**: 신규 테이블 생성, 인덱스 최적화, 기본 데이터 삽입

### 1.2 마이그레이션 개요

| 작업 | 테이블/필드 | 설명 |
|------|------------|------|
| CREATE | `time_based_auto_run` | 시간 기반 자동 실행 설정 (최대 10개) |
| CREATE | `location_based_auto_run` | 위치 기반 자동 실행 설정 (최대 5개) |
| CREATE | `custom_timer_preset` | 커스텀 타이머 프리셋 (최대 10개) |
| CREATE | `auto_run_log` | 자동 실행 이력 로그 |
| ALTER | `user_settings` | 2개 필드 추가 (자동 실행 관련 설정) |
| CREATE INDEX | 3개 인덱스 | 쿼리 성능 최적화 |

### 1.3 스키마 변경 요약

```sql
-- 신규 테이블 4개
time_based_auto_run (id, hour, minute, durationMinutes, presetType, enabledDays, label, isEnabled, createdAt)
location_based_auto_run (id, label, address, latitude, longitude, radiusMeters, durationMinutes, presetType, triggerType, periodicIntervalMinutes, dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt)
custom_timer_preset (id, name, durationMinutes, presetType, usageCount, displayOrder, createdAt)
auto_run_log (id, triggerType, triggerSourceId, triggerTime, result, failureReason, sessionId)

-- 기존 테이블 확장
user_settings: + autoRunPauseUntil INTEGER, autoRunMasterEnabled INTEGER
```

---

## 2. 신규 엔티티 상세

### 2.1 TimeBasedAutoRun (시간 기반 자동 실행)

**목적**: 특정 시간에 자동으로 타이머를 시작하는 설정 저장

**스키마**:
```kotlin
@Entity(tableName = "time_based_auto_run")
data class TimeBasedAutoRun(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val hour: Int,              // 0-23
    val minute: Int,            // 0-59
    val durationMinutes: Int,   // 타이머 시간 (5-180)
    val presetType: String,     // FULL_BLOCK, STANDARD, RELAXED
    val enabledDays: String,    // JSON: ["MON", "TUE", "WED", ...]
    val label: String? = null,  // 사용자 지정 라벨 (예: "오전 업무")
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

**SQL**:
```sql
CREATE TABLE IF NOT EXISTS time_based_auto_run (
    id TEXT PRIMARY KEY NOT NULL,
    hour INTEGER NOT NULL,
    minute INTEGER NOT NULL,
    durationMinutes INTEGER NOT NULL,
    presetType TEXT NOT NULL,
    enabledDays TEXT NOT NULL,
    label TEXT,
    isEnabled INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_time_based_hour_minute 
ON time_based_auto_run(hour, minute);

CREATE INDEX IF NOT EXISTS idx_time_based_enabled 
ON time_based_auto_run(isEnabled);
```

**비즈니스 로직**:
- 최대 10개까지 등록 가능
- `enabledDays`: JSON 배열 형식 (예: `["MON", "WED", "FRI"]`)
- `hour`, `minute` 조합으로 다음 알람 시간 계산
- `isEnabled == 0`이면 AlarmManager에서 제거

**데이터 예시**:
```json
{
  "id": "uuid-1",
  "hour": 9,
  "minute": 0,
  "durationMinutes": 45,
  "presetType": "STANDARD",
  "enabledDays": "[\"MON\", \"TUE\", \"WED\", \"THU\", \"FRI\"]",
  "label": "오전 업무 집중",
  "isEnabled": true,
  "createdAt": 1697800000000
}
```

### 2.2 LocationBasedAutoRun (위치 기반 자동 실행)

**목적**: 특정 위치 도착 시 자동으로 타이머를 시작하는 설정 저장

**스키마**:
```kotlin
@Entity(tableName = "location_based_auto_run")
data class LocationBasedAutoRun(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,                    // 위치 라벨 (예: "회사", "도서관")
    val address: String? = null,          // 주소 (검색 실패 시 null 허용) 🆕
    val latitude: Double,                 // 위도
    val longitude: Double,                // 경도
    val radiusMeters: Int,                // 반경 (50, 100, 200, 500)
    val durationMinutes: Int,             // 타이머 시간
    val presetType: String,               // 차단 프리셋
    val triggerType: String,              // ENTER, PERIODIC
    val periodicIntervalMinutes: Int? = null,  // 주기적 트리거 간격
    val dwellTimeMinutes: Int = 0,        // 🆕 체류 시간 (0/1/3/5분)
    val requiresUserConfirmation: Boolean = false,  // 🆕 도착 후 확인
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
```

**SQL**:
```sql
CREATE TABLE IF NOT EXISTS location_based_auto_run (
    id TEXT PRIMARY KEY NOT NULL,
    label TEXT NOT NULL,
    address TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    radiusMeters INTEGER NOT NULL,
    durationMinutes INTEGER NOT NULL,
    presetType TEXT NOT NULL,
    triggerType TEXT NOT NULL,
    periodicIntervalMinutes INTEGER,
    dwellTimeMinutes INTEGER NOT NULL DEFAULT 0,
    requiresUserConfirmation INTEGER NOT NULL DEFAULT 0,
    isEnabled INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_location_based_enabled 
ON location_based_auto_run(isEnabled);
```

**비즈니스 로직**:
- 최대 5개까지 등록 가능 (Geofencing API 제한 고려)
- `radiusMeters`: 50, 100, 200, 500m 중 선택
- `triggerType == "ENTER"`: 지오펜스 진입 시 1회 트리거
- `triggerType == "PERIODIC"`: 진입 후 주기적 트리거
- `dwellTimeMinutes > 0`: N분 체류 확인 후 트리거 (GPS 정확도 향상)
- `requiresUserConfirmation == true`: 알림만 표시, 수동 시작
- **`address` nullable 처리** 🆕: 
  - Google Places API 검색 실패 시 주소 없이 저장 가능
  - UI에서는 "주소 없음" 또는 좌표로 표시
  - 좌표만으로 Geofence 등록 가능

**데이터 예시**:
```json
{
  "id": "uuid-2",
  "label": "회사",
  "address": "서울시 강남구 테헤란로 123",
  "latitude": 37.5012345,
  "longitude": 127.0398765,
  "radiusMeters": 100,
  "durationMinutes": 45,
  "presetType": "STANDARD",
  "triggerType": "ENTER",
  "periodicIntervalMinutes": null,
  "dwellTimeMinutes": 1,
  "requiresUserConfirmation": false,
  "isEnabled": true,
  "createdAt": 1697800000000
}
```

### 2.3 CustomTimerPreset (커스텀 타이머 프리셋)

**목적**: 사용자가 도넛 그래프로 생성한 커스텀 시간 프리셋 저장

**스키마**:
```kotlin
@Entity(tableName = "custom_timer_preset")
data class CustomTimerPreset(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,                    // 프리셋 이름 (예: "오후 집중")
    val durationMinutes: Int,            // 시간 (5-180분)
    val presetType: String? = null,      // 연결된 차단 프리셋 (선택)
    val usageCount: Int = 0,             // 사용 횟수 (통계용)
    val displayOrder: Int = 0,           // 화면 표시 순서 (0부터)
    val createdAt: Long = System.currentTimeMillis()
)
```

**SQL**:
```sql
CREATE TABLE IF NOT EXISTS custom_timer_preset (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    durationMinutes INTEGER NOT NULL,
    presetType TEXT,
    usageCount INTEGER NOT NULL DEFAULT 0,
    displayOrder INTEGER NOT NULL DEFAULT 0,
    createdAt INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_custom_preset_order 
ON custom_timer_preset(displayOrder);
```

**비즈니스 로직**:
- 최대 10개까지 생성 가능
- 기본 프리셋 (25분, 45분, 60분)은 `displayOrder 0-2` 고정
- 사용자 커스텀 프리셋은 `displayOrder 3-12`
- `usageCount`: 프리셋 선택 시 자동 증가 (인기 프리셋 표시용)

**데이터 예시**:
```json
{
  "id": "uuid-3",
  "name": "오후 집중",
  "durationMinutes": 37,
  "presetType": "STANDARD",
  "usageCount": 12,
  "displayOrder": 3,
  "createdAt": 1697800000000
}
```

### 2.4 AutoRunLog (자동 실행 이력)

**목적**: 자동 실행 트리거 이력 및 결과 기록 (Analytics, 통계용)

**스키마**:
```kotlin
@Entity(
    tableName = "auto_run_log",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class AutoRunLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val triggerType: String,            // TIME, LOCATION
    val triggerSourceId: String,        // TimeBasedAutoRun.id 또는 LocationBasedAutoRun.id (soft reference)
    val triggerTime: Long,              // 트리거 발생 시간
    val result: String,                 // STARTED, SKIPPED, FAILED
    val failureReason: String? = null,  // 실패 사유 (PERMISSION_DENIED, TIMER_RUNNING, ...)
    @ColumnInfo(index = true)           // FK 인덱스 자동 생성
    val sessionId: String? = null       // 생성된 FocusSession.id (result == STARTED일 때)
)
```

**SQL**:
```sql
CREATE TABLE IF NOT EXISTS auto_run_log (
    id TEXT PRIMARY KEY NOT NULL,
    triggerType TEXT NOT NULL,
    triggerSourceId TEXT NOT NULL,
    triggerTime INTEGER NOT NULL,
    result TEXT NOT NULL,
    failureReason TEXT,
    sessionId TEXT,
    FOREIGN KEY (sessionId) REFERENCES focus_sessions(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_auto_run_log_time 
ON auto_run_log(triggerTime DESC);
```

**비즈니스 로직**:
- 모든 자동 실행 시도 기록 (성공/실패 무관)
- `result`:
  - `STARTED`: 타이머 정상 시작
  - `SKIPPED`: 사용자가 건너뜀
  - `FAILED`: 실패 (권한, 이미 실행 중 등)
- `failureReason` (result == FAILED일 때):
  - `PERMISSION_DENIED`: 권한 없음
  - `TIMER_ALREADY_RUNNING`: 이미 타이머 실행 중
  - `LOCATION_DISABLED`: 위치 서비스 OFF
  - `USER_PAUSED`: 사용자가 일시중지
- 90일 이상 된 로그 자동 삭제 (정기 정리)
- **참조 무결성** 🆕:
  - `sessionId`: FocusSession FK (ON DELETE SET NULL)
  - `triggerSourceId`: Soft reference (FK 없음)
    - 원본 TimeBasedAutoRun/LocationBasedAutoRun 삭제 시에도 로그는 유지
    - 통계 분석을 위해 과거 데이터 보존
    - UI에서 원본 조회 실패 시 "삭제된 설정" 표시

**데이터 예시**:
```json
{
  "id": "uuid-4",
  "triggerType": "TIME",
  "triggerSourceId": "uuid-1",
  "triggerTime": 1697868000000,
  "result": "STARTED",
  "failureReason": null,
  "sessionId": "session-uuid-1"
}
```

---

## 3. 기존 엔티티 확장

### 3.1 UserSettings 필드 추가

**추가 필드**:
```kotlin
data class UserSettings(
    // ... 기존 필드 ...
    val autoRunPauseUntil: Long? = null,        // 자동 실행 일시중지 종료 시간 (timestamp) 🆕
    val autoRunMasterEnabled: Boolean = true    // 자동 실행 마스터 토글 🆕
)
```

**SQL**:
```sql
ALTER TABLE user_settings 
ADD COLUMN autoRunPauseUntil INTEGER;

ALTER TABLE user_settings 
ADD COLUMN autoRunMasterEnabled INTEGER NOT NULL DEFAULT 1;
```

**비즈니스 로직**:
- `autoRunPauseUntil`:
  - `null`: 일시중지 없음
  - `timestamp`: 해당 시간까지 모든 자동 실행 일시중지
  - 시간 경과 후 자동으로 `null`로 리셋 (AlarmManager로 구현)
- `autoRunMasterEnabled`:
  - `false`: 모든 자동 실행 비활성화 (개별 설정은 유지)
  - `true`: 개별 설정에 따라 자동 실행

**PRD 연계** 🆕:
- **§4.4.1 자동 실행 대시보드** (PRD): 마스터 토글 UI
- **§4.4.4 예외 상황 처리** (PRD): 일시중지 기능
- **§5.1.2 Wireframe**: AutoRunDashboardScreen의 "자동 실행 제어 카드"에서 사용

---

## 4. Migration_3_4 구현

### 4.1 마이그레이션 클래스

**파일**: `app/src/main/java/com/allday/detoxy/data/local/migrations/Migration_3_4.kt`

```kotlin
package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. TimeBasedAutoRun 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS time_based_auto_run (
                id TEXT PRIMARY KEY NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                presetType TEXT NOT NULL,
                enabledDays TEXT NOT NULL,
                label TEXT,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_time_based_hour_minute 
            ON time_based_auto_run(hour, minute)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_time_based_enabled 
            ON time_based_auto_run(isEnabled)
        """)
        
        // 2. LocationBasedAutoRun 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS location_based_auto_run (
                id TEXT PRIMARY KEY NOT NULL,
                label TEXT NOT NULL,
                address TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                radiusMeters INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                presetType TEXT NOT NULL,
                triggerType TEXT NOT NULL,
                periodicIntervalMinutes INTEGER,
                dwellTimeMinutes INTEGER NOT NULL DEFAULT 0,
                requiresUserConfirmation INTEGER NOT NULL DEFAULT 0,
                isEnabled INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_location_based_enabled 
            ON location_based_auto_run(isEnabled)
        """)
        
        // 3. CustomTimerPreset 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_timer_preset (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                durationMinutes INTEGER NOT NULL,
                presetType TEXT,
                usageCount INTEGER NOT NULL DEFAULT 0,
                displayOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_custom_preset_order 
            ON custom_timer_preset(displayOrder)
        """)
        
        // 기본 프리셋 3개 삽입 (25분, 45분, 60분)
        val now = System.currentTimeMillis()
        database.execSQL("""
            INSERT INTO custom_timer_preset 
            (id, name, durationMinutes, presetType, usageCount, displayOrder, createdAt)
            VALUES 
            ('preset-25', '25분', 25, NULL, 0, 0, $now),
            ('preset-45', '45분', 45, NULL, 0, 1, $now),
            ('preset-60', '60분', 60, NULL, 0, 2, $now)
        """)
        
        // 4. AutoRunLog 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS auto_run_log (
                id TEXT PRIMARY KEY NOT NULL,
                triggerType TEXT NOT NULL,
                triggerSourceId TEXT NOT NULL,
                triggerTime INTEGER NOT NULL,
                result TEXT NOT NULL,
                failureReason TEXT,
                sessionId TEXT,
                FOREIGN KEY (sessionId) REFERENCES focus_sessions(id) ON DELETE SET NULL
            )
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_auto_run_log_time 
            ON auto_run_log(triggerTime DESC)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_auto_run_log_type_time 
            ON auto_run_log(triggerType, triggerTime DESC)
        """)
        
        // 5. UserSettings 필드 추가
        database.execSQL("""
            ALTER TABLE user_settings 
            ADD COLUMN autoRunPauseUntil INTEGER
        """)
        
        database.execSQL("""
            ALTER TABLE user_settings 
            ADD COLUMN autoRunMasterEnabled INTEGER NOT NULL DEFAULT 1
        """)
    }
}
```

### 4.2 DatabaseModule 업데이트

**파일**: `app/src/main/java/com/allday/detoxy/core/di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDetoxyDatabase(
        @ApplicationContext context: Context
    ): DetoxyDatabase {
        return Room.databaseBuilder(
            context,
            DetoxyDatabase::class.java,
            "detoxy_database"
        )
            .addMigrations(
                MIGRATION_1_2,  // 1차 고도화 Week 2A
                MIGRATION_2_3,  // 1차 고도화 Week 2B
                MIGRATION_3_4   // 2차 고도화 Week 1 🆕
            )
            .build()
    }
    
    // ... DAO 제공 메서드들 ...
}
```

### 4.3 DetoxyDatabase 업데이트

**파일**: `app/src/main/java/com/allday/detoxy/data/local/DetoxyDatabase.kt`

```kotlin
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class,
        FocusInterruption::class,      // v2
        FocusDistraction::class,       // v3
        DetoxyRoutineLog::class,       // v3
        FocusSettings::class,          // v3
        TimeBasedAutoRun::class,       // v4 🆕
        LocationBasedAutoRun::class,   // v4 🆕
        CustomTimerPreset::class,      // v4 🆕
        AutoRunLog::class              // v4 🆕
    ],
    version = 4,  // v3 → v4 🆕
    exportSchema = true
)
abstract class DetoxyDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun focusInterruptionDao(): FocusInterruptionDao
    abstract fun focusDistractionDao(): FocusDistractionDao
    abstract fun detoxyRoutineLogDao(): DetoxyRoutineLogDao
    abstract fun focusSettingsDao(): FocusSettingsDao
    abstract fun timeBasedAutoRunDao(): TimeBasedAutoRunDao         // 🆕
    abstract fun locationBasedAutoRunDao(): LocationBasedAutoRunDao // 🆕
    abstract fun customTimerPresetDao(): CustomTimerPresetDao       // 🆕
    abstract fun autoRunLogDao(): AutoRunLogDao                     // 🆕
}
```

---

## 5. DAO 인터페이스 스펙

### 5.1 TimeBasedAutoRunDao

**파일**: `app/src/main/java/com/allday/detoxy/data/local/dao/TimeBasedAutoRunDao.kt`

```kotlin
@Dao
interface TimeBasedAutoRunDao {
    @Query("SELECT * FROM time_based_auto_run ORDER BY hour, minute")
    fun getAll(): Flow<List<TimeBasedAutoRun>>
    
    @Query("SELECT * FROM time_based_auto_run WHERE id = :id")
    fun getById(id: String): Flow<TimeBasedAutoRun?>
    
    @Query("SELECT * FROM time_based_auto_run WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabled(): Flow<List<TimeBasedAutoRun>>
    
    @Query("SELECT * FROM time_based_auto_run WHERE hour = :hour AND minute = :minute")
    suspend fun getByTime(hour: Int, minute: Int): List<TimeBasedAutoRun>
    
    @Query("SELECT * FROM time_based_auto_run WHERE isEnabled = 1 AND enabledDays LIKE '%' || :dayOfWeek || '%'")
    suspend fun getEnabledForDay(dayOfWeek: String): List<TimeBasedAutoRun>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(autoRun: TimeBasedAutoRun)
    
    @Update
    suspend fun update(autoRun: TimeBasedAutoRun)
    
    @Query("UPDATE time_based_auto_run SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, isEnabled: Boolean)
    
    @Query("DELETE FROM time_based_auto_run WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM time_based_auto_run")
    suspend fun getCount(): Int
}
```

### 5.2 LocationBasedAutoRunDao

**파일**: `app/src/main/java/com/allday/detoxy/data/local/dao/LocationBasedAutoRunDao.kt`

```kotlin
@Dao
interface LocationBasedAutoRunDao {
    @Query("SELECT * FROM location_based_auto_run ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LocationBasedAutoRun>>
    
    @Query("SELECT * FROM location_based_auto_run WHERE id = :id")
    fun getById(id: String): Flow<LocationBasedAutoRun?>
    
    @Query("SELECT * FROM location_based_auto_run WHERE isEnabled = 1")
    fun getEnabled(): Flow<List<LocationBasedAutoRun>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(autoRun: LocationBasedAutoRun)
    
    @Update
    suspend fun update(autoRun: LocationBasedAutoRun)
    
    @Query("UPDATE location_based_auto_run SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, isEnabled: Boolean)
    
    @Query("DELETE FROM location_based_auto_run WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM location_based_auto_run")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM location_based_auto_run WHERE isEnabled = 1")
    suspend fun getEnabledCount(): Int
}
```

### 5.3 CustomTimerPresetDao

**파일**: `app/src/main/java/com/allday/detoxy/data/local/dao/CustomTimerPresetDao.kt`

```kotlin
@Dao
interface CustomTimerPresetDao {
    @Query("SELECT * FROM custom_timer_preset ORDER BY displayOrder")
    fun getAll(): Flow<List<CustomTimerPreset>>
    
    @Query("SELECT * FROM custom_timer_preset WHERE id = :id")
    fun getById(id: String): Flow<CustomTimerPreset?>
    
    @Query("SELECT * FROM custom_timer_preset WHERE displayOrder >= 3 ORDER BY displayOrder")
    fun getCustomPresets(): Flow<List<CustomTimerPreset>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: CustomTimerPreset)
    
    @Update
    suspend fun update(preset: CustomTimerPreset)
    
    @Query("UPDATE custom_timer_preset SET displayOrder = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: String, order: Int)
    
    @Query("UPDATE custom_timer_preset SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)
    
    @Query("DELETE FROM custom_timer_preset WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM custom_timer_preset WHERE displayOrder >= 3")
    suspend fun getCustomPresetsCount(): Int
    
    @Query("SELECT MAX(displayOrder) FROM custom_timer_preset")
    suspend fun getMaxDisplayOrder(): Int?
}
```

### 5.4 AutoRunLogDao

**파일**: `app/src/main/java/com/allday/detoxy/data/local/dao/AutoRunLogDao.kt`

```kotlin
@Dao
interface AutoRunLogDao {
    @Query("SELECT * FROM auto_run_log ORDER BY triggerTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AutoRunLog>>
    
    @Query("SELECT * FROM auto_run_log WHERE triggerType = :type ORDER BY triggerTime DESC")
    fun getLogsByType(type: String): Flow<List<AutoRunLog>>
    
    @Query("SELECT * FROM auto_run_log WHERE triggerTime >= :startTime AND triggerTime <= :endTime ORDER BY triggerTime DESC")
    suspend fun getLogsInRange(startTime: Long, endTime: Long): List<AutoRunLog>
    
    @Query("SELECT * FROM auto_run_log WHERE triggerSourceId = :sourceId ORDER BY triggerTime DESC")
    suspend fun getLogsBySource(sourceId: String): List<AutoRunLog>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AutoRunLog)
    
    @Query("SELECT COUNT(*) FROM auto_run_log WHERE triggerTime >= :startTime AND result = :result")
    suspend fun getCountByResult(startTime: Long, result: String): Int
    
    @Query("SELECT COUNT(*) FROM auto_run_log WHERE triggerTime >= :startTime")
    suspend fun getTotalCount(startTime: Long): Int
    
    @Query("DELETE FROM auto_run_log WHERE triggerTime < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
}
```

---

## 6. 테스트 전략

### 6.1 마이그레이션 테스트

**파일**: `app/src/androidTest/java/com/allday/detoxy/data/local/migrations/MigrationTest_3_4.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest_3_4 {
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Test
    fun migrate3To4_CreatesNewTables() {
        // v3 DB 생성
        val dbV3 = helper.createDatabase(TEST_DB, 3)
        
        // 마이그레이션 실행
        val dbV4 = helper.runMigrationsAndValidate(
            TEST_DB, 
            4, 
            true, 
            MIGRATION_3_4
        )
        
        // 테이블 존재 확인
        assertTableExists(dbV4, "time_based_auto_run")
        assertTableExists(dbV4, "location_based_auto_run")
        assertTableExists(dbV4, "custom_timer_preset")
        assertTableExists(dbV4, "auto_run_log")
        
        // 인덱스 존재 확인
        assertIndexExists(dbV4, "idx_time_based_hour_minute")
        assertIndexExists(dbV4, "idx_location_based_enabled")
        assertIndexExists(dbV4, "idx_custom_preset_order")
        assertIndexExists(dbV4, "idx_auto_run_log_time")
        
        dbV4.close()
    }
    
    @Test
    fun migrate3To4_InsertsDefaultPresets() {
        val dbV3 = helper.createDatabase(TEST_DB, 3)
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        
        // 기본 프리셋 3개 확인
        val cursor = dbV4.query("SELECT * FROM custom_timer_preset WHERE displayOrder < 3")
        assertThat(cursor.count).isEqualTo(3)
        
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex("name"))).isEqualTo("25분")
        
        cursor.close()
        dbV4.close()
    }
    
    @Test
    fun migrate3To4_AddsUserSettingsFields() {
        val dbV3 = helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO user_settings (id, totalPoints, currentStreak) VALUES (1, 100, 5)")
        }
        
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        
        // 신규 필드 확인
        val cursor = dbV4.query("SELECT autoRunPauseUntil, autoRunMasterEnabled FROM user_settings WHERE id = 1")
        cursor.moveToFirst()
        
        assertThat(cursor.isNull(cursor.getColumnIndex("autoRunPauseUntil"))).isTrue()
        assertThat(cursor.getInt(cursor.getColumnIndex("autoRunMasterEnabled"))).isEqualTo(1)
        
        cursor.close()
        dbV4.close()
    }
    
    companion object {
        private const val TEST_DB = "detoxy_test_db"
    }
}
```

### 6.2 DAO 단위 테스트

**각 DAO 테스트 작성**:
- CRUD 동작 확인
- Flow 데이터 변경 감지 확인
- 인덱스 사용 쿼리 성능 확인
- 최대 개수 제한 로직 확인

**예시** (TimeBasedAutoRunDaoTest.kt):
```kotlin
@Test
fun insertAndRetrieve_timeBasedAutoRun() = runTest {
    val autoRun = TimeBasedAutoRun(
        id = "test-1",
        hour = 9,
        minute = 0,
        durationMinutes = 45,
        presetType = "STANDARD",
        enabledDays = """["MON", "WED", "FRI"]""",
        label = "테스트",
        isEnabled = true,
        createdAt = System.currentTimeMillis()
    )
    
    dao.insert(autoRun)
    
    val retrieved = dao.getById("test-1").first()
    assertThat(retrieved).isEqualTo(autoRun)
}
```

### 6.3 통합 테스트

**Repository-DAO 통합 테스트**:
- AlarmManager 연동 시나리오
- Geofence 등록/해제 시나리오
- 자동 실행 로그 기록 시나리오

---

## 7. 성능 최적화

### 7.1 인덱스 전략

| 테이블 | 인덱스 | 이유 |
|--------|--------|------|
| `time_based_auto_run` | `(hour, minute)` | 다음 알람 계산 쿼리 최적화 |
| `time_based_auto_run` | `(isEnabled)` | 활성화된 항목만 필터링 |
| `location_based_auto_run` | `(isEnabled)` | 활성화된 항목만 필터링 |
| `custom_timer_preset` | `(displayOrder)` | 화면 표시 순서 정렬 |
| `auto_run_log` | `(triggerTime DESC)` | 최근 이력 조회 최적화 |
| `auto_run_log` | `(triggerType, triggerTime DESC)` 🆕 | 타입별 이력 조회 (대시보드) |
| `auto_run_log` | `(sessionId)` 🆕 | FK 인덱스 (자동 생성) |

**인덱스 추가 근거** 🆕:
- `(triggerType, triggerTime DESC)`: 대시보드에서 "시간 기반" 또는 "위치 기반" 이력만 조회 시 성능 향상
- `(sessionId)`: FocusSession 삭제 시 참조 무결성 확인 성능 향상

### 7.2 데이터 정리 정책

- **AutoRunLog**: 90일 이상 된 로그 자동 삭제 (WorkManager 정기 작업)
- **CustomTimerPreset**: 최대 10개 제한 (삽입 시 검증)
- **TimeBasedAutoRun**: 최대 10개 제한
- **LocationBasedAutoRun**: 최대 5개 제한

---

## 8. 백업 및 복원

### 8.1 데이터 백업

**백업 대상**:
- TimeBasedAutoRun (전체)
- LocationBasedAutoRun (전체)
- CustomTimerPreset (사용자 생성만, displayOrder >= 3)
- UserSettings (전체)

**백업 제외**:
- AutoRunLog (재생성 가능)

### 8.2 복원 시나리오

1. **신규 기기로 복원**:
   - DB 백업 파일 복원
   - AlarmManager/Geofence 재등록
   - 권한 재요청

2. **부분 복원** (선택적):
   - 자동 실행 설정만 복원
   - 프리셋만 복원

---

## 9. 롤백 전략

### 9.1 마이그레이션 실패 시

**자동 처리**:
- Room이 마이그레이션 실패 감지
- `fallbackToDestructiveMigration()` 비활성화 (데이터 보존)
- 사용자에게 오류 안내

**수동 처리**:
- 로그 수집 (Crashlytics)
- v3 DB 백업 유지
- 앱 재설치 안내

### 9.2 데이터 손실 방지

- **Migration 전 자동 백업**: Room 자체 기능
- **검증 로직**: 마이그레이션 후 데이터 무결성 확인
- **사용자 데이터 우선**: 기존 데이터 절대 삭제 안 함

---

## 10. 체크리스트

### 10.1 구현 체크리스트

- [ ] 4개 엔티티 클래스 작성
- [ ] 4개 DAO 인터페이스 작성
- [ ] Migration_3_4 작성
- [ ] DatabaseModule 업데이트
- [ ] DetoxyDatabase 업데이트 (version = 4)
- [ ] 마이그레이션 테스트 작성
- [ ] DAO 단위 테스트 작성
- [ ] 빌드 및 컴파일 검증

### 10.2 검증 체크리스트

- [ ] v3 → v4 마이그레이션 성공
- [ ] 신규 테이블 4개 생성 확인
- [ ] 인덱스 5개 생성 확인
- [ ] 기본 프리셋 3개 삽입 확인
- [ ] UserSettings 필드 2개 추가 확인
- [ ] 기존 데이터 손실 없음 확인

---

## 부록: 참조 링크

- [2차 고도화 PRD §5.1](./02_advanced_autosetting_prd.md#51-로컬-db-room-v3v4)
- [2차 고도화 작업 계획 §2.1](./02_advanced_autosetting_todolist.md#21-room-마이그레이션-v3v4-day-4-5)
- [1차 고도화 마이그레이션 전략](./01_advanced_room_migration_strategy.md)
- [Room 마이그레이션 가이드](https://developer.android.com/training/data-storage/room/migrating-db-versions)

---

**문서 버전**: v1.0  
**최종 수정**: 2025-10-20  
**작성자**: AI Assistant

