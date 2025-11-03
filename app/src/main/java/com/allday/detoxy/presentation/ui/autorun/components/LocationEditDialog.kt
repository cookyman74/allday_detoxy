package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.allday.detoxy.data.local.entity.LocationBasedAutoRun
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
                        onCheckedChange = { isEnabled = it }
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
                        enabled = isEnabled
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
                        enabled = isEnabled
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("위치 검색") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        
                        // 디바운스: 이전 검색 취소
                        searchJob?.cancel()
                        
                        if (query.length >= 2) {
                            searchJob = scope.launch {
                                delay(500)  // 500ms 대기
                                isSearching = true
                                val result = GeocoderUtils.searchLocation(context, query)
                                searchResults = result.getOrElse { emptyList() }  // 🆕 Result 처리
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    label = { Text("주소 또는 장소 이름") },
                    placeholder = { Text("예: 내곡중학교, 서울시청") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
                
                if (searchQuery.length < 2) {
                    Text(
                        text = "최소 2글자 이상 입력하세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (searchResults.isEmpty() && !isSearching) {
                    Text(
                        text = "검색 결과가 없습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(searchResults) { result ->
                            Surface(
                                onClick = { onLocationSelected(result) },  // 🆕 클릭 이벤트
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = result.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = result.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

