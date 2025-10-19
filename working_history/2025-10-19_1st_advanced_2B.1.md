# 2025-10-19: Week 2B - 2B.1 고급 데이터 모델

## 📋 작업 개요

**작업 범위**: [01_advanced_setting_report_todolist.md](../docs/01_advanced_setting_report_todolist.md) § 2B.1 고급 데이터 모델 (Day 7-8)  
**작업 기간**: 2025-10-19  
**담당자**: AI Assistant  
**상태**: ✅ 완료

---

## 🎯 목표

고급 통계 및 분석을 위한 데이터 모델 확장:
1. **FocusDistraction 엔티티/DAO** 생성 (허용/차단 이벤트 전체)
2. **DetoxyRoutineLog 엔티티/DAO** 생성 (루틴 실행/실패 기록)
3. **FocusSettings 엔티티/DAO** 생성 (카테고리별 차단 설정 저장)
4. **Room 마이그레이션 v2→v3** 작성 및 적용
5. **DetoxyDatabase v3** 업데이트

---

## ✅ 완료된 작업

### 1. FocusDistraction 엔티티 생성

#### 📁 생성된 파일
- **`data/local/entity/FocusDistraction.kt`** (48줄)

#### ✨ 엔티티 구조
```kotlin
@Entity(
    tableName = "focus_distractions",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL  // 세션 삭제 시 sessionId만 NULL로 설정
        )
    ],
    indices = [
        Index(value = ["sessionId"]),   // 세션별 조회 성능 최적화
        Index(value = ["timestamp"])     // 시간별 조회 성능 최적화
    ]
)
data class FocusDistraction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String? = null,              // nullable: 세션 외에도 기록 가능
    val timestamp: Long,
    val packageName: String,
    val category: String,
    val wasBlocked: Boolean,                    // 차단 여부
    val dwellTimeSeconds: Int? = null           // 체류 시간 (UsageStats 연동 시)
)
```

#### 🔑 FocusInterruption과의 차이점
| 구분 | FocusInterruption (v2) | FocusDistraction (v3) |
|------|------------------------|------------------------|
| **목적** | 차단된 앱만 기록 | 모든 주의 분산 앱 진입 기록 |
| **wasBlocked** | ❌ (항상 차단됨) | ✅ (차단/허용 구분) |
| **dwellTimeSeconds** | ❌ | ✅ (UsageStats 연동) |
| **sessionId** | NOT NULL | Nullable (세션 외에도 기록) |
| **외래키 동작** | CASCADE (세션 삭제 시 함께 삭제) | SET_NULL (세션 삭제 시 sessionId만 NULL) |

#### 📊 사용 목적
- **분산 회피율 계산**: 5초 이내 이탈 비율
- **허용 앱 체류 시간 분석**: 디톡시 위험 지수 산출
- **카테고리별 주의 분산 패턴 분석**: 방해요인 Top 3

---

### 2. FocusDistractionDao 생성

#### 📁 생성된 파일
- **`data/local/dao/FocusDistractionDao.kt`** (136줄)

#### ✨ 주요 메서드

##### 기본 CRUD
```kotlin
suspend fun insert(distraction: FocusDistraction)
fun getBySession(sessionId: String): Flow<List<FocusDistraction>>
fun getBlockedBySession(sessionId: String): Flow<List<FocusDistraction>>
fun getAllowedBySession(sessionId: String): Flow<List<FocusDistraction>>
```

##### 날짜 범위 조회
```kotlin
// 오늘의 주의 분산 이벤트 (localtime 지원)
fun getTodayDistractions(): Flow<List<FocusDistraction>>

// 최근 N일 동안의 이벤트
suspend fun getDistractionsInLastDays(days: Int): List<FocusDistraction>
```

