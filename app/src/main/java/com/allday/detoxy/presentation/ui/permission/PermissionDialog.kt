package com.allday.detoxy.presentation.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 접근성 권한 안내 다이얼로그
 *
 * 사용자에게 접근성 서비스 권한이 필요한 이유를 설명하고
 * 설정 화면으로 이동할 수 있는 버튼을 제공합니다.
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onOpenSettings 설정 화면 이동 콜백
 */
@Composable
fun AccessibilityPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "접근성 권한 필요",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ScreenSence가 정상적으로 작동하려면 접근성 서비스 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "이 권한은 다음과 같은 용도로 사용됩니다:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• 타이머 실행 중 특정 앱(Instagram, TikTok, YouTube, Facebook) 차단",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 차단된 앱 실행 시 홈 화면으로 이동",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 집중 시간 향상을 위한 앱 사용 제한",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "설정 화면에서 'ScreenSence'를 찾아 활성화해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("설정 열기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

/**
 * 간단한 권한 안내 카드 (MVP 버전)
 *
 * 다이얼로그 대신 인라인으로 표시할 수 있는 간단한 권한 안내 UI
 *
 * @param isGranted 권한 부여 여부
 * @param onOpenSettings 설정 화면 이동 콜백
 */
@Composable
fun PermissionGuideCard(
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isGranted) "✓ 접근성 권한 활성화됨" else "⚠ 접근성 권한 필요",
                style = MaterialTheme.typography.titleMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )

            if (!isGranted) {
                Text(
                    text = "앱 차단 기능을 사용하려면 접근성 서비스를 활성화해야 합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("설정 화면 열기")
                }
            } else {
                Text(
                    text = "앱 차단 기능이 정상적으로 작동합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
