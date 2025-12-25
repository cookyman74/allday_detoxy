package com.allday.detoxy.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 8 → 9: 할일 관리 기능 추가
 *
 * ## 변경 사항
 * - TimeBasedAutoRun 테이블에 scheduleInfoJson 필드 추가
 * - LocationBasedAutoRun 테이블에 scheduleInfoJson 필드 추가
 * - focus_session_todo_result 테이블 신규 생성
 *
 * ## 목적
 * 스케줄에 목표(title) + 할일(todos)을 추가하여 집중 목적을 명확화합니다.
 * 세션 종료 시 할일 완료 상태를 기록합니다.
 *
 * ## 새 필드 설명
 * - scheduleInfoJson: ScheduleInfo 객체를 JSON으로 직렬화하여 저장
 *   - null: 목표/할일 없음 (기존 스케줄 호환)
 *   - JSON: {"title":"...", "todos":[...]}
 *
 * ## 새 테이블 설명
 * - focus_session_todo_result: 세션 종료 시 할일 완료 상태 저장
 *   - sessionId: 세션 ID (PK)
 *   - scheduleId: 스케줄 ID (FK 없음, 스케줄 삭제 허용)
 *   - scheduleType: TIME_BASED / LOCATION_BASED
 *   - scheduleTitleSnapshot: 스케줄 제목 스냅샷
 *   - todoResultsJson: List<TodoStatus> JSON
 *   - completedAt: 완료 시각
 *   - lastModifiedAt: 수정 시각 (할일 관리 페이지에서 수정 시)
 *
 * @see com.allday.detoxy.domain.model.ScheduleInfo
 * @see com.allday.detoxy.data.local.entity.FocusSessionTodoResultEntity
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. TimeBasedAutoRun 테이블에 scheduleInfoJson 필드 추가
        db.execSQL(
            """
            ALTER TABLE time_based_auto_run 
            ADD COLUMN scheduleInfoJson TEXT DEFAULT NULL
            """.trimIndent()
        )

        // 2. LocationBasedAutoRun 테이블에 scheduleInfoJson 필드 추가
        db.execSQL(
            """
            ALTER TABLE location_based_auto_run 
            ADD COLUMN scheduleInfoJson TEXT DEFAULT NULL
            """.trimIndent()
        )

        // 3. focus_session_todo_result 테이블 생성
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS focus_session_todo_result (
                sessionId TEXT NOT NULL PRIMARY KEY,
                scheduleId TEXT NOT NULL,
                scheduleType TEXT NOT NULL,
                scheduleTitleSnapshot TEXT NOT NULL,
                todoResultsJson TEXT NOT NULL,
                completedAt INTEGER NOT NULL,
                lastModifiedAt INTEGER
            )
            """.trimIndent()
        )
    }
}
