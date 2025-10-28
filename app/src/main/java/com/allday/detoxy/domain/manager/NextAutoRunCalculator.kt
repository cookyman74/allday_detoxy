package com.allday.detoxy.domain.manager

import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.AutoRunSettingsRepository
import com.allday.detoxy.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 다음 예정 자동 실행 계산기
 *
 * 활성화된 시간 기반 자동 실행 중 가장 가까운 시각을 계산합니다.
 * 
 * ## 주요 기능
 * - 시간 기반 자동 실행 중 가장 가까운 시각 계산
 * - 요일별 활성화 상태 고려
 * - 글로벌 일시중지 상태 고려 (`autoRunPauseUntil`)
 * - 주말 제외 옵션 지원
 *
 * ## 위치 기반 자동 실행 처리
 * 위치 기반 자동 실행은 Geofence 진입 시간을 예측할 수 없으므로
 * 현재 버전에서는 "다음 예정"에 포함하지 않습니다.
 * 
 * @see docs/02.5_autosetting_todolist.md §1.1.1
 * @see com.allday.detoxy.core.manager.AutoRunAlarmManager.calculateNextTriggerTime
 *
 * @param autoRunSettingsRepository 자동 실행 글로벌 설정 Repository
 * @param userSettingsRepository 사용자 설정 Repository
 */
