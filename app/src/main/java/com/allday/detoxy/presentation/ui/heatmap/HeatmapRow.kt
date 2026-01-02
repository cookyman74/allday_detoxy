package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.model.HeatmapRow
import com.allday.detoxy.presentation.model.Period
import java.time.format.TextStyle
import java.util.Locale

/**
 * 히트맵 행 Composable
 *
 * 하나의 요일+시간대(AM/PM)를 표시하며, 12개의 셀과 라벨을 포함합니다.
 * 같은 요일의 AM/PM 행은 붙어서 표시되고, 다른 요일 사이에는 구분선이 추가됩니다.
 *
 * ## 접근성
 * - 최소 터치 타겟: 32dp 높이 (컴팩트)
 * - 스크린 리더: "월 오전, 총 60분 계획됨" 형식으로 읽기
 *
 * @param row 히트맵 행 데이터
 * @param onRowClick 행 클릭 콜백 (상세 정보 표시용)
 * @param modifier Modifier
 */
@Composable
fun HeatmapRowItem(
    row: HeatmapRow,
    onRowClick: (HeatmapRow) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabel = row.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
    val periodLabel = when (row.period) {
        Period.AM -> "오전"
        Period.PM -> "오후"
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)  // 더 컴팩트한 높이
            .clickable { onRowClick(row) }
            .semantics {
                contentDescription = "$dayLabel $periodLabel, 총 ${row.totalMinutes}분 계획됨"
            },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 라벨: "월" (AM일 때만 표시, PM은 빈칸)
        Text(
            text = if (row.period == Period.AM) dayLabel else "",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 12개 셀
        row.cells.forEach { cell ->
            HeatmapCell(
                cell = cell,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
