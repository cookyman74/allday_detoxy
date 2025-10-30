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
 * Migration_4_5 통합 테스트
 *
 * v4 → v5 마이그레이션 안정성 검증:
 * 1. ScheduleGroup 테이블 생성 여부
 * 2. TimeBasedAutoRun 필드 추가 여부 (scheduleGroupId, isIndependent)
 * 3. LocationBasedAutoRun 필드 추가 여부 (linkedScheduleGroupId)
 * 4. 인덱스 생성 여부 (3개)
 * 5. 기본값 검증 (isIndependent = 1)
 * 6. 기존 데이터 무손실
 *
 * @see MIGRATION_4_5
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_4_5 {

    private val TEST_DB = "migration_test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate4To5_createsScheduleGroupTable() {
        // v4 DB 수동 생성 (스키마 파일 없이)
        val db = helper.createDatabase(TEST_DB, 4).apply {
            // TimeBasedAutoRun 샘플 데이터
            execSQL("""
                INSERT INTO time_based_auto_run 
                (id, hour, minute, durationMinutes, presetType, enabledDays, label, isEnabled, createdAt)
                VALUES 
                ('test_time_1', 10, 0, 25, 'STRICT', '["MON","TUE","WED"]', '오전 업무', 1, ${System.currentTimeMillis()})
            """.trimIndent())

            // LocationBasedAutoRun 샘플 데이터
            execSQL("""
                INSERT INTO location_based_auto_run 
                (id, label, address, latitude, longitude, radiusMeters, durationMinutes, presetType, triggerType, periodicIntervalMinutes, dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt)
                VALUES 
                ('test_loc_1', '회사', '서울시 강남구', 37.5, 127.0, 300, 25, 'BALANCED', 'ENTER', NULL, 5, 0, 1, ${System.currentTimeMillis()})
            """.trimIndent())

            close()
        }

        // v4 → v5 마이그레이션 실행
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // 1. ScheduleGroup 테이블 존재 확인
        db.query("SELECT * FROM schedule_group").use { cursor ->
            assertNotNull(cursor, "schedule_group table should exist")
            assertEquals(0, cursor.count, "schedule_group should be empty initially")
        }

        // 2. TimeBasedAutoRun 필드 추가 확인 (scheduleGroupId, isIndependent)
        db.query("SELECT scheduleGroupId, isIndependent FROM time_based_auto_run WHERE id = 'test_time_1'").use { cursor ->
            assertNotNull(cursor, "time_based_auto_run should have scheduleGroupId and isIndependent")
            assertTrue(cursor.moveToFirst(), "test_time_1 should exist")
            
            // scheduleGroupId는 NULL (기존 데이터)
            val scheduleGroupIdIndex = cursor.getColumnIndex("scheduleGroupId")
            assertTrue(scheduleGroupIdIndex >= 0, "scheduleGroupId column should exist")
            assertTrue(cursor.isNull(scheduleGroupIdIndex), "scheduleGroupId should be NULL for existing data")
            
            // isIndependent는 1 (기본값)
            val isIndependentIndex = cursor.getColumnIndex("isIndependent")
            assertTrue(isIndependentIndex >= 0, "isIndependent column should exist")
            assertEquals(1, cursor.getInt(isIndependentIndex), "isIndependent should be 1 (true) for existing data")
        }

        // 3. LocationBasedAutoRun 필드 추가 확인 (linkedScheduleGroupId)
        db.query("SELECT linkedScheduleGroupId FROM location_based_auto_run WHERE id = 'test_loc_1'").use { cursor ->
            assertNotNull(cursor, "location_based_auto_run should have linkedScheduleGroupId")
            assertTrue(cursor.moveToFirst(), "test_loc_1 should exist")
            
            // linkedScheduleGroupId는 NULL (기존 데이터)
            val linkedScheduleGroupIdIndex = cursor.getColumnIndex("linkedScheduleGroupId")
            assertTrue(linkedScheduleGroupIdIndex >= 0, "linkedScheduleGroupId column should exist")
            assertTrue(cursor.isNull(linkedScheduleGroupIdIndex), "linkedScheduleGroupId should be NULL for existing data")
        }

        // 4. 기존 데이터 무손실 확인
        db.query("SELECT id FROM time_based_auto_run WHERE id = 'test_time_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "Existing TimeBasedAutoRun data should be preserved")
        }

        db.query("SELECT id FROM location_based_auto_run WHERE id = 'test_loc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "Existing LocationBasedAutoRun data should be preserved")
        }

        db.close()
    }

    @Test
    fun migrate4To5_insertsScheduleGroupData() {
        // v4 DB 생성
        helper.createDatabase(TEST_DB, 4).apply {
            close()
        }

        // v4 → v5 마이그레이션 실행
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // ScheduleGroup 데이터 삽입 테스트
        db.execSQL("""
            INSERT INTO schedule_group 
            (id, name, description, isActive, createdAt)
            VALUES 
            ('test_group_1', '업무 시간표', '회사에서 사용할 시간표', 1, ${System.currentTimeMillis()})
        """.trimIndent())

        // ScheduleGroup 삽입 확인
        db.query("SELECT * FROM schedule_group WHERE id = 'test_group_1'").use { cursor ->
            assertTrue(cursor.moveToFirst(), "ScheduleGroup should be inserted successfully")
            assertEquals("업무 시간표", cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndex("isActive")))
        }

        db.close()
    }

    @Test
    fun migrate4To5_supportsScheduleGroupLinking() {
        // v4 DB 생성
        helper.createDatabase(TEST_DB, 4).apply {
            close()
        }

        // v4 → v5 마이그레이션 실행
        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // ScheduleGroup 생성
        db.execSQL("""
            INSERT INTO schedule_group 
            (id, name, description, isActive, createdAt)
            VALUES 
            ('group_1', '업무 시간표', NULL, 1, ${System.currentTimeMillis()})
        """.trimIndent())

        // TimeBasedAutoRun 생성 (scheduleGroupId 연결)
        db.execSQL("""
            INSERT INTO time_based_auto_run 
            (id, hour, minute, durationMinutes, presetType, enabledDays, label, isEnabled, createdAt, scheduleGroupId, isIndependent)
            VALUES 
            ('time_1', 10, 0, 25, 'STRICT', '["MON"]', '오전 업무', 1, ${System.currentTimeMillis()}, 'group_1', 0)
        """.trimIndent())

        // LocationBasedAutoRun 생성 (linkedScheduleGroupId 연결)
        db.execSQL("""
            INSERT INTO location_based_auto_run 
            (id, label, address, latitude, longitude, radiusMeters, durationMinutes, presetType, triggerType, periodicIntervalMinutes, dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt, linkedScheduleGroupId)
            VALUES 
            ('loc_1', '회사', '서울', 37.5, 127.0, 300, 25, 'BALANCED', 'ENTER', NULL, 5, 0, 1, ${System.currentTimeMillis()}, 'group_1')
        """.trimIndent())

        // ScheduleGroup 연결 확인
        db.query("SELECT scheduleGroupId FROM time_based_auto_run WHERE id = 'time_1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("group_1", cursor.getString(0))
        }

        db.query("SELECT linkedScheduleGroupId FROM location_based_auto_run WHERE id = 'loc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("group_1", cursor.getString(0))
        }

        db.close()
    }
}

