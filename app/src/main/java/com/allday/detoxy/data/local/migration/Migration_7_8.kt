package com.allday.detoxy.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 7 → 8: 스케줄 그룹 통합 제어 필드 추가
 *
 * ## 변경 사항
 * - ScheduleGroup 테이블에 manualOverrideState 필드 추가
 * - ScheduleGroup 테이블에 pauseUntil 필드 추가
 *
 * ## 목적
 * 스케줄 그룹의 활성/비활성/일시중지 상태를 사용자가 명시적으로 제어할 수 있도록 합니다.
 * 위치기반 스케줄에서 자동 활성화를 사용자가 차단할 수 있습니다.
 *
 * ## 새 필드 설명
 * - manualOverrideState: 사용자 수동 제어 상태
 *   - null: 자동 모드 (기본값)
 *   - "INACTIVE": 사용자가 명시적으로 비활성화
 *   - "PAUSED": 사용자가 명시적으로 일시중지
 *
 * - pauseUntil: 일시중지 해제 시각 (timestamp)
 *   - null: 일시중지 아님 또는 무기한
 *   - timestamp: 해당 시각에 자동으로 활성 상태로 전환
 *
 * ## 기존 데이터 마이그레이션
 * - 기존 isActive=false인 그룹은 manualOverrideState='INACTIVE'로 설정
 *   → 사용자가 비활성화한 것으로 간주하여 위치 진입 시 자동 활성화 방지
 *
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 * @see docs/04_버튼역할변경_prd.md
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. ScheduleGroup 테이블에 manualOverrideState 필드 추가
        // TEXT 타입, nullable, 기본값 NULL
        db.execSQL(
            """
            ALTER TABLE schedule_group 
            ADD COLUMN manualOverrideState TEXT DEFAULT NULL
            """.trimIndent()
        )

        // 2. ScheduleGroup 테이블에 pauseUntil 필드 추가
        // INTEGER 타입, nullable, 기본값 NULL
        db.execSQL(
            """
            ALTER TABLE schedule_group 
            ADD COLUMN pauseUntil INTEGER DEFAULT NULL
            """.trimIndent()
        )

        // 3. 기존 데이터 마이그레이션
        // isActive=false인 그룹은 사용자가 명시적으로 비활성화한 것으로 간주
        // → manualOverrideState='INACTIVE'로 설정
        db.execSQL(
            """
            UPDATE schedule_group 
            SET manualOverrideState = 'INACTIVE' 
            WHERE isActive = 0
            """.trimIndent()
        )
    }
}

