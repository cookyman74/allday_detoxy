package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 데이터베이스 마이그레이션 v3 → v4
 *
 * 2차 고도화: 자동 실행 기능 추가
 *
 * ## 변경 사항
 * 1. **TimeBasedAutoRun 테이블 생성**
 *    - 시간 기반 자동 실행 설정
 *    - 매일 특정 시간에 집중 모드 자동 시작
 *    - 요일별 활성화/비활성화
 *    - 인덱스: (hour, minute), (isEnabled)
 *
 * 2. **LocationBasedAutoRun 테이블 생성**
 *    - 위치 기반 자동 실행 설정
 *    - Geofencing 기반 자동 시작
 *    - GPS 정확도 향상 옵션 (체류 시간, 확인 필요)
 *    - 인덱스: (isEnabled)
 *
 * 3. **CustomTimerPreset 테이블 생성**
 *    - 사용자 정의 타이머 프리셋
 *    - 도넛 그래프로 생성한 커스텀 시간
 *    - 사용 횟수 추적
 *    - 인덱스: (displayOrder)
 *
 * 4. **AutoRunLog 테이블 생성**
 *    - 자동 실행 이력 로그
 *    - 성공/실패/건너뜀 기록
 *    - GPS 정확도 로깅 (위치 기반)
 *    - 인덱스: (triggerTime), (triggerType, triggerTime), (sessionId)
 *    - Foreign Key: sessionId → focus_sessions(id) ON DELETE SET NULL
 *
 * 5. **UserSettings 테이블 확장**
 *    - autoRunMasterEnabled: 자동 실행 마스터 토글 (기본: true)
 *    - autoRunPauseUntil: 자동 실행 일시중지 종료 시간 (nullable)
 *
 * ## 마이그레이션 전략
 * - 기존 데이터 영향 없음 (신규 테이블만 추가, UserSettings 필드 추가)
 * - 모든 필드에 NOT NULL 또는 DEFAULT 설정
 * - AutoRunLog는 FocusSession FK (ON DELETE SET NULL)
 *
 * @see com.allday.detoxy.data.local.entity.TimeBasedAutoRun
 * @see com.allday.detoxy.data.local.entity.LocationBasedAutoRun
 * @see com.allday.detoxy.data.local.entity.CustomTimerPreset
 * @see com.allday.detoxy.data.local.entity.AutoRunLog
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. TimeBasedAutoRun 테이블 생성
        db.execSQL(
            """
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
            """.trimIndent()
        )

        // 2. TimeBasedAutoRun 인덱스 생성
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_time_based_auto_run_hour_minute ON time_based_auto_run(hour, minute)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_time_based_auto_run_isEnabled ON time_based_auto_run(isEnabled)"
        )

        // 3. LocationBasedAutoRun 테이블 생성
        db.execSQL(
            """
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
            """.trimIndent()
        )

        // 4. LocationBasedAutoRun 인덱스 생성
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_location_based_auto_run_isEnabled ON location_based_auto_run(isEnabled)"
        )

        // 5. CustomTimerPreset 테이블 생성
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_timer_preset (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                durationMinutes INTEGER NOT NULL,
                presetType TEXT,
                usageCount INTEGER NOT NULL DEFAULT 0,
                displayOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // 6. CustomTimerPreset 인덱스 생성
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_timer_preset_displayOrder ON custom_timer_preset(displayOrder)"
        )

        // 7. AutoRunLog 테이블 생성 (FK: sessionId)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS auto_run_log (
                id TEXT PRIMARY KEY NOT NULL,
                triggerType TEXT NOT NULL,
                triggerSourceId TEXT NOT NULL,
                triggerTime INTEGER NOT NULL,
                result TEXT NOT NULL,
                failureReason TEXT,
                sessionId TEXT,
                gpsAccuracyMeters REAL,
                dwellSeconds INTEGER,
                metaJson TEXT,
                FOREIGN KEY(sessionId) REFERENCES focus_sessions(id) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        // 8. AutoRunLog 인덱스 생성
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_auto_run_log_triggerTime ON auto_run_log(triggerTime)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_auto_run_log_triggerType_triggerTime ON auto_run_log(triggerType, triggerTime)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_auto_run_log_sessionId ON auto_run_log(sessionId)"
        )

        // 9. UserSettings 테이블 확장 (새 필드 추가)
        db.execSQL(
            "ALTER TABLE user_settings ADD COLUMN autoRunMasterEnabled INTEGER NOT NULL DEFAULT 1"
        )
        db.execSQL(
            "ALTER TABLE user_settings ADD COLUMN autoRunPauseUntil INTEGER"
        )

        // 10. 기본 커스텀 타이머 프리셋 추가 (25/45/60분)
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT OR IGNORE INTO custom_timer_preset (id, name, durationMinutes, presetType, usageCount, displayOrder, createdAt)
            VALUES 
              ('preset_default_25', '25분', 25, NULL, 0, 0, $now),
              ('preset_default_45', '45분', 45, NULL, 0, 1, $now),
              ('preset_default_60', '60분', 60, NULL, 0, 2, $now)
            """.trimIndent()
        )
    }
}

