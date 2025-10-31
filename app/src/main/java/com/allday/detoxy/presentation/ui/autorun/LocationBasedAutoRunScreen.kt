package com.allday.detoxy.presentation.ui.autorun

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.allday.detoxy.core.manager.AutoRunGeofenceManager
import com.allday.detoxy.core.utils.PermissionUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.allday.detoxy.presentation.ui.autorun.components.*
import com.allday.detoxy.presentation.viewmodel.LocationBasedAutoRunViewModel
import com.allday.detoxy.presentation.viewmodel.LocationError

/**
 * 위치 기반 자동 실행 설정 화면
 *
 * 위치 기반 자동 실행 설정을 관리하는 메인 화면입니다.
 *
 * 주요 기능:
 * - Play Services 미지원 경고 표시
 * - 위치 권한 안내 및 요청
 * - 등록된 위치 리스트 표시
 * - 위치 추가 버튼 (최대 5개 제한)
 * - 배터리 영향 안내
 *
 * @param onBack 뒤로가기 콜백
 * @param onNavigateToTimeBased 시간 기반 자동 실행으로 이동 콜백
 * @param viewModel LocationBasedAutoRunViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationBasedAutoRunScreen(
    onBack: () -> Unit = {},
    onNavigateToTimeBased: () -> Unit = {},
    onNavigateToScheduleGroup: () -> Unit = {},  // 🆕 3차 고도화: 시간표 관리로 이동
    viewModel: LocationBasedAutoRunViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // StateFlow 수집
    val locations by viewModel.locations.collectAsState()
    val playServicesAvailable by viewModel.playServicesAvailable.collectAsState()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()
    val backgroundLocationPermissionGranted by viewModel.backgroundLocationPermissionGranted.collectAsState()
    val hasFullLocationPermission by viewModel.hasFullLocationPermission.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    // Snackbar 상태
    val snackbarHostState = remember { SnackbarHostState() }

    // 에러 표시
    LaunchedEffect(errorState) {
        errorState?.let { error ->
            val message = when (error) {
                is LocationError.GeofenceError -> "Geofence 오류: ${error.message}"
                is LocationError.PermissionError -> "권한 오류: ${error.message}"
                is LocationError.DatabaseError -> "저장소 오류: ${error.message}"
                is LocationError.UnknownError -> "알 수 없는 오류: ${error.message}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // 화면 복귀 시 권한 재확인 (설정 화면에서 돌아올 때)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 다이얼로그 상태
    var showAddDialog by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<LocationBasedAutoRun?>(null) }
    var showBackgroundLocationRationaleDialog by remember { mutableStateOf(false) }
    var showLocationDeniedDialog by remember { mutableStateOf(false) }
    var showOpenSettingsDialog by remember { mutableStateOf(false) }

    // 위치 권한 요청 런처
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 백그라운드 위치 권한 설명 다이얼로그 표시
            showBackgroundLocationRationaleDialog = true
            viewModel.checkPermissions()
        } else {
            // 권한 거부 다이얼로그 표시
            showLocationDeniedDialog = true
        }
    }

    // 백그라운드 위치 권한 요청 런처
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkPermissions()
        if (!isGranted) {
            // 설정 화면으로 이동 안내
            showOpenSettingsDialog = true
        }
    }

    // 권한 요청 핸들러
    val requestLocationPermission: () -> Unit = {
        when {
            !locationPermissionGranted -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            !backgroundLocationPermissionGranted -> {
                // Android 10+ 에서만 백그라운드 위치 권한 필요
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    showBackgroundLocationRationaleDialog = true
                }
            }
            else -> {
                // 모든 권한 있음, 설정 화면으로 이동
                PermissionUtils.openAppLocationSettings(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("위치 기반 자동 실행") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    // 🆕 3차 고도화: 시간표 관리 버튼
                    IconButton(onClick = onNavigateToScheduleGroup) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "시간표 관리"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // 권한이 모두 있고, Play Services가 사용 가능하며, 활성화된 개수가 5개 미만일 때만 표시
            val enabledCount = locations.count { it.isEnabled }
            if (playServicesAvailable && hasFullLocationPermission && enabledCount < AutoRunGeofenceManager.MAX_GEOFENCES) {
                ExtendedFloatingActionButton(
                    onClick = {
                        showAddDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "위치 추가"
                        )
                    },
                    text = { Text("위치 추가") }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Play Services 미지원 경고
            if (!playServicesAvailable) {
                item {
                    PlayServicesWarningCard(
                        onNavigateToTimeBased = onNavigateToTimeBased
                    )
                }
                // Play Services 없으면 이후 항목 표시 안 함
            } else if (!hasFullLocationPermission) {
                // 위치 권한 안내 (Play Services는 있지만 권한 없음)
                item {
                    LocationPermissionCard(
                        hasLocationPermission = locationPermissionGranted,
                        hasBackgroundPermission = backgroundLocationPermissionGranted,
                        onRequestPermission = requestLocationPermission,
                        onNavigateToTimeBased = onNavigateToTimeBased
                    )
                }
            } else {
                // Play Services 있고 권한도 있음 - 위치 리스트 표시
                
                // 등록된 위치 리스트
                if (locations.isEmpty()) {
                    item {
                        EmptyLocationState()
                    }
                } else {
                    items(
                        items = locations,
                        key = { it.id }
                    ) { location ->
                        LocationBasedAutoRunCard(
                            location = location,
                            successRate = null, // TODO: AutoRunLog에서 성공률 계산 (Day 16-17)
                            gpsAccuracy = null, // TODO: AutoRunLog에서 GPS 정확도 계산 (Day 16-17)
                            onToggle = { isEnabled ->
                                viewModel.toggleLocation(location.id, isEnabled)
                            },
                            onEdit = {
                                locationToEdit = location
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteLocation(location.id)
                            }
                        )
                    }
                }

                // 최대 개수 안내 (활성화된 개수 기준)
                val enabledCount = locations.count { it.isEnabled }
                if (enabledCount >= AutoRunGeofenceManager.MAX_GEOFENCES) {
                    item {
                        MaxLocationLimitWarning()
                    }
                }

                // 배터리 영향 안내
                if (locations.isNotEmpty()) {
                    item {
                        LocationBatteryImpactCard(
                            enabledCount = locations.count { it.isEnabled }
                        )
                    }
                }
            }
        }
    }

    // 다이얼로그 표시
    if (showAddDialog) {
        AddLocationAutoRunDialog(
            existingLocation = locationToEdit,
            onDismiss = {
                showAddDialog = false
                locationToEdit = null
            },
            onSave = { location ->
                if (locationToEdit != null) {
                    viewModel.updateLocation(location)
                } else {
                    viewModel.addLocation(location)
                }
            }
        )
    }
    
    // 백그라운드 위치 권한 설명 다이얼로그
    if (showBackgroundLocationRationaleDialog) {
        BackgroundLocationRationaleDialog(
            onProceed = {
                showBackgroundLocationRationaleDialog = false
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
            },
            onDismiss = {
                showBackgroundLocationRationaleDialog = false
            }
        )
    }
    
    // 위치 권한 거부 다이얼로그
    if (showLocationDeniedDialog) {
        LocationPermissionDeniedDialog(
            onRetry = {
                showLocationDeniedDialog = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onNavigateToTimeBased = {
                showLocationDeniedDialog = false
                onNavigateToTimeBased()
            },
            onDismiss = {
                showLocationDeniedDialog = false
            }
        )
    }
    
    // 설정 화면 이동 안내 다이얼로그
    if (showOpenSettingsDialog) {
        OpenSettingsDialog(
            onOpenSettings = {
                showOpenSettingsDialog = false
                PermissionUtils.openAppLocationSettings(context)
            },
            onDismiss = {
                showOpenSettingsDialog = false
            }
        )
    }
}

/**
 * 빈 상태 (위치가 등록되지 않았을 때)
 */
@Composable
private fun EmptyLocationState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📍",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "등록된 위치가 없습니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "특정 장소 도착 시 자동으로 집중 모드를 시작하세요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 최대 개수 초과 경고
 */
@Composable
private fun MaxLocationLimitWarning() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "최대 ${AutoRunGeofenceManager.MAX_GEOFENCES}개까지 등록 가능합니다.\n배터리 효율을 위해 개수가 제한됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

