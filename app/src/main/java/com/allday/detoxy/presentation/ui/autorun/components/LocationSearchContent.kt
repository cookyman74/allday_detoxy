package com.allday.detoxy.presentation.ui.autorun.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.allday.detoxy.core.utils.GeocoderUtils

/**
 * 위치 검색 공통 컴포넌트
 *
 * AddLocationAutoRunDialog와 LocationEditDialog에서 공유하여 사용합니다.
 *
 * - 주소 검색 기능
 * - 현재 위치 찾기 버튼 (신규)
 * - 검색 결과 리스트 (정확도 표시 포함)
 */
@Composable
fun LocationSearchContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<GeocoderUtils.LocationInfo>,
    isSearching: Boolean,
    isLocatingCurrentPosition: Boolean = false, // 현재 위치 찾는 중 여부
    onSearch: () -> Unit,
    onCurrentLocationClick: () -> Unit, // 현재 위치 버튼 클릭 시
    onLocationSelected: (GeocoderUtils.LocationInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        
        // 1. 검색 입력 창
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("주소 또는 장소 이름") },
            placeholder = { Text("예: 강남역, 스타벅스") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "검색")
            },
            trailingIcon = {
                if (searchQuery.isBlank()) {
                    // 검색어가 없을 때는 현재 위치 아이콘
                    IconButton(onClick = onCurrentLocationClick) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "현재 위치",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        // 2. 현재 위치로 설정 버튼 (명시적)
        TextButton(
            onClick = onCurrentLocationClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLocatingCurrentPosition
        ) {
                if (isLocatingCurrentPosition) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("위치 찾는 중...")
            } else {
                Icon(Icons.Default.Place, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("📍 현재 위치로 설정")
            }
        }

        // 3. 결과 리스트 영역
        if (isLocatingCurrentPosition) {
            // 현재 위치 찾는 중 로딩 UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GPS 신호를 수신하고 있습니다...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (searchResults.isNotEmpty()) {
            Text(
                text = "검색 결과:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // 높이 제한
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { location ->
                    LocationSearchResultCard(
                        location = location,
                        onClick = { onLocationSelected(location) }
                    )
                }
            }
        } else if (searchQuery.isNotBlank() && !isSearching) {
            // 검색어는 있는데 결과가 없는 경우
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색 결과가 없습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 위치 검색 결과 카드 (내부용)
 */
@Composable
private fun LocationSearchResultCard(
    location: GeocoderUtils.LocationInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // 정확도 표시 (값이 있을 경우에만)
                location.accuracy?.let { acc ->
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                text = "±${acc.toInt()}m",
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            Text(
                text = location.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
