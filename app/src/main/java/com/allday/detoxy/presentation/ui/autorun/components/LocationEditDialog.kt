package com.allday.detoxy.presentation.ui.autorun.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allday.detoxy.core.utils.GeocoderUtils
import com.allday.detoxy.core.utils.LocationUtils
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 위치 정보 수정 다이얼로그
 *
 * 기존 LocationBasedAutoRun의 정보를 수정합니다.
 * - 라벨 (이름)
 * - 반경
 * - 활성화 여부
 *
 * @param location 수정할 위치 정보
 * @param onDismiss 취소 콜백
 * @param onSave 저장 콜백
 */
@Composable
fun LocationEditDialog(
    location: LocationBasedAutoRun,
    onDismiss: () -> Unit,
    onSave: (LocationBasedAutoRun) -> Unit
) {
    var label by remember { mutableStateOf(location.label) }
    var address by remember { mutableStateOf(location.address ?: "") }
    var latitude by remember { mutableDoubleStateOf(location.latitude) }
    var longitude by remember { mutableDoubleStateOf(location.longitude) }
    var radiusMeters by remember { mutableIntStateOf(location.radiusMeters) }
    var isEnabled by remember { mutableStateOf(location.isEnabled) }
    var activateOnEnter by remember { mutableStateOf(location.activateScheduleOnEnter) }
    var deactivateOnExit by remember { mutableStateOf(location.deactivateScheduleOnExit) }
    
    // 🔍 초기값 로깅
    LaunchedEffect(location.id) {
        Log.d("LocationEditDialog", "=== 🔍 Initial Values ===")
        Log.d("LocationEditDialog", "location.id: ${location.id}")
        Log.d("LocationEditDialog", "location.isEnabled: ${location.isEnabled}")
        Log.d("LocationEditDialog", "location.linkedScheduleGroupId: ${location.linkedScheduleGroupId}")
        Log.d("LocationEditDialog", "location.activateScheduleOnEnter: ${location.activateScheduleOnEnter}")
        Log.d("LocationEditDialog", "location.deactivateScheduleOnExit: ${location.deactivateScheduleOnExit}")
        Log.d("LocationEditDialog", "isEnabled (UI state): $isEnabled")
        Log.d("LocationEditDialog", "activateOnEnter (UI state): $activateOnEnter")
        Log.d("LocationEditDialog", "deactivateOnExit (UI state): $deactivateOnExit")
    }
    
    // 🆕 위치 검색 다이얼로그 상태
    var showLocationSearch by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "위치 정보 수정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 라벨 (이름)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("위치 이름 *") },
                    placeholder = { Text("예: 학교, 독서실, 카페") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                HorizontalDivider()
                
                // 🆕 주소 (수정 가능)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "주소",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (address.isNotBlank()) address else "주소 없음",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (address.isNotBlank()) 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { showLocationSearch = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "주소 변경",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // 🆕 지도에서 확인 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                try {
                                    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label.ifBlank { "위치" })})")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "지도 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "지도에서 확인",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = "위치를 변경하려면 검색 버튼을 눌러 새 위치를 선택하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 반경
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "반경: ${radiusMeters}m",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = radiusMeters.toFloat(),
                        onValueChange = { radiusMeters = it.toInt() },
                        valueRange = 50f..500f,
                        steps = (500 - 50) / 50 - 1,  // 50m 단위
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "이 반경 내에 진입하면 자동으로 시간표가 활성화됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 활성화 설정
                Text(
                    text = "자동 활성화 설정",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 위치 기반 자동 실행 활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "위치 기반 자동 실행",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "이 위치에서 Geofence를 사용합니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { newValue ->
                            Log.d("LocationEditDialog", "🔄 위치 기반 자동 실행 스위치 변경: $isEnabled → $newValue")
                            isEnabled = newValue
                        }
                    )
                }
                
                // 진입 시 활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "진입 시 시간표 활성화",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "위치에 도착하면 연결된 시간표를 켭니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = activateOnEnter,
                        onCheckedChange = { activateOnEnter = it },
                        enabled = location.linkedScheduleGroupId != null  // 🐛 버그 수정: linkedScheduleGroupId가 있을 때만 활성화
                    )
                }
                
                // 이탈 시 비활성화
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "이탈 시 시간표 비활성화",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "위치를 벗어나면 연결된 시간표를 끕니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = deactivateOnExit,
                        onCheckedChange = { deactivateOnExit = it },
                        enabled = location.linkedScheduleGroupId != null  // 🐛 버그 수정: linkedScheduleGroupId가 있을 때만 활성화
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Log.d("LocationEditDialog", "=== 💾 Saving Location ===")
                    Log.d("LocationEditDialog", "isEnabled: $isEnabled")
                    Log.d("LocationEditDialog", "activateOnEnter: $activateOnEnter")
                    Log.d("LocationEditDialog", "deactivateOnExit: $deactivateOnExit")
                    
                    val updatedLocation = location.copy(
                        label = label.trim(),
                        address = address.trim().ifBlank { null },  // 🆕 주소 업데이트
                        latitude = latitude,  // 🆕 위도 업데이트
                        longitude = longitude,  // 🆕 경도 업데이트
                        radiusMeters = radiusMeters,
                        isEnabled = isEnabled,
                        activateScheduleOnEnter = activateOnEnter,
                        deactivateScheduleOnExit = deactivateOnExit
                    )
                    
                    Log.d("LocationEditDialog", "📤 updatedLocation.activateScheduleOnEnter: ${updatedLocation.activateScheduleOnEnter}")
                    Log.d("LocationEditDialog", "📤 updatedLocation.deactivateScheduleOnExit: ${updatedLocation.deactivateScheduleOnExit}")
                    
                    onSave(updatedLocation)
                },
                enabled = label.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
    
    // 🆕 위치 검색 다이얼로그
    if (showLocationSearch) {
        LocationSearchDialog(
            onDismiss = { showLocationSearch = false },
            onLocationSelected = { selectedLocation ->
                label = selectedLocation.name
                address = selectedLocation.address
                latitude = selectedLocation.latitude
                longitude = selectedLocation.longitude
                showLocationSearch = false
            }
        )
    }
}