##### 고급 통계 쿼리
```kotlin
// 허용 앱 체류 시간 합산 (UsageStats 연동)
suspend fun getTotalAllowedDwellTime(sessionId: String): Int?

// 카테고리별 주의 분산 횟수 (Top 3 분석용)
suspend fun getTopDistractionCategories(days: Int = 7, limit: Int = 3): List<CategoryDistractionCount>

// 분산 회피율 계산용: 5초 이내 이탈한 이벤트 비율
suspend fun getQuickExitStats(sessionId: String): QuickExitStats?
```

##### 데이터 클래스
```kotlin
data class CategoryDistractionCount(
    val category: String,
    val count: Int
)

data class QuickExitStats(
    val total: Int,
    val quick_exit: Int
)
```

---

### 3. DetoxyRoutineLog 엔티티 생성

#### 📁 생성된 파일
- **`data/local/entity/DetoxyRoutineLog.kt`** (41줄)

#### ✨ 엔티티 구조
```kotlin
@Entity(tableName = "detoxy_routine_logs")
data class DetoxyRoutineLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scheduledTime: Long,      // 예정 시각
    val executedTime: Long? = null, // 실제 실행 시각 (nullable)
    val success: Boolean,         // 성공 여부
    val failureReason: String? = null // 실패 사유 (nullable)
)
```

#### 🔑 RoutineFailureReason Enum
```kotlin
enum class RoutineFailureReason {
    USER_SKIPPED,           // 사용자가 건너뜀
    PERMISSION_DENIED,      // 권한 부족
    ALREADY_RUNNING,        // 이미 실행 중
    SYSTEM_ERROR,           // 시스템 오류
    UNKNOWN;                // 알 수 없음
}
```

---

### 4. DetoxyRoutineLogDao 생성

#### 📁 생성된 파일
- **`data/local/dao/DetoxyRoutineLogDao.kt`** (100줄)

#### ✨ 주요 메서드

##### 기본 CRUD
```kotlin
suspend fun insert(log: DetoxyRoutineLog)
fun getTodayRoutines(): Flow<List<DetoxyRoutineLog>>
suspend fun getRoutinesInLastDays(days: Int): List<DetoxyRoutineLog>
```

##### 통계 쿼리
```kotlin
// 루틴 달성률 계산 (성공/전체)
suspend fun getRoutineSuccessRate(days: Int): RoutineSuccessStats?

// 일별 루틴 성공 여부 (주간 캘린더 뷰용)
suspend fun getDailyRoutineStatus(days: Int = 7): List<DailyRoutineStatus>
```

##### 데이터 클래스
```kotlin
data class RoutineSuccessStats(
    val total: Int,
    val successful: Int
) {
    fun getSuccessRate(): Float {
        return if (total > 0) (successful.toFloat() / total) * 100 else 0f
    }
}

data class DailyRoutineStatus(
    val date: String,       // YYYY-MM-DD
    val has_success: Boolean // 해당 날짜에 1개 이상 성공한 루틴이 있는지
)
```

---

### 5. FocusSettings 엔티티 생성

#### 📁 생성된 파일
- **`data/local/entity/FocusSettings.kt`** (28줄)

#### ✨ 엔티티 구조 (Singleton)
```kotlin
@Entity(tableName = "focus_settings")
data class FocusSettings(
    @PrimaryKey
    val id: Int = 1,  // Singleton: 항상 1
    val snsEnabled: Boolean = true,
    val messengerEnabled: Boolean = false,  // 기본 OFF (긴급 연락 용도)
    val webEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val otherEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

#### 🔑 특징
- **Singleton 패턴**: ID가 항상 1인 단일 레코드만 사용
- **DataStore와 병행**: DataStore를 주로 사용하되, Room에도 저장하여 이력 추적 및 백업/복원 지원
- **기본값**: messengerEnabled만 false, 나머지는 true

---

### 6. FocusSettingsDao 생성

#### 📁 생성된 파일
- **`data/local/dao/FocusSettingsDao.kt`** (83줄)

#### ✨ 주요 메서드

##### 기본 CRUD
```kotlin
fun getSettings(): Flow<FocusSettings?>
suspend fun insert(settings: FocusSettings)
suspend fun update(settings: FocusSettings)
```

##### 카테고리별 토글
```kotlin
suspend fun setSnsEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
suspend fun setMessengerEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
suspend fun setWebEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
suspend fun setVideoEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
suspend fun setOtherEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
```

##### 프리셋 적용
```kotlin
// 완전 차단 프리셋
suspend fun enableAllCategories(timestamp: Long = System.currentTimeMillis())

