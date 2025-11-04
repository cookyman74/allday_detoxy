package com.allday.detoxy.presentation.ui.settings.focus

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.allday.detoxy.core.manager.DndManager
import com.allday.detoxy.core.utils.AppCategory
import com.allday.detoxy.core.utils.AppCategoryMapper
import com.allday.detoxy.presentation.ui.theme.DetoxyTheme
import com.allday.detoxy.presentation.viewmodel.FocusSettingsViewModel
import com.allday.detoxy.presentation.viewmodel.FocusSettingsUiState

/**
 * 디톡시 제어 설정 화면
 *
 * 집중모드 중 차단할 앱 카테고리를 선택하는 화면
 * - 프리셋 선택 (완전 차단/표준 디톡시/완화)
 * - 카테고리별 토글 (SNS, 메신저, Web, 영상, 기타)
 * - 권한 상태 표시
 *
 * @see [01_advanced_wireframe_spec.md](../../../../../../../../docs/01_advanced_wireframe_spec.md)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetoxyControlSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: FocusSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 메신저 카테고리 안내 다이얼로그 상태
    var showMessengerDialog by remember { mutableStateOf(false) }
    var pendingMessengerState by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("디톡시 제어 설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveSettings()
                            Toast.makeText(context, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: 프리셋 선택
            PresetSelectionSection(
                selectedPreset = uiState.selectedPreset,
                onPresetSelected = { preset -> viewModel.applyPreset(preset) }
            )

            // Section 2: 카테고리별 토글
            CategoryTogglesSection(
                enabledCategories = uiState.enabledCategories,
                otherAppsEnabled = uiState.otherAppsEnabled,
                messengerHasBeenEnabled = uiState.messengerHasBeenEnabled,
                onCategoryToggle = { category, enabled ->
                    if (category == AppCategory.MESSENGER && enabled && !uiState.messengerHasBeenEnabled) {
                        // 메신저 첫 활성화 시 안내 다이얼로그 표시
                        showMessengerDialog = true
                        pendingMessengerState = enabled
                    } else {
                        viewModel.toggleCategory(category, enabled)
                    }
                },
                onOtherAppsToggle = { enabled ->
                    viewModel.toggleOtherApps(enabled)
                }
            )

            // Section 3: 현재 설정 프리뷰
            SettingsPreviewSection(
                enabledCategories = uiState.enabledCategories,
                otherAppsEnabled = uiState.otherAppsEnabled,
                selectedPreset = uiState.selectedPreset
            )

            // Section 4: 권한 상태
            PermissionStatusSection(
                dndPermissionState = uiState.dndPermissionState,
                accessibilityEnabled = uiState.accessibilityEnabled,
                overlayEnabled = uiState.overlayEnabled
            )

            // 🆕 스케줄 탭 안내 (v0.10 UI/UX 개선)
            ScheduleTabInfoCard()

            // Section 5: 디톡시 루틴 (향후 구현)
            DetoxyRoutineSection(
                routineEnabled = uiState.routineEnabled,
                onRoutineToggle = { enabled ->
                    viewModel.toggleRoutine(enabled)
                }
            )
        }
    }

    // 메신저 카테고리 안내 다이얼로그
    if (showMessengerDialog) {
        MessengerCategoryDialog(
            onConfirm = {
                viewModel.toggleCategory(AppCategory.MESSENGER, pendingMessengerState)
                viewModel.setMessengerHasBeenEnabled()
                showMessengerDialog = false
            },
            onDismiss = {
                showMessengerDialog = false
            }
        )
    }
}

/**
 * 프리셋 선택 섹션
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSelectionSection(
    selectedPreset: AppCategoryMapper.DetoxyPreset?,
    onPresetSelected: (AppCategoryMapper.DetoxyPreset) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "디톡시 강도 프리셋",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                AppCategoryMapper.DetoxyPreset.values().forEachIndexed { index, preset ->
                    SegmentedButton(
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppCategoryMapper.DetoxyPreset.values().size
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = preset.recoveryStage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            selectedPreset?.let { preset ->
                Text(
                    text = preset.getRecoveryMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 카테고리 토글 섹션
 */