/**
 * 위치 검색 다이얼로그
 */
@Composable
private fun LocationSearchDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (GeocoderUtils.LocationInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocoderUtils.LocationInfo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    // 🆕 현재 위치 상태 및 로직 (AddLocationAutoRunDialog와 동일 패턴, 나중에 더 추상화 가능)
    var isLocating by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            scope.launch {
                isLocating = true
                val result = LocationUtils.getCurrentLocation(fusedLocationClient)
                result.onSuccess { location ->
                    val addressResult = GeocoderUtils.getAddressFromCoordinates(
                        context, location.latitude, location.longitude
                    )
                    addressResult.onSuccess { info ->
                        val infoWithAccuracy = info.copy(accuracy = location.accuracy)
                        searchResults = listOf(infoWithAccuracy) + searchResults
                        searchQuery = info.address
                    }.onFailure {
                        Toast.makeText(context, "주소를 가져오지 못했지만 좌표를 등록합니다.", Toast.LENGTH_SHORT).show()
                        val fallbackInfo = GeocoderUtils.LocationInfo(
                            name = "현재 위치",
                            address = "위도: ${location.latitude}, 경도: ${location.longitude}",
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy
                        )
                        searchResults = listOf(fallbackInfo) + searchResults
                    }
                }.onFailure {
                    Toast.makeText(context, "위치를 찾을 수 없습니다. GPS 설정을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
                isLocating = false
            }
        } else {
            Toast.makeText(context, "현재 위치를 찾으려면 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 검색 함수
    val performSearch = {
         if (searchQuery.length >= 2) {
             searchJob?.cancel()
             searchJob = scope.launch {
                 delay(500) // Debounce (버튼 클릭시는 필요없지만 로직 통일)
                 isSearching = true
                 val result = GeocoderUtils.searchLocation(context, searchQuery)
                 searchResults = result.getOrElse { emptyList() }
                 isSearching = false
             }
         }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("위치 검색") },
        text = {
            LocationSearchContent(
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> 
                    searchQuery = query 
                    // Auto-search logic handled in LocationSearchContent logic? 
                    // No, LocationSearchContent is stateless specifically for UI.
                    // We need to implement debounce here if we want auto-search on typing.
                    if (query.length >= 2) {
                        searchJob?.cancel()
                        searchJob = scope.launch {
                            delay(500)
                            isSearching = true
                            val result = GeocoderUtils.searchLocation(context, query)
                            searchResults = result.getOrElse { emptyList() }
                            isSearching = false
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                searchResults = searchResults,
                isSearching = isSearching,
                isLocatingCurrentPosition = isLocating,
                onSearch = { 
                    searchJob?.cancel()
                    scope.launch {
                        isSearching = true
                        val result = GeocoderUtils.searchLocation(context, searchQuery)
                        searchResults = result.getOrElse { emptyList() }
                        isSearching = false
                    }
                },
                onCurrentLocationClick = {
                     requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onLocationSelected = onLocationSelected
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

