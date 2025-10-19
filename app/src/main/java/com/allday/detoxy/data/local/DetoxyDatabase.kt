package com.allday.detoxy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allday.detoxy.data.local.dao.DetoxyRoutineLogDao
import com.allday.detoxy.data.local.dao.FocusDistractionDao
import com.allday.detoxy.data.local.dao.FocusInterruptionDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.dao.FocusSettingsDao
import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.entity.DetoxyRoutineLog
import com.allday.detoxy.data.local.entity.FocusDistraction
import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.FocusSettings
import com.allday.detoxy.data.local.entity.UserSettings

/**
 * Detoxy 앱의 Room 데이터베이스
 *
 * 모든 엔티티와 DAO를 관리합니다.
 *
 * ## 버전 히스토리
 * - v1 (MVP): FocusSession, UserSettings
 * - v2 (1차 고도화 Week 2A): FocusSession 확장 + FocusInterruption 추가
 * - v3 (1차 고도화 Week 2B): FocusDistraction, DetoxyRoutineLog, FocusSettings 추가
 *
 * @property sessionDao FocusSession DAO
 * @property settingsDao UserSettings DAO
 * @property interruptionDao FocusInterruption DAO (v2+)
 * @property distractionDao FocusDistraction DAO (v3+)
 * @property routineLogDao DetoxyRoutineLog DAO (v3+)
 * @property focusSettingsDao FocusSettings DAO (v3+)
 */
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class,
        FocusInterruption::class,
        FocusDistraction::class,
        DetoxyRoutineLog::class,
        FocusSettings::class
    ],
    version = 3,
    exportSchema = true
)
abstract class DetoxyDatabase : RoomDatabase() {

    /**
     * FocusSession DAO 반환
     */
    abstract fun sessionDao(): FocusSessionDao

    /**
     * UserSettings DAO 반환
     */
    abstract fun settingsDao(): UserSettingsDao

    /**
     * FocusInterruption DAO 반환 (v2+)
     */
    abstract fun interruptionDao(): FocusInterruptionDao

    /**
     * FocusDistraction DAO 반환 (v3+)
     */
    abstract fun distractionDao(): FocusDistractionDao

    /**
     * DetoxyRoutineLog DAO 반환 (v3+)
     */
    abstract fun routineLogDao(): DetoxyRoutineLogDao

    /**
     * FocusSettings DAO 반환 (v3+)
     */
    abstract fun focusSettingsDao(): FocusSettingsDao
}
