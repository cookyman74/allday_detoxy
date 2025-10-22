package com.allday.detoxy.core.di

import android.content.Context
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Geofencing 관련 Hilt 의존성 주입 모듈
 *
 * 제공하는 의존성:
 * - GeofencingClient: Google Play Services Location API의 Geofencing 클라이언트
 *
 * @see com.allday.detoxy.core.manager.AutoRunGeofenceManager
 */
@Module
@InstallIn(SingletonComponent::class)
object GeofenceModule {
    
    /**
     * GeofencingClient 제공
     *
     * Google Play Services Location API를 통해 Geofence 등록/해제를 수행합니다.
     *
     * @param context Application Context
     * @return GeofencingClient
     */
    @Provides
    @Singleton
    fun provideGeofencingClient(@ApplicationContext context: Context): GeofencingClient {
        return LocationServices.getGeofencingClient(context)
    }
}

