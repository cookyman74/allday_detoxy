package com.allday.detoxy.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.allday.detoxy.data.local.DetoxyDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration 6 → 7 테스트
 *
 * updatedAt 필드 추가 검증:
 * - ScheduleGroup 테이블에 updatedAt 컬럼 추가
 * - LocationBasedAutoRun 테이블에 updatedAt 컬럼 추가
 * - TimeBasedAutoRun 테이블에 updatedAt 컬럼 추가
 *
 * ## 테스트 시나리오
 * 1. v6 데이터베이스 생성
 * 2. 각 테이블에 테스트 데이터 삽입
 * 3. v7로 마이그레이션 실행
 * 4. updatedAt 컬럼 존재 및 기본값 검증
 * 5. 기존 데이터 보존 검증
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_6_7 {

    companion object {
        private const val TEST_DB = "migration_test"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DetoxyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate6To7_addsUpdatedAtToScheduleGroup() {
        // 1. v6 데이터베이스 생성 및 테스트 데이터 삽입
        val db = helper.createDatabase(TEST_DB, 6)
        val testId = "test_schedule_group_1"
        val testName = "Test Schedule"
        val testCreatedAt = System.currentTimeMillis()

        db.execSQL(
            """
            INSERT INTO schedule_group (id, name, description, isActive, createdAt, iconType, colorHex, lastActivatedAt)
            VALUES ('$testId', '$testName', 'Test Description', 1, $testCreatedAt, 'WORK', '#4CAF50', NULL)
            """.trimIndent()
        )
        db.close()

        // 2. v7로 마이그레이션 실행
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // 3. updatedAt 컬럼 존재 확인
        val cursor = dbV7.query("SELECT id, name, updatedAt FROM schedule_group WHERE id = '$testId'")
        assertTrue("Should have one record", cursor.moveToFirst())

        // 4. 기존 데이터 보존 확인
        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        assertEquals("ID should be preserved", testId, id)
        assertEquals("Name should be preserved", testName, name)

        // 5. updatedAt 기본값 확인 (0보다 큰 timestamp)
        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
        assertTrue("updatedAt should be greater than 0", updatedAt > 0)

        cursor.close()
        dbV7.close()
    }

    @Test
    fun migrate6To7_addsUpdatedAtToLocationBasedAutoRun() {
        // 1. v6 데이터베이스 생성 및 테스트 데이터 삽입
        val db = helper.createDatabase(TEST_DB, 6)
        val testId = "test_location_1"
        val testLabel = "Test Location"
        val testCreatedAt = System.currentTimeMillis()

        db.execSQL(
            """
            INSERT INTO location_based_auto_run (
                id, label, address, latitude, longitude, radiusMeters, 
                durationMinutes, presetType, triggerType, periodicIntervalMinutes,
                dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt,
                linkedScheduleGroupId, activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType
            )
            VALUES (
                '$testId', '$testLabel', 'Test Address', 37.123456, 127.123456, 100,
                60, 'STANDARD', 'ENTER', NULL,
                0, 0, 1, $testCreatedAt,
                NULL, 0, 0, 'DEACTIVATE'
            )
            """.trimIndent()
        )
        db.close()

        // 2. v7로 마이그레이션 실행
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // 3. updatedAt 컬럼 존재 확인
        val cursor = dbV7.query("SELECT id, label, updatedAt FROM location_based_auto_run WHERE id = '$testId'")
        assertTrue("Should have one record", cursor.moveToFirst())

        // 4. 기존 데이터 보존 확인
        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
        assertEquals("ID should be preserved", testId, id)
        assertEquals("Label should be preserved", testLabel, label)

        // 5. updatedAt 기본값 확인
        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
        assertTrue("updatedAt should be greater than 0", updatedAt > 0)

        cursor.close()
        dbV7.close()
    }

    @Test
    fun migrate6To7_addsUpdatedAtToTimeBasedAutoRun() {
        // 1. v6 데이터베이스 생성 및 테스트 데이터 삽입
        val db = helper.createDatabase(TEST_DB, 6)
        val testId = "test_time_1"
        val testLabel = "Test Time"
        val testCreatedAt = System.currentTimeMillis()

        db.execSQL(
            """
            INSERT INTO time_based_auto_run (
                id, hour, minute, durationMinutes, presetType, enabledDays,
                label, isEnabled, createdAt, scheduleGroupId, isIndependent, groupPriority
            )
            VALUES (
                '$testId', 14, 0, 60, 'STANDARD', '["MON","TUE","WED","THU","FRI"]',
                '$testLabel', 1, $testCreatedAt, NULL, 1, 0
            )
            """.trimIndent()
        )
        db.close()

        // 2. v7로 마이그레이션 실행
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // 3. updatedAt 컬럼 존재 확인
        val cursor = dbV7.query("SELECT id, label, updatedAt FROM time_based_auto_run WHERE id = '$testId'")
        assertTrue("Should have one record", cursor.moveToFirst())

        // 4. 기존 데이터 보존 확인
        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val label = cursor.getString(cursor.getColumnIndexOrThrow("label"))
        assertEquals("ID should be preserved", testId, id)
        assertEquals("Label should be preserved", testLabel, label)

        // 5. updatedAt 기본값 확인
        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
        assertTrue("updatedAt should be greater than 0", updatedAt > 0)

        cursor.close()
        dbV7.close()
    }

    @Test
    fun migrate6To7_preservesAllExistingData() {
        // 1. v6 데이터베이스 생성 및 여러 테스트 데이터 삽입
        val db = helper.createDatabase(TEST_DB, 6)
        val testCreatedAt = System.currentTimeMillis()

        // 여러 레코드 삽입
        db.execSQL(
            """
            INSERT INTO schedule_group (id, name, description, isActive, createdAt, iconType, colorHex, lastActivatedAt)
            VALUES 
                ('group1', 'Group 1', 'Test 1', 1, $testCreatedAt, 'WORK', '#4CAF50', NULL),
                ('group2', 'Group 2', 'Test 2', 0, $testCreatedAt, 'STUDY', '#2196F3', NULL)
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO location_based_auto_run (
                id, label, address, latitude, longitude, radiusMeters, 
                durationMinutes, presetType, triggerType, periodicIntervalMinutes,
                dwellTimeMinutes, requiresUserConfirmation, isEnabled, createdAt,
                linkedScheduleGroupId, activateScheduleOnEnter, deactivateScheduleOnExit, exitActionType
            )
            VALUES (
                'loc1', 'Location 1', 'Addr 1', 37.1, 127.1, 100,
                60, 'STANDARD', 'ENTER', NULL,
                0, 0, 1, $testCreatedAt,
                NULL, 0, 0, 'DEACTIVATE'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO time_based_auto_run (
                id, hour, minute, durationMinutes, presetType, enabledDays,
                label, isEnabled, createdAt, scheduleGroupId, isIndependent, groupPriority
            )
            VALUES (
                'time1', 9, 0, 90, 'STANDARD', '["MON","TUE","WED","THU","FRI"]',
                'Time 1', 1, $testCreatedAt, NULL, 1, 0
            )
            """.trimIndent()
        )

        db.close()

        // 2. v7로 마이그레이션 실행
        val dbV7 = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // 3. 모든 테이블의 레코드 수 확인
        val scheduleGroupCursor = dbV7.query("SELECT COUNT(*) FROM schedule_group")
        scheduleGroupCursor.moveToFirst()
        assertEquals("Should have 2 schedule groups", 2, scheduleGroupCursor.getInt(0))
        scheduleGroupCursor.close()

        val locationCursor = dbV7.query("SELECT COUNT(*) FROM location_based_auto_run")
        locationCursor.moveToFirst()
        assertEquals("Should have 1 location", 1, locationCursor.getInt(0))
        locationCursor.close()

        val timeCursor = dbV7.query("SELECT COUNT(*) FROM time_based_auto_run")
        timeCursor.moveToFirst()
        assertEquals("Should have 1 time slot", 1, timeCursor.getInt(0))
        timeCursor.close()

        dbV7.close()
    }
}

