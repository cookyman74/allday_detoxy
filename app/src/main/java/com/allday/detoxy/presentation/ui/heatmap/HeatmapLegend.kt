package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.model.HeatmapLevel

/**
 * 히트맵 범례 Composable
 *
 * 히트맵 색상 레벨의 의미를 설명하는 범례입니다.
 *
 * | 색상 | 의미 |
 * |------|------|
 * | #E0E0E0 | 계획 없음 |
 * | #90CAF9 | 1~15분 |
 * | #42A5F5 | 16~30분 |
 * | #1976D2 | 31~45분 |
 * | #0D47A1 | 46~60분 |
 *
 * @param modifier Modifier
 */
@Composable
fun HeatmapLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "범례:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        LegendItem(
            level = HeatmapLevel.NONE,
            label = "없음"
        )
        
        LegendItem(
            level = HeatmapLevel.LIGHT,
            label = "~15분"
        )
        
        LegendItem(
            level = HeatmapLevel.MEDIUM,
            label = "~30분"
        )
        
        LegendItem(
            level = HeatmapLevel.HIGH,
            label = "~45분"
        )
        
        LegendItem(
            level = HeatmapLevel.MAX,
            label = "~60분"
        )
    }
}

/**
 * 범례 아이템
 */
@Composable
private fun LegendItem(
    level: HeatmapLevel,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(level.colorHex))
    } catch (e: Exception) {
        Color.LightGray
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(backgroundColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
