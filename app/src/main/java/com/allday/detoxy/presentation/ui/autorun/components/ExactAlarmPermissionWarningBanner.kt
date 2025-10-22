package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 정확 알람 권한 경고 배너
 *
 * Android 12+ 기기에서 정확 알람 권한이 없을 때 화면 상단에 표시됩니다.
 * WorkManager fallback을 사용 중임을 안내하고, 권한 설정 화면으로 이동할 수 있는 버튼을 제공합니다.
 *
 * @param onSettingsClick 권한 설정하기 버튼 클릭 콜백
 */
@Composable
fun ExactAlarmPermissionWarningBanner(
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "경고",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "정확한 실행을 위해 권한을 허용하세요",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "정확 알람 권한이 없으면 자동 실행 시간이 최대 15분 정도 지연될 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("권한 설정하기")
            }
        }
    }
}

