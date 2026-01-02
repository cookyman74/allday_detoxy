package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.model.HeatmapRow
import com.allday.detoxy.presentation.model.WeeklyHeatmapUiModel

/**
 * 주간 히트맵 Composable
 *
 * 7일 × 2(AM/PM) = 14개 행으로 구성된 히트맵을 표시합니다.
 * 상단에 요약 정보와 시간 축 헤더를 포함합니다.
 *
 * ## UI 구조
 * - 요약 정보: 오전/오후 총 시간, 밀집 시간대
 * - 시간 축 헤더: 0-11시 또는 12-23시 레이블
 * - 14개 히트맵 행
 * - 범례
 *
 * @param heatmap 주간 히트맵 데이터
 * @param onRowClick 행 클릭 콜백 (상세 정보 표시용)
 * @param modifier Modifier
 */
@Composable
fun WeeklyHeatmap(
    heatmap: WeeklyHeatmapUiModel,
    onRowClick: (HeatmapRow) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 요약 정보
        if (heatmap.summary.amTotalMinutes > 0 || heatmap.summary.pmTotalMinutes > 0) {
            HeatmapSummarySection(
                amTotal = heatmap.summary.getAmDisplayTime(),
                pmTotal = heatmap.summary.getPmDisplayTime(),
                peakTime = heatmap.summary.peakTimeRange
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 히트맵 행들
        heatmap.rows.forEach { row ->
            HeatmapRowItem(
                row = row,
                onRowClick = onRowClick
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 범례
        HeatmapLegend()
    }
}

/**
 * 히트맵 요약 섹션
 */
@Composable
private fun HeatmapSummarySection(
    amTotal: String,
    pmTotal: String,
    peakTime: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 오전 총 시간
            SummaryItem(
                label = "오전",
                value = amTotal
            )
            
            // 오후 총 시간
            SummaryItem(
                label = "오후",
                value = pmTotal
            )
        }
        
        // 밀집 시간대
        if (peakTime.isNotEmpty()) {
            Text(
                text = "📍 밀집 시간대: $peakTime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 요약 아이템
 */
@Composable
private fun SummaryItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
