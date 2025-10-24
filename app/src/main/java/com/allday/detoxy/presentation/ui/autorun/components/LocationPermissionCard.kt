package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 위치 권한 안내 카드
 *
 * 위치 권한이 없을 때 표시되는 카드입니다.
 * 권한의 필요성을 설명하고 권한 설정 화면으로 이동하는 버튼을 제공합니다.
 *
 * @param hasLocationPermission 정확한 위치 권한 부여 여부
 * @param hasBackgroundPermission 백그라운드 위치 권한 부여 여부
 * @param onRequestPermission 권한 설정 요청 콜백
 * @param onNavigateToTimeBased 시간 기반 자동 실행으로 이동 콜백
 */
@Composable
fun LocationPermissionCard(
    hasLocationPermission: Boolean,
    hasBackgroundPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToTimeBased: () -> Unit
) {
    val title = when {
        !hasLocationPermission -> "📍 위치 권한이 필요해요"
        !hasBackgroundPermission -> "📍 백그라운드 위치 권한 필요"
        else -> return // 모든 권한이 있으면 카드 표시 안 함
    }

    val description = when {
        !hasLocationPermission -> "특정 장소 도착 시 자동으로 집중 모드를 시작합니다."
        !hasBackgroundPermission -> "앱이 백그라운드에 있을 때도 위치를 감지하려면\n백그라운드 위치 권한이 필요합니다.\n\n다음 화면에서 \"항상 허용\"을 선택해주세요."
        else -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 설명
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // 현재 권한 상태 표시
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PermissionStatusItem(
                    label = "정확한 위치",
                    isGranted = hasLocationPermission
                )
                PermissionStatusItem(
                    label = "백그라운드 위치",
                    isGranted = hasBackgroundPermission
                )
            }

            // 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToTimeBased,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("시간 기반 사용")
                }

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("권한 설정하기")
                }
            }
        }
    }
}

/**
 * 권한 상태 아이템
 */
@Composable
private fun PermissionStatusItem(
    label: String,
    isGranted: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isGranted) "✅" else "❌",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = if (isGranted) "허용됨" else "거부됨",
            style = MaterialTheme.typography.bodySmall,
            color = if (isGranted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

