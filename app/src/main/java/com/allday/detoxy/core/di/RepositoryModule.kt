package com.allday.detoxy.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository 인터페이스와 구현체 바인딩 모듈
 *
 * TODO: Week 2.3에서 FocusRepository 및 구현체 생성 후 바인딩 메서드 추가
 *
 * @Binds를 사용하여 인터페이스를 구현체로 매핑
 * Clean Architecture의 의존성 역전 원칙(DIP) 구현
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // TODO: Week 2.3에서 아래 메서드 구현
    //
    // @Binds
    // @Singleton
    // abstract fun bindFocusRepository(
    //     impl: FocusRepositoryImpl
    // ): FocusRepository
}
