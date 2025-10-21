package com.allday.detoxy.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 위치 기반 자동 실행 엔티티
 *
 * 특정 위치에 도착하면 집중 모드를 자동으로 시작하는 설정을 저장합니다.
 *
 * ## 주요 기능
 * - 특정 위치(회사, 학교 등) 도착 시 자동 실행
 * - 반경 설정 (50m, 100m, 200m, 500m)
 * - 체류 시간 설정 (0/1/3/5분)
 * - 도착 후 사용자 확인 옵션
 * - 주기적 트리거 설정
 *
 * ## 예시
 * ```kotlin
 * LocationBasedAutoRun(
 *     label = "회사",
 *     address = "서울특별시 강남구 ...",
 *     latitude = 37.123456,
 *     longitude = 127.123456,
 *     radiusMeters = 100,
 *     durationMinutes = 60,
 *     presetType = "STANDARD",
 *     triggerType = "ENTER",
 *     dwellTimeMinutes = 3
 * )
 * ```
 *
 * @property id 고유 ID (UUID)
 * @property label 위치 라벨 (예: "회사", "도서관")
 * @property address 주소 (옵션, Google Places API 검색 실패 시 null)
 * @property latitude 위도
 * @property longitude 경도
 * @property radiusMeters 반경 (미터)
 * @property durationMinutes 타이머 시간 (분)
 * @property presetType 차단 프리셋
 * @property triggerType 트리거 타입 (ENTER, PERIODIC)
 * @property periodicIntervalMinutes 주기적 트리거 간격 (분, PERIODIC일 때만)
 * @property dwellTimeMinutes 체류 시간 (0/1/3/5분, GPS 정확도 향상)
 * @property requiresUserConfirmation 도착 후 확인 필요 (true면 알림만, 수동 시작)
 * @property isEnabled 활성화 여부
 * @property createdAt 생성 시간 (timestamp)
 */
@Entity(
    tableName = "location_based_auto_run",
    indices = [
        Index(value = ["isEnabled"])
    ]
)
data class LocationBasedAutoRun(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 위치 라벨
     *
     * 사용자가 지정한 이름 (예: "회사", "도서관", "학교")
     */
    val label: String,

    /**
     * 주소 (옵션)
     *
     * Google Places API 검색 결과 주소.
     * 검색 실패 시 null 가능 (좌표만으로 Geofence 등록 가능)
     */
    val address: String? = null,

    /**
     * 위도
     */
    val latitude: Double,

    /**
     * 경도
     */
    val longitude: Double,

    /**
     * 반경 (미터)
     *
     * 허용값: 50, 100, 200, 500
     */
    val radiusMeters: Int,

    /**
     * 타이머 시간 (분)
     */
    val durationMinutes: Int,

    /**
     * 차단 프리셋 타입
     * - FULL_BLOCK: 완전 차단
     * - STANDARD: 표준 (메신저 허용)
     * - RELAXED: 완화 (SNS 허용)
     */
    val presetType: String,

    /**
     * 트리거 타입
     * - ENTER: 지오펜스 진입 시 1회 트리거
     * - PERIODIC: 진입 후 주기적 트리거
     */
    val triggerType: String,

    /**
     * 주기적 트리거 간격 (분)
     *
     * triggerType이 PERIODIC일 때만 사용
     */
    val periodicIntervalMinutes: Int? = null,

    /**
     * 체류 시간 (분)
     *
     * 위치에 도착한 후 N분 체류 확인 시 트리거.
     * GPS 정확도 향상 및 오차 보정에 사용.
     * 허용값: 0, 1, 3, 5
     *
     * @see [PRD §4.4.5 위치 기반 신뢰도 강화]
     */
    val dwellTimeMinutes: Int = 0,

    /**
     * 도착 후 확인 필요
     *
     * true: 알림만 표시, 사용자 확인 후 수동 시작
     * false: 자동 시작 (dwellTimeMinutes 후)
     *
     * @see [PRD §4.4.5 위치 기반 신뢰도 강화]
     */
    val requiresUserConfirmation: Boolean = false,

    /**
     * 활성화 여부
     */
    val isEnabled: Boolean = true,

    /**
     * 생성 시간 (timestamp)
     */
    val createdAt: Long = System.currentTimeMillis()
)

