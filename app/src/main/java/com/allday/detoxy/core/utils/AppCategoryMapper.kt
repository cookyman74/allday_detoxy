package com.allday.detoxy.core.utils

import android.util.Log

/**
 * 디톡시 제어: 앱 카테고리 매핑 및 차단 로직
 *
 * 주요 기능:
 * 1. 패키지명 → 카테고리 매핑 (40개 앱)
 * 2. 카테고리별 차단 여부 판단
 * 3. 프리셋(완전 차단/표준 디톡시/완화) 제공
 * 4. 시스템 앱 필터링
 *
 * @see [01_advanced_app_category_mapping.md](../../../../../../../../docs/01_advanced_app_category_mapping.md)
 */
object AppCategoryMapper {
    private const val TAG = "AppCategoryMapper"

    /**
     * 카테고리별 패키지 리스트 (총 40개 앱)
     */
    private val categoryMap = mapOf(
        AppCategory.SNS to setOf(
            "com.instagram.android",      // Instagram
            "com.facebook.katana",         // Facebook
            "com.twitter.android",         // Twitter (X)
            "com.instagram.barcelona",     // Threads
            "com.linkedin.android",        // LinkedIn
            "com.snapchat.android",        // Snapchat
            "com.pinterest",               // Pinterest
            "com.reddit.frontpage",        // Reddit
            "com.tumblr"                   // Tumblr
        ),
        AppCategory.MESSENGER to setOf(
            "com.kakao.talk",              // 카카오톡
            "com.whatsapp",                // WhatsApp
            "org.telegram.messenger",      // Telegram
            "jp.naver.line.android",       // LINE
            "com.discord",                 // Discord
            "com.Slack",                   // Slack
            "com.microsoft.teams",         // Microsoft Teams
            "com.facebook.orca",           // Messenger
            "org.thoughtcrime.securesms",  // Signal
            "com.tencent.mm",              // WeChat
            "com.viber.voip"               // Viber
        ),
        AppCategory.WEB to setOf(
            "com.android.chrome",          // Chrome
            "com.sec.android.app.sbrowser", // Samsung Internet
            "org.mozilla.firefox",         // Firefox
            "com.microsoft.emmx",          // Edge
            "com.opera.browser",           // Opera
            "com.brave.browser",           // Brave
            "com.naver.whale",             // Whale
            "com.duckduckgo.mobile.android" // DuckDuckGo
        ),
        AppCategory.VIDEO_SHORTS to setOf(
            "com.google.android.youtube",  // YouTube
            "com.zhiliaoapp.musically",    // TikTok
            "com.instagram.android",       // Instagram (Reels, 중복)
            "com.facebook.katana",         // Facebook (Watch, 중복)
            "com.netflix.mediaclient",     // Netflix
            "com.disney.disneyplus",       // Disney+
            "com.pooq.wavve",              // Wavve
            "com.pooq.skb",                // Tving
            "com.coupang.mobile.play",     // Coupang Play
            "tv.twitch.android.app",       // Twitch
            "com.rsupport.android.afreeca", // AfreecaTV
            "com.naver.vapp"               // NAVER NOW (V LIVE)
        )
    )

    /**
     * 시스템 앱 & 필수 앱 (차단 제외)
     *
     * 디톡시 중에도 정상 동작해야 하는 앱들
     */
    private val systemApps = setOf(
        "com.android.settings",        // 설정
        "com.android.phone",           // 전화
        "com.android.contacts",        // 연락처
        "com.android.launcher",        // 런처
        "com.android.launcher3",       // Pixel 런처
        "com.sec.android.app.launcher", // Samsung 런처
        "com.android.vending",         // Play Store
        "com.google.android.gms",      // Google Play Services
        "com.android.systemui",        // System UI
        "com.allday.detoxy"            // Allday Detoxy (자사 앱)
    )

    // ==================== 카테고리 조회 ====================

    /**
     * 패키지명으로 카테고리 찾기
     *
     * @param packageName 앱 패키지명
     * @return 매핑된 카테고리 (없으면 null)
     */
    fun getCategoryByPackage(packageName: String): AppCategory? {
        return categoryMap.entries.find { packageName in it.value }?.key
    }

    /**
     * 카테고리별 패키지 리스트 조회
     *
     * @param category 앱 카테고리
     * @return 카테고리에 속한 패키지 Set (없으면 빈 Set)
     */
    fun getPackagesByCategory(category: AppCategory): Set<String> {
        return categoryMap[category] ?: emptySet()
    }

    /**
     * 모든 카테고리 패키지 리스트 (시스템 앱 제외)
     */
    fun getAllCategorizedPackages(): Set<String> {
        return categoryMap.values.flatten().toSet()
    }

    // ==================== 차단 로직 ====================

