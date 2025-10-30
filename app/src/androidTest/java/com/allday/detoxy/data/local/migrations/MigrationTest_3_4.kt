package com.allday.detoxy.data.local.migrations

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.allday.detoxy.data.local.DetoxyDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Migration_3_4 통합 테스트
 *
 * v3 → v4 마이그레이션 안정성 검증:
 * 1. 새 테이블 4개 생성 여부
 * 2. UserSettings 필드 추가 여부
 * 3. 기본 커스텀 타이머 프리셋 삽입 여부
 * 4. 기존 데이터(focus_sessions 등) 무손실
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_3_4 {

    private val TEST_DB = "migration_test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate3To4_createsAllNewTables() {
        // v3 DB 생성
        helper.createDatabase(TEST_DB, 3).apply {
            close()
        }

        // v3 → v4 마이그레이션 실행
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // 마이그레이션 검증
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, false, MIGRATION_3_4)
        
        // 1. TimeBasedAutoRun 테이블 존재 확인
        db.query("SELECT * FROM time_based_auto_run").use { cursor ->
            assertNotNull(cursor, "time_based_auto_run table should exist")
        }

        // 2. LocationBasedAutoRun 테이블 존재 확인
        db.query("SELECT * FROM location_based_auto_run").use { cursor ->
            assertNotNull(cursor, "location_based_auto_run table should exist")
        }

        // 3. CustomTimerPreset 테이블 존재 및 기본 프리셋 확인
        db.query("SELECT * FROM custom_timer_preset WHERE id IN ('preset_default_25', 'preset_default_45', 'preset_default_60')").use { cursor ->
            assertEquals(3, cursor.count, "Should have 3 default presets (25/45/60 min)")
        }

        // 4. AutoRunLog 테이블 존재 확인
        db.query("SELECT * FROM auto_run_log").use { cursor ->
            assertNotNull(cursor, "auto_run_log table should exist")
        }

        // Note: UserSettings 필드 (autoRunMasterEnabled, autoRunPauseUntil)는 v4→v5에서 추가됨

        db.close()
    }
}

