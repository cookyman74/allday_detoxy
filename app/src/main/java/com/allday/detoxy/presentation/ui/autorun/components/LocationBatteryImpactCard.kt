package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 위치 기반 배터리 영향 안내 카드
 *
 * 위치 기반 자동 실행의 배터리 영향을 안내하고, 최적화 팁을 제공합니다.
 *
 * @param enabledCount 활성화된 위치 기반 자동 실행 개수
 */
@Composable
fun LocationBatteryImpactCard(
    enabledCount: Int
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 예상 배터리 소모 (위치 개수에 따라)
    val estimatedPercentage = when (enabledCount) {
        0 -> "0%/일"
        1 -> "2~3%/일"
        2 -> "3~4%/일"
        3 -> "4~5%/일"
        4 -> "5~6%/일"
        5 -> "6~7%/일"
        else -> "6~7%/일"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔋",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Column {
                        Text(
                            text = "배터리 영향: 최소 수준",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = estimatedPercentage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "현재 설정으로 예상 배터리 소모입니다.\n(활성화된 위치: ${enabledCount}개)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // 확장된 내용 (최적화 팁)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "💡 최적화 팁",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    LocationOptimizationTip(
                        icon = "📍",
                        text = "위치 기반 자동 실행을 2개 이하로 유지하세요"
                    )

                    LocationOptimizationTip(
                        icon = "📏",
                        text = "반경을 너무 작게 설정하지 마세요 (100m 이상 권장)"
                    )

                    LocationOptimizationTip(
                        icon = "🔕",
                        text = "사용하지 않는 위치는 비활성화하세요"
                    )

                    LocationOptimizationTip(
                        icon = "🔋",
                        text = "체류 시간을 1분 이상으로 설정하면 더 효율적입니다"
                    )

                    HorizontalDivider()

                    Text(
                        text = "📊 실제 소모량은 설정 → 배터리에서 확인 가능합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 최적화 팁 아이템
 */
@Composable
private fun LocationOptimizationTip(
    icon: String,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

