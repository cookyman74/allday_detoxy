package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 시간 기반 자동 실행 글로벌 옵션 섹션
 *
 * 주말 제외, 자동 시작 딜레이, 사전 알림 시간 설정을 제공합니다.
 *
 * @param excludeWeekends 주말 제외 설정
 * @param autoStartDelayMinutes 자동 시작 딜레이 (분)
 * @param preNotificationMinutes 사전 알림 시간 (분)
 * @param onExcludeWeekendsChange 주말 제외 설정 변경 콜백
 * @param onAutoStartDelayChange 자동 시작 딜레이 변경 콜백
 * @param onPreNotificationChange 사전 알림 시간 변경 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalOptionsSection(
    excludeWeekends: Boolean,
    autoStartDelayMinutes: Int,
    preNotificationMinutes: Int,
    onExcludeWeekendsChange: (Boolean) -> Unit,
    onAutoStartDelayChange: (Int) -> Unit,
    onPreNotificationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 섹션 헤더
        Text(
            text = "글로벌 옵션",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 주말 제외 토글
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "주말 제외",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "토요일과 일요일에는 자동 실행하지 않습니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = excludeWeekends,
                        onCheckedChange = onExcludeWeekendsChange
                    )
                }

                HorizontalDivider()

                // 2. 자동 시작 딜레이 선택
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "자동 시작 딜레이",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "알림 후 사용자 응답이 없을 때 자동으로 타이머를 시작합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0, 5, 10).forEachIndexed { index, minutes ->
                            SegmentedButton(
                                selected = autoStartDelayMinutes == minutes,
                                onClick = { onAutoStartDelayChange(minutes) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = 3
                                )
                            ) {
                                Text(
                                    text = if (minutes == 0) "즉시" else "${minutes}분"
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 3. 사전 알림 시간 선택
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "사전 알림",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "실행 시간 N분 전에 사전 알림을 보냅니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0, 5, 10, 15).forEachIndexed { index, minutes ->
                            SegmentedButton(
                                selected = preNotificationMinutes == minutes,
                                onClick = { onPreNotificationChange(minutes) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = 4
                                )
                            ) {
                                Text(
                                    text = if (minutes == 0) "없음" else "${minutes}분"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

