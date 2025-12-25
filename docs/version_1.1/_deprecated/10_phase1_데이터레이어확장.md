# Phase 1. 데이터 레이어 확장 상세 작업 지시서

> 상위 문서: [할 일 기능 작업 계획](./10_추가_할일기능_todolist.md)  
> 기능 설계: [할 일 기능 설계 문서](./10_추가기능_할일관리기능.md)

---

## 1.1 Schema & Migration

### 1.1.1 ScheduleInfo Embedded 클래스 생성

**파일**: `app/src/main/java/com/allday/detoxy/data/local/entity/ScheduleInfo.kt`

```kotlin
package com.allday.detoxy.data.local.entity

/**
 * 스케줄 공통 정보를 담는 Embedded 클래스
 * TimeBasedAutoRun, LocationBasedAutoRun 엔티티에 포함됨
 */
data class ScheduleInfo(
    val scheduleTitle: String = "",        // 스케줄 제목
    val scheduleDescription: String = "",   // 스케줄 설명
    val scheduleMemo: String? = null        // 메모 (선택)
)
```

> **주의**: Room `@Embedded` 사용 시 컬럼명 충돌 방지를 위해 `schedule` prefix 사용

### 1.1.2 기존 엔티티 수정

**TimeBasedAutoRun.kt** 수정:
```kotlin
@Entity(tableName = "time_based_auto_run")
data class TimeBasedAutoRun(
    // ... 기존 필드 유지 ...
    
    // 🆕 스케줄 정보 (Embedded)
    @Embedded
    val scheduleInfo: ScheduleInfo = ScheduleInfo()
)
```

**LocationBasedAutoRun.kt** 동일 적용

### 1.1.3 새 테이블 엔티티 생성

**파일**: `app/src/main/java/com/allday/detoxy/data/local/entity/ScheduleTodoItem.kt`

