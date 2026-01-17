package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.allday.detoxy.R

/**
 * 백그라운드 위치 권한 설명 다이얼로그
 *
 * ACCESS_FINE_LOCATION 승인 후, ACCESS_BACKGROUND_LOCATION 요청 전에 표시됩니다.
 * Android 11+ 정책 준수를 위해 반드시 표시해야 합니다.
 *
 * @param onProceed 다음 단계(백그라운드 위치 권한 요청) 진행
 * @param onCancel 나중에 하기
 */
@Composable
fun BackgroundLocationRationaleDialog(
    onProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_bg_location_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_bg_location_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.permission_bg_location_guide),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = stringResource(R.string.permission_bg_location_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text(stringResource(R.string.btn_next))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_later))
            }
        }
    )
}

/**
 * 위치 권한 거부 다이얼로그
 *
 * 위치 권한 거부 시 표시됩니다.
 * 대체 기능(시간 기반 자동 실행)을 제안합니다.
 *
 * @param onRetry 권한 재요청
 * @param onNavigateToTimeBased 시간 기반 자동 실행으로 이동
 * @param onDismiss 취소
 */
@Composable
fun LocationPermissionDeniedDialog(
    onRetry: () -> Unit,
    onNavigateToTimeBased: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_location_needed_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_location_needed_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = stringResource(R.string.permission_suggestion_time_based),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = stringResource(R.string.permission_suggestion_time_based_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.btn_retry_permission))
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onNavigateToTimeBased) {
                    Text(stringResource(R.string.btn_move_to_time_based))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    )
}

/**
 * 설정 화면 이동 안내 다이얼로그
 *
 * 백그라운드 위치 권한 거부 시, 설정 화면으로 이동을 안내합니다.
 *
 * @param onOpenSettings 설정 화면으로 이동
 * @param onDismiss 나중에 하기
 */
@Composable
fun OpenSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_settings_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.permission_settings_step_1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = stringResource(R.string.permission_settings_step_2),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = stringResource(R.string.permission_settings_step_3),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.btn_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_later))
            }
        }
    )
}

/**
 * 위치 권한 해제 안내 다이얼로그
 *
 * 권한 변경 감지 시 표시됩니다.
 * Geofence가 자동으로 해제되었음을 안내합니다.
 *
 * @param onOpenSettings 설정 화면으로 이동
 * @param onDismiss 확인
 */
@Composable
fun LocationPermissionRevokedDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_revoked_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_revoked_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.permission_revoked_guide),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.btn_go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_confirm))
            }
        }
    )
}