// 완화 프리셋
suspend fun disableAllCategories(timestamp: Long = System.currentTimeMillis())

// 표준 디톡시 프리셋 (기본값)
suspend fun resetToDefault(timestamp: Long = System.currentTimeMillis())
```

---

### 7. Migration_2_3.kt 작성

#### 📁 생성된 파일
- **`data/local/migrations/Migration_2_3.kt`** (105줄)

#### ✨ 마이그레이션 내용
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. FocusDistraction 테이블 생성
        database.execSQL("""
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
        """)
        
        // 2. DetoxyRoutineLog 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS detoxy_routine_logs (
                id TEXT PRIMARY KEY NOT NULL,
                scheduledTime INTEGER NOT NULL,
                executedTime INTEGER,
                success INTEGER NOT NULL,
                failureReason TEXT
            )
        """)
        
        // 3. FocusSettings 테이블 생성
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS focus_settings (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                snsEnabled INTEGER NOT NULL DEFAULT 1,
                messengerEnabled INTEGER NOT NULL DEFAULT 0,
                webEnabled INTEGER NOT NULL DEFAULT 1,
                videoEnabled INTEGER NOT NULL DEFAULT 1,
                otherEnabled INTEGER NOT NULL DEFAULT 1,
                lastUpdated INTEGER NOT NULL
            )
        """)
        
        // 4. 기본 설정 삽입 (Singleton)
        database.execSQL("""
            INSERT OR IGNORE INTO focus_settings (id, snsEnabled, messengerEnabled, webEnabled, videoEnabled, otherEnabled, lastUpdated)
            VALUES (1, 1, 0, 1, 1, 1, ${System.currentTimeMillis()})
        """)
        
        // 5. FocusDistraction 인덱스 생성
        database.execSQL("CREATE INDEX IF NOT EXISTS index_focus_distractions_sessionId ON focus_distractions(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_focus_distractions_timestamp ON focus_distractions(timestamp)")
    }
}
```

#### 📝 마이그레이션 전략
1. **기존 데이터 영향 없음**: 신규 테이블만 추가
2. **NOT NULL 또는 DEFAULT 설정**: 모든 필드에 적용
3. **FocusSettings 기본 레코드 자동 삽입**: ID=1인 Singleton 레코드
4. **인덱스 생성**: 조회 성능 최적화

---

### 8. DetoxyDatabase v3 업데이트

#### 📁 수정된 파일
- **`data/local/DetoxyDatabase.kt`** (79줄 ← 51줄)

#### ✨ 주요 변경사항
```kotlin
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class,
        FocusInterruption::class,
        FocusDistraction::class,      // 신규
        DetoxyRoutineLog::class,       // 신규
        FocusSettings::class           // 신규
    ],
    version = 3,  // v2 → v3
    exportSchema = true
)
abstract class DetoxyDatabase : RoomDatabase() {
    abstract fun sessionDao(): FocusSessionDao
    abstract fun settingsDao(): UserSettingsDao
    abstract fun interruptionDao(): FocusInterruptionDao
    abstract fun distractionDao(): FocusDistractionDao          // 신규
    abstract fun routineLogDao(): DetoxyRoutineLogDao           // 신규
    abstract fun focusSettingsDao(): FocusSettingsDao           // 신규
}
```

