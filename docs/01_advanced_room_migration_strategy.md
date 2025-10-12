# Room 데이터베이스 마이그레이션 전략

## 개요
- **작성 일자**: 2025-10-12
- **현재 버전**: v1 (MVP)
- **목표 버전**: v3 (1차 고도화)
- **마이그레이션 단계**: v1 → v2 → v3 (점진적 마이그레이션)

---

## 1. 현재 상태 (v1)

### 엔티티
1. **FocusSession** (focus_sessions 테이블)
   ```kotlin
   @Entity(tableName = "focus_sessions")
   data class FocusSession(
       @PrimaryKey val id: String,
       val startTime: Long,
       val endTime: Long?,
       val durationMinutes: Int,
       val success: Boolean
   )
   ```

2. **UserSettings** (user_settings 테이블)
   ```kotlin
   @Entity(tableName = "user_settings")
   data class UserSettings(
       @PrimaryKey val id: Int = 1,
       val totalPoints: Int,
       val currentStreak: Int,
       val lastSuccessDate: String?
   )
   ```

### 데이터베이스 버전
```kotlin
@Database(
    entities = [FocusSession::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
```

---

## 2. 마이그레이션 로드맵

### Phase 1: v1 → v2 (Week 2A)
**목적**: FocusSession 확장 + FocusInterruption 추가

#### 변경 사항
1. **FocusSession 필드 추가**
   - `interruptedSeconds: Int` (중도 포기 시 경과 시간)
   - `primaryDistractionCategory: String?` (주요 방해요인 카테고리)
   - `giveUpReason: String?` (포기 사유)

2. **FocusInterruption 엔티티 신규 추가**
   - 세션 중 차단 이벤트 로그 저장
   - `sessionId`, `timestamp`, `packageName`, `category`

#### 마이그레이션 코드
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. FocusSession 테이블에 컬럼 추가
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN interruptedSeconds INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN primaryDistractionCategory TEXT"
        )
        database.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN giveUpReason TEXT"
        )
        
        // 2. FocusInterruption 테이블 생성
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_interruptions (
                id TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                packageName TEXT NOT NULL,
                category TEXT NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES focus_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        
        // 3. 인덱스 생성 (쿼리 성능 최적화)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_interruptions_sessionId ON focus_interruptions(sessionId)"
        )
    }
}
```

---

### Phase 2: v2 → v3 (Week 2B)
**목적**: 고급 분석을 위한 추가 엔티티

#### 변경 사항
1. **FocusDistraction 엔티티 추가**
   - 허용/차단 이벤트 전체 로그
   - UsageStats 데이터 연동

2. **DetoxyRoutineLog 엔티티 추가**
   - 루틴 실행/실패 기록

3. **FocusSettings 엔티티 추가**
   - 카테고리별 차단 설정 저장

#### 마이그레이션 코드
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. FocusDistraction 테이블 생성
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_distractions (
                id TEXT PRIMARY KEY NOT NULL,
                sessionId TEXT,
                timestamp INTEGER NOT NULL,
                packageName TEXT NOT NULL,
                category TEXT NOT NULL,
                wasBlocked INTEGER NOT NULL,
                dwellTimeSeconds INTEGER,
                FOREIGN KEY(sessionId) REFERENCES focus_sessions(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )
        
        // 2. DetoxyRoutineLog 테이블 생성
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS detoxy_routine_logs (
                id TEXT PRIMARY KEY NOT NULL,
                scheduledTime INTEGER NOT NULL,
                executedTime INTEGER,
                success INTEGER NOT NULL,
                failureReason TEXT
            )
            """.trimIndent()
        )
        
        // 3. FocusSettings 테이블 생성
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                snsEnabled INTEGER NOT NULL DEFAULT 1,
                messengerEnabled INTEGER NOT NULL DEFAULT 0,
                webEnabled INTEGER NOT NULL DEFAULT 1,
                videoEnabled INTEGER NOT NULL DEFAULT 1,
                otherEnabled INTEGER NOT NULL DEFAULT 1,
                lastUpdated INTEGER NOT NULL
            )
            """.trimIndent()
        )
        
        // 4. 기본 설정 삽입
        database.execSQL(
            """
            INSERT OR IGNORE INTO focus_settings (id, snsEnabled, messengerEnabled, webEnabled, videoEnabled, otherEnabled, lastUpdated)
            VALUES (1, 1, 0, 1, 1, 1, ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        // 5. 인덱스 생성
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_distractions_sessionId ON focus_distractions(sessionId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_distractions_timestamp ON focus_distractions(timestamp)"
        )
    }
}
```

---

## 3. 엔티티 상세 정의

