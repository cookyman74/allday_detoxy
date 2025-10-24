package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Play Services 미지원 경고 카드
 *
 * Google Play Services가 미지원되는 기기에서 표시됩니다.
 * 위치 기반 자동 실행 기능을 사용할 수 없음을 안내하고 대체 방법을 제시합니다.
 *
 * @param onNavigateToTimeBased 시간 기반 자동 실행으로 이동 콜백
 */
@Composable
fun PlayServicesWarningCard(
    onNavigateToTimeBased: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
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
                    text = "⚠️",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Google Play Services 미지원 기기",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // 설명
            Text(
                text = "위치 기반 자동 실행은 Google Play Services가 필요합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            // 대체 방법
            Text(
                text = "대체 방법:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = "• 시간 기반 자동 실행 사용",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            // 버튼
            FilledTonalButton(
                onClick = onNavigateToTimeBased,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("시간 기반 설정으로 이동")
            }
        }
    }
}

