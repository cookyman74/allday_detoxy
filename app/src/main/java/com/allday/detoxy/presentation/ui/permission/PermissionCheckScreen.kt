package com.allday.detoxy.presentation.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.allday.detoxy.core.utils.PermissionUtils

/**
 * 권한 확인 및 안내 화면
 *
 * 앱 최초 실행 시 필요한 권한들을 안내하고,
 * 모든 필수 권한이 부여되면 자동으로 메인 화면으로 이동합니다.
 *
 * @param onAllPermissionsGranted 모든 필수 권한이 부여되었을 때 호출되는 콜백
 */
@Composable
fun PermissionCheckScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 권한 상태 추적
    var accessibilityGranted by remember {
        mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context))
    }
    var overlayGranted by remember {
        mutableStateOf(PermissionUtils.canDrawOverlays(context))
    }
    var dndGranted by remember {
        mutableStateOf(PermissionUtils.hasNotificationPolicyAccess(context))
    }

    // 화면 재진입 시 권한 상태 재확인
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 권한 상태 업데이트
                accessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
                overlayGranted = PermissionUtils.canDrawOverlays(context)
                dndGranted = PermissionUtils.hasNotificationPolicyAccess(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 모든 필수 권한이 부여되면 자동으로 다음 화면
    LaunchedEffect(accessibilityGranted, overlayGranted) {
        if (accessibilityGranted && overlayGranted) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 헤더
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "ScreenSence 시작하기",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "집중 모드를 위한 권한 설정이 필요합니다",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 필수 권한 안내
        Text(
            text = "필수 권한",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // 접근성 서비스 권한
        PermissionCard(
            title = "앱 차단 기능",
            description = "Instagram, TikTok, YouTube, Facebook 등의 앱을 차단하기 위해 필요합니다. 설정에서 'ScreenSence'를 찾아 활성화해주세요.",
            icon = Icons.Default.Info,
            isGranted = accessibilityGranted,
            isRequired = true,
            onRequestClick = {
                PermissionUtils.openAccessibilitySettings(context)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 오버레이 권한
        PermissionCard(
            title = "잠금 화면 표시",
            description = "차단된 앱 실행 시 집중 모드 잠금 화면을 표시하기 위해 필요합니다. '다른 앱 위에 표시' 권한을 허용해주세요.",
            icon = Icons.Default.Lock,
            isGranted = overlayGranted,
            isRequired = true,
            onRequestClick = {
                PermissionUtils.openOverlaySettings(context)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 선택 권한 안내
        Text(
            text = "선택 권한",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // DND 권한 (선택)
        PermissionCard(
            title = "알림 차단 (선택)",
            description = "집중 모드 중 알림을 자동으로 차단합니다. 필수는 아니지만, 더 나은 집중 환경을 위해 권장됩니다.",
            icon = Icons.Default.Close,
            isGranted = dndGranted,
            isRequired = false,
            onRequestClick = {
                PermissionUtils.openNotificationPolicySettings(context)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 안내 메시지
        if (!accessibilityGranted || !overlayGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 권한 설정 방법",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 위의 '권한 설정하기' 버튼을 눌러주세요\n" +
                                "2. 설정 화면에서 권한을 허용해주세요\n" +
                                "3. 뒤로 가기를 눌러 앱으로 돌아오세요\n" +
                                "4. 모든 필수 권한이 허용되면 자동으로 시작됩니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            // 모든 권한이 허용된 경우 (실제로는 LaunchedEffect에서 자동 이동)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "모든 필수 권한이 허용되었습니다!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 권한 안내 카드 컴포넌트
 *
 * @param title 권한 제목
 * @param description 권한 설명
 * @param icon 권한 아이콘
 * @param isGranted 권한 부여 여부
 * @param isRequired 필수 권한 여부
 * @param onRequestClick 권한 설정 버튼 클릭 콜백
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean = true,
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 4.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isRequired) {
                            Text(
                                text = " *",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 상태 표시 및 버튼
            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "권한 허용됨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onRequestClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("권한 설정하기")
                }
            }
        }
    }
}

