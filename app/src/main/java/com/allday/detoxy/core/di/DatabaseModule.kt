package com.allday.detoxy.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Room 데이터베이스 의존성 제공 모듈
 *
 * TODO: Week 2.3에서 DetoxyDatabase, DAO 생성 후 의존성 제공 메서드 추가
 *
 * @InstallIn(SingletonComponent::class)로 앱 전체 생명주기 동안 싱글톤 유지
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // TODO: Week 2.3에서 아래 메서드 구현
    //
    // @Provides
    // @Singleton
    // fun provideDetoxyDatabase(
    //     @ApplicationContext context: Context
    // ): DetoxyDatabase {
    //     return Room.databaseBuilder(
    //         context,
    //         DetoxyDatabase::class.java,
    //         "detoxy_database"
    //     ).build()
    // }
    //
    // @Provides
    // fun provideFocusSessionDao(database: DetoxyDatabase) =
    //     database.sessionDao()
    //
    // @Provides
    // fun provideUserSettingsDao(database: DetoxyDatabase) =
    //     database.settingsDao()
}
