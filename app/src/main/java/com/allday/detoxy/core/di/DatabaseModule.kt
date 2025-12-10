package com.allday.detoxy.core.di

import android.content.Context
import androidx.room.Room
import com.allday.detoxy.data.local.DetoxyDatabase
import com.allday.detoxy.data.local.dao.AutoRunLogDao
import com.allday.detoxy.data.local.dao.CustomTimerPresetDao
import com.allday.detoxy.data.local.dao.DetoxyRoutineLogDao
import com.allday.detoxy.data.local.dao.FocusDistractionDao
import com.allday.detoxy.data.local.dao.FocusInterruptionDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.dao.FocusSettingsDao
import com.allday.detoxy.data.local.dao.LocationBasedAutoRunDao
import com.allday.detoxy.data.local.dao.ScheduleGroupDao
import com.allday.detoxy.data.local.dao.TimeBasedAutoRunDao
import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.migration.MIGRATION_6_7
import com.allday.detoxy.data.local.migration.MIGRATION_7_8
import com.allday.detoxy.data.local.migrations.MIGRATION_1_2
import com.allday.detoxy.data.local.migrations.MIGRATION_2_3
import com.allday.detoxy.data.local.migrations.MIGRATION_3_4
import com.allday.detoxy.data.local.migrations.MIGRATION_4_5
import com.allday.detoxy.data.local.migrations.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 데이터베이스 의존성 제공 모듈
 *
 * DetoxyDatabase와 DAO들을 Hilt를 통해 의존성 주입합니다.
 * 싱글톤으로 제공되어 앱 전체에서 하나의 데이터베이스 인스턴스만 사용합니다.
 *
 * ## 마이그레이션
 * - v1 → v2 (Week 2A): FocusSession 확장 + FocusInterruption 추가 (MIGRATION_1_2)
 * - v2 → v3 (Week 2B): FocusDistraction, DetoxyRoutineLog, FocusSettings 추가 (MIGRATION_2_3)
 * - v3 → v4 (2차 고도화): TimeBasedAutoRun, LocationBasedAutoRun, CustomTimerPreset, AutoRunLog 추가 + UserSettings 확장 (MIGRATION_3_4)
 * - v4 → v5 (2.5차 고도화): ScheduleGroup 추가 + TimeBasedAutoRun/LocationBasedAutoRun 확장 (MIGRATION_4_5)
 * - v5 → v6 (3차 고도화): ScheduleGroup UI 필드 + LocationBasedAutoRun 연동 옵션 + TimeBasedAutoRun 우선순위 (MIGRATION_5_6)
 * - v6 → v7 (3.5차 고도화 Phase 0): updatedAt 필드 추가 (MIGRATION_6_7)
 * - v7 → v8 (버튼 역할 변경): manualOverrideState, pauseUntil 필드 추가 (MIGRATION_7_8)
 *
 * @InstallIn(SingletonComponent::class)로 앱 전체 생명주기 동안 싱글톤 유지
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * DetoxyDatabase 인스턴스 제공
     *
     * @param context Application Context
     * @return DetoxyDatabase 싱글톤 인스턴스
     */
    @Provides
    @Singleton
    fun provideDetoxyDatabase(
        @ApplicationContext context: Context
    ): DetoxyDatabase {
        return Room.databaseBuilder(
            context,
            DetoxyDatabase::class.java,
            "detoxy_database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8
            )
            .build()
    }

    /**
     * FocusSessionDao 제공
     *
     * @param database DetoxyDatabase 인스턴스
     * @return FocusSessionDao
     */
    @Provides
    fun provideFocusSessionDao(database: DetoxyDatabase): FocusSessionDao {
        return database.sessionDao()
    }

    /**
     * UserSettingsDao 제공
     *
     * @param database DetoxyDatabase 인스턴스
     * @return UserSettingsDao
     */
    @Provides
    fun provideUserSettingsDao(database: DetoxyDatabase): UserSettingsDao {
        return database.settingsDao()
    }

    /**
     * FocusInterruptionDao 제공 (v2+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return FocusInterruptionDao
     */
    @Provides
    fun provideFocusInterruptionDao(database: DetoxyDatabase): FocusInterruptionDao {
        return database.interruptionDao()
    }

    /**
     * FocusDistractionDao 제공 (v3+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return FocusDistractionDao
     */
    @Provides
    fun provideFocusDistractionDao(database: DetoxyDatabase): FocusDistractionDao {
        return database.distractionDao()
    }

    /**
     * DetoxyRoutineLogDao 제공 (v3+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return DetoxyRoutineLogDao
     */
    @Provides
    fun provideDetoxyRoutineLogDao(database: DetoxyDatabase): DetoxyRoutineLogDao {
        return database.routineLogDao()
    }

    /**
     * FocusSettingsDao 제공 (v3+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return FocusSettingsDao
     */
    @Provides
    fun provideFocusSettingsDao(database: DetoxyDatabase): FocusSettingsDao {
        return database.focusSettingsDao()
    }

    /**
     * TimeBasedAutoRunDao 제공 (v4+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return TimeBasedAutoRunDao
     */
    @Provides
    fun provideTimeBasedAutoRunDao(database: DetoxyDatabase): TimeBasedAutoRunDao {
        return database.timeBasedAutoRunDao()
    }

    /**
     * LocationBasedAutoRunDao 제공 (v4+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return LocationBasedAutoRunDao
     */
    @Provides
    fun provideLocationBasedAutoRunDao(database: DetoxyDatabase): LocationBasedAutoRunDao {
        return database.locationBasedAutoRunDao()
    }

    /**
     * CustomTimerPresetDao 제공 (v4+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return CustomTimerPresetDao
     */
    @Provides
    fun provideCustomTimerPresetDao(database: DetoxyDatabase): CustomTimerPresetDao {
        return database.customTimerPresetDao()
    }

    /**
     * AutoRunLogDao 제공 (v4+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return AutoRunLogDao
     */
    @Provides
    fun provideAutoRunLogDao(database: DetoxyDatabase): AutoRunLogDao {
        return database.autoRunLogDao()
    }

    /**
     * ScheduleGroupDao 제공 (v5+)
     *
     * @param database DetoxyDatabase 인스턴스
     * @return ScheduleGroupDao
     */
    @Provides
    fun provideScheduleGroupDao(database: DetoxyDatabase): ScheduleGroupDao {
        return database.scheduleGroupDao()
    }
}
