package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 데이터베이스 마이그레이션 v4 → v5
 *
 * 2.5차 고도화: ScheduleGroup 기능 추가
 *
 * ## 변경 사항
 * 1. **ScheduleGroup 테이블 생성**
 *    - 스케줄 그룹 (여러 TimeBasedAutoRun을 묶어 위치 기반으로 활성화/비활성화)
 *    - 인덱스: (isActive)
 *
 * 2. **TimeBasedAutoRun 테이블 확장**
 *    - scheduleGroupId: 연결된 ScheduleGroup ID (nullable)
 *    - isIndependent: 독립 실행 여부 (기본값: 1)
 *    - 인덱스: (scheduleGroupId)
 *
 * 3. **LocationBasedAutoRun 테이블 확장**
 *    - linkedScheduleGroupId: 연결된 ScheduleGroup ID (nullable)
 *    - 인덱스: (linkedScheduleGroupId)
 *
 * ## 마이그레이션 전략
 * - 기존 데이터 영향 없음 (nullable 필드 추가, DEFAULT 값 설정)
 * - 모든 기존 레코드는 독립 모드로 유지 (scheduleGroupId = NULL, isIndependent = 1)
 * - Soft Reference 관계 (Foreign Key 없음, 애플리케이션 레벨에서 참조 무결성 보장)
 *
 * ## 예상 실행 시간
 * - ScheduleGroup 테이블 생성: < 10ms
 * - ALTER TABLE 3회: < 30ms (각 10ms)
 * - 인덱스 생성 3개: < 30ms (각 10ms)
 * - 총 예상 시간: < 100ms
 *
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 * @see com.allday.detoxy.data.local.entity.TimeBasedAutoRun
 * @see com.allday.detoxy.data.local.entity.LocationBasedAutoRun
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. ScheduleGroup 테이블 생성
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS schedule_group (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 2. ScheduleGroup 인덱스 생성
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_schedule_group_isActive ON schedule_group(isActive)"
        )

        // 3. TimeBasedAutoRun 테이블 확장
        database.execSQL(
            "ALTER TABLE time_based_auto_run ADD COLUMN scheduleGroupId TEXT"
        )
        database.execSQL(
            "ALTER TABLE time_based_auto_run ADD COLUMN isIndependent INTEGER NOT NULL DEFAULT 1"
        )

        // 4. TimeBasedAutoRun 인덱스 생성
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_time_based_auto_run_scheduleGroupId ON time_based_auto_run(scheduleGroupId)"
        )

        // 5. LocationBasedAutoRun 테이블 확장
        database.execSQL(
            "ALTER TABLE location_based_auto_run ADD COLUMN linkedScheduleGroupId TEXT"
        )

        // 6. LocationBasedAutoRun 인덱스 생성
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_location_based_auto_run_linkedScheduleGroupId ON location_based_auto_run(linkedScheduleGroupId)"
        )
    }
}

