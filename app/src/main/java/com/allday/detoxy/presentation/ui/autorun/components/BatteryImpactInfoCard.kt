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
 * 배터리 영향 안내 카드
 *
 * 시간 기반 자동 실행의 배터리 영향을 안내하고, 최적화 팁을 제공합니다.
 * 접을 수 있는 카드로 구현되어 있습니다.
 *
 * @param impactLevel 영향 수준 (예: "최소", "낮음")
 * @param estimatedPercentage 예상 배터리 소모 퍼센트 (예: "< 1%/일")
 * @param description 설명 텍스트
 */
@Composable
fun BatteryImpactInfoCard(
    impactLevel: String,
    estimatedPercentage: String,
    description: String
) {
    var isExpanded by remember { mutableStateOf(false) }

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
                            text = "배터리 영향: $impactLevel",
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
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // 확장된 내용 (최적화 팁)
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider()

                    Text(
                        text = "최적화 팁",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OptimizationTip(
                        icon = "💡",
                        text = "사용하지 않는 시간대는 비활성화하세요"
                    )

                    OptimizationTip(
                        icon = "⏰",
                        text = "시간대를 5개 이하로 유지하면 배터리 소모를 최소화할 수 있습니다"
                    )

                    OptimizationTip(
                        icon = "🔋",
                        text = "정확 알람 권한을 허용하면 더 효율적으로 작동합니다"
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
private fun OptimizationTip(
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