@Singleton
class NextAutoRunCalculator @Inject constructor(
    private val autoRunSettingsRepository: AutoRunSettingsRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    companion object {
        private const val TAG = "NextAutoRunCalculator"
        
        // 유효한 요일 코드 (대문자)
        private val VALID_DAY_CODES = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        
        // 최대 검색 일수 (2주)
        private const val MAX_SEARCH_DAYS = 13
    }

    /**
     * 활성화된 시간 기반 자동 실행 중 가장 가까운 시각을 계산합니다.
     *
     * ## 계산 로직
     * 1. 마스터 스위치가 꺼져있으면 null 반환
     * 2. 글로벌 일시중지 상태이면 null 반환 (일시중지 종료 시각 이후부터 재계산)
     * 3. 활성화된 자동 실행 리스트를 순회하며 각각의 다음 트리거 시각 계산
     * 4. 가장 가까운 시각을 반환
     *
     * ## 글로벌 옵션 적용
     * - **주말 제외**: excludeWeekends 설정이 true이면 토요일/일요일 건너뛰기
     * - **일시중지**: autoRunPauseUntil 설정이 현재 시각보다 미래이면 일시중지 상태로 처리
     *
     * @param enabledAutoRuns 활성화된 시간 기반 자동 실행 리스트
     * @return 가장 가까운 자동 실행 정보, null: 계산 불가 또는 일시중지 상태
     */
    suspend fun calculateNextAutoRun(enabledAutoRuns: List<TimeBasedAutoRun>): NextAutoRunInfo? {
        // 1. 마스터 스위치 체크
        val masterEnabled = userSettingsRepository.getAutoRunMasterEnabled()
        if (!masterEnabled) {
            return null
        }

        // 2. 글로벌 일시중지 상태 체크
        val pauseUntil = userSettingsRepository.getAutoRunPauseUntil()
        val now = System.currentTimeMillis()
        if (pauseUntil > now) {
            // 일시중지 중 - null 반환
            return null
        }

        // 3. 활성화된 자동 실행이 없으면 null 반환
        if (enabledAutoRuns.isEmpty()) {
            return null
        }

        // 4. 글로벌 옵션: 주말 제외 설정 확인
        val excludeWeekends = autoRunSettingsRepository.getExcludeWeekends().first()

        // 5. 각 자동 실행의 다음 트리거 시각 계산
        val nextTriggers = enabledAutoRuns.mapNotNull { autoRun ->
            calculateNextTriggerTime(autoRun, excludeWeekends)?.let { triggerTime ->
                NextAutoRunInfo(
                    triggerType = "TIME",
                    triggerTime = triggerTime,
                    timeUntilTrigger = formatTimeUntil(triggerTime),
                    label = autoRun.label ?: formatTime(autoRun.hour, autoRun.minute),
                    durationMinutes = autoRun.durationMinutes,
                    presetType = autoRun.presetType,
                    sourceId = autoRun.id,
                    confidence = null // 시간 기반은 신뢰도 100%이므로 null
                )
            }
        }

        // 6. 가장 가까운 시각 반환
        return nextTriggers.minByOrNull { it.triggerTime }
    }

    /**
     * 특정 시간 기반 자동 실행의 다음 트리거 시각을 계산합니다.
     *
     * @param autoRun 시간 기반 자동 실행 설정
     * @param excludeWeekends 주말 제외 여부
     * @return 다음 트리거 시각 (epoch millis), null: 계산 실패
     */
    private fun calculateNextTriggerTime(autoRun: TimeBasedAutoRun, excludeWeekends: Boolean): Long? {
        val enabledDays = parseEnabledDays(autoRun.enabledDays)
        if (enabledDays.isEmpty()) {
            return null
        }

        val now = Calendar.getInstance()
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, autoRun.hour)
            set(Calendar.MINUTE, autoRun.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 오늘부터 최대 13일(2주) 검색
        for (daysToAdd in 0..MAX_SEARCH_DAYS) {
            val checkCalendar = targetCalendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
            
            // 현재 시각 이후만 허용
            if (checkCalendar.timeInMillis <= now.timeInMillis) {
                continue
            }
            
            // 요일 확인
            val dayOfWeek = getDayOfWeekString(checkCalendar)
            
            // 주말 제외 옵션 적용
            if (excludeWeekends && (dayOfWeek == "SAT" || dayOfWeek == "SUN")) {
                continue
            }
            
            if (enabledDays.contains(dayOfWeek)) {
                return checkCalendar.timeInMillis
            }
        }

        return null
    }

    /**
     * enabledDays JSON 파싱
     *
     * @param enabledDaysJson JSON 문자열 (예: "[\"MON\",\"TUE\",\"WED\"]")
     * @return 요일 코드 Set (예: setOf("MON", "TUE", "WED"))
     */
    private fun parseEnabledDays(enabledDaysJson: String): Set<String> {
        if (enabledDaysJson.isBlank() || enabledDaysJson == "[]") {
            return emptySet()
        }
        
        return try {
            // 간단한 JSON 파싱 (정규식 사용)
            val regex = Regex(""""(\w+)"""")
            regex.findAll(enabledDaysJson)
                .map { it.groupValues[1].uppercase() }
                .filter { it in VALID_DAY_CODES }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Calendar 요일 → 문자열 변환
     *
     * @param calendar Calendar 인스턴스
     * @return 요일 코드 (MON, TUE, WED, THU, FRI, SAT, SUN)
     */
    private fun getDayOfWeekString(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            Calendar.SUNDAY -> "SUN"
            else -> "UNKNOWN"
        }
    }

    /**
     * 시각 포맷팅 (HH:mm 형식)
     *
     * @param hour 시간 (0-23)
     * @param minute 분 (0-59)
     * @return 포맷된 시각 문자열 (예: "14:30")
     */
    private fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * 남은 시간 포맷팅
     *
     * @param targetTimeMillis 목표 시각 (epoch millis)
     * @return 포맷된 남은 시간 문자열 (예: "1시간 30분 후", "5분 후", "내일 오전 9시")
     */
    private fun formatTimeUntil(targetTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = targetTimeMillis - now
        
        if (diffMillis < 0) {
            return "지금"
        }

        val diffMinutes = (diffMillis / (1000 * 60)).toInt()
        
        return when {
            diffMinutes < 60 -> "${diffMinutes}분 후"
            diffMinutes < 1440 -> { // 24시간 이내
                val hours = diffMinutes / 60
                val minutes = diffMinutes % 60
                if (minutes == 0) {
                    "${hours}시간 후"
                } else {
                    "${hours}시간 ${minutes}분 후"
                }
            }
            diffMinutes < 2880 -> { // 48시간 이내
                val targetCalendar = Calendar.getInstance().apply {
                    timeInMillis = targetTimeMillis
                }
                val hour = targetCalendar.get(Calendar.HOUR_OF_DAY)
                val minute = targetCalendar.get(Calendar.MINUTE)
                val amPm = if (hour < 12) "오전" else "오후"
                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "내일 $amPm ${displayHour}:${String.format("%02d", minute)}"
            }
            else -> { // 48시간 이후
                val targetCalendar = Calendar.getInstance().apply {
                    timeInMillis = targetTimeMillis
                }
                val dayOfWeek = when (targetCalendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "월"
                    Calendar.TUESDAY -> "화"
                    Calendar.WEDNESDAY -> "수"
                    Calendar.THURSDAY -> "목"
                    Calendar.FRIDAY -> "금"
                    Calendar.SATURDAY -> "토"
                    Calendar.SUNDAY -> "일"
                    else -> ""
                }
                val hour = targetCalendar.get(Calendar.HOUR_OF_DAY)
                val minute = targetCalendar.get(Calendar.MINUTE)
                val amPm = if (hour < 12) "오전" else "오후"
                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "$dayOfWeek요일 $amPm ${displayHour}:${String.format("%02d", minute)}"
            }
        }
    }
}

/**
 * 다음 예정 자동 실행 정보 데이터 클래스
 *
 * @property triggerType 트리거 타입 (TIME, LOCATION)
 * @property triggerTime 트리거 시각 (epoch millis)
 * @property timeUntilTrigger 남은 시간 (예: "1시간 30분 후")
 * @property label 라벨 (예: "오후 업무", "회사")
 * @property durationMinutes 타이머 시간 (분)
 * @property presetType 차단 프리셋 (FULL_BLOCK, STANDARD, RELAXED)
 * @property sourceId 자동 실행 ID
 * @property confidence 신뢰도 (0.0~1.0, 위치 기반일 때만 사용)
 */
data class NextAutoRunInfo(
    val triggerType: String, // TIME, LOCATION
    val triggerTime: Long,
    val timeUntilTrigger: String, // "1시간 30분 후"
    val label: String, // "오후 업무", "회사"
    val durationMinutes: Int,
    val presetType: String,
    val sourceId: String,
    val confidence: Float? = null // 위치 기반일 때 신뢰도 (0.0~1.0)
)

