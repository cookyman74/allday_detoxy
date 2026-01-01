package com.allday.detoxy.presentation.model

import java.time.DayOfWeek

/**
 * 주간 히트맵 UI 모델
 *
 * 스케줄 그룹 상세 화면에서 시간대별 집중 계획을 시각화하는 히트맵을 위한 데이터 모델입니다.
 *
 * @property rows 14개의 히트맵 행 (7요일 × 2 AM/PM)
 * @property summary 주간 요약 정보 (오전/오후 총 시간, 밀집 시간대)
 */
data class WeeklyHeatmapUiModel(
    val rows: List<HeatmapRow>,
    val summary: HeatmapSummary
) {
    companion object {
        val EMPTY = WeeklyHeatmapUiModel(
            rows = emptyList(),
            summary = HeatmapSummary(0, 0, "")
        )
    }
}

/**
 * 히트맵 행 모델
 *
 * 하나의 요일 + 시간대(AM/PM)에 해당하는 12개 셀 정보와 상세 시간 슬롯 정보를 포함합니다.
 *
 * @property dayOfWeek 요일
 * @property period 시간대 (AM: 0~11시, PM: 12~23시)
 * @property cells 12개의 히트맵 셀 (1시간 단위)
 * @property timeSlots 해당 행에 속하는 시간 스케줄 상세 정보 (행 탭 시 표시용)
 * @property totalMinutes 해당 행의 총 계획 시간 (분)
 */
data class HeatmapRow(
    val dayOfWeek: DayOfWeek,
    val period: Period,
    val cells: List<HeatmapCell>,
    val timeSlots: List<TimeSlotInfo>,
    val totalMinutes: Int
)

/**
 * 히트맵 셀 모델
 *
 * 1시간 단위의 셀 정보로, 해당 시간에 계획된 총 분과 색상 레벨을 포함합니다.
 *
 * @property hour 시간 (0-23)
 * @property totalMinutes 해당 시간에 계획된 총 분 (최대 60분으로 cap)
 * @property level 색상 레벨 (NONE, LIGHT, MEDIUM, HIGH, MAX)
 */
data class HeatmapCell(
    val hour: Int,
    val totalMinutes: Int,
    val level: HeatmapLevel
)

/**
 * 시간 슬롯 상세 정보
 *
 * 행 탭 시 표시되는 개별 시간 스케줄의 상세 정보입니다.
 *
 * @property id TimeBasedAutoRun ID (UUID)
 * @property hour 시작 시간
 * @property minute 시작 분
 * @property durationMinutes 집중 시간 (분)
 * @property presetType 차단 프리셋 타입 (FULL_BLOCK, STANDARD, RELAXED)
 * @property label 사용자 지정 라벨 (있는 경우)
 */
data class TimeSlotInfo(
    val id: String,
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int,
    val presetType: String,
    val label: String?
)

/**
 * 히트맵 요약 정보
 *
 * 주간 히트맵 상단에 표시되는 요약 정보입니다.
 *
 * @property amTotalMinutes 오전 총 계획 시간 (분)
 * @property pmTotalMinutes 오후 총 계획 시간 (분)
 * @property peakTimeRange 밀집 시간대 문자열 (예: "오전 9~10시")
 */
data class HeatmapSummary(
    val amTotalMinutes: Int,
    val pmTotalMinutes: Int,
    val peakTimeRange: String
) {
    /**
     * 오전 총 계획 시간을 "n시간 m분" 형식으로 반환
     */
    fun getAmDisplayTime(): String = formatMinutes(amTotalMinutes)

    /**
     * 오후 총 계획 시간을 "n시간 m분" 형식으로 반환
     */
    fun getPmDisplayTime(): String = formatMinutes(pmTotalMinutes)

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            minutes > 0 -> "${minutes}분"
            else -> "0분"
        }
    }
}

/**
 * 시간대 구분 (AM/PM)
 */
enum class Period {
    AM,  // 오전 (0~11시)
    PM   // 오후 (12~23시)
}

/**
 * 히트맵 색상 레벨
 *
 * 계획된 집중 시간에 따라 색상 강도를 5단계로 구분합니다.
 *
 * | Duration | 기호 | 색상 (HEX) | 의미 |
 * |----------|------|-----------|------|
 * | 0분 | ▢ | #E0E0E0 | 계획 없음 |
 * | 1~15분 | ░ | #90CAF9 | 가벼운 집중 |
 * | 16~30분 | ▒ | #42A5F5 | 중간 집중 |
 * | 31~45분 | ▓ | #1976D2 | 높은 집중 |
 * | 46~60분 | █ | #0D47A1 | 최대 집중 |
 */
enum class HeatmapLevel(val minMinutes: Int, val colorHex: String) {
    NONE(0, "#E0E0E0"),
    LIGHT(1, "#90CAF9"),
    MEDIUM(16, "#42A5F5"),
    HIGH(31, "#1976D2"),
    MAX(46, "#0D47A1");

    companion object {
        /**
         * 분 단위 시간에서 적절한 색상 레벨을 반환
         */
        fun fromMinutes(minutes: Int): HeatmapLevel = when {
            minutes >= 46 -> MAX
            minutes >= 31 -> HIGH
            minutes >= 16 -> MEDIUM
            minutes >= 1 -> LIGHT
            else -> NONE
        }
    }
}
