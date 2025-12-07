package com.allday.detoxy.presentation.ui.autorun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allday.detoxy.presentation.ui.autorun.components.AutoRunHistoryItem
import com.allday.detoxy.presentation.ui.autorun.components.StatisticsCard
import com.allday.detoxy.presentation.viewmodel.AutoRunHistoryViewModel

/**
 * 자동 실행 이력 화면
 *
 * 2.5차 고도화 Week 2, Day 11-13: AutoRunHistoryScreen 구현
 *
 * ## 주요 기능
 * - 자동 실행 이력 리스트 (날짜별 그룹핑)
 * - 필터링 (트리거 타입, 결과, 날짜 범위)
 * - 기본 통계 표시
 *
 * @param onBack 뒤로가기 콜백
 * @param viewModel AutoRunHistoryViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoRunHistoryScreen(
    onBack: () -> Unit,
    viewModel: AutoRunHistoryViewModel = hiltViewModel()
) {
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val triggerTypeFilter by viewModel.triggerTypeFilter.collectAsState()
    val resultFilter by viewModel.resultFilter.collectAsState()
    val dateRangeFilter by viewModel.dateRangeFilter.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "자동 실행 이력",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "필터"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 통계 카드
                item {
                    StatisticsCard(statistics = statistics)
                }

                // 필터 요약
                item {
                    FilterSummaryCard(
                        triggerTypeFilter = triggerTypeFilter,
                        resultFilter = resultFilter,
                        dateRangeFilter = dateRangeFilter,
                        onClearFilters = { viewModel.resetFilters() }
                    )
                }

                // 이력 리스트
                if (filteredLogs.isEmpty()) {
                    item {
                        EmptyHistoryCard()
                    }
                } else {
                    filteredLogs.forEach { (date, logs) ->
                        // 날짜 헤더
                        item(key = "header_$date") {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // 해당 날짜의 로그들
                        items(
                            items = logs,
                            key = { it.id }
                        ) { log ->
                            AutoRunHistoryItem(log = log)
                        }
                    }
                }
            }
        }

        // 필터 다이얼로그
        if (showFilterDialog) {
            FilterDialog(
                triggerTypeFilter = triggerTypeFilter,
                resultFilter = resultFilter,
                dateRangeFilter = dateRangeFilter,
                onTriggerTypeChange = { viewModel.setTriggerTypeFilter(it) },
                onResultChange = { viewModel.setResultFilter(it) },
                onDateRangeChange = { viewModel.setDateRangeFilter(it) },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

/**
 * 필터 요약 카드
 *
 * @param triggerTypeFilter 트리거 타입 필터
 * @param resultFilter 결과 필터
 * @param dateRangeFilter 날짜 범위 필터
 * @param onClearFilters 필터 초기화 콜백
 */
@Composable
private fun FilterSummaryCard(
    triggerTypeFilter: String,
    resultFilter: String,
    dateRangeFilter: String,
    onClearFilters: () -> Unit
) {
    val hasActiveFilters = triggerTypeFilter != "ALL" || resultFilter != "ALL" || dateRangeFilter != "WEEK"

    if (hasActiveFilters) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "필터 적용 중",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildFilterSummary(triggerTypeFilter, resultFilter, dateRangeFilter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                TextButton(onClick = onClearFilters) {
                    Text("초기화")
                }
            }
        }
    }
}

/**
 * 빈 이력 카드
 */
@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "자동 실행 이력이 없습니다",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "자동 실행이 트리거되면 이력이 표시됩니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 필터 다이얼로그
 */
@Composable
private fun FilterDialog(
    triggerTypeFilter: String,
    resultFilter: String,
    dateRangeFilter: String,
    onTriggerTypeChange: (String) -> Unit,
    onResultChange: (String) -> Unit,
    onDateRangeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("필터 설정")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 트리거 타입 필터
                Text(
                    text = "트리거 타입",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = triggerTypeFilter == "ALL",
                        onClick = { onTriggerTypeChange("ALL") },
                        label = { Text("전체") }
                    )
                    FilterChip(
                        selected = triggerTypeFilter == "TIME",
                        onClick = { onTriggerTypeChange("TIME") },
                        label = { Text("시간") }
                    )
                    FilterChip(
                        selected = triggerTypeFilter == "LOCATION",
                        onClick = { onTriggerTypeChange("LOCATION") },
                        label = { Text("위치") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 결과 필터
                Text(
                    text = "결과",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resultFilter == "ALL",
                        onClick = { onResultChange("ALL") },
                        label = { Text("전체") }
                    )
                    FilterChip(
                        selected = resultFilter == "STARTED",
                        onClick = { onResultChange("STARTED") },
                        label = { Text("성공") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resultFilter == "SKIPPED",
                        onClick = { onResultChange("SKIPPED") },
                        label = { Text("건너뜀") }
                    )
                    FilterChip(
                        selected = resultFilter == "FAILED",
                        onClick = { onResultChange("FAILED") },
                        label = { Text("실패") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 날짜 범위 필터
                Text(
                    text = "날짜 범위",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateRangeFilter == "WEEK",
                        onClick = { onDateRangeChange("WEEK") },
                        label = { Text("이번 주") }
                    )
                    FilterChip(
                        selected = dateRangeFilter == "MONTH",
                        onClick = { onDateRangeChange("MONTH") },
                        label = { Text("이번 달") }
                    )
                    FilterChip(
                        selected = dateRangeFilter == "ALL",
                        onClick = { onDateRangeChange("ALL") },
                        label = { Text("전체") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

/**
 * 필터 요약 텍스트 생성
 */
private fun buildFilterSummary(
    triggerTypeFilter: String,
    resultFilter: String,
    dateRangeFilter: String
): String {
    val parts = mutableListOf<String>()

    if (triggerTypeFilter != "ALL") {
        parts.add(when (triggerTypeFilter) {
            "TIME" -> "시간 기반"
            "LOCATION" -> "위치 기반"
            else -> ""
        })
    }

    if (resultFilter != "ALL") {
        parts.add(when (resultFilter) {
            "STARTED" -> "성공"
            "SKIPPED" -> "건너뜀"
            "FAILED" -> "실패"
            else -> ""
        })
    }

    if (dateRangeFilter != "WEEK") {
        parts.add(when (dateRangeFilter) {
            "MONTH" -> "이번 달"
            "ALL" -> "전체 기간"
            else -> ""
        })
    } else {
        parts.add("이번 주")
    }

    return parts.joinToString(" · ")
}

