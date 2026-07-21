package com.example.smarthome.ui.reporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.data.model.UsageLog
import com.example.smarthome.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

/** Data for a single bar in the 7-day chart. */
data class DayUsage(
    val dayLabel: String,   // "Mon", "Tue" …
    val minutesOn: Int      // total ON-minutes that day
)

/** Per-device summary shown as a summary card. */
data class DeviceSummary(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val floorPlanName: String,
    val todayMinutesOn: Long,
    val totalEvents: Int
)

class ReportingViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    // ── raw logs ────────────────────────────────────────────────────────────
    private val _allLogs = MutableStateFlow<List<UsageLog>>(emptyList())

    // ── filter chip ─────────────────────────────────────────────────────────
    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    // ── exposed state ────────────────────────────────────────────────────────
    private val _filteredLogs = MutableStateFlow<List<UsageLog>>(emptyList())
    val filteredLogs: StateFlow<List<UsageLog>> = _filteredLogs.asStateFlow()

    private val _deviceSummaries = MutableStateFlow<List<DeviceSummary>>(emptyList())
    val deviceSummaries: StateFlow<List<DeviceSummary>> = _deviceSummaries.asStateFlow()

    private val _weekChart = MutableStateFlow<List<DayUsage>>(emptyList())
    val weekChart: StateFlow<List<DayUsage>> = _weekChart.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUsageLogs().collect { logs ->
                _allLogs.value = logs
                _isLoading.value = false
                recompute(logs, _selectedFilter.value)
            }
        }
        // Recompute whenever filter changes
        viewModelScope.launch {
            combine(_allLogs, _selectedFilter) { logs, filter ->
                Pair(logs, filter)
            }.collect { (logs, filter) ->
                recompute(logs, filter)
            }
        }
    }

    fun setFilter(type: String?) {
        _selectedFilter.value = type
    }

    private fun recompute(logs: List<UsageLog>, filter: String?) {
        val filtered = if (filter == null) logs
        else logs.filter { it.event.contains(filter, ignoreCase = true) || it.deviceId.isNotEmpty() }

        _filteredLogs.value = filtered

        // ── 7-day chart: bucket ON events by day ─────────────────────────
        val calendar = Calendar.getInstance()
        val dayBuckets = mutableMapOf<Int, Int>() // dayOfYear → minutes ON
        val dayLabels = mutableMapOf<Int, String>()
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Fill last 7 days with 0
        for (offset in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            val doy = c.get(Calendar.DAY_OF_YEAR)
            dayBuckets[doy] = 0
            dayLabels[doy] = dayNames[c.get(Calendar.DAY_OF_WEEK) - 1]
        }

        // Pair consecutive ON→OFF events per device to get duration
        val onEvents = mutableMapOf<String, Long>() // deviceId → turnedOnMs
        logs.sortedBy { it.timestamp?.seconds ?: 0L }.forEach { log ->
            val ts = log.timestamp?.seconds?.times(1000) ?: return@forEach
            when (log.event) {
                "ON", "SCHEDULE_ON" -> onEvents[log.deviceId] = ts
                "OFF", "CUTOFF", "SCHEDULE_OFF" -> {
                    val onTs = onEvents.remove(log.deviceId) ?: return@forEach
                    val durationMs = ts - onTs
                    val durationMin = (durationMs / 60_000).toInt().coerceAtLeast(0)
                    val c = Calendar.getInstance()
                    c.timeInMillis = onTs
                    val doy = c.get(Calendar.DAY_OF_YEAR)
                    if (dayBuckets.containsKey(doy)) {
                        dayBuckets[doy] = (dayBuckets[doy] ?: 0) + durationMin
                    }
                }
            }
        }

        val sortedDays = dayBuckets.keys.sorted()
        _weekChart.value = sortedDays.map { doy ->
            DayUsage(dayLabels[doy] ?: "?", dayBuckets[doy] ?: 0)
        }

        // ── per-device summaries ─────────────────────────────────────────
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val grouped = logs.groupBy { it.deviceId }
        val summaries = grouped.map { (deviceId, deviceLogs) ->
            val first = deviceLogs.first()
            val todayMs = computeTodayMinutes(deviceLogs, todayStart)
            DeviceSummary(
                deviceId = deviceId,
                deviceName = first.deviceName.ifBlank { "Unknown" },
                deviceType = "",
                floorPlanName = first.floorPlanName.ifBlank { "" },
                todayMinutesOn = todayMs,
                totalEvents = deviceLogs.size
            )
        }.sortedByDescending { it.todayMinutesOn }
        _deviceSummaries.value = summaries
    }

    private fun computeTodayMinutes(logs: List<UsageLog>, todayStart: Long): Long {
        var total = 0L
        var onTs: Long? = null
        logs.sortedBy { it.timestamp?.seconds ?: 0L }.forEach { log ->
            val ts = (log.timestamp?.seconds ?: 0L) * 1000
            if (ts < todayStart) {
                // before today — track ON state
                when (log.event) {
                    "ON", "SCHEDULE_ON" -> onTs = ts
                    "OFF", "CUTOFF", "SCHEDULE_OFF" -> onTs = null
                }
                return@forEach
            }
            when (log.event) {
                "ON", "SCHEDULE_ON" -> onTs = ts
                "OFF", "CUTOFF", "SCHEDULE_OFF" -> {
                    val startMs = if ((onTs ?: 0L) < todayStart) todayStart else onTs
                    if (startMs != null) {
                        total += (ts - startMs) / 60_000
                        onTs = null
                    }
                }
            }
        }
        // If still ON, count until now
        onTs?.let { start ->
            val startMs = if (start < todayStart) todayStart else start
            total += (System.currentTimeMillis() - startMs) / 60_000
        }
        return total.coerceAtLeast(0L)
    }
}