### 3.1 FocusSession (v2)
```kotlin
@Entity(
    tableName = "focus_sessions"
)
data class FocusSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    // 기존 필드
    val startTime: Long,
    val endTime: Long? = null,
    val durationMinutes: Int,
    val success: Boolean = false,
    
    // v2 추가 필드
    val interruptedSeconds: Int = 0,              // 중도 포기 시 경과 시간
    val primaryDistractionCategory: String? = null, // 주요 방해요인
    val giveUpReason: String? = null               // 포기 사유
)
```

**giveUpReason enum**:
```kotlin
enum class GiveUpReason {
    USER_CANCEL,          // 사용자가 직접 포기
    SYSTEM_INTERRUPT,     // 시스템 인터럽트 (재부팅 등)
    PERMISSION_REVOKED,   // 권한 해제
    UNKNOWN;              // 알 수 없음
    
    fun toDisplayString(): String = when (this) {
        USER_CANCEL -> "사용자 포기"
        SYSTEM_INTERRUPT -> "시스템 중단"
        PERMISSION_REVOKED -> "권한 해제"
        UNKNOWN -> "알 수 없음"
    }
}
```

---

### 3.2 FocusInterruption (v2 신규)
```kotlin
@Entity(
    tableName = "focus_interruptions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class FocusInterruption(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val sessionId: String,        // 세션 ID (FK)
    val timestamp: Long,          // 발생 시각
    val packageName: String,      // 차단된 앱 패키지명
    val category: String          // AppCategory.name (SNS, WEB, etc.)
)
```

---

### 3.3 FocusDistraction (v3 신규)
```kotlin
@Entity(
    tableName = "focus_distractions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"])
    ]
)
data class FocusDistraction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val sessionId: String?,       // 세션 ID (nullable, 세션 외에도 기록 가능)
    val timestamp: Long,          // 발생 시각
    val packageName: String,      // 앱 패키지명
    val category: String,         // AppCategory.name
    val wasBlocked: Boolean,      // 차단 여부 (true: 차단됨, false: 허용됨)
    val dwellTimeSeconds: Int? = null // 체류 시간 (UsageStats 연동 시)
)
```

---

### 3.4 DetoxyRoutineLog (v3 신규)
```kotlin
@Entity(tableName = "detoxy_routine_logs")
data class DetoxyRoutineLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val scheduledTime: Long,      // 예정 시각
    val executedTime: Long? = null, // 실제 실행 시각
    val success: Boolean,         // 성공 여부
    val failureReason: String? = null // 실패 사유
)
```

---

### 3.5 FocusSettings (v3 신규)
```kotlin
@Entity(tableName = "focus_settings")
data class FocusSettings(
    @PrimaryKey
    val id: Int = 1,  // Singleton
    
    val snsEnabled: Boolean = true,
    val messengerEnabled: Boolean = false,  // 기본 OFF
    val webEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val otherEnabled: Boolean = true,
    
    val lastUpdated: Long = System.currentTimeMillis()
)
```

---

## 4. DAO 업데이트

### 4.1 FocusSessionDao (v2)
```kotlin
@Dao
interface FocusSessionDao {
    // 기존 메서드...
    
    // v2 추가
    @Query("""
        SELECT * FROM focus_sessions 
        WHERE interruptedSeconds > 0 
        ORDER BY startTime DESC
    """)
    fun getGiveUpSessions(): Flow<List<FocusSession>>
    
    @Query("""
        SELECT primaryDistractionCategory, COUNT(*) as count 
        FROM focus_sessions 
        WHERE primaryDistractionCategory IS NOT NULL 
        GROUP BY primaryDistractionCategory 
        ORDER BY count DESC
    """)
    suspend fun getTopDistractionCategories(): List<CategoryCount>
}

data class CategoryCount(
    val primaryDistractionCategory: String,
    val count: Int
)
```

---

### 4.2 FocusInterruptionDao (v2 신규)
```kotlin
@Dao
interface FocusInterruptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interruption: FocusInterruption)
    
    @Query("SELECT * FROM focus_interruptions WHERE sessionId = :sessionId ORDER BY timestamp")
    fun getBySession(sessionId: String): Flow<List<FocusInterruption>>
    
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM focus_interruptions 
        WHERE sessionId = :sessionId 
        GROUP BY category 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getPrimaryDistractionCategory(sessionId: String): String?
    
    // 방해요인 Top 3
    @Query("""
        SELECT packageName, category, COUNT(*) as count 
        FROM focus_interruptions 
        WHERE timestamp >= :startTime 
        GROUP BY packageName, category 
        ORDER BY count DESC 
        LIMIT 3
    """)
    suspend fun getTop3Distractions(startTime: Long): List<DistractionStat>
}

data class DistractionStat(
    val packageName: String,
    val category: String,
    val count: Int
)
```

---

