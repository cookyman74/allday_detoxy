package com.allday.detoxy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.allday.detoxy.data.local.entity.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * UserSettings DAO (Data Access Object)
 *
 * 사용자 설정 데이터에 대한 데이터베이스 작업을 정의합니다.
 * 단일 레코드(ID=1)만 사용하므로 Insert 시 충돌 전략을 REPLACE로 설정합니다.
 */
@Dao
interface UserSettingsDao {

    /**
     * 사용자 설정 조회
     *
     * ID가 1인 단일 레코드를 반환합니다.
     * 레코드가 없으면 null을 반환합니다.
     *
     * @return 사용자 설정 (Flow)
     */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    /**
     * 사용자 설정 추가
     *
     * 이미 존재하면 교체합니다 (REPLACE 전략).
     * 앱 최초 실행 시 기본 설정을 생성하는 데 사용됩니다.
     *
     * @param settings 추가할 설정
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserSettings)

    /**
     * 사용자 설정 업데이트
     *
     * @param settings 업데이트할 설정
     */
    @Update
    suspend fun update(settings: UserSettings)

    /**
     * 총 포인트 증가
     *
     * @param points 증가할 포인트
     */
    @Query("UPDATE user_settings SET totalPoints = totalPoints + :points WHERE id = 1")
    suspend fun addPoints(points: Int)

    /**
     * 스트릭 업데이트
     *
     * @param streak 새로운 스트릭 값
     * @param date 마지막 성공 날짜
     */
    @Query("UPDATE user_settings SET currentStreak = :streak, lastSuccessDate = :date WHERE id = 1")
    suspend fun updateStreak(streak: Int, date: String)

    /**
     * 스트릭 초기화
     */
    @Query("UPDATE user_settings SET currentStreak = 0 WHERE id = 1")
    suspend fun resetStreak()
}
