package com.allday.detoxy.presentation.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.model.HeatmapCell

/**
 * 히트맵 셀 Composable
 *
 * 1시간 슬롯의 집중 계획 밀도를 색상으로 표현합니다.
 *
 * ## 크기 정책
 * - 높이: 32dp 고정
 * - 폭: 호출부에서 Modifier.weight(1f)로 유동 조정 (화면 적응형)
 * - 패딩: 1dp (셀 간 간격)
 *
 * ## 색상 레벨 (HeatmapLevel) - 청록색(Teal) 계열
 * - NONE: 계획 없음 (#E0F2F1 - Teal 50)
 * - LIGHT: 1~15분 (#80CBC4 - Teal 200)
 * - MEDIUM: 16~30분 (#26A69A - Teal 400)
 * - HIGH: 31~45분 (#00897B - Teal 600)
 * - MAX: 46~60분 (#004D40 - Teal 900)
 *
 * @param cell 히트맵 셀 데이터
 * @param modifier Modifier
 */
@Composable
fun HeatmapCell(
    cell: HeatmapCell,
    modifier: Modifier = Modifier
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(cell.level.colorHex))
    } catch (e: Exception) {
        Color.LightGray
    }
    
    Box(
        modifier = modifier
            .height(32.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .semantics {
                contentDescription = when {
                    cell.totalMinutes == 0 -> "${cell.hour}시 계획 없음"
                    else -> "${cell.hour}시 ${cell.totalMinutes}분 계획됨"
                }
            }
    )
}
