package com.allday.detoxy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allday.detoxy.data.local.dao.FocusSessionDao
import com.allday.detoxy.data.local.dao.UserSettingsDao
import com.allday.detoxy.data.local.entity.FocusSession
import com.allday.detoxy.data.local.entity.UserSettings

/**
 * Detoxy 앱의 Room 데이터베이스
 *
 * FocusSession과 UserSettings 엔티티를 관리합니다.
 * MVP 버전으로 간소화되어 2개 테이블만 사용합니다.
 *
 * @property sessionDao FocusSession DAO
 * @property settingsDao UserSettings DAO
 */
@Database(
    entities = [
        FocusSession::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
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
}