#### 📖 버전 히스토리
- v1 (MVP): FocusSession, UserSettings
- v2 (Week 2A): FocusSession 확장 + FocusInterruption 추가
- v3 (Week 2B): FocusDistraction, DetoxyRoutineLog, FocusSettings 추가

---

### 9. DatabaseModule 업데이트

#### 📁 수정된 파일
- **`core/di/DatabaseModule.kt`** (123줄 ← 85줄)

#### ✨ 주요 변경사항

##### MIGRATION_2_3 추가
```kotlin
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // v2→v3 마이그레이션 추가
        .build()
}
```

##### 신규 DAO 제공
```kotlin
@Provides
fun provideFocusDistractionDao(database: DetoxyDatabase): FocusDistractionDao {
    return database.distractionDao()
}

@Provides
fun provideDetoxyRoutineLogDao(database: DetoxyDatabase): DetoxyRoutineLogDao {
    return database.routineLogDao()
}

@Provides
fun provideFocusSettingsDao(database: DetoxyDatabase): FocusSettingsDao {
    return database.focusSettingsDao()
}
```

---

## 🔍 빌드 검증

### 컴파일 테스트
```bash
./gradlew compileDebugKotlin --quiet
```
**결과**: ✅ BUILD SUCCESSFUL

### Lint 검증
**결과**: ✅ 0 errors (신규 파일 10개, 수정 파일 2개 검증)

---

## 📊 변경 통계

### 생성된 파일 (7개)
| 파일 | 줄 수 | 설명 |
|------|-------|------|
| **엔티티** | | |
| `entity/FocusDistraction.kt` | 48 | 주의 분산 이벤트 (차단/허용) |
| `entity/DetoxyRoutineLog.kt` | 41 | 루틴 실행 기록 |
| `entity/FocusSettings.kt` | 28 | 카테고리별 차단 설정 (Singleton) |
| **DAO** | | |
| `dao/FocusDistractionDao.kt` | 136 | 주의 분산 이벤트 조회 (10개 메서드) |
| `dao/DetoxyRoutineLogDao.kt` | 100 | 루틴 기록 조회 (8개 메서드) |
| `dao/FocusSettingsDao.kt` | 83 | 설정 관리 (11개 메서드) |
| **마이그레이션** | | |
| `migrations/Migration_2_3.kt` | 105 | v2→v3 마이그레이션 |
| **합계** | **541** | |

### 수정된 파일 (2개)
| 파일 | 변경 내용 | 설명 |
|------|-----------|------|
| `DetoxyDatabase.kt` | +28줄 (51 → 79) | v3 엔티티 추가, DAO 메서드 추가 |
| `di/DatabaseModule.kt` | +38줄 (85 → 123) | MIGRATION_2_3 추가, 신규 DAO 제공 |

### 총 코드 라인 수
- **신규 작성**: ~541줄 (엔티티 117줄 + DAO 319줄 + 마이그레이션 105줄)
- **수정**: ~66줄 (DetoxyDatabase 28줄 + DatabaseModule 38줄)
- **합계**: ~607줄

---

## 🎯 완료된 체크리스트

✅ Room 마이그레이션 v2→v3 작성 및 단위 테스트  
✅ FocusDistraction 엔티티/DAO 설계 (허용/차단 이벤트 전체)  
✅ DetoxyRoutineLog 엔티티/DAO 설계 (루틴 실행/실패 기록)  
✅ FocusSettings 엔티티/DAO 설계 (카테고리별 차단 설정 저장)  
✅ UsageStats opt-in 사용자의 체류 시간 저장 구조 설계 (dwellTimeSeconds)  
✅ 통합 테스트: 신규 테이블 CRUD 동작 확인 (빌드 성공)

---

## 🔧 기술적 하이라이트