```kotlin
@Entity(
    tableName = "schedule_todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TimeBasedAutoRun::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId")]
)
data class ScheduleTodoItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scheduleId: String,           // TimeBasedAutoRun 또는 LocationBasedAutoRun의 ID
    val scheduleType: String,         // "TIME" 또는 "LOCATION"
    val content: String,              // 할 일 내용 (1~120자)
    val isRequired: Boolean = false,  // 필수 여부
    val orderIndex: Int = 0,          // 정렬 순서
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**파일**: `app/src/main/java/com/allday/detoxy/data/local/entity/FocusSessionTodoProgress.kt`

```kotlin
@Entity(
    tableName = "focus_session_todo_progress",
    primaryKeys = ["sessionId", "todoId"],
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class FocusSessionTodoProgress(
    val sessionId: String,
    val todoId: String,
    val checked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
```

**파일**: `app/src/main/java/com/allday/detoxy/data/local/entity/FocusSessionTodoResult.kt`

```kotlin
@Entity(
    tableName = "focus_session_todo_result",
    primaryKeys = ["sessionId", "todoId"],
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class FocusSessionTodoResult(
    val sessionId: String,
    val todoId: String,
    val todoContentSnapshot: String,    // 스케줄 편집 시 참조 깨짐 방지
    val isRequiredSnapshot: Boolean,    // 필수 여부 스냅샷
    val completed: Boolean = false,
    val completedAt: Long? = null
)
```

### 1.1.4 Migration 작성

**파일**: `app/src/main/java/com/allday/detoxy/data/local/migration/Migration_X_Y.kt`

> X, Y는 현재 DB 버전에 따라 결정 (예: 8_9)

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. TimeBasedAutoRun에 ScheduleInfo 컬럼 추가
        db.execSQL("ALTER TABLE time_based_auto_run ADD COLUMN scheduleTitle TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE time_based_auto_run ADD COLUMN scheduleDescription TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE time_based_auto_run ADD COLUMN scheduleMemo TEXT")
        
        // 2. LocationBasedAutoRun에 ScheduleInfo 컬럼 추가
        db.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN scheduleTitle TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN scheduleDescription TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE location_based_auto_run ADD COLUMN scheduleMemo TEXT")
        
        // 3. schedule_todo_items 테이블 생성
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS schedule_todo_items (
                id TEXT PRIMARY KEY NOT NULL,
                scheduleId TEXT NOT NULL,
                scheduleType TEXT NOT NULL,
                content TEXT NOT NULL,
                isRequired INTEGER NOT NULL DEFAULT 0,
                orderIndex INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_todo_items_scheduleId ON schedule_todo_items(scheduleId)")
        
        // 4. focus_session_todo_progress 테이블 생성
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS focus_session_todo_progress (
                sessionId TEXT NOT NULL,
                todoId TEXT NOT NULL,
                checked INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(sessionId, todoId),
                FOREIGN KEY(sessionId) REFERENCES focus_sessions(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_session_todo_progress_sessionId ON focus_session_todo_progress(sessionId)")
        
        // 5. focus_session_todo_result 테이블 생성
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS focus_session_todo_result (
                sessionId TEXT NOT NULL,
                todoId TEXT NOT NULL,
                todoContentSnapshot TEXT NOT NULL,
                isRequiredSnapshot INTEGER NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                PRIMARY KEY(sessionId, todoId),
                FOREIGN KEY(sessionId) REFERENCES focus_sessions(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_session_todo_result_sessionId ON focus_session_todo_result(sessionId)")
    }
}
```

### 1.1.5 AppDatabase 수정

```kotlin
@Database(
    entities = [
        // ... 기존 엔티티 ...
        ScheduleTodoItem::class,
        FocusSessionTodoProgress::class,
        FocusSessionTodoResult::class
    ],
    version = Y,  // 버전 증가
    // ...
)
abstract class AppDatabase : RoomDatabase() {
    // ... 기존 DAO ...
    abstract fun scheduleTodoItemDao(): ScheduleTodoItemDao
    abstract fun focusSessionTodoProgressDao(): FocusSessionTodoProgressDao
    abstract fun focusSessionTodoResultDao(): FocusSessionTodoResultDao
}
```

---

## 1.2 DAO / Repository

### 1.2.1 ScheduleTodoItemDao

**파일**: `app/src/main/java/com/allday/detoxy/data/local/dao/ScheduleTodoItemDao.kt`

```kotlin
@Dao
interface ScheduleTodoItemDao {
    
    @Query("SELECT * FROM schedule_todo_items WHERE scheduleId = :scheduleId ORDER BY orderIndex ASC")
    fun getByScheduleId(scheduleId: String): Flow<List<ScheduleTodoItem>>
    
    @Query("SELECT * FROM schedule_todo_items WHERE scheduleId = :scheduleId ORDER BY orderIndex ASC")
    suspend fun getByScheduleIdOnce(scheduleId: String): List<ScheduleTodoItem>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleTodoItem>)
    
    @Update
    suspend fun update(item: ScheduleTodoItem)
    
    @Query("DELETE FROM schedule_todo_items WHERE scheduleId = :scheduleId")
    suspend fun deleteByScheduleId(scheduleId: String)
    
    @Query("DELETE FROM schedule_todo_items WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Transaction
    suspend fun replaceAll(scheduleId: String, items: List<ScheduleTodoItem>) {
        deleteByScheduleId(scheduleId)
        insertAll(items)
    }
}
```

### 1.2.2 FocusSessionTodoProgressDao

```kotlin
@Dao
interface FocusSessionTodoProgressDao {
    
    @Query("SELECT * FROM focus_session_todo_progress WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: String): Flow<List<FocusSessionTodoProgress>>
    
    @Query("SELECT * FROM focus_session_todo_progress WHERE sessionId = :sessionId")
    suspend fun getBySessionIdOnce(sessionId: String): List<FocusSessionTodoProgress>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: FocusSessionTodoProgress)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<FocusSessionTodoProgress>)
    
    @Query("DELETE FROM focus_session_todo_progress WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
    
    @Query("UPDATE focus_session_todo_progress SET checked = :checked, updatedAt = :updatedAt WHERE sessionId = :sessionId AND todoId = :todoId")
    suspend fun updateChecked(sessionId: String, todoId: String, checked: Boolean, updatedAt: Long = System.currentTimeMillis())
}
```

### 1.2.3 FocusSessionTodoResultDao

```kotlin
@Dao
interface FocusSessionTodoResultDao {
    
    @Query("SELECT * FROM focus_session_todo_result WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: String): Flow<List<FocusSessionTodoResult>>
    
    @Query("SELECT * FROM focus_session_todo_result WHERE sessionId = :sessionId")
    suspend fun getBySessionIdOnce(sessionId: String): List<FocusSessionTodoResult>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<FocusSessionTodoResult>)
    
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN isRequiredSnapshot = 1 THEN 1 ELSE 0 END) as requiredTotal,
            SUM(CASE WHEN isRequiredSnapshot = 1 AND completed = 1 THEN 1 ELSE 0 END) as requiredCompleted
        FROM focus_session_todo_result 
        WHERE sessionId = :sessionId
    """)
    suspend fun getCompletionStats(sessionId: String): TodoCompletionStats
}

data class TodoCompletionStats(
    val total: Int,
    val completed: Int,
    val requiredTotal: Int,
    val requiredCompleted: Int
)
```

### 1.2.4 Repository 구현

**파일**: `app/src/main/java/com/allday/detoxy/data/repository/ScheduleTodoRepository.kt`

```kotlin
@Singleton
class ScheduleTodoRepository @Inject constructor(
    private val todoItemDao: ScheduleTodoItemDao,
    private val progressDao: FocusSessionTodoProgressDao,
    private val resultDao: FocusSessionTodoResultDao
) {
    
    // === Todo Items (스케줄 정의) ===
    
    fun getTodoItems(scheduleId: String): Flow<List<ScheduleTodoItem>> =
        todoItemDao.getByScheduleId(scheduleId)
    
    suspend fun saveTodoItems(scheduleId: String, scheduleType: String, items: List<ScheduleTodoItem>) {
        val itemsWithScheduleId = items.mapIndexed { index, item ->
            item.copy(
                scheduleId = scheduleId,
                scheduleType = scheduleType,
                orderIndex = index,
                updatedAt = System.currentTimeMillis()
            )
        }
        todoItemDao.replaceAll(scheduleId, itemsWithScheduleId)
    }
    
    // === Progress (세션 중 진행 상태) ===
    
    suspend fun initializeProgress(sessionId: String, scheduleId: String) {
        val todoItems = todoItemDao.getByScheduleIdOnce(scheduleId)
        val progressList = todoItems.map { item ->
            FocusSessionTodoProgress(
                sessionId = sessionId,
                todoId = item.id,
                checked = false,
                updatedAt = System.currentTimeMillis()
            )
        }
        progressDao.insertAll(progressList)
    }
    
    fun getProgress(sessionId: String): Flow<List<FocusSessionTodoProgress>> =
        progressDao.getBySessionId(sessionId)
    
    suspend fun updateProgress(sessionId: String, todoId: String, checked: Boolean) {
        progressDao.updateChecked(sessionId, todoId, checked)
    }
    
    // === Result (세션 종료 후 결과) ===
    
    suspend fun finalizeResults(sessionId: String, scheduleId: String) {
        val todoItems = todoItemDao.getByScheduleIdOnce(scheduleId)
        val progressList = progressDao.getBySessionIdOnce(sessionId)
        val progressMap = progressList.associateBy { it.todoId }
        
        val results = todoItems.map { item ->
            val progress = progressMap[item.id]
            FocusSessionTodoResult(
                sessionId = sessionId,
                todoId = item.id,
                todoContentSnapshot = item.content,
                isRequiredSnapshot = item.isRequired,
                completed = progress?.checked ?: false,
                completedAt = if (progress?.checked == true) progress.updatedAt else null
            )
        }
        
        resultDao.insertAll(results)
        progressDao.deleteBySessionId(sessionId)  // Progress 정리
    }
    
    suspend fun getCompletionStats(sessionId: String): TodoCompletionStats =
        resultDao.getCompletionStats(sessionId)
}
```

---

## 1.3 Converter & Snapshot

### 1.3.1 도메인 모델

**파일**: `app/src/main/java/com/allday/detoxy/domain/model/ScheduleTodo.kt`

```kotlin
/**
 * UI/도메인 레이어에서 사용하는 Todo 모델
 * Entity와 분리하여 UI 로직에 필요한 필드만 포함
 */
data class ScheduleTodo(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isRequired: Boolean = false,
    val orderIndex: Int = 0,
    val isChecked: Boolean = false  // UI 표시용 (Progress에서 매핑)
)
```

### 1.3.2 Mapper

**파일**: `app/src/main/java/com/allday/detoxy/data/mapper/ScheduleTodoMapper.kt`

```kotlin
fun ScheduleTodoItem.toDomain(isChecked: Boolean = false): ScheduleTodo =
    ScheduleTodo(
        id = id,
        content = content,
        isRequired = isRequired,
        orderIndex = orderIndex,
        isChecked = isChecked
    )

fun ScheduleTodo.toEntity(scheduleId: String, scheduleType: String): ScheduleTodoItem =
    ScheduleTodoItem(
        id = id,
        scheduleId = scheduleId,
        scheduleType = scheduleType,
        content = content,
        isRequired = isRequired,
        orderIndex = orderIndex
    )

fun List<ScheduleTodoItem>.toDomainList(progressMap: Map<String, Boolean> = emptyMap()): List<ScheduleTodo> =
    map { it.toDomain(progressMap[it.id] ?: false) }
```

---

## 검증 체크리스트

### Migration 테스트
```bash
./gradlew test --tests "com.allday.detoxy.data.local.migration.*"
```

### DAO 테스트
```bash
./gradlew test --tests "com.allday.detoxy.data.local.dao.ScheduleTodoItemDaoTest"
./gradlew test --tests "com.allday.detoxy.data.local.dao.FocusSessionTodo*Test"
```

### Repository 테스트
```bash
./gradlew test --tests "com.allday.detoxy.data.repository.ScheduleTodoRepositoryTest"
```

### 통합 빌드 확인
```bash
./gradlew assembleDebug
```

---

## 작업 완료 후 문서화

작업 완료 시 아래 파일 생성:
```
working_history/version_2.0/{작업날짜}_phase1_데이터레이어확장.md
```

**포함 내용**:
- 작업 범위 및 변경 파일 목록
- DB 스키마 변경 사항 (테이블/컬럼)
- Migration 버전 정보
- 검증 명령 및 결과
- 발생한 이슈 및 해결 방법

---
*작성일: 2025-12-07 · Phase 1 상세 작업 지시서*