@Composable
fun CategoryTogglesSection(
    enabledCategories: Set<AppCategory>,
    otherAppsEnabled: Boolean,
    messengerHasBeenEnabled: Boolean,
    onCategoryToggle: (AppCategory, Boolean) -> Unit,
    onOtherAppsToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "차단 앱 카테고리",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // SNS
            CategoryToggleItem(
                icon = AppCategory.SNS.getIcon(),
                title = AppCategory.SNS.getDisplayName(),
                description = "Instagram, Facebook, Twitter 등",
                isEnabled = AppCategory.SNS in enabledCategories,
                onToggle = { enabled -> onCategoryToggle(AppCategory.SNS, enabled) }
            )

            Divider()

            // 메신저
            CategoryToggleItem(
                icon = AppCategory.MESSENGER.getIcon(),
                title = AppCategory.MESSENGER.getDisplayName(),
                description = "KakaoTalk, WhatsApp, LINE 등",
                isEnabled = AppCategory.MESSENGER in enabledCategories,
                showWarning = true,
                warningText = "긴급 연락 유지 권장",
                onToggle = { enabled -> onCategoryToggle(AppCategory.MESSENGER, enabled) }
            )

            Divider()

            // Web 서핑
            CategoryToggleItem(
                icon = AppCategory.WEB.getIcon(),
                title = AppCategory.WEB.getDisplayName(),
                description = "Chrome, Samsung Internet 등",
                isEnabled = AppCategory.WEB in enabledCategories,
                onToggle = { enabled -> onCategoryToggle(AppCategory.WEB, enabled) }
            )

            Divider()

            // 영상 & 쇼츠
            CategoryToggleItem(
                icon = AppCategory.VIDEO_SHORTS.getIcon(),
                title = AppCategory.VIDEO_SHORTS.getDisplayName(),
                description = "YouTube, TikTok, Netflix 등",
                isEnabled = AppCategory.VIDEO_SHORTS in enabledCategories,
                onToggle = { enabled -> onCategoryToggle(AppCategory.VIDEO_SHORTS, enabled) }
            )

            Divider()

            // 기타 앱
            CategoryToggleItem(
                icon = AppCategory.OTHER.getIcon(),
                title = AppCategory.OTHER.getDisplayName(),
                description = "기본 차단 앱 이외 모든 앱",
                isEnabled = otherAppsEnabled,
                onToggle = { enabled -> onOtherAppsToggle(enabled) }
            )
        }
    }
}

/**
 * 카테고리 토글 아이템
 */
