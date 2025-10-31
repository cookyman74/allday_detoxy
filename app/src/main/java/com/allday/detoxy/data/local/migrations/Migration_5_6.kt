package com.allday.detoxy.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 데이터베이스 마이그레이션 v5 → v6
 *
 * 3차 고도화: 위치-시간표 연동 옵션 및 UI 필드 추가
 *
 * ## 변경 사항
 * 1. **ScheduleGroup 테이블 확장**
 *    - iconType: UI 표시용 아이콘 타입 (기본값: "WORK")
 *    - colorHex: UI 표시용 색상 코드 (기본값: "#4CAF50")
 *    - lastActivatedAt: 마지막 활성화 시각 (nullable, 통계용)
 *
 * 2. **LocationBasedAutoRun 테이블 확장**
 *    - activateScheduleOnEnter: 위치 진입 시 시간표 자동 활성화 (기본값: 0)
 *    - deactivateScheduleOnExit: 위치 이탈 시 시간표 자동 비활성화 (기본값: 0)
 *    - exitActionType: 위치 이탈 시 액션 타입 (기본값: "DEACTIVATE")
 *
 * 3. **TimeBasedAutoRun 테이블 확장**
 *    - groupPriority: 그룹 내 우선순위 (기본값: 0, 선택적)
 *
 * ## 마이그레이션 전략
 * - 기존 데이터 영향 없음 (nullable 필드 추가 또는 DEFAULT 값 설정)
 * - 모든 기존 레코드는 기본값으로 설정됨
 * - Soft Reference 관계 유지 (Foreign Key 없음)
 *
 * ## 예상 실행 시간
 * - ScheduleGroup ALTER TABLE 3회: < 30ms (각 10ms)
 * - LocationBasedAutoRun ALTER TABLE 3회: < 30ms (각 10ms)
 * - TimeBasedAutoRun ALTER TABLE 1회: < 10ms
 * - 총 예상 시간: < 70ms
 *
 * @see com.allday.detoxy.data.local.entity.ScheduleGroup
 * @see com.allday.detoxy.data.local.entity.LocationBasedAutoRun
 * @see com.allday.detoxy.data.local.entity.TimeBasedAutoRun
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. ScheduleGroup 테이블 확장 (UI 및 통계 필드)
        db.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN iconType TEXT NOT NULL DEFAULT 'WORK'"
        )
        
        db.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#4CAF50'"
        )
        
        db.execSQL(
            "ALTER TABLE schedule_group ADD COLUMN lastActivatedAt INTEGER"
        )

        // 2. LocationBasedAutoRun 테이블 확장 (위치-시간표 연동 옵션)
        db.execSQL(
            "ALTER TABLE location_based_auto_run ADD COLUMN activateScheduleOnEnter INTEGER NOT NULL DEFAULT 0"
        )
        
        db.execSQL(
            "ALTER TABLE location_based_auto_run ADD COLUMN deactivateScheduleOnExit INTEGER NOT NULL DEFAULT 0"
        )
        
        db.execSQL(
            "ALTER TABLE location_based_auto_run ADD COLUMN exitActionType TEXT NOT NULL DEFAULT 'DEACTIVATE'"
        )

        // 3. TimeBasedAutoRun 테이블 확장 (그룹 내 우선순위)
        db.execSQL(
            "ALTER TABLE time_based_auto_run ADD COLUMN groupPriority INTEGER NOT NULL DEFAULT 0"
        )
    }
}

