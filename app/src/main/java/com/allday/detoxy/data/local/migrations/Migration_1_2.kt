package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 데이터베이스 마이그레이션: v1 → v2
 *
 * ## 변경 사항
 * 1. **FocusSession 테이블 확장**
 *    - `interruptedSeconds` 컬럼 추가 (중도 포기 시 경과 시간)
 *    - `primaryDistractionCategory` 컬럼 추가 (주요 방해요인 카테고리)
 *    - `giveUpReason` 컬럼 추가 (포기 사유)
 *
 * 2. **FocusInterruption 테이블 생성**
 *    - 세션 중 차단 이벤트 로그 저장
 *    - `sessionId` 외래키 (CASCADE 삭제)
 *    - `sessionId` 인덱스 (조회 성능 최적화)
 *
 * ## 데이터 백필
 * - 기존 세션의 `interruptedSeconds`는 0으로 초기화
 * - 기존 세션의 `primaryDistractionCategory`, `giveUpReason`은 NULL
 *
 * @see com.allday.detoxy.data.local.entity.FocusSession
 * @see com.allday.detoxy.data.local.entity.FocusInterruption
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. FocusSession 테이블에 컬럼 추가
        db.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN interruptedSeconds INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN primaryDistractionCategory TEXT"
        )
        db.execSQL(
            "ALTER TABLE focus_sessions ADD COLUMN giveUpReason TEXT"
        )

        // 2. FocusInterruption 테이블 생성
        db.execSQL(
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

        // 3. FocusInterruption 테이블 인덱스 생성 (쿼리 성능 최적화)
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_focus_interruptions_sessionId ON focus_interruptions(sessionId)"
        )
    }
}

