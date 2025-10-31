package com.allday.detoxy.data.local.migrations

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
import kotlin.test.assertTrue

/**
 * Migration_5_6 통합 테스트
 *
 * v5 → v6 마이그레이션 안정성 검증:
 * 1. ScheduleGroup 새 컬럼 확인 (iconType, colorHex, lastActivatedAt)
 * 2. LocationBasedAutoRun 새 컬럼 확인 (activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType)
 * 3. TimeBasedAutoRun 새 컬럼 확인 (groupPriority)
 * 4. 기본값 검증 (iconType="WORK", colorHex="#4CAF50", activateScheduleOnEnter=0, 등)
 * 5. 기존 데이터 무손실
 *
 * @see MIGRATION_5_6
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_5_6 {

    private val TEST_DB = "migration_test_5_6"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_addsNewFields() {
        // v5 DB 생성 및 샘플 데이터 삽입
        helper.createDatabase(TEST_DB, 5).apply {
            // ScheduleGroup 샘플 데이터
            execSQL("""
                INSERT INTO schedule_group 
                (id, name, description, isActive, createdAt)
                VALUES 
                ('test_group', '테스트 시간표', '테스트용 시간표', 0, ${System.currentTimeMillis()})
            """.trimIndent())

            // LocationBasedAutoRun 샘플 데이터
            execSQL("""
                INSERT INTO location_based_auto_run 
                (id, label, address, latitude, longitude, radiusMeters, durationMinutes, presetType, triggerType, periodicIntervalMinutes, dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt, linkedScheduleGroupId)
                VALUES 
                ('test_loc_1', '회사', '서울시 강남구', 37.5, 127.0, 300, 25, 'BALANCED', 'ENTER', NULL, 5, 0, 1, ${System.currentTimeMillis()}, 'test_group')
            """.trimIndent())

            // TimeBasedAutoRun 샘플 데이터
            execSQL("""
                INSERT INTO time_based_auto_run 
                (id, hour, minute, durationMinutes, presetType, enabledDays, label, isEnabled, createdAt, scheduleGroupId, isIndependent)
                VALUES 
                ('test_time_1', 10, 0, 25, 'STRICT', '["MON","TUE"]', '오전 업무', 1, ${System.currentTimeMillis()}, 'test_group', 0)
            """.trimIndent())

            close()
        }

        // v5 → v6 마이그레이션 실행 및 검증
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // 1. ScheduleGroup 새 컬럼 확인
        db.query("PRAGMA table_info(schedule_group)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndex("name")))
            }
            assertTrue(columns.contains("iconType"), "iconType column should exist")
            assertTrue(columns.contains("colorHex"), "colorHex column should exist")
            assertTrue(columns.contains("lastActivatedAt"), "lastActivatedAt column should exist")
        }

        // 2. ScheduleGroup 기본값 확인
        db.query("SELECT iconType, colorHex, lastActivatedAt FROM schedule_group WHERE id = 'test_group'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "test_group should exist")
            assertEquals("WORK", cursor.getString(0), "iconType default should be 'WORK'")
            assertEquals("#4CAF50", cursor.getString(1), "colorHex default should be '#4CAF50'")
            assertTrue(cursor.isNull(2), "lastActivatedAt should be NULL for existing data")
        }

        // 3. LocationBasedAutoRun 새 컬럼 확인
        db.query("PRAGMA table_info(location_based_auto_run)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndex("name")))
            }
            assertTrue(columns.contains("activateScheduleOnEnter"), "activateScheduleOnEnter column should exist")
            assertTrue(columns.contains("deactivateScheduleOnExit"), "deactivateScheduleOnExit column should exist")
            assertTrue(columns.contains("exitActionType"), "exitActionType column should exist")
        }

        // 4. LocationBasedAutoRun 기본값 확인
        db.query("SELECT activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType FROM location_based_auto_run WHERE id = 'test_loc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "test_loc_1 should exist")
            assertEquals(0, cursor.getInt(0), "activateScheduleOnEnter default should be 0 (false)")
            assertEquals(0, cursor.getInt(1), "deactivateScheduleOnExit default should be 0 (false)")
            assertEquals("DEACTIVATE", cursor.getString(2), "exitActionType default should be 'DEACTIVATE'")
        }

        // 5. TimeBasedAutoRun 새 컬럼 확인
        db.query("PRAGMA table_info(time_based_auto_run)").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndex("name")))
            }
            assertTrue(columns.contains("groupPriority"), "groupPriority column should exist")
        }

        // 6. TimeBasedAutoRun 기본값 확인
        db.query("SELECT groupPriority FROM time_based_auto_run WHERE id = 'test_time_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "test_time_1 should exist")
            assertEquals(0, cursor.getInt(0), "groupPriority default should be 0")
        }

        // 7. 기존 데이터 무손실 확인
        db.query("SELECT id, name FROM schedule_group WHERE id = 'test_group'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "Existing ScheduleGroup data should be preserved")
            assertEquals("테스트 시간표", cursor.getString(1), "ScheduleGroup name should be preserved")
        }

        db.query("SELECT id, label FROM location_based_auto_run WHERE id = 'test_loc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "Existing LocationBasedAutoRun data should be preserved")
            assertEquals("회사", cursor.getString(1), "LocationBasedAutoRun label should be preserved")
        }

        db.query("SELECT id, hour, minute FROM time_based_auto_run WHERE id = 'test_time_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "Existing TimeBasedAutoRun data should be preserved")
            assertEquals(10, cursor.getInt(1), "TimeBasedAutoRun hour should be preserved")
            assertEquals(0, cursor.getInt(2), "TimeBasedAutoRun minute should be preserved")
        }

        db.close()
    }

    @Test
    fun migrate5To6_insertsNewFields() {
        // v5 DB 생성
        helper.createDatabase(TEST_DB, 5).apply {
            close()
        }

        // v5 → v6 마이그레이션 실행
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // v6 필드 포함 데이터 삽입 테스트
        db.execSQL("""
            INSERT INTO schedule_group 
            (id, name, description, isActive, createdAt, iconType, colorHex, lastActivatedAt)
            VALUES 
            ('group_v6', 'v6 시간표', NULL, 1, ${System.currentTimeMillis()}, 'STUDY', '#FF5722', ${System.currentTimeMillis()})
        """.trimIndent())

        db.execSQL("""
            INSERT INTO location_based_auto_run 
            (id, label, address, latitude, longitude, radiusMeters, durationMinutes, presetType, triggerType, periodicIntervalMinutes, dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt, linkedScheduleGroupId, activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType)
            VALUES 
            ('loc_v6', '도서관', '서울시', 37.6, 127.1, 200, 30, 'STRICT', 'ENTER', NULL, 3, 0, 1, ${System.currentTimeMillis()}, 'group_v6', 1, 1, 'DEACTIVATE')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO time_based_auto_run 
            (id, hour, minute, durationMinutes, presetType, enabledDays, label, isEnabled, createdAt, scheduleGroupId, isIndependent, groupPriority)
            VALUES 
            ('time_v6', 14, 0, 30, 'STRICT', '["MON","TUE","WED"]', '오후 집중', 1, ${System.currentTimeMillis()}, 'group_v6', 0, 5)
        """.trimIndent())

        // v6 필드 포함 데이터 확인
        db.query("SELECT iconType, colorHex, lastActivatedAt FROM schedule_group WHERE id = 'group_v6'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("STUDY", cursor.getString(0))
            assertEquals("#FF5722", cursor.getString(1))
            assertNotNull(cursor.getLong(2))
        }

        db.query("SELECT activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType FROM location_based_auto_run WHERE id = 'loc_v6'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("DEACTIVATE", cursor.getString(2))
        }

        db.query("SELECT groupPriority FROM time_based_auto_run WHERE id = 'time_v6'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
        }

        db.close()
    }
}