    /**
     * 앱 차단 여부 확인 (디톡시 제어 설정 기반)
     *
     * 우선순위:
     * 1. 시스템 앱 → 항상 허용
     * 2. 명시적 카테고리 → 설정된 카테고리면 차단
     * 3. 기타 앱 → otherAppsEnabled 설정에 따름
     *
     * @param packageName 앱 패키지명
     * @param enabledCategories 차단 활성화된 카테고리 Set
     * @param otherAppsEnabled 기타 앱 차단 여부
     * @return true: 차단, false: 허용
     */
    fun isBlocked(
        packageName: String,
        enabledCategories: Set<AppCategory>,
        otherAppsEnabled: Boolean
    ): Boolean {
        // 1. 시스템 앱은 항상 허용
        if (packageName in systemApps) {
            Log.d(TAG, "✅ System app allowed: $packageName")
            return false
        }

        // 2. 명시적 카테고리 확인
        val category = getCategoryByPackage(packageName)
        if (category != null) {
            val blocked = category in enabledCategories
            Log.d(TAG, "🔍 Category $category: ${if (blocked) "🔒 Blocked" else "✅ Allowed"} - $packageName")
            return blocked
        }

        // 3. 기타 앱 차단 여부
        Log.d(TAG, "📦 Other app: ${if (otherAppsEnabled) "🔒 Blocked" else "✅ Allowed"} - $packageName")
        return otherAppsEnabled
    }

    /**
     * 시스템 앱 여부 확인
     */
    fun isSystemApp(packageName: String): Boolean {
        return packageName in systemApps
    }

    // ==================== 프리셋 정의 (디톡시 제어 관점) ====================

    /**
     * 디톡시 제어 프리셋
     *
     * 중독 진단 및 회복 단계별로 권장되는 차단 설정
     */
    enum class DetoxyPreset(
        val displayName: String,
        val description: String,
        val enabledCategories: Set<AppCategory>,
        val otherAppsEnabled: Boolean,
        val recoveryStage: String
    ) {
        /**
         * 완전 차단 (디지털 디톡스 초기)
         * 목적: 중독에서 벗어나기 위한 강력한 초기 회복
         * 권장 대상: 고위험 사용자, 디톡시 첫 1-2주
         */
        COMPLETE_BLOCK(
            displayName = "완전 차단",
            description = "모든 카테고리 + 기타 앱 차단 (디톡시 회복 초기)",
            enabledCategories = setOf(
                AppCategory.SNS,
                AppCategory.MESSENGER,
                AppCategory.WEB,
                AppCategory.VIDEO_SHORTS,
                AppCategory.OTHER
            ),
            otherAppsEnabled = true,
            recoveryStage = "회복 초기 (1-2주)"
        ),

        /**
         * 표준 디톡시 (균형 잡힌 회복)
         * 목적: SNS/영상/웹 차단으로 핵심 중독 요소 제거
         * 권장 대상: 중간 위험 사용자, 디톡시 2-4주
         */
        STANDARD_DETOXY(
            displayName = "표준 디톡시",
            description = "SNS/영상/Web 차단, 메신저 허용 (균형 회복)",
            enabledCategories = setOf(
                AppCategory.SNS,
                AppCategory.WEB,
                AppCategory.VIDEO_SHORTS
            ),
            otherAppsEnabled = false,
            recoveryStage = "회복 중기 (2-4주)"
        ),

        /**
         * 완화 (유지 단계)
         * 목적: 영상 중독만 방지, 일상 생활 유지
         * 권장 대상: 저위험 사용자, 디톡시 4주 이상
         */
        RELAXED(
            displayName = "완화",
            description = "영상 & 쇼츠만 차단 (회복 유지 단계)",
            enabledCategories = setOf(
                AppCategory.VIDEO_SHORTS
            ),
            otherAppsEnabled = false,
            recoveryStage = "회복 후기 (4주+)"
        );

        /**
         * 회복 효과 메시지 (디톡시 관점)
         */
        fun getRecoveryMessage(): String = when (this) {
            COMPLETE_BLOCK -> "디지털 중독에서 벗어나는 첫 걸음입니다. 강력하지만 효과적입니다."
            STANDARD_DETOXY -> "핵심 중독 요소를 차단하면서 필수 소통은 유지합니다."
            RELAXED -> "짧은 영상 중독만 방지하고, 일상 생활을 자유롭게 유지합니다."
        }
    }

    /**
     * 프리셋 적용
     *
     * @param preset 적용할 프리셋
     * @return Pair<활성화된 카테고리 Set, 기타 앱 차단 여부>
     */
    fun applyPreset(preset: DetoxyPreset): Pair<Set<AppCategory>, Boolean> {
        Log.i(TAG, "🎯 Preset applied: ${preset.displayName} (${preset.recoveryStage})")
        return Pair(preset.enabledCategories, preset.otherAppsEnabled)
    }

    /**
     * 현재 설정에서 프리셋 감지
     *
     * @param enabledCategories 활성화된 카테고리
     * @param otherAppsEnabled 기타 앱 차단 여부
     * @return 감지된 프리셋 (없으면 null, 커스텀 설정 의미)
     */
    fun detectPreset(
        enabledCategories: Set<AppCategory>,
        otherAppsEnabled: Boolean
    ): DetoxyPreset? {
        return DetoxyPreset.values().find {
            it.enabledCategories == enabledCategories && it.otherAppsEnabled == otherAppsEnabled
        }
    }

    // ==================== 통계 유틸 ====================

    /**
     * 카테고리별 앱 개수
     */
    fun getCategoryAppCount(category: AppCategory): Int {
        return categoryMap[category]?.size ?: 0
    }

    /**
     * 전체 매핑된 앱 개수
     */
    fun getTotalMappedApps(): Int {
        return categoryMap.values.sumOf { it.size }
    }

    /**
     * 카테고리별 앱 개수 맵
     */
    fun getCategoryAppCounts(): Map<AppCategory, Int> {
        return categoryMap.mapValues { it.value.size }
    }
}

