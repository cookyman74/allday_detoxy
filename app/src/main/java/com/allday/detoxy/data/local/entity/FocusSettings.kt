package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 디톡시 제어 설정 (카테고리별 차단 설정)
 *
 * 현재는 DataStore를 통해 관리되지만, v3에서는 Room DB에 추가로 저장하여
 * 설정 변경 이력 추적과 백업/복원을 용이하게 합니다.
 *
 * v3 신규 엔티티 (Week 2B)
 *
 * **Singleton 패턴**: ID가 항상 1인 단일 레코드만 사용
 *
 * @property id 고유 ID (항상 1, Singleton)
 * @property snsEnabled SNS 카테고리 차단 활성화 여부
 * @property messengerEnabled 메신저 카테고리 차단 활성화 여부 (기본 OFF)
 * @property webEnabled 웹 브라우저 카테고리 차단 활성화 여부
 * @property videoEnabled 영상/숏폼 카테고리 차단 활성화 여부
 * @property otherEnabled 기타 앱 카테고리 차단 활성화 여부
 * @property lastUpdated 마지막 업데이트 시각 (Unix timestamp, milliseconds)
 */
@Entity(tableName = "focus_settings")
data class FocusSettings(
    @PrimaryKey
    val id: Int = 1,  // Singleton: 항상 1
    val snsEnabled: Boolean = true,
    val messengerEnabled: Boolean = false,  // 기본 OFF (메신저는 긴급 연락 용도)
    val webEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val otherEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

