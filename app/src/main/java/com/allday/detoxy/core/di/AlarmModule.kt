package com.allday.detoxy.core.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AlarmManager, WorkManager, NotificationManager 의존성 주입 모듈
 *
 * 시간 기반 자동 실행을 위한 AlarmManager, WorkManager, NotificationManager를 제공합니다.
 * - AlarmManager: 정확한 시간 트리거
 * - WorkManager: AlarmManager fallback (권한 없을 때)
 * - NotificationManager: 자동 실행 알림 표시
 *
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager
 * @see com.allday.detoxy.core.manager.AutoRunNotificationManager
 * @see com.allday.detoxy.worker.AutoRunWorker
 */
@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {

    /**
     * AlarmManager 시스템 서비스 제공
     *
     * @param context Application Context
     * @return AlarmManager 인스턴스
     */
    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    /**
     * WorkManager 인스턴스 제공
     *
     * AlarmManager 실패 시 fallback으로 사용됩니다.
     * - 정확 알람 권한 없을 때
     * - 배터리 최적화로 인한 AlarmManager 실패 시
     *
     * @param context Application Context
     * @return WorkManager 인스턴스
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * NotificationManager 시스템 서비스 제공
     *
     * 자동 실행 알림 표시를 위해 사용됩니다.
     * - 사전 알림 (N분 전)
     * - 실행 알림 (액션 버튼 포함)
     *
     * ⚠️ **Critical Fix**: Hilt 바인딩 누락 해결
     * - AutoRunNotificationManager 생성자가 NotificationManager를 주입받기 위해 필요
     * - 기존에는 바인딩이 없어 "No binding for NotificationManager" 컴파일 오류 발생
     *
     * @param context Application Context
     * @return NotificationManager 인스턴스
     */
    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}

