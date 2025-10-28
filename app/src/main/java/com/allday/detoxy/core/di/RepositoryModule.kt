package com.allday.detoxy.core.di

import android.content.Context
import com.allday.detoxy.data.repository.AutoRunLogRepository
import com.allday.detoxy.data.repository.AutoRunSettingsRepositoryImpl
import com.allday.detoxy.data.repository.FocusRepositoryImpl
import com.allday.detoxy.data.repository.FocusSessionRepository
import com.allday.detoxy.data.repository.FocusSettingsRepositoryImpl
import com.allday.detoxy.data.repository.UserSettingsRepository
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.domain.repository.FocusRepository
import com.allday.detoxy.domain.repository.FocusSettingsRepository
import com.allday.detoxy.domain.repository.IAutoRunLogRepository
import com.allday.detoxy.domain.repository.IFocusSessionRepository
import com.allday.detoxy.domain.repository.IUserSettingsRepository
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

    /**
     * IUserSettingsRepository 바인딩
     *
     * UserSettingsRepository를 IUserSettingsRepository 인터페이스로 제공합니다.
     * Clean Architecture의 의존성 역전 원칙을 준수하여
     * domain 계층이 data 계층을 직접 의존하지 않도록 합니다.
     *
     * @param impl UserSettingsRepository 구현체
     * @return IUserSettingsRepository 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindUserSettingsRepository(
        impl: UserSettingsRepository
    ): IUserSettingsRepository

    /**
     * IAutoRunLogRepository 바인딩
     *
     * AutoRunLogRepository를 IAutoRunLogRepository 인터페이스로 제공합니다.
     * 통계 계산 등 도메인 로직에서 자동 실행 로그 데이터에 접근할 때 사용합니다.
     *
     * @param impl AutoRunLogRepository 구현체
     * @return IAutoRunLogRepository 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindAutoRunLogRepository(
        impl: AutoRunLogRepository
    ): IAutoRunLogRepository

    /**
     * IFocusSessionRepository 바인딩
     *
     * FocusSessionRepository를 IFocusSessionRepository 인터페이스로 제공합니다.
     * 통계 계산 등 도메인 로직에서 집중 세션 데이터에 접근할 때 사용합니다.
     *
     * @param impl FocusSessionRepository 구현체
     * @return IFocusSessionRepository 인터페이스
     */
    @Binds
    @Singleton
    abstract fun bindFocusSessionRepository(
        impl: FocusSessionRepository
    ): IFocusSessionRepository

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

        /**
         * AutoRunSettingsRepository 제공
         *
         * DataStore 기반 자동 실행 글로벌 설정 Repository
         * 주말 제외, 자동 시작 딜레이, 사전 알림 시간 설정 관리
         * Clean Architecture: 인터페이스(domain) ← 구현체(data)
         */
        @Provides
        @Singleton
        fun provideAutoRunSettingsRepository(
            @ApplicationContext context: Context
        ): AutoRunSettingsRepository {
            return AutoRunSettingsRepositoryImpl(context)
        }
    }
}
