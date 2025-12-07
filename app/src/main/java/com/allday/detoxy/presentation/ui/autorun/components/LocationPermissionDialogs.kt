package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                text = "백그라운드 위치 권한이 필요합니다",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "위치 기반 자동 실행을 사용하려면 백그라운드 위치 권한이 필요합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "다음 화면에서 '항상 허용'을 선택해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "이 권한은 등록한 장소(회사, 학교 등)에 도착할 때만 사용되며, 위치 데이터는 로컬에만 저장됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text("다음")
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
                text = "위치 권한이 필요합니다",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "위치 권한이 없으면 위치 기반 자동 실행을 사용할 수 없습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "💡 대신 시간 기반 자동 실행을 사용해보세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "특정 시간대에 자동으로 집중 모드를 시작할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("권한 재요청")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onNavigateToTimeBased) {
                    Text("시간 기반으로 이동")
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
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
                text = "설정에서 권한을 허용해주세요",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "백그라운드 위치 권한을 허용하려면 설정 화면으로 이동해야 합니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "1. '권한' 탭 선택",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "2. '위치' 선택",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "3. '항상 허용' 선택",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
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
                text = "위치 권한이 해제되었습니다",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "위치 권한이 해제되어 위치 기반 자동 실행이 비활성화되었습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "다시 사용하려면 위치 권한을 '항상 허용'으로 설정해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

