package com.allday.detoxy.core.di

import android.app.AlarmManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AlarmManager 의존성 주입 모듈
 *
 * 시간 기반 자동 실행을 위한 AlarmManager 시스템 서비스를 제공합니다.
 *
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager
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
}

