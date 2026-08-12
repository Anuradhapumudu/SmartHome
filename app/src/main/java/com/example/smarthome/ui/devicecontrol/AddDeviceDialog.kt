package com.example.smarthome.ui.devicecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.data.model.SwitchState
import com.example.smarthome.ui.theme.*

/**
 * Add Device dialog with dynamic fields based on selected device type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    occupiedPositions: Set<Pair<Int, Int>> = emptySet(),
    initialPosition: Pair<Int, Int>? = null,
    onConfirm: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DeviceType.OUTLET) }
    var gridX by remember { mutableIntStateOf(initialPosition?.first ?: 0) }
    var gridY by remember { mutableIntStateOf(initialPosition?.second ?: 0) }

    // Type-specific fields
    var switchCount by remember { mutableIntStateOf(2) }
    var maxOnDuration by remember { mutableStateOf("30") }
    var turnOnTime by remember { mutableStateOf("18:00") }
    var turnOffTime by remember { mutableStateOf("06:00") }
    var snapshotUrl by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }

    val positionOccupied = Pair(gridX, gridY) in occupiedPositions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Device",
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Device Name ──────────────────────────────────────────────
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device name") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Device Type Selector ─────────────────────────────────────
                Text(
                    "Device Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                DeviceTypeSelector(
                    selected = selectedType,
                    onSelect = { selectedType = it }
                )

                // ── Grid Position ────────────────────────────────────────────
                Text(
                    "Grid Position (0–7)",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = gridX.toString(),
                        onValueChange = { gridX = it.toIntOrNull()?.coerceIn(0, 7) ?: gridX },
                        label = { Text("Column (X)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = dialogTextFieldColors(),
                        isError = positionOccupied,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gridY.toString(),
                        onValueChange = { gridY = it.toIntOrNull()?.coerceIn(0, 7) ?: gridY },
                        label = { Text("Row (Y)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = dialogTextFieldColors(),
                        isError = positionOccupied,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (positionOccupied) {
                    Text(
                        "Position ($gridX, $gridY) is already occupied",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusError
                    )
                }

                // ── Type-Specific Fields ─────────────────────────────────────
                when (selectedType) {
                    DeviceType.MULTI_SWITCH -> {
                        Text(
                            "Number of Switches",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                        SwitchCountSelector(
                            selected = switchCount,
                            onSelect = { switchCount = it }
                        )
                    }

                    DeviceType.IRON -> {
                        OutlinedTextField(
                            value = maxOnDuration,
                            onValueChange = { maxOnDuration = it },
                            label = { Text("Max ON duration (minutes)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    DeviceType.LIGHT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = turnOnTime,
                                onValueChange = { turnOnTime = it },
                                label = { Text("Turn ON (HH:mm)") },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = turnOffTime,
                                onValueChange = { turnOffTime = it },
                                label = { Text("Turn OFF (HH:mm)") },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    DeviceType.CAMERA -> {
                        OutlinedTextField(
                            value = snapshotUrl,
                            onValueChange = { snapshotUrl = it },
                            label = { Text("Snapshot URL (optional)") },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = streamUrl,
                            onValueChange = { streamUrl = it },
                            label = { Text("Stream URL (optional)") },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> { /* OUTLET — no extra fields */ }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val device = buildDevice(
                        name = name.trim(),
                        type = selectedType,
                        gridX = gridX,
                        gridY = gridY,
                        switchCount = switchCount,
                        maxOnDuration = maxOnDuration.toIntOrNull() ?: 30,
                        turnOnTime = turnOnTime,
                        turnOffTime = turnOffTime,
                        snapshotUrl = snapshotUrl.trim(),
                        streamUrl = streamUrl.trim()
                    )
                    onConfirm(device)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = name.isNotBlank() && !positionOccupied
            ) {
                Text("Add", color = BackgroundDark, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = SurfaceVariantDark
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceTypeSelector(
    selected: DeviceType,
    onSelect: (DeviceType) -> Unit
) {
    val types = listOf(
        DeviceType.OUTLET to "Outlet",
        DeviceType.MULTI_SWITCH to "Multi-Switch",
        DeviceType.IRON to "Iron",
        DeviceType.LIGHT to "Light",
        DeviceType.CAMERA to "Camera"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        types.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, label) ->
                    val isSelected = selected == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(type) },
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
    }
}

@Composable
private fun SwitchCountSelector(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 5).forEach { count ->
            val isSelected = selected == count
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(count) },
                label = {
                    Text(
                        "$count switches",
                        style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TealPrimary,
    focusedLabelColor = TealPrimary,
    cursorColor = TealPrimary,
    unfocusedContainerColor = SurfaceDark,
    focusedContainerColor = SurfaceDark
)

// ─────────────────────────────────────────────────────────────────────────────
// Builder helper
// ─────────────────────────────────────────────────────────────────────────────

private fun buildDevice(
    name: String,
    type: DeviceType,
    gridX: Int,
    gridY: Int,
    switchCount: Int,
    maxOnDuration: Int,
    turnOnTime: String,
    turnOffTime: String,
    snapshotUrl: String,
    streamUrl: String
): Device {
    val switches = if (type == DeviceType.MULTI_SWITCH) {
        (0 until switchCount).map { i ->
            SwitchState(
                switchIndex = i,
                label = "Switch ${i + 1}",
                status = DeviceStatus.OFF.name
            )
        }
    } else emptyList()

    return Device(
        name = name,
        type = type.name,
        gridX = gridX,
        gridY = gridY,
        status = DeviceStatus.OFF.name,
        switchCount = if (type == DeviceType.MULTI_SWITCH) switchCount else 2,
        switches = switches,
        maxOnDurationMinutes = maxOnDuration,
        turnOnTime = turnOnTime,
        turnOffTime = turnOffTime,
        cameraSnapshotUrl = snapshotUrl,
        cameraStreamUrl = streamUrl
    )
}