### 4.3 FocusDistractionDao (v3 신규)
```kotlin
@Dao
interface FocusDistractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(distraction: FocusDistraction)
    
    @Query("""
        SELECT * FROM focus_distractions 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        ORDER BY timestamp DESC
    """)
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<FocusDistraction>>
    
    // 분산 회피율 계산
    @Query("""
        SELECT 
            SUM(CASE WHEN wasBlocked = 1 THEN 1 ELSE 0 END) * 1.0 / COUNT(*) as avoidanceRate
        FROM focus_distractions 
        WHERE timestamp >= :startTime
    """)
    suspend fun getAvoidanceRate(startTime: Long): Float
}
```

---

### 4.4 FocusSettingsDao (v3 신규)
```kotlin
@Dao
interface FocusSettingsDao {
    @Query("SELECT * FROM focus_settings WHERE id = 1")
    fun getSettings(): Flow<FocusSettings>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: FocusSettings)
    
    @Update
    suspend fun update(settings: FocusSettings)
    
    // 카테고리별 토글
    @Query("UPDATE focus_settings SET snsEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setSnsEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE focus_settings SET messengerEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setMessengerEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // 프리셋 적용
    @Transaction
    suspend fun applyPreset(preset: String) {
        when (preset) {
            "FULL_BLOCK" -> {
                setSnsEnabled(true)
                setMessengerEnabled(false)
                setWebEnabled(true)
                setVideoEnabled(true)
                setOtherEnabled(true)
            }
            "FOCUS" -> {
                setSnsEnabled(true)
                setMessengerEnabled(false)
                setWebEnabled(false)
                setVideoEnabled(true)
                setOtherEnabled(false)
            }
            "RELAXED" -> {
                setSnsEnabled(false)
                setMessengerEnabled(false)
                setWebEnabled(false)
                setVideoEnabled(true)
                setOtherEnabled(false)
            }
        }
    }
}
```

---

## 5. Database 클래스 업데이트

### v3 최종 버전
```kotlin
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class,
        FocusInterruption::class,      // v2
        FocusDistraction::class,        // v3
        DetoxyRoutineLog::class,        // v3
        FocusSettings::class            // v3
    ],
    version = 3,
    exportSchema = true,  // 스키마 변경 추적을 위해 true로 변경
    autoMigrations = []   // 수동 마이그레이션 사용
)
abstract class DetoxyDatabase : RoomDatabase() {
    
    // 기존 DAO
    abstract fun sessionDao(): FocusSessionDao
    abstract fun settingsDao(): UserSettingsDao
    
    // v2 DAO
    abstract fun interruptionDao(): FocusInterruptionDao
    
    // v3 DAO
    abstract fun distractionDao(): FocusDistractionDao
    abstract fun routineLogDao(): DetoxyRoutineLogDao
    abstract fun focusSettingsDao(): FocusSettingsDao
}
```

---

## 6. Hilt Module 업데이트

### DatabaseModule.kt
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DetoxyDatabase {
        return Room.databaseBuilder(
            context,
            DetoxyDatabase::class.java,
            "detoxy_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // 마이그레이션 추가
        .fallbackToDestructiveMigration()  // 개발 중에만 사용, 릴리스 전에 제거
        .build()
    }
    
    @Provides
    fun provideSessionDao(db: DetoxyDatabase) = db.sessionDao()
    
    @Provides
    fun provideSettingsDao(db: DetoxyDatabase) = db.settingsDao()
    
    // v2
    @Provides
    fun provideInterruptionDao(db: DetoxyDatabase) = db.interruptionDao()
    
    // v3
    @Provides
    fun provideDistractionDao(db: DetoxyDatabase) = db.distractionDao()
    
    @Provides
    fun provideRoutineLogDao(db: DetoxyDatabase) = db.routineLogDao()
    
    @Provides
    fun provideFocusSettingsDao(db: DetoxyDatabase) = db.focusSettingsDao()
}
```

---

## 7. 마이그레이션 테스트 전략

### 7.1 단위 테스트
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java
    )
    
    @Test
    fun migrate1To2() {
        // v1 DB 생성
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO focus_sessions (id, startTime, endTime, durationMinutes, success)
                VALUES ('test-id', ${System.currentTimeMillis()}, NULL, 25, 0)
                """.trimIndent()
            )
            close()
        }
        
        // v2로 마이그레이션
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        
        // 새 컬럼 확인
        db.query("SELECT * FROM focus_sessions WHERE id = 'test-id'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val interruptedSecondsIndex = cursor.getColumnIndex("interruptedSeconds")
            assertTrue(interruptedSecondsIndex >= 0)
            assertEquals(0, cursor.getInt(interruptedSecondsIndex))
        }
        
        // FocusInterruption 테이블 확인
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='focus_interruptions'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }
    
    @Test
    fun migrate2To3() {
        // v2 DB 생성 (이미 MIGRATION_1_2 적용됨)
        var db = helper.createDatabase(TEST_DB, 2)
        db.close()
        
        // v3로 마이그레이션
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        
        // 새 테이블 확인
        val tables = listOf("focus_distractions", "detoxy_routine_logs", "focus_settings")
        tables.forEach { tableName ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'").use { cursor ->
                assertTrue("$tableName should exist", cursor.moveToFirst())
            }
        }
        
        // 기본 설정 삽입 확인
        db.query("SELECT * FROM focus_settings WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }
}
```

