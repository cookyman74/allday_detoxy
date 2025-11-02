package com.allday.detoxy.core.manager

import android.content.Context
import android.util.Log
import com.allday.detoxy.data.local.entity.ScheduleGroup
import com.allday.detoxy.data.local.entity.TimeBasedAutoRun
import com.allday.detoxy.domain.repository.ScheduleGroupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "어디서나 적용" 스케줄 관리자 (Phase 4)
 *
 * 사용자가 어떤 위치 기반 스케줄에도 속하지 않을 때 적용되는
 * 시간만 기반 스케줄의 활성화 및 우선순위를 관리합니다.
 *
 * ## 주요 기능
 * 1. **"어디서나 적용" 스케줄 식별**
 *    - 위치 기반 자동 실행이 연결되지 않은 스케줄 그룹
 *    - linkedScheduleGroupId == null인 그룹
 *
 * 2. **현재 시각/요일 기반 필터링**
 *    - 현재 시각이 시간대 범위 내인지 확인
 *    - 현재 요일이 활성화된 요일인지 확인
 *
 * 3. **우선순위 적용** (기반: 스케줄가동프로세스.md)
 *    - 더 구체적인 스케줄 우선 (평일 > 매일, 주말 > 매일)
 *    - 최근 수정(updatedAt) 우선 (TODO: Phase 0 완료 후)
 *    - 생성 순서(createdAt) 우선
 *
 * ## 사용 시나리오
 * 1. **위치 이탈 후**: GeofenceTransitionsReceiver에서 EXIT 이벤트 발생 시
 * 2. **시간대 전환 시**: AutoRunAlarmReceiver에서 시간대 시작/종료 시
 * 3. **앱 시작 시**: 현재 위치가 어떤 geofence에도 속하지 않을 때
 *
 * @param repository ScheduleGroup 및 연결된 자동 실행 조회
 * @param scheduleGroupManager 스케줄 그룹 활성화/비활성화 관리 (단일 활성화 원칙)
 * @param context Application Context (DI)
 *
 * @see com.allday.detoxy.core.manager.ScheduleGroupManager
 * @see com.allday.detoxy.core.manager.LocationConflictResolver
 * @see docs/03.5_스케쥴가동프로세스.md
 */
