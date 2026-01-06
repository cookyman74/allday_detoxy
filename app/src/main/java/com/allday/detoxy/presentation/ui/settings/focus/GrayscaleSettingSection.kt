package com.allday.detoxy.presentation.ui.settings.focus

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.component.GlassSurface

/**
 * 흑백 모드 설정 섹션 (Glass 스타일)
 * 
 * Phase 5: 설정 페이지 UI
 * - Android 15+: 토글 스위치 (활성화/비활성화)
 * - Android 14 이하: 경고 배너 (시스템 설정 안내)
 * 
 * @param grayscaleModeEnabled 현재 흑백 모드 활성화 상태
 * @param showPermissionDialog 권한 다이얼로그 표시 여부
 * @param onToggle 토글 변경 콜백
 * @param onDismissDialog 다이얼로그 닫기 콜백
 * @param onNavigateToSettings 설정 화면 이동 콜백
 */
@Composable
fun GrayscaleSettingSection(
    grayscaleModeEnabled: Boolean,
    showPermissionDialog: Boolean,
    onToggle: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val isAndroid15OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    // 권한 다이얼로그
    if (showPermissionDialog) {
        GrayscalePermissionDialog(
            onConfirm = {
                // 시스템 설정으로 이동
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                )
                onDismissDialog()
            },
            onDismiss = onDismissDialog
        )
    }

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.35f
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 섹션 헤더
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎨")
                Text(
                    text = "흑백 모드",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isAndroid15OrAbove) {
                // Android 15+: 토글 스위치
                GrayscaleToggleItem(
                    isEnabled = grayscaleModeEnabled,
                    onToggle = onToggle
                )
            } else {
                // Android 14 이하: 경고 배너
                GrayscaleWarningBanner(
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

/**
 * 흑백 모드 토글 아이템 (Android 15+용)
 */
@Composable
private fun GrayscaleToggleItem(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "집중 모드 시 자동 활성화",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "타이머 시작 시 화면이 흑백으로 전환됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * 흑백 모드 경고 배너 (Android 14 이하용)
 * 
 * @param onNavigateToSettings 시스템 설정 이동 콜백
 */
@Composable
fun GrayscaleWarningBanner(
    onNavigateToSettings: () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.2f,
        tint = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "ℹ️", style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text(
                        text = "수동 흑백 모드 안내",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Android 14 이하에서는 시스템 설정에서 직접 활성화해야 합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ⚠️ 리뷰 반영: onNavigateToSettings 콜백 사용
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("시스템 설정으로 이동")
            }
        }
    }
}

/**
 * 흑백 모드 권한 다이얼로그
 */
@Composable
fun GrayscalePermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "흑백 모드 권한 필요",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "흑백 모드를 사용하려면 '알림 접근 권한'이 필요합니다.\n\n" +
                "이 권한은 집중 모드 시 화면을 흑백으로 전환하는 데만 사용됩니다."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}
