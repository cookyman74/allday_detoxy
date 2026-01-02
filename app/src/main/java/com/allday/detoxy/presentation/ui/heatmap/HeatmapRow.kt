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
import java.time.format.TextStyle
import java.util.Locale

/**
 * 히트맵 행 Composable (v1.1: 7행 × 24셀 컴팩트 구조)
 *
 * 하나의 요일을 표시하며, 24개의 셀(0~23시)과 라벨을 포함합니다.
 * 행 전체를 탭하면 해당 요일의 상세 정보를 표시합니다.
 *
 * ## 접근성
 * - 최소 터치 타겟: 48dp 높이
 * - 스크린 리더: "월, 총 60분 계획됨" 형식으로 읽기
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
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)  // v1.1: 더 컴팩트한 높이
            .clickable { onRowClick(row) }
            .semantics {
                contentDescription = "$dayLabel, 총 ${row.totalMinutes}분 계획됨"
            },
        horizontalArrangement = Arrangement.spacedBy(1.dp),  // v1.1: 셀 간격 줄임
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 라벨: "월"
        Text(
            text = dayLabel,
            modifier = Modifier.width(32.dp),  // v1.1: 더 컴팩트한 라벨
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 24개 셀 (0~23시)
        row.cells.forEach { cell ->
            HeatmapCell(
                cell = cell,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
