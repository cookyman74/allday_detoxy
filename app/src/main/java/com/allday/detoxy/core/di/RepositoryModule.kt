package com.allday.detoxy.core.di

import com.allday.detoxy.data.repository.FocusRepositoryImpl
import com.allday.detoxy.domain.repository.FocusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
}
