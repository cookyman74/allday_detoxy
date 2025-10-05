package com.allday.detoxy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt 의존성 주입을 위한 Application 클래스
 *
 * @HiltAndroidApp 어노테이션으로 Hilt의 코드 생성 트리거
 * 앱의 전체 생명주기 동안 DI 컨테이너 유지
 */
@HiltAndroidApp
class DetoxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Timber 로그 초기화 (추후 추가)
        // TODO: Crashlytics 초기화 (Week 4)
    }
}
