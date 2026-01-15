package com.allday.detoxy.core.locale

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 앱 언어 설정 관리 유틸리티
 *
 * Android 13+ 공식 API (AppCompatDelegate.setApplicationLocales)를 사용하여
 * 앱별 언어 설정을 관리합니다.
 *
 * ## 주요 기능
 * - 앱 언어 설정/조회
 * - 시스템 기본 언어 조회
 * - 지원 언어 목록 제공
 *
 * ## 지원 언어
 * - 한국어 (ko) - 기본값
 * - 영어 (en)
 * - 스페인어 (es)
 * - 일본어 (ja)
 * - 중국어 간체 (zh-Hans)
 */
object AppLocaleManager {

    /**
     * 지원 언어 목록
     */
    enum class SupportedLanguage(
        val languageTag: String,
        val displayName: String,
        val nativeName: String
    ) {
        SYSTEM(languageTag = "", displayName = "시스템 기본값", nativeName = "System Default"),
        KOREAN(languageTag = "ko", displayName = "한국어", nativeName = "한국어"),
        ENGLISH(languageTag = "en", displayName = "영어", nativeName = "English"),
        SPANISH(languageTag = "es", displayName = "스페인어", nativeName = "Español"),
        JAPANESE(languageTag = "ja", displayName = "일본어", nativeName = "日本語"),
        CHINESE_SIMPLIFIED(languageTag = "zh-Hans", displayName = "중국어 (간체)", nativeName = "中文 (简体)")
    }

    /**
     * 앱 언어 설정
     *
     * @param languageTag BCP-47 태그 (예: "ko", "en", "zh-Hans") 또는 null/빈 문자열 (시스템 기본)
     */
    fun setAppLocale(languageTag: String?) {
        val localeList = if (languageTag.isNullOrEmpty()) {
            // 시스템 기본값으로 설정
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * 현재 앱 언어 가져오기
     *
     * @return 현재 설정된 언어 태그 또는 null (시스템 기본)
     */
    fun getAppLocale(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales[0]?.toLanguageTag()
    }

    /**
     * 시스템 기본 언어 가져오기
     *
     * ⚠️ 범위 제한: 사용자가 설정한 다중 언어 목록(우선순위 리스트)이 아닌,
     * 시스템의 **1순위 단일 로케일**만 반환합니다.
     *
     * 다중 로케일 폴백이 필요하면 ConfigurationCompat.getLocales()를 사용하세요.
     *
     * @return 시스템 1순위 로케일
     */
    fun getSystemPrimaryLocale(): Locale {
        return Resources.getSystem().configuration.locales[0]
    }

    /**
     * 현재 앱에서 사용 중인 언어 가져오기
     *
     * 앱에서 명시적으로 설정한 언어가 없으면 시스템 언어를 반환합니다.
     *
     * @return 현재 사용 중인 언어 태그
     */
    fun getCurrentLanguageTag(): String {
        return getAppLocale() ?: getSystemPrimaryLocale().toLanguageTag()
    }

    /**
     * 주어진 언어 태그에 해당하는 SupportedLanguage 찾기
     *
     * 지역 코드가 포함된 태그(예: en-US, es-ES, zh-Hans-CN)도 올바르게 매칭합니다.
     * 매칭 우선순위:
     * 1. 정확히 일치하는 태그 (예: zh-Hans)
     * 2. 언어 코드만 일치 (예: en-US → en)
     * 3. 스크립트 포함 언어 코드 일치 (예: zh-Hans-CN → zh-Hans)
     *
     * @param languageTag 언어 태그 (null/빈 문자열은 SYSTEM 반환)
     * @return 해당하는 SupportedLanguage
     */
    fun findSupportedLanguage(languageTag: String?): SupportedLanguage {
        if (languageTag.isNullOrEmpty()) return SupportedLanguage.SYSTEM
        
        // 1. 정확히 일치하는 태그 찾기
        SupportedLanguage.entries.find { it.languageTag == languageTag }?.let { return it }
        
        // 2. Locale 파싱하여 언어 코드 기반 매칭
        val locale = Locale.forLanguageTag(languageTag)
        val language = locale.language  // "en", "es", "ko", "ja", "zh" 등
        val script = locale.script      // "Hans", "Hant" 등 (중국어 간체/번체 구분)
        
        // 스크립트가 있는 경우 (예: zh-Hans-CN → zh-Hans)
        if (script.isNotEmpty()) {
            val languageWithScript = "$language-$script"
            SupportedLanguage.entries.find { it.languageTag == languageWithScript }?.let { return it }
        }
        
        // 언어 코드만으로 매칭 (예: en-US → en, es-ES → es)
        SupportedLanguage.entries.find { 
            it.languageTag.isNotEmpty() && it.languageTag == language 
        }?.let { return it }
        
        return SupportedLanguage.SYSTEM
    }

    /**
     * 모든 지원 언어 목록 반환
     *
     * @return 지원 언어 목록 (시스템 기본값 포함)
     */
    fun getAllSupportedLanguages(): List<SupportedLanguage> {
        return SupportedLanguage.entries.toList()
    }
}
