package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 히트맵 빈 상태 UI
 *
 * 시간 스케줄이 없을 때 표시되는 안내 UI입니다.
 * CTA 버튼을 통해 시간표 관리 화면으로 이동합니다.
 *
 * ## 디자인
 * - 중앙 정렬된 안내 텍스트
 * - "시간표 관리로 이동" 버튼
 * - 주변 컴포넌트와 일관된 패딩
 *
 * @param onNavigateToTimeScreen 시간표 관리 화면 이동 콜백
 * @param modifier Modifier
 */
@Composable
fun HeatmapEmptyState(
    onNavigateToTimeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 안내 아이콘
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.height(32.dp)
        )
        
        // 안내 텍스트
        Text(
            text = "시간 스케줄을 추가해보세요",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // CTA 버튼
        Button(onClick = onNavigateToTimeScreen) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("시간표 관리로 이동")
        }
    }
}