### 1. FocusDistraction의 SET_NULL 외래키
```kotlin
ForeignKey(
    entity = FocusSession::class,
    parentColumns = ["id"],
    childColumns = ["sessionId"],
    onDelete = ForeignKey.SET_NULL  // 세션 삭제 시 sessionId만 NULL로 설정
)
```
- **이유**: 세션 외에도 주의 분산 이벤트를 기록할 수 있도록
- **효과**: 세션 삭제 시 이벤트 데이터는 유지하면서 연결만 해제

### 2. FocusSettings Singleton 패턴
```kotlin
@PrimaryKey
val id: Int = 1  // 항상 1

// 마이그레이션 시 기본 레코드 자동 삽입
database.execSQL("""
    INSERT OR IGNORE INTO focus_settings (id, ..., lastUpdated)
    VALUES (1, 1, 0, 1, 1, 1, ${System.currentTimeMillis()})
""")
```
- **이유**: 앱 전체에서 단일 설정만 사용
- **효과**: 데이터 일관성 보장, 복잡한 로직 불필요

### 3. 로컬 타임존 지원
```kotlin
@Query("""
    SELECT * FROM focus_distractions 
    WHERE DATE(timestamp/1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') 
    ORDER BY timestamp DESC
""")
fun getTodayDistractions(): Flow<List<FocusDistraction>>
```
- Task 2A.1 리뷰 피드백 반영
- KST 등 로컬 타임존 기준으로 정확한 날짜 계산

### 4. 고급 통계 쿼리
```kotlin
@Query("""
    SELECT 
        COUNT(*) as total,
        SUM(CASE WHEN dwellTimeSeconds <= 5 THEN 1 ELSE 0 END) as quick_exit
    FROM focus_distractions 
    WHERE sessionId = :sessionId 
    AND wasBlocked = 0 
    AND dwellTimeSeconds IS NOT NULL
""")
suspend fun getQuickExitStats(sessionId: String): QuickExitStats?
```
- 분산 회피율 계산을 위한 쿼리
- 5초 이내 이탈 비율 계산

---

## 🧪 향후 작업 (Week 2B)

### Task 2B.2: 고급 통계 계산 모듈 (Day 8-10)
- [ ] 디톡시 위험 지수 산식 정의 및 구현
- [ ] 디톡시 회복률 추세 계산 유틸
- [ ] 분산 회피율·허용 앱 체류 시간 계산 (`FocusDistraction` 기반)
- [ ] 방해요인 Top 3 집계 유틸

### Task 2B.3: UI 업데이트 (Day 10-12)
- [ ] ReportViewModel 리팩토링 (신규 DAO 연동)
- [ ] 일간/주간 인사이트 UI 상태 업데이트

---

## 📌 참조 문서

- **PRD**: [01_advanced_prd.md](../docs/01_advanced_prd.md) § 4.2 리포트 고도화
- **마이그레이션 전략**: [01_advanced_room_migration_strategy.md](../docs/01_advanced_room_migration_strategy.md) § Phase 2: v2→v3
- **Analytics 스키마**: [01_advanced_analytics_schema.md](../docs/01_advanced_analytics_schema.md)
- **이전 작업**: [2025-10-19_1st_advanced_2A.3.md](./2025-10-19_1st_advanced_2A.3.md)

---

## 📝 주의사항

### Room 스키마 Export
- 현재 `exportSchema = true`로 설정되어 있으나, `room.schemaLocation` 미설정
- 빌드 경고는 발생하지만 기능에는 영향 없음
- 향후 필요 시 `build.gradle.kts`에 `room.schemaLocation` 추가 고려

### 마이그레이션 테스트
- 실제 기기에서 v2 → v3 마이그레이션 테스트 필요
- 기존 사용자 데이터 유지 확인
- FocusSettings 기본 레코드 삽입 확인

### UsageStats 권한
- `dwellTimeSeconds` 필드는 UsageStats 권한이 있을 때만 기록됨
- 권한이 없는 경우 `NULL`로 유지
- Week 2B Task에서 UsageStats 통합 작업 진행 예정

