package com.example.smarthome.ui.reporting

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.UsageLog
import com.example.smarthome.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: ReportingViewModel = viewModel()
) {
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val summaries by viewModel.deviceSummaries.collectAsStateWithLifecycle()
    val weekChart by viewModel.weekChart.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Insights into your home",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                SectionHeader(title = "Weekly Activity", icon = Icons.Outlined.BarChart)
            }
            item {
                WeeklyBarChart(days = weekChart)
            }

            if (summaries.isNotEmpty()) {
                item {
                    SectionHeader(title = "Usage Leaderboard", icon = Icons.AutoMirrored.Outlined.TrendingUp)
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(summaries.take(8)) { summary ->
                            DeviceSummaryCard(summary = summary)
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "History", icon = Icons.AutoMirrored.Outlined.List)
            }

            item {
                EventFilterRow(
                    selectedFilter = selectedFilter,
                    onSelect = { viewModel.setFilter(it) }
                )
            }

            if (logs.isEmpty()) {
                item {
                    EmptyLogState()
                }
            } else {
                items(logs.take(100), key = { it.id }) { log ->
                    LogEntry(log = log)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(days: List<DayUsage>) {
    val maxMin = days.maxOfOrNull { it.minutesOn } ?: 0
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bar_anim"
    )

    val primary = MaterialTheme.colorScheme.primary
    val primaryVariant = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (days.isEmpty() || maxMin == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data for this period", color = onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height - 24.dp.toPx()
                    val barCount = days.size
                    val barWidth = (chartWidth / barCount) * 0.4f
                    val gap = (chartWidth / barCount) * 0.6f
                    val halfGap = gap / 2

                    days.forEachIndexed { idx, day ->
                        val barHeightFraction = if (maxMin > 0)
                            (day.minutesOn.toFloat() / maxMin) * animationProgress
                        else 0f

                        val barH = (chartHeight * barHeightFraction).coerceAtLeast(2.dp.toPx())
                        val left = idx * (barWidth + gap) + halfGap
                        val top = chartHeight - barH

                        drawRoundRect(
                            color = if (day.minutesOn > 0) primary else primaryVariant.copy(alpha = 0.1f),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = onSurfaceVariant.toArgb()
                                textSize = 10.sp.toPx()
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.nativeCanvas.drawText(
                                day.dayLabel,
                                left + barWidth / 2,
                                chartHeight + 18.dp.toPx(),
                                paint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryCard(summary: DeviceSummary) {
    val hours = summary.todayMinutesOn / 60
    val mins = summary.todayMinutesOn % 60
    val timeText = when {
        hours > 0 -> "${hours}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "0m"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DeviceHub, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = summary.deviceName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    text = "Active Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.todayMinutesOn > 0) SoftGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EventFilterRow(selectedFilter: String?, onSelect: (String?) -> Unit) {
    val filters = listOf(
        null to "All",
        "ON" to "On",
        "OFF" to "Off",
        "CUTOFF" to "Alerts",
        "SCHEDULE" to "Schedules"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { (value, label) ->
            val isSelected = selectedFilter == value
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun LogEntry(log: UsageLog) {
    val eventColor = when {
        log.event.contains("CUTOFF") -> SoftRed
        log.event.contains("ON") -> SoftGreen
        log.event.contains("OFF") -> SoftGrey
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val eventIcon = when {
        log.event.contains("CUTOFF") -> Icons.Outlined.WarningAmber
        log.event.contains("ON") -> Icons.Outlined.Power
        log.event.contains("SCHEDULE") -> Icons.Outlined.Schedule
        else -> Icons.Outlined.PowerOff
    }

    val timeText = log.safeTimestamp()?.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.seconds * 1000))
    } ?: ""

    val dateText = log.safeTimestamp()?.let {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it.seconds * 1000))
    } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(eventColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = eventIcon,
                contentDescription = null,
                tint = eventColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.deviceName.ifBlank { "Unknown Device" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${log.event.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} • $dateText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyLogState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "History is currently empty",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
