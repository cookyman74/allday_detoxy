package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.FocusSettings
import kotlinx.coroutines.flow.Flow

/**
 * FocusSettings DAO (Data Access Object)
 *
 * 디톡시 제어 설정 관리 (카테고리별 차단 설정)
 *
 * Week 2B: Room DB에 설정을 추가로 저장하여 이력 추적 및 백업/복원 지원
 *
 * **Singleton 패턴**: ID=1인 단일 레코드만 사용
 */
@Dao
interface FocusSettingsDao {

    /**
     * 설정 조회
     *
     * Singleton이므로 ID=1인 레코드만 존재
     */
    @Query("SELECT * FROM focus_settings WHERE id = 1")
    fun getSettings(): Flow<FocusSettings?>

    /**
     * 설정 추가 (첫 실행 시)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: FocusSettings)

    /**
     * 설정 업데이트
     */
    @Update
    suspend fun update(settings: FocusSettings)

    /**
     * SNS 카테고리 활성화/비활성화
     */
    @Query("UPDATE focus_settings SET snsEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setSnsEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * 메신저 카테고리 활성화/비활성화
     */
    @Query("UPDATE focus_settings SET messengerEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setMessengerEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * 웹 브라우저 카테고리 활성화/비활성화
     */
    @Query("UPDATE focus_settings SET webEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setWebEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * 영상/숏폼 카테고리 활성화/비활성화
     */
    @Query("UPDATE focus_settings SET videoEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setVideoEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * 기타 앱 카테고리 활성화/비활성화
     */
    @Query("UPDATE focus_settings SET otherEnabled = :enabled, lastUpdated = :timestamp WHERE id = 1")
    suspend fun setOtherEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    /**
     * 모든 카테고리 활성화 (완전 차단 프리셋)
     */
    @Query("""
        UPDATE focus_settings 
        SET snsEnabled = 1, messengerEnabled = 1, webEnabled = 1, videoEnabled = 1, otherEnabled = 1, 
            lastUpdated = :timestamp 
        WHERE id = 1
    """)
    suspend fun enableAllCategories(timestamp: Long = System.currentTimeMillis())

    /**
     * 모든 카테고리 비활성화 (완화 프리셋)
     */
    @Query("""
        UPDATE focus_settings 
        SET snsEnabled = 0, messengerEnabled = 0, webEnabled = 0, videoEnabled = 0, otherEnabled = 0, 
            lastUpdated = :timestamp 
        WHERE id = 1
    """)
    suspend fun disableAllCategories(timestamp: Long = System.currentTimeMillis())

    /**
     * 기본 설정으로 초기화 (표준 디톡시 프리셋)
     */
    @Query("""
        UPDATE focus_settings 
        SET snsEnabled = 1, messengerEnabled = 0, webEnabled = 1, videoEnabled = 1, otherEnabled = 1, 
            lastUpdated = :timestamp 
        WHERE id = 1
    """)
    suspend fun resetToDefault(timestamp: Long = System.currentTimeMillis())
}