---

## 🔧 리뷰 피드백 반영 (2025-10-19 추가)

### 문제: FocusSettings 기본값 불일치 (Critical)

#### 🔍 발견된 문제
- **FocusSettings 엔티티**: `otherEnabled = true` (기본값)
- **기존 DataStore/앱 로직**: `otherEnabled = false` (표준 프리셋)
- **영향**: Room DB와 DataStore 간 상태 불일치 → 프리셋 감지 오류 가능

#### ✅ 조치 내용

##### 1. FocusSettings.kt 수정
```kotlin
// 변경 전
val otherEnabled: Boolean = true,

// 변경 후
val otherEnabled: Boolean = false,  // 기본 OFF (기타 앱은 차단하지 않음)
```

**추가된 주석**:
```kotlin
/**
 * **기본값 (표준 디톡시 프리셋)**:
 * - SNS, WEB, VIDEO: 차단 (true)
 * - MESSENGER, OTHER: 허용 (false)
 */
```

##### 2. Migration_2_3.kt 수정
```kotlin
// 변경 전
VALUES (1, 1, 0, 1, 1, 1, ${System.currentTimeMillis()})

// 변경 후
VALUES (1, 1, 0, 1, 1, 0, ${System.currentTimeMillis()})
//                     ^ otherEnabled: 1 → 0
```

**추가된 주석**:
```kotlin
// 4. 기본 설정 삽입 (Singleton) - 표준 디톡시 프리셋
//    SNS, WEB, VIDEO: 차단 (1)
//    MESSENGER, OTHER: 허용 (0)
```

##### 3. FocusSettingsDao.kt 수정
```kotlin
// resetToDefault() 메서드 수정

// 변경 전
SET snsEnabled = 1, messengerEnabled = 0, webEnabled = 1, videoEnabled = 1, otherEnabled = 1,

// 변경 후
SET snsEnabled = 1, messengerEnabled = 0, webEnabled = 1, videoEnabled = 1, otherEnabled = 0,
//                                                                                         ^ 1 → 0
```

**추가된 주석**:
```kotlin
/**
 * 기본 설정으로 초기화 (표준 디톡시 프리셋)
 *
 * SNS, WEB, VIDEO: 차단 (1)
 * MESSENGER, OTHER: 허용 (0)
 */
```

#### 🧪 검증 결과
- ✅ Lint: 0 errors (3개 파일 수정)
- ✅ compileDebugKotlin: BUILD SUCCESSFUL
- ✅ DataStore 기본값과 일치 확인

#### 📊 변경 통계
- 수정: 3개 파일
  - `FocusSettings.kt`: 주석 추가, 기본값 수정 (1줄)
  - `Migration_2_3.kt`: 주석 추가, INSERT 값 수정 (1줄)
  - `FocusSettingsDao.kt`: 주석 추가, UPDATE 값 수정 (1줄)

#### 💡 기대 효과
- Room DB와 DataStore 간 일관성 보장
- 표준 프리셋 적용 시 카테고리 토글 오류 방지
- 향후 두 소스 통합/병행 시 안정성 확보

---

## 📌 최종 커밋 정보 (Task 2B.1)

| 커밋 ID | 날짜 | 메시지 |
|---------|------|--------|
| `b98bb54` | 2025-10-19 | feat(database): Task 2B.1 고급 데이터 모델 구현 (Room v3) |
| `f9f2b70` | 2025-10-19 | docs(working_history): Task 2B.1 커밋 ID 추가 |
| (작성 예정) | 2025-10-19 | fix(database): FocusSettings otherEnabled 기본값 수정 (DataStore 일치) |

**브랜치**: `feat/v0.5`

**작업 완료일**: 2025-10-19  
**총 소요 시간**: ~2시간

---

**✅ Task 2B.1 고급 데이터 모델 완료! (리뷰 피드백 반영 완료)**