@Composable
fun CategoryToggleItem(
    icon: String,
    title: String,
    description: String,
    isEnabled: Boolean,
    showWarning: Boolean = false,
    warningText: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showWarning && warningText != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

/**
 * 설정 프리뷰 섹션
 */
@Composable
fun SettingsPreviewSection(
    enabledCategories: Set<AppCategory>,
    otherAppsEnabled: Boolean,
    selectedPreset: AppCategoryMapper.DetoxyPreset?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "현재 설정 요약",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            val blockedCategories = mutableListOf<String>()
            if (AppCategory.SNS in enabledCategories) blockedCategories.add("SNS")
            if (AppCategory.WEB in enabledCategories) blockedCategories.add("Web")
            if (AppCategory.VIDEO_SHORTS in enabledCategories) blockedCategories.add("영상")
            if (otherAppsEnabled) blockedCategories.add("기타 앱")

            val summaryText = when {
                blockedCategories.isEmpty() -> "차단할 앱이 없습니다."
                else -> "${blockedCategories.joinToString(", ")}을(를) 차단합니다."
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            if (AppCategory.MESSENGER !in enabledCategories) {
                Text(
                    text = "메신저는 긴급 연락을 위해 사용 가능합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            selectedPreset?.let { preset ->
                Text(
                    text = "${preset.displayName} 모드 (${preset.recoveryStage})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            } ?: run {
                Text(
                    text = "커스텀 설정",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 권한 상태 섹션
 */
@Composable
fun PermissionStatusSection(
    dndPermissionState: DndManager.DndPermissionState,
    accessibilityEnabled: Boolean,
    overlayEnabled: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "권한 상태",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 접근성 권한
            PermissionStatusItem(
                icon = if (accessibilityEnabled) "✅" else "❌",
                title = "앱 차단 기능",
                description = if (accessibilityEnabled)
                    "활성화됨"
                else
                    "권한이 필요합니다",
                isGranted = accessibilityEnabled,
                onSettingsClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            // 오버레이 권한
            PermissionStatusItem(
                icon = if (overlayEnabled) "✅" else "❌",
                title = "잠금 화면 표시",
                description = if (overlayEnabled)
                    "허용됨"
                else
                    "권한이 필요합니다",
                isGranted = overlayEnabled,
                onSettingsClick = {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            // DND 권한
            val dndIcon = when (dndPermissionState) {
                DndManager.DndPermissionState.GRANTED -> "✅"
                DndManager.DndPermissionState.DENIED -> "⚪"
                DndManager.DndPermissionState.NOT_SUPPORTED -> "❌"
            }

            PermissionStatusItem(
                icon = dndIcon,
                title = "알림 차단 (선택)",
                description = dndPermissionState.getDescription(),
                isGranted = dndPermissionState == DndManager.DndPermissionState.GRANTED,
                onSettingsClick = {
                    if (dndPermissionState == DndManager.DndPermissionState.DENIED) {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        )
                    }
                }
            )
        }
    }
}

/**
 * 권한 상태 아이템
 */
@Composable
fun PermissionStatusItem(
    icon: String,
    title: String,
    description: String,
    isGranted: Boolean,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isGranted) {
            TextButton(onClick = onSettingsClick) {
                Text("설정하기")
            }
        }
    }
}

/**
 * 예약설정 섹션 (2차 고도화)
 * 
 * 시간 기반 자동 실행 화면으로 이동하는 버튼을 제공합니다.
 */
@Composable
fun TimeBasedAutoRunSection(
    onNavigateToAutoRun: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏰")
                    Text(
                        text = "예약설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "원하는 시간에 자동으로 집중모드 시작",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onNavigateToAutoRun) {
                Text("설정하기")
            }
        }
    }
}

/**
 * 디톡시 루틴 섹션
 */
@Composable
fun DetoxyRoutineSection(
    routineEnabled: Boolean,
    onRoutineToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔄")
                    Text(
                        text = "디톡시 루틴 (예정)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "매일 정해진 시간에 자동으로 디톡시 시작",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = routineEnabled,
                onCheckedChange = onRoutineToggle,
                enabled = false // 향후 구현 예정
            )
        }
    }
}

/**
 * 스케줄 탭 안내 카드 (v0.10 UI/UX 개선)
 * 
 * 하단 네비게이션의 "스케줄" 탭으로 안내하는 카드입니다.
 * 기존 예약설정 기능은 이제 별도 스케줄 탭에서 관리됩니다.
 */
@Composable
fun ScheduleTabInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💡 스케줄 관리는 이제 별도 탭에서",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "하단의 [스케줄] 탭에서 시간표를 관리하세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 메신저 카테고리 안내 다이얼로그
 */
@Composable
fun MessengerCategoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(text = "💬", style = MaterialTheme.typography.headlineLarge)
        },
        title = {
            Text(
                text = "메신저 차단 안내",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "메신저 앱은 긴급 연락이 필요할 수 있어 기본적으로 차단하지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "디지털 중독 회복을 위해 완전한 디톡시가 필요하다면 메신저도 차단할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⚠️")
                        Text(
                            text = "긴급 연락이 불가능해질 수 있으니 신중히 선택하세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("메신저도 차단")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("허용 유지 (권장)")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DetoxyControlSettingsScreenPreview() {
    DetoxyTheme {
        DetoxyControlSettingsScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun MessengerCategoryDialogPreview() {
    DetoxyTheme {
        MessengerCategoryDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}