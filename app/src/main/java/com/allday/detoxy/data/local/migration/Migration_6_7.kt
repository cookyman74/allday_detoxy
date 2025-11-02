package com.allday.detoxy.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 6 → 7: updatedAt 필드 추가
 *
 * ## 변경 사항
 * - ScheduleGroup 테이블에 updatedAt 필드 추가
 * - LocationBasedAutoRun 테이블에 updatedAt 필드 추가
 * - TimeBasedAutoRun 테이블에 updatedAt 필드 추가
 *
 * ## 목적
 * 우선순위 규칙 구현을 위한 "최근 수정" 기준 필드 추가.
 * - 위치 충돌 해소: 면적 → 히스테리시스 → 거리 → 최근 수정
 * - "어디서나 적용" 스케줄 충돌: 구체성 → 최근 수정 → 생성 순서
 *
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 * @see com.allday.detoxy.data.local.entity.LocationBasedAutoRun
 * @see com.allday.detoxy.data.local.entity.TimeBasedAutoRun
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val currentTime = System.currentTimeMillis()

        // 1. ScheduleGroup 테이블에 updatedAt 추가
        database.execSQL(
            """
            ALTER TABLE schedule_group 
            ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime
            """.trimIndent()
        )

        // 2. LocationBasedAutoRun 테이블에 updatedAt 추가
        database.execSQL(
            """
            ALTER TABLE location_based_auto_run 
            ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime
            """.trimIndent()
        )

        // 3. TimeBasedAutoRun 테이블에 updatedAt 추가
        database.execSQL(
            """
            ALTER TABLE time_based_auto_run 
            ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $currentTime
            """.trimIndent()
        )
    }
}

