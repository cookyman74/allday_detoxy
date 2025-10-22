package com.allday.detoxy.core.di

import android.app.AlarmManager
import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AlarmManager 및 WorkManager 의존성 주입 모듈
 *
 * 시간 기반 자동 실행을 위한 AlarmManager와 WorkManager를 제공합니다.
 * WorkManager는 AlarmManager fallback으로 사용됩니다.
 *
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager
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
}

