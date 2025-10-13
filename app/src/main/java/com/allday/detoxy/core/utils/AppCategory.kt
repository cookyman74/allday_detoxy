package com.allday.detoxy.core.utils

/**
 * 디톡시 제어 앱 카테고리
 *
 * 디지털 중독 진단 및 회복 관점에서 앱을 5개 카테고리로 분류
 *
 * @see [01_advanced_app_category_mapping.md](../../../../../../../../docs/01_advanced_app_category_mapping.md)
 */
enum class AppCategory {
    /**
     * SNS (소셜 네트워크)
     * 중독 위험도: 높음
     * 회복 효과: 정신적 휴식, 비교 스트레스 완화
     *
     * 예: Instagram, Facebook, Twitter, Threads
     */
    SNS,

    /**
     * 메신저 & 커뮤니케이션
     * 중독 위험도: 중간
     * 회복 효과: 긴급 연락 허용 옵션 제공, 업무/사생활 분리
     *
     * 예: KakaoTalk, WhatsApp, Telegram, LINE
     */
    MESSENGER,

    /**
     * Web 서핑 (브라우저)
     * 중독 위험도: 높음
     * 회복 효과: 무분별한 정보 소비 차단
     *
     * 예: Chrome, Samsung Browser, Firefox, Edge
     */
    WEB,

    /**
     * 영상 & 쇼츠
     * 중독 위험도: 매우 높음
     * 회복 효과: 짧은 영상 중독 방지, 시간 낭비 최소화
     *
     * 예: YouTube, TikTok, Instagram Reels, Netflix
     */
    VIDEO_SHORTS,

    /**
     * 기타 앱 (Catch-All)
     * 중독 위험도: 낮음
     * 회복 효과: 완전 차단 프리셋에서 활용
     *
     * 명시적 카테고리에 속하지 않는 모든 앱
     */
    OTHER;

    /**
     * 사용자에게 표시될 카테고리 이름
     */
    fun getDisplayName(): String = when (this) {
        SNS -> "SNS"
        MESSENGER -> "메신저"
        WEB -> "Web 서핑"
        VIDEO_SHORTS -> "영상 & 쇼츠"
        OTHER -> "기타 앱"
    }

    /**
     * 카테고리 아이콘 (UI 표시용)
     */
    fun getIcon(): String = when (this) {
        SNS -> "📱"
        MESSENGER -> "💬"
        WEB -> "🌐"
        VIDEO_SHORTS -> "🎬"
        OTHER -> "📦"
    }

    /**
     * 중독 위험도 설명 (디톡시 관점)
     */
    fun getRiskDescription(): String = when (this) {
        SNS -> "타인과의 비교, 무한 스크롤로 인한 시간 낭비"
        MESSENGER -> "업무/사생활 경계 모호, 알림 중독"
        WEB -> "무분별한 정보 탐색, 생산성 저하"
        VIDEO_SHORTS -> "짧은 영상 중독, 도파민 과다 자극"
        OTHER -> "일반 앱 사용 제한"
    }

    /**
     * 회복 효과 설명
     */
    fun getRecoveryEffect(): String = when (this) {
        SNS -> "정신적 휴식, 비교 스트레스 완화, 자존감 회복"
        MESSENGER -> "업무 시간 경계 설정, 긴급 연락 선택 허용"
        WEB -> "목표 지향적 정보 탐색, 집중력 향상"
        VIDEO_SHORTS -> "긴 호흡의 콘텐츠 소비, 창의성 회복"
        OTHER -> "전반적 디지털 사용 감소"
    }
}