@Singleton
class NonLocationScheduleManager @Inject constructor(
    private val repository: ScheduleGroupRepository,
    private val scheduleGroupManager: ScheduleGroupManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NonLocationScheduleMgr"
        
        /**
         * 유효한 요일 코드 목록
         */
        private val VALID_DAY_CODES = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    }

    /**
     * "어디서나 적용" 스케줄 활성화
     *
     * 사용자가 어떤 위치 기반 스케줄에도 속하지 않을 때 호출됩니다.
     * 현재 시각/요일에 맞는 스케줄 중 우선순위가 가장 높은 스케줄을 활성화합니다.
     *
     * ## 처리 흐름
     * 1. 모든 스케줄 그룹 조회
     * 2. "어디서나 적용" 스케줄만 필터링 (위치 연결 없음)
     * 3. 현재 시각/요일에 맞는 스케줄 필터링
     * 4. 우선순위 규칙 적용 (구체성 → updatedAt → createdAt)
     * 5. ScheduleGroupManager.activateGroup()으로 활성화 (단일 활성화 원칙)
     *
     * ## 우선순위 규칙 (기반: 스케줄가동프로세스.md)
     * 1. **더 구체적인 스케줄 우선**
     *    - 특정 요일만 (예: 월,수,금): 3점
     *    - 평일 또는 주말: 2점
     *    - 매일: 1점
     * 2. **최근 수정(updatedAt) 우선** (TODO: Phase 0 완료 후)
     * 3. **생성 순서(createdAt) 우선**: 최신 생성된 스케줄
     *
     * ## 에러 처리
     * - "어디서나 적용" 스케줄 없음: 조기 성공 반환
     * - 현재 시각/요일에 맞는 스케줄 없음: 조기 성공 반환
     * - 활성화 실패: Result.failure 반환
     *
     * @return Result<Unit> 성공 시 Success, 실패 시 Failure
     */
    suspend fun activateDefaultPolicy(): Result<Unit> = runCatching {
        Log.i(TAG, "🔄 Evaluating 'anywhere' schedule policy")
        
        // 1. 모든 스케줄 그룹 조회
        val allGroups = repository.getAll().first()
        Log.d(TAG, "Total schedule groups: ${allGroups.size}")

        // 2. "어디서나 적용" 스케줄만 필터링 (linkedScheduleGroupId == null)
        // ⚠️ getLinkedLocations는 suspend 함수이므로 filter 블록 안에서 호출 불가
        val nonLocationGroups = mutableListOf<ScheduleGroup>()
        for (group in allGroups) {
            val linkedLocations = repository.getLinkedLocations(group.id)
            if (linkedLocations.isEmpty()) {
                nonLocationGroups.add(group)
            }
        }

        Log.d(TAG, "'Anywhere' schedule candidates: ${nonLocationGroups.size}")
        
        if (nonLocationGroups.isEmpty()) {
            Log.d(TAG, "ℹ️ No applicable 'anywhere' schedule")
            return Result.success(Unit)
        }

        // 3. 현재 시각/요일에 맞는 스케줄 필터링
        val now = LocalDateTime.now()
        val currentDay = now.dayOfWeek
        val currentTime = now.toLocalTime()

        Log.d(TAG, "Current time: $currentTime, day: $currentDay")

        // ⚠️ getLinkedTimeBasedAutoRuns는 suspend 함수이므로 filter 블록 안에서 호출 불가
        val applicableCandidates = mutableListOf<ScheduleGroup>()
        for (group in nonLocationGroups) {
            val timeSlots = repository.getLinkedTimeBasedAutoRuns(group.id)
            val hasApplicableSlot = timeSlots.any { slot ->
                // 요일 체크
                val enabledDaysSet = parseEnabledDays(slot.enabledDays)
                val currentDayCode = dayOfWeekToCode(currentDay)
                val daysEnabled = enabledDaysSet.contains(currentDayCode)

                // 시간대 체크
                val slotStart = LocalTime.of(slot.hour, slot.minute)
                val slotEnd = slotStart.plusMinutes(slot.durationMinutes.toLong())
                val inTimeRange = !currentTime.isBefore(slotStart) && !currentTime.isAfter(slotEnd)

                val applicable = daysEnabled && inTimeRange
                
                if (applicable) {
                    Log.d(TAG, "  ✓ ${group.name}: ${slotStart}-${slotEnd}, days: ${enabledDaysSet.size}")
                }
                
                applicable
            }
            
            if (hasApplicableSlot) {
                applicableCandidates.add(group)
            }
        }

        Log.d(TAG, "Applicable 'anywhere' schedules: ${applicableCandidates.size}")

        if (applicableCandidates.isEmpty()) {
            Log.d(TAG, "ℹ️ No applicable 'anywhere' schedule for current time/day")
            return Result.success(Unit)
        }

        // 4. 우선순위 적용 (2개 이상일 때)
        val selectedPolicy = if (applicableCandidates.size == 1) {
            Log.d(TAG, "Single applicable schedule: ${applicableCandidates.first().name}")
            applicableCandidates.first()
        } else {
            Log.d(TAG, "Multiple applicable 'anywhere' schedules: ${applicableCandidates.size}, applying priority rules")
            selectByPriority(applicableCandidates)
        }

        // 5. "어디서나 적용" 스케줄 활성화
        // ⚠️ ScheduleGroupManager.activateGroup()을 사용하여 단일 활성화 원칙 준수
        scheduleGroupManager.activateGroup(selectedPolicy.id).getOrThrow()

        Log.i(TAG, "✅ 'Anywhere' schedule activated: ${selectedPolicy.name}")
    }

    /**
     * "어디서나 적용" 스케줄 우선순위 선택
     *
     * 여러 "어디서나 적용" 스케줄이 현재 시각/요일에 모두 해당할 때
     * 우선순위 규칙에 따라 단일 스케줄을 선택합니다.
     *
     * ## 우선순위 규칙
     * 1. **더 구체적인 스케줄 (평일 > 매일, 주말 > 매일)**
     *    - calculateSpecificityScore()로 점수 계산
     *    - 점수가 높을수록 구체적
     * 2. **최근 수정(updatedAt) 우선** (TODO: updatedAt 필드 추가 시)
     * 3. **생성 순서(createdAt) 우선**
     *    - 가장 최근에 생성된 스케줄
     *
     * @param candidates 후보 스케줄 리스트 (2개 이상)
     * @return 선택된 스케줄
     */
    private suspend fun selectByPriority(candidates: List<ScheduleGroup>): ScheduleGroup {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔍 Priority Selection Start")
        
        // 1. 구체성 점수 계산
        val scored = candidates.map { group ->
            val timeSlots = repository.getLinkedTimeBasedAutoRuns(group.id)
            val specificityScore = calculateSpecificityScore(timeSlots)
            group to specificityScore
        }

        Log.d(TAG, "📊 Specificity scores:")
        scored.forEach { (group, score) ->
            Log.d(TAG, "  - ${group.name}: $score")
        }

        val maxScore = scored.maxOf { it.second }
        var filtered = scored.filter { it.second == maxScore }.map { it.first }

        if (filtered.size == 1) {
            val winner = filtered.first()
            Log.d(TAG, "✅ Winner: ${winner.name} (specificity)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return winner
        }

        Log.d(TAG, "🔄 Tie-breaking: ${filtered.size} schedules with same specificity")

        // 2. 최근 수정 우선
        // TODO: updatedAt 필드 추가 시 사용
        // filtered = filtered.sortedByDescending { it.updatedAt }
        // if (filtered.first().updatedAt != filtered.last().updatedAt) {
        //     val winner = filtered.first()
        //     Log.d(TAG, "✅ Winner: ${winner.name} (recent modification)")
        //     return winner
        // }

        // 3. 생성 순서 우선
        val winner = filtered.maxByOrNull { it.createdAt }!!
        Log.d(TAG, "✅ Winner: ${winner.name} (created at: ${winner.createdAt})")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return winner
    }

    /**
     * 구체성 점수 계산
     *
     * 스케줄의 시간대들이 활성화된 요일 수를 기반으로 구체성을 계산합니다.
     * 활성화된 요일이 적을수록 더 구체적인 스케줄로 간주하여 높은 점수를 부여합니다.
     *
     * ## 점수 체계
     * - **7일 모두 활성화**: 1점 (매일) - 가장 일반적
     * - **5일 활성화**:
     *   - 평일(월~금): 2점
     *   - 기타 5일: 3점 (더 구체적)
     * - **2일 활성화**:
     *   - 주말(토~일): 2점
     *   - 기타 2일: 3점 (더 구체적)
     * - **그 외**: 3점 (가장 구체적)
     *
     * ## 계산 방식
     * 모든 시간대의 활성화된 요일을 합쳐서(distinct) 전체 활성화 요일 수를 구합니다.
     *
     * @param timeSlots 스케줄의 시간대 리스트
     * @return 구체성 점수 (1~3, 높을수록 구체적)
     */
    private fun calculateSpecificityScore(timeSlots: List<TimeBasedAutoRun>): Int {
        if (timeSlots.isEmpty()) {
            return 1  // 시간대 없음 = 가장 일반적
        }
        
        // 모든 시간대의 활성화 요일을 합침
        val allDays = timeSlots.flatMap { parseEnabledDays(it.enabledDays) }.distinct()

        return when (allDays.size) {
            7 -> 1  // 매일 - 가장 일반적
            5 -> {
                val weekdayCodes = setOf("MON", "TUE", "WED", "THU", "FRI")
                val isWeekday = allDays.containsAll(weekdayCodes)
                if (isWeekday) 2 else 3  // 평일 or 특정 5일
            }
            2 -> {
                val weekendCodes = setOf("SAT", "SUN")
                val isWeekend = allDays.containsAll(weekendCodes)
                if (isWeekend) 2 else 3  // 주말 or 특정 2일
            }
            else -> 3  // 특정 요일만 - 가장 구체적
        }
    }

    /**
     * enabledDays JSON 문자열 파싱
     *
     * JSON 배열 문자열을 요일 코드 Set으로 변환합니다.
     * 예: "[\"MON\",\"TUE\",\"WED\"]" → setOf("MON", "TUE", "WED")
     *
     * @param enabledDaysJson JSON 배열 문자열
     * @return 요일 코드 Set (유효한 코드만 포함)
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
            Log.e(TAG, "❌ Failed to parse enabledDays: $enabledDaysJson", e)
            emptySet()
        }
    }

    /**
     * DayOfWeek를 요일 코드로 변환
     *
     * @param dayOfWeek DayOfWeek enum
     * @return 요일 코드 ("MON", "TUE", ...)
     */
    private fun dayOfWeekToCode(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "MON"
            DayOfWeek.TUESDAY -> "TUE"
            DayOfWeek.WEDNESDAY -> "WED"
            DayOfWeek.THURSDAY -> "THU"
            DayOfWeek.FRIDAY -> "FRI"
            DayOfWeek.SATURDAY -> "SAT"
            DayOfWeek.SUNDAY -> "SUN"
        }
    }
}

