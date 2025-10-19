package com.allday.detoxy.core.di

import android.content.Context
import com.allday.detoxy.data.repository.FocusRepositoryImpl
import com.allday.detoxy.data.repository.FocusSettingsRepositoryImpl
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 인터페이스와 구현체 바인딩 모듈
 *
 * @Binds를 사용하여 인터페이스를 구현체로 매핑합니다.
 * Clean Architecture의 의존성 역전 원칙(DIP)을 구현합니다.
 *
 * domain 계층은 data 계층을 직접 의존하지 않고,
 * 추상화된 Repository 인터페이스에만 의존합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * FocusRepository 바인딩
     *
     * FocusRepositoryImpl을 FocusRepository 인터페이스로 제공합니다.
     *
     * @param impl FocusRepositoryImpl 구현체
     * @return FocusRepository 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindFocusRepository(
        impl: FocusRepositoryImpl
    ): FocusRepository

    companion object {
        /**
         * FocusSettingsRepository 제공
         *
         * DataStore 기반 디톡시 제어 설정 Repository
         * Clean Architecture: 인터페이스(domain) ← 구현체(data)
         */
        @Provides
        @Singleton
        fun provideFocusSettingsRepository(
            @ApplicationContext context: Context
        ): FocusSettingsRepository {
            return FocusSettingsRepositoryImpl(context)
        }
    }
}
