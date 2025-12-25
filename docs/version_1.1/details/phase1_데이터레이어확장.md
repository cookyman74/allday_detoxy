# Phase 1. 데이터 레이어 확장 상세 작업 지시서

> **상위 문서**: [할 일 기능 작업 계획](../10_추가기능_할일관리기능_todolist.md)  
> **기능 설계**: [할 일 기능 설계 문서](../10_추가기능_할일관리기능.md)

---

## 📥 사전 작업 (Pre-Work)

| 항목 | 확인 |
|------|------|
| Phase 0 작업결과서 확인 | `working_history/version_1.1/Phase0_준비단계_{날짜}.md` |
| 확정된 DB 버전 및 Migration 번호 확인 | `AppDatabase.kt`에서 현재 버전 확인 |
| 기획서 섹션 3 (데이터 스키마 설계) 재확인 | [10_추가기능_할일관리기능.md#3](../10_추가기능_할일관리기능.md) |
| 현재 빌드 상태 확인 | `./gradlew assembleDebug` 성공 |

**이전 작업에서 확인할 내용**:
- [ ] 현재 Room DB 버전 번호
- [ ] 기존 스케줄 엔티티 구조 (TimeBasedAutoRun, LocationBasedAutoRun)
- [ ] 기존 Migration 파일 위치 및 네이밍 규칙

---


## 1.1 ScheduleInfo JSON 필드 추가

### 1.1.1 ScheduleInfo data class 생성

**파일**: `app/src/main/java/com/allday/detoxy/domain/model/ScheduleInfo.kt`

```kotlin
// MVP 단순화 버전 - JSON 직렬화
data class ScheduleInfo(
    val title: String = "",
    val description: String = "",  // MVP: 항상 빈 문자열
    val memo: String = "",         // MVP: 항상 빈 문자열 (null 대신 "" 사용)
    val todos: List<ScheduleTodo> = emptyList()
)

data class ScheduleTodo(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isRequired: Boolean = false,
    val orderIndex: Int = 0
) {
    companion object {
        const val MAX_TODO_COUNT = 5
        const val MAX_CONTENT_LENGTH = 100
    }
}
```

### 1.1.2 기존 엔티티 수정

**TimeBasedAutoRun.kt** 수정:
```kotlin
@Entity(tableName = "time_based_auto_run")
data class TimeBasedAutoRun(
    // ... 기존 필드 유지 ...
    
    // 🆕 스케줄 정보 (JSON 컬럼)
    // 🔧 8차 리뷰: nullable로 유지 (신규 필드 추가 시 기존 데이터 호환성)
    // 파싱 시 null -> ScheduleInfo() 빈 객체로 fallback 처리
    val scheduleInfoJson: String? = null
)
```

**LocationBasedAutoRun.kt** 동일 적용

### 1.1.3 TypeConverter 구현

**파일**: `app/src/main/java/com/allday/detoxy/data/local/converter/ScheduleInfoConverter.kt`

```kotlin
class ScheduleInfoConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromScheduleInfo(info: ScheduleInfo?): String? =
        info?.let { gson.toJson(it) }

    @TypeConverter
    fun toScheduleInfo(json: String?): ScheduleInfo? =
        json?.let {
            try { gson.fromJson(it, ScheduleInfo::class.java) }
            catch (e: Exception) { ScheduleInfo() }  // fallback
        }
}
```

---

## 1.2 세션 결과 테이블 생성

### 1.2.1 FocusSessionTodoResultEntity

**파일**: `app/src/main/java/com/allday/detoxy/data/local/entity/FocusSessionTodoResultEntity.kt`

```kotlin
@Entity(tableName = "focus_session_todo_result")
data class FocusSessionTodoResultEntity(
    @PrimaryKey
    val sessionId: String,
    val scheduleId: String,
    val scheduleType: ScheduleType,
    val scheduleTitleSnapshot: String,
    val todoResultsJson: String,  // List<TodoStatus> JSON
    val completedAt: Long,
    val lastModifiedAt: Long? = null
)

enum class ScheduleType {
    TIME_BASED,
    LOCATION_BASED
}

enum class TodoCompletionStatus {
    COMPLETED,
    NOT_COMPLETED,
    NO_RESPONSE
}
```

### 1.2.2 Enum TypeConverters

```kotlin
class ScheduleTypeConverter {
    @TypeConverter
    fun fromScheduleType(type: ScheduleType): String = type.name
    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)
}

class TodoCompletionStatusConverter {
    @TypeConverter
    fun fromStatus(status: TodoCompletionStatus): String = status.name
    @TypeConverter
    fun toStatus(value: String): TodoCompletionStatus = TodoCompletionStatus.valueOf(value)
}
```

---

## 1.3 Migration & DAO

### 1.3.1 Migration 작성

**파일**: `app/src/main/java/com/allday/detoxy/data/local/migration/Migration_X_Y.kt`

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. TimeBasedAutoRun에 JSON 컬럼 추가
        db.execSQL("ALTER TABLE time_based_auto_run ADD COLUMN scheduleInfoJson TEXT")
        
        // 2. LocationBasedAutoRun에 JSON 컬럼 추가
        db.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN scheduleInfoJson TEXT")
        
        // 3. focus_session_todo_result 테이블 생성
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS focus_session_todo_result (
                sessionId TEXT PRIMARY KEY NOT NULL,
                scheduleId TEXT NOT NULL,
                scheduleType TEXT NOT NULL,
                scheduleTitleSnapshot TEXT NOT NULL,
                todoResultsJson TEXT NOT NULL,
                completedAt INTEGER NOT NULL,
                lastModifiedAt INTEGER
            )
        """)
    }
}
```

### 1.3.2 FocusSessionTodoResultDao

```kotlin
@Dao
interface FocusSessionTodoResultDao {
    @Query("SELECT * FROM focus_session_todo_result WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): FocusSessionTodoResultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: FocusSessionTodoResultEntity)
    
    @Query("UPDATE focus_session_todo_result SET todoResultsJson = :json, lastModifiedAt = :time WHERE sessionId = :sessionId")
    suspend fun updateResults(sessionId: String, json: String, time: Long)
}
```

### 1.3.3 AppDatabase 수정

```kotlin
@Database(
    entities = [
        // ... 기존 엔티티 ...
        FocusSessionTodoResultEntity::class
    ],
    version = Y
)
@TypeConverters(
    ScheduleInfoConverter::class,
    ScheduleTypeConverter::class,
    TodoCompletionStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    // ... 기존 DAO ...
    abstract fun focusSessionTodoResultDao(): FocusSessionTodoResultDao
}
```

---

## 검증 체크리스트

```bash
# Migration 테스트
./gradlew test --tests "*Migration*"

# DAO 테스트
./gradlew test --tests "*TodoResultDao*"

# 빌드 확인
./gradlew assembleDebug
```

---

## 완료 기준

- [x] ScheduleInfo data class 생성
- [x] ScheduleInfoConverter 구현
- [x] TimeBasedAutoRun에 scheduleInfoJson 컬럼 추가
- [x] LocationBasedAutoRun에 scheduleInfoJson 컬럼 추가
- [x] FocusSessionTodoResultEntity 생성
- [x] Enum TypeConverters 구현
- [x] Migration 작성 및 테스트 통과 (v8 → v9)
- [x] DB 스키마 변경 후 앱 정상 실행 (BUILD SUCCESSFUL)

---

## 📤 사후 작업 (Post-Work)

### 작업결과서 작성

**저장 경로**: `working_history/version_1.1/Phase1_데이터레이어확장_{YYYY-MM-DD}.md`

**필수 포함 내용**:
- [ ] 생성된 Entity 클래스 목록 및 파일 경로
- [ ] 생성된 DAO 인터페이스 목록
- [ ] Migration 버전 정보 (X → Y)
- [ ] TypeConverter 목록
- [ ] 검증 결과 (테스트 통과 여부)

### 다음 Phase를 위한 정보

| 항목 | 값 |
|------|-----|
| 새 DB 버전 | Y |
| ScheduleInfo 클래스 경로 | `domain/model/ScheduleInfo.kt` |
| FocusSessionTodoResultEntity 경로 | `data/local/entity/...` |
| FocusSessionTodoResultDao 경로 | `data/local/dao/...` |

> Phase 2에서 위 정보를 참고하여 UseCase 및 검증 로직 구현

---

*작성일: 2025-12-25 · Phase 1 상세 작업 지시서*

