package com.allday.detoxy.presentation.util

import com.allday.detoxy.domain.model.TimeSlot
import java.time.DayOfWeek

/**
 * TimeSlot 포맷팅 확장 함수 모음
 *
 * ## Clean Architecture 준수
 * - 도메인 모델(TimeSlot)은 순수 데이터만 담습니다
 * - UI 표시를 위한 포맷팅 로직은 프리젠테이션 레이어에서 처리합니다
 * - 언어별 문자열 리소스화가 필요한 경우 이 레이어에서 처리합니다
 *
 * ## 사용 예시
 * ```kotlin
 * val slot = TimeSlot(9, 0, 90, "STANDARD", listOf(DayOfWeek.MONDAY))
 * Text(slot.formatStartTime())  // "09:00"
 * Text(slot.formatDuration())    // "1시간 30분"
 * Text(slot.formatEnabledDays()) // "월"
 * ```
 */

/**
 * 시작 시간 포맷팅
 *
 * @return "09:00" 형식의 시간 문자열
 */
fun TimeSlot.formatStartTime(): String {
    return String.format("%02d:%02d", startHour, startMinute)
}

/**
 * 기간 포맷팅
 *
 * @return "1시간 30분", "2시간", "45분" 형식의 기간 문자열
 */
fun TimeSlot.formatDuration(): String {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60
    
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}

/**
 * 요일 포맷팅
 *
 * @return "매일", "평일", "주말", "월,수,금" 형식의 요일 문자열
 */
fun TimeSlot.formatEnabledDays(): String {
    return enabledDays.formatDays()
}

/**
 * 요일 목록 포맷팅 (독립 함수)
 *
 * QuickCreateScheduleDialog 등에서 List<DayOfWeek>를 직접 포맷팅할 때 사용
 *
 * @return "매일", "평일", "주말", "월,수,금" 형식의 요일 문자열
 */
fun List<DayOfWeek>.formatDays(): String {
    return when {
        size == 7 -> "매일"
        size == 5 && containsAll(listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )) -> "평일"
        size == 2 && containsAll(listOf(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        )) -> "주말"
        else -> joinToString(",") { it.toKoreanShort() }
    }
}

/**
 * DayOfWeek를 한국어 짧은 형식으로 변환
 *
 * @return "월", "화", "수", "목", "금", "토", "일"
 */
fun DayOfWeek.toKoreanShort(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}

