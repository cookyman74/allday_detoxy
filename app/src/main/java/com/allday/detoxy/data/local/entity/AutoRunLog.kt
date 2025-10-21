package com.allday.detoxy.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 자동 실행 이력 엔티티
 *
 * 시간/위치 기반 자동 실행 트리거 이력 및 결과를 기록합니다.
 * Analytics 및 통계 분석에 사용됩니다.
 *
 * ## 주요 기능
 * - 모든 자동 실행 시도 기록 (성공/실패 무관)
 * - 실패 사유 상세 기록
 * - GPS 정확도 및 체류 시간 로깅 (위치 기반)
 * - FocusSession 연계 (성공 시)
 *
 * ## 예시
 * ```kotlin
 * // 시간 기반 자동 실행 성공
 * AutoRunLog(
 *     triggerType = "TIME",
 *     triggerSourceId = "time-uuid-1",
 *     triggerTime = System.currentTimeMillis(),
 *     result = "STARTED",
 *     sessionId = "session-uuid-1"
 * )
 *
 * // 위치 기반 자동 실행 성공 (GPS 정보 포함)
 * AutoRunLog(
 *     triggerType = "LOCATION",
 *     triggerSourceId = "location-uuid-1",
 *     triggerTime = System.currentTimeMillis(),
 *     result = "STARTED",
 *     sessionId = "session-uuid-2",
 *     gpsAccuracyMeters = 18.5f,
 *     dwellSeconds = 65
 * )
 * ```
 *
 * @property id 고유 ID (UUID)
 * @property triggerType 트리거 타입 (TIME, LOCATION)
 * @property triggerSourceId 트리거 소스 ID (TimeBasedAutoRun.id 또는 LocationBasedAutoRun.id)
 * @property triggerTime 트리거 발생 시간 (timestamp)
 * @property result 결과 (STARTED, SKIPPED, FAILED)
 * @property failureReason 실패 사유 (result == FAILED일 때)
 * @property sessionId 생성된 FocusSession ID (result == STARTED일 때)
 * @property gpsAccuracyMeters GPS 정확도 (미터, LOCATION 트리거만)
 * @property dwellSeconds 실제 체류 시간 (초, LOCATION 트리거만)
 * @property metaJson 추가 메타데이터 (JSON 형식)
 */
@Entity(
    tableName = "auto_run_log",
    foreignKeys = [
        ForeignKey(
            entity = FocusSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["triggerTime"]),
        Index(value = ["triggerType", "triggerTime"]),
        Index(value = ["sessionId"])
    ]
)
data class AutoRunLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 트리거 타입
     * - TIME: 시간 기반 자동 실행
     * - LOCATION: 위치 기반 자동 실행
     */
    val triggerType: String,

    /**
     * 트리거 소스 ID (Soft Reference)
     *
     * - TIME: TimeBasedAutoRun.id
     * - LOCATION: LocationBasedAutoRun.id
     *
     * 참고: Foreign Key 없음 (원본 삭제 시에도 로그 유지)
     */
    val triggerSourceId: String,

    /**
     * 트리거 발생 시간 (timestamp)
     */
    val triggerTime: Long,

    /**
     * 결과
     * - STARTED: 타이머 정상 시작
     * - SKIPPED: 사용자가 건너뜀
     * - FAILED: 실패 (권한, 이미 실행 중 등)
     */
    val result: String,

    /**
     * 실패 사유 (result == FAILED일 때)
     *
     * - PERMISSION_DENIED: 권한 없음
     * - TIMER_ALREADY_RUNNING: 이미 타이머 실행 중
     * - LOCATION_DISABLED: 위치 서비스 OFF
     * - USER_PAUSED: 사용자가 일시중지
     * - GEOFENCE_ERROR: Geofence 오류
     */
    val failureReason: String? = null,

    /**
     * 생성된 FocusSession ID (result == STARTED일 때)
     *
     * FocusSession FK (ON DELETE SET NULL)
     */
    val sessionId: String? = null,

    /**
     * GPS 정확도 (미터, LOCATION 트리거만)
     *
     * GeofencingEvent에서 가져온 GPS 정확도.
     * UI에서 "높음(< 20m)", "보통(20~50m)", "낮음(> 50m)" 표시에 활용.
     * 위치별 성공률 분석 시 정확도가 낮은 경우 필터링.
     *
     * @see [PRD §4.4.5 위치 기반 신뢰도 강화]
     * @see [마이그레이션 전략 §2.4 GPS 정확도 로깅]
     */
    val gpsAccuracyMeters: Float? = null,

    /**
     * 실제 체류 시간 (초, LOCATION 트리거만)
     *
     * 지오펜스 진입부터 트리거까지 경과 시간.
     * LocationBasedAutoRun.dwellTimeMinutes와 비교하여 정확도 검증.
     *
     * @see [PRD §4.4.5 위치 기반 신뢰도 강화]
     */
    val dwellSeconds: Int? = null,

    /**
     * 추가 메타데이터 (JSON 형식)
     *
     * 향후 확장성을 위한 유연한 필드.
     * 예: {"battery": 85, "network": "WIFI", "playServicesVersion": "21.0.0"}
     */
    val metaJson: String? = null
)

