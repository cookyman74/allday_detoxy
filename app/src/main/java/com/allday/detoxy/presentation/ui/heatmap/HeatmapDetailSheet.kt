package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.model.HeatmapRow
import com.allday.detoxy.presentation.model.Period
import com.allday.detoxy.presentation.model.TimeSlotInfo
import java.time.format.TextStyle
import java.util.Locale

/**
 * 히트맵 상세 정보 BottomSheet
 *
 * 히트맵 행(요일+AM/PM)을 탭했을 때 표시되는 바텀시트입니다.
 * 해당 시간대에 계획된 시간 스케줄들의 상세 정보를 보여줍니다.
 *
 * ## 표시 정보
 * - 행 제목: "월요일 오전" 등
 * - 총 계획 시간
 * - 개별 시간 스케줄 목록
 *   - 시작 시간, 집중 시간
 *   - 차단 프리셋
 *   - 라벨 (있는 경우)
 *
 * @param row 선택된 히트맵 행
 * @param onDismiss 바텀시트 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapDetailSheet(
    row: HeatmapRow,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    val dayLabel = row.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    val periodLabel = when (row.period) {
        Period.AM -> "오전"
        Period.PM -> "오후"
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 헤더
            Text(
                text = "$dayLabel $periodLabel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 총 계획 시간
            Text(
                text = "총 ${row.totalMinutes}분 계획됨",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider()
            
            // 시간 스케줄 목록
            if (row.timeSlots.isEmpty()) {
                Text(
                    text = "이 시간대에 계획된 스케줄이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                row.timeSlots.forEach { slot ->
                    TimeSlotItem(slot = slot)
                }
            }
        }
    }
}

/**
 * 시간 스케줄 아이템
 */
@Composable
private fun TimeSlotItem(
    slot: TimeSlotInfo,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            // 시간 및 집중 시간
            Text(
                text = "${formatTime(slot.hour, slot.minute)} - ${slot.durationMinutes}분",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            // 라벨 (있는 경우)
            if (!slot.label.isNullOrEmpty()) {
                Text(
                    text = slot.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 차단 프리셋
        Text(
            text = getPresetDisplayName(slot.presetType),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * 시간 포맷 함수
 */
private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "오전" else "오후"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%s %d:%02d", period, displayHour, minute)
}

/**
 * 프리셋 표시명 반환
 */
private fun getPresetDisplayName(presetType: String): String = when (presetType) {
    "FULL_BLOCK" -> "완전 차단"
    "STANDARD" -> "표준"
    "RELAXED" -> "여유"
    else -> presetType
}
