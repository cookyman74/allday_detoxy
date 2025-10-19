package com.allday.detoxy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allday.detoxy.data.local.dao.FocusInterruptionDao
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.entity.FocusInterruption
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings

/**
 * Detoxy 앱의 Room 데이터베이스
 *
 * FocusSession, UserSettings, FocusInterruption 엔티티를 관리합니다.
 *
 * ## 버전 히스토리
 * - v1 (MVP): FocusSession, UserSettings
 * - v2 (1차 고도화): FocusSession 확장 + FocusInterruption 추가
 *
 * @property sessionDao FocusSession DAO
 * @property settingsDao UserSettings DAO
 * @property interruptionDao FocusInterruption DAO (v2+)
 */
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class,
        FocusInterruption::class
    ],
    version = 2,
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
}
