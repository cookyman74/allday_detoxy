package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 데이터베이스 마이그레이션 v2 → v3
 *
 * Week 2B: 고급 데이터 모델 추가
 *
 * ## 변경 사항
 * 1. **FocusDistraction 테이블 생성**
 *    - 허용/차단 이벤트 전체 로그
 *    - UsageStats 데이터 연동 (dwellTimeSeconds)
 *    - sessionId는 nullable (세션 외에도 기록 가능)
 *
 * 2. **DetoxyRoutineLog 테이블 생성**
 *    - 루틴 실행/실패 기록
 *    - scheduledTime, executedTime, success, failureReason
 *
 * 3. **FocusSettings 테이블 생성**
 *    - 카테고리별 차단 설정 저장
 *    - Singleton (ID=1)
 *    - 기본 설정 자동 삽입
 *
 * 4. **인덱스 생성**
 *    - FocusDistraction: sessionId, timestamp
 *
 * ## 마이그레이션 전략
 * - 기존 데이터 영향 없음 (신규 테이블만 추가)
 * - 모든 필드에 NOT NULL 또는 DEFAULT 설정
 * - FocusSettings 기본 레코드 자동 삽입
 *
 * @see com.allday.detoxy.data.local.entity.FocusDistraction
 * @see com.allday.detoxy.data.local.entity.DetoxyRoutineLog
 * @see com.allday.detoxy.data.local.entity.FocusSettings
 */
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

        // 4. 기본 설정 삽입 (Singleton) - 표준 디톡시 프리셋
        //    SNS, WEB, VIDEO: 차단 (1)
        //    MESSENGER, OTHER: 허용 (0)
        database.execSQL(
            """
            INSERT OR IGNORE INTO focus_settings (id, snsEnabled, messengerEnabled, webEnabled, videoEnabled, otherEnabled, lastUpdated)
            VALUES (1, 1, 0, 1, 1, 0, ${System.currentTimeMillis()})
            """.trimIndent()
        )

        // 5. FocusDistraction 인덱스 생성
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_distractions_sessionId ON focus_distractions(sessionId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_distractions_timestamp ON focus_distractions(timestamp)"
        )
    }
}