---

### 7.2 통합 테스트
```kotlin
@Test
fun fullMigrationPath() {
    // v1 → v2 → v3 전체 마이그레이션 테스트
    var db = helper.createDatabase(TEST_DB, 1).apply {
        // v1 데이터 삽입
        execSQL("""
            INSERT INTO focus_sessions (id, startTime, endTime, durationMinutes, success)
            VALUES ('session-1', ${System.currentTimeMillis()}, NULL, 25, 0)
        """)
        close()
    }
    
    // v1 → v2
    db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    
    // v2 → v3
    db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
    
    // 기존 데이터 보존 확인
    db.query("SELECT * FROM focus_sessions WHERE id = 'session-1'").use { cursor ->
        assertTrue(cursor.moveToFirst())
    }
}
```

---

## 8. 데이터 백필 전략

### 8.1 기존 세션 데이터 백필
```kotlin
class MigrationBackfillUseCase(
    private val sessionDao: FocusSessionDao
) {
    /**
     * v1 → v2 마이그레이션 후 실행
     * 기존 세션의 interruptedSeconds를 0으로 초기화 (이미 마이그레이션 SQL에서 처리)
     */
    suspend fun backfillInterruptedSeconds() {
        // 마이그레이션 SQL에서 DEFAULT 0으로 처리되므로 추가 작업 불필요
        // 필요 시 추가 로직 구현
    }
}
```

---

### 8.2 설정 초기화
```kotlin
class SettingsInitUseCase(
    private val focusSettingsDao: FocusSettingsDao
) {
    /**
     * 앱 업데이트 후 첫 실행 시 기본 설정 생성
     */
    suspend fun initializeSettings() {
        val settings = focusSettingsDao.getSettings().first()
        if (settings == null) {
            focusSettingsDao.insert(
                FocusSettings(
                    id = 1,
                    snsEnabled = true,
                    messengerEnabled = false,
                    webEnabled = true,
                    videoEnabled = true,
                    otherEnabled = true
                )
            )
        }
    }
}
```

---

## 9. 롤백 전략

### 9.1 마이그레이션 실패 시
```kotlin
.fallbackToDestructiveMigration()  // 개발 중에만 사용
```

**주의**: 프로덕션에서는 사용하지 말 것!

---

### 9.2 데이터 백업
```kotlin
class DatabaseBackupUseCase(
    private val context: Context
) {
    /**
     * 마이그레이션 전 데이터베이스 백업
     */
    suspend fun backupDatabase(): File? {
        val dbFile = context.getDatabasePath("detoxy_database")
        if (!dbFile.exists()) return null
        
        val backupDir = File(context.filesDir, "db_backups")
        backupDir.mkdirs()
        
        val backupFile = File(backupDir, "detoxy_database_v1_${System.currentTimeMillis()}.db")
        dbFile.copyTo(backupFile, overwrite = true)
        
        return backupFile
    }
}
```

---

## 10. 구현 일정

### Week 2A (Day 1-3): v1 → v2 마이그레이션
- [x] FocusSession 필드 추가
- [x] FocusInterruption 엔티티 생성
- [x] DAO 업데이트
- [x] 마이그레이션 코드 작성
- [x] 단위 테스트

### Week 2B (Day 7-8): v2 → v3 마이그레이션
- [x] FocusDistraction 엔티티 생성
- [x] DetoxyRoutineLog 엔티티 생성
- [x] FocusSettings 엔티티 생성
- [x] DAO 업데이트
- [x] 마이그레이션 코드 작성
- [x] 통합 테스트

---

## 11. 주의사항

### 11.1 외래 키 제약 조건
- FocusInterruption: `ON DELETE CASCADE` (세션 삭제 시 함께 삭제)
- FocusDistraction: `ON DELETE SET NULL` (세션 삭제 시 NULL로 설정)

### 11.2 인덱스 최적화
- sessionId, timestamp에 인덱스 생성
- 쿼리 성능 모니터링

### 11.3 마이그레이션 테스트 필수
- 실제 사용자 데이터로 테스트
- 다양한 Android 버전에서 검증

---

**최종 업데이트**: 2025-10-12
**버전 계획**: v1 (현재) → v2 (Week 2A) → v3 (Week 2B)

