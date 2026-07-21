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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
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

// ──────────────────────────────────────────────────────────────────────────────
// Root Screen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingScreen(
    viewModel: ReportingViewModel = viewModel()
) {
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val summaries by viewModel.deviceSummaries.collectAsStateWithLifecycle()
    val weekChart by viewModel.weekChart.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TealPrimary, TealPrimaryLight, TealPrimary.copy(alpha = 0f))
                            )
                        )
                )
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(TealPrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart Home",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealPrimary
                                )
                                Text(
                                    text = "Usage Reports",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
                )
            }
        },
        containerColor = BackgroundDark
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TealPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 7-day bar chart ─────────────────────────────────────────────
            item {
                SectionHeader(title = "7-Day Activity", icon = Icons.Filled.BarChart)
            }
            item {
                WeeklyBarChart(days = weekChart)
            }

            // ── Device summary cards ─────────────────────────────────────────
            if (summaries.isNotEmpty()) {
                item {
                    SectionHeader(title = "Today's Usage", icon = Icons.Filled.DeviceHub)
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

            // ── Event log ────────────────────────────────────────────────────
            item {
                SectionHeader(title = "Event Log", icon = Icons.AutoMirrored.Filled.List)
            }

            // Filter row
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

// ──────────────────────────────────────────────────────────────────────────────
// Weekly bar chart — drawn on Canvas, no external library
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(days: List<DayUsage>) {
    val maxMin = days.maxOfOrNull { it.minutesOn } ?: 0

    // Animate each bar height
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "bar_anim"
    )

    val teal = TealPrimary
    val tealDim = TealContainer
    val labelColor = OnSurfaceVariant
    val textColor = OnSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (days.isEmpty() || maxMin == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.BarChart, null,
                            tint = OnSurfaceVariant, modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No activity data yet",
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height - 28.dp.toPx() // reserve label space
                    val barCount = days.size
                    val barWidth = (chartWidth / barCount) * 0.55f
                    val gap = (chartWidth / barCount) * 0.45f
                    val halfGap = gap / 2

                    days.forEachIndexed { idx, day ->
                        val barHeightFraction = if (maxMin > 0)
                            (day.minutesOn.toFloat() / maxMin) * animationProgress
                        else 0f

                        val barH = (chartHeight * barHeightFraction).coerceAtLeast(4.dp.toPx())
                        val left = idx * (barWidth + gap) + halfGap
                        val top = chartHeight - barH

                        // Bar gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(teal, tealDim),
                                startY = top, endY = chartHeight
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Minutes label above bar
                        if (day.minutesOn > 0) {
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = teal.toArgb()
                                    textSize = 9.sp.toPx()
                                    textAlign = Paint.Align.CENTER
                                    isFakeBoldText = true
                                }
                                canvas.nativeCanvas.drawText(
                                    "${day.minutesOn}m",
                                    left + barWidth / 2,
                                    top - 4.dp.toPx(),
                                    paint
                                )
                            }
                        }

                        // Day label below chart
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = labelColor.toArgb()
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

                    // Baseline
                    drawLine(
                        color = OnSurfaceVariant.copy(alpha = 0.15f),
                        start = Offset(0f, chartHeight),
                        end = Offset(chartWidth, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "ON minutes per day (last 7 days)",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Device summary card (horizontal scroll)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceSummaryCard(summary: DeviceSummary) {
    val hours = summary.todayMinutesOn / 60
    val mins = summary.todayMinutesOn % 60
    val timeText = when {
        hours > 0 -> "${hours}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "< 1m"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DeviceHub, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            }
            Text(
                text = summary.deviceName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Today: $timeText",
                style = MaterialTheme.typography.labelSmall,
                color = if (summary.todayMinutesOn > 0) TealPrimaryLight else OnSurfaceVariant
            )
            Text(
                text = "${summary.totalEvents} events",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Filter chips
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EventFilterRow(selectedFilter: String?, onSelect: (String?) -> Unit) {
    val filters = listOf(
        null to "All",
        "ON" to "ON",
        "OFF" to "OFF",
        "CUTOFF" to "⚠ Cutoff",
        "SCHEDULE" to "Scheduled"
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPrimary.copy(alpha = 0.25f),
                    selectedLabelColor = TealPrimary,
                    containerColor = SurfaceDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = TealPrimary,
                    borderColor = OnSurfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Individual log entry row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LogEntry(log: UsageLog) {
    val eventColor = when {
        log.event.contains("CUTOFF") -> StatusError
        log.event.contains("ON") -> StatusOn
        log.event.contains("OFF") -> StatusOff
        else -> OnSurfaceVariant
    }
    val eventIcon = when {
        log.event.contains("CUTOFF") -> Icons.Filled.Warning
        log.event.contains("ON") -> Icons.Filled.Power
        log.event.contains("SCHEDULE") -> Icons.Filled.Schedule
        else -> Icons.Filled.PowerOff
    }

    val timeText = log.timestamp?.let {
        SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()).format(Date(it.seconds * 1000))
    } ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Event icon dot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(eventColor.copy(alpha = 0.15f))
                    .border(1.dp, eventColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon,
                    contentDescription = null,
                    tint = eventColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.deviceName.ifBlank { "Unknown device" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    EventBadge(event = log.event, color = eventColor)
                }
                if (log.floorPlanName.isNotBlank()) {
                    Text(
                        text = log.floorPlanName,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Timestamp
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun EventBadge(event: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = event,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(3.dp, 24.dp)
                .clip(CircleShape)
                .background(TealPrimary)
        )
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.List, null,
                tint = OnSurfaceVariant, modifier = Modifier.size(40.dp)
            )
            Text(
                "No events logged yet.\nToggle a device to start tracking.",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
