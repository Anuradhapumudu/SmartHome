package com.example.smarthome.ui.devicecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.data.model.SwitchState
import com.example.smarthome.ui.theme.*

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

    var switchCount by remember { mutableIntStateOf(2) }
    var maxOnDuration by remember { mutableStateOf("30") }
    var turnOnTime by remember { mutableStateOf("18:00") }
    var turnOffTime by remember { mutableStateOf("06:00") }
    var snapshotUrl by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }

    val timeRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
    val isTimeValid = if (selectedType == DeviceType.LIGHT) {
        timeRegex.matches(turnOnTime.trim()) && timeRegex.matches(turnOffTime.trim())
    } else true

    val isNameValid = name.isNotBlank()
    val canConfirm = isNameValid && isTimeValid
    val positionOccupied = Pair(gridX, gridY) in occupiedPositions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Integrate Device",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Living Room Lamp") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DeviceTypeSelector(
                        selected = selectedType,
                        onSelect = { selectedType = it }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Placement (0–7)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gridX.toString(),
                            onValueChange = { gridX = it.toIntOrNull()?.coerceIn(0, 7) ?: gridX },
                            label = { Text("Col") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogTextFieldColors(),
                            isError = positionOccupied,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gridY.toString(),
                            onValueChange = { gridY = it.toIntOrNull()?.coerceIn(0, 7) ?: gridY },
                            label = { Text("Row") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogTextFieldColors(),
                            isError = positionOccupied,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (positionOccupied) {
                        Text(
                            "This coordinate is already in use",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                when (selectedType) {
                    DeviceType.MULTI_SWITCH -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Configuration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SwitchCountSelector(
                                selected = switchCount,
                                onSelect = { switchCount = it }
                            )
                        }
                    }

                    DeviceType.IRON -> {
                        OutlinedTextField(
                            value = maxOnDuration,
                            onValueChange = { maxOnDuration = it },
                            label = { Text("Auto-cutoff limit (min)") },
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
                                label = { Text("Daily Start") },
                                singleLine = true,
                                colors = dialogTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = turnOffTime,
                                onValueChange = { turnOffTime = it },
                                label = { Text("Daily End") },
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
                            label = { Text("Static Snapshot URL") },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = streamUrl,
                            onValueChange = { streamUrl = it },
                            label = { Text("Live Feed URL") },
                            singleLine = true,
                            colors = dialogTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
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
                        turnOnTime = turnOnTime.trim(),
                        turnOffTime = turnOffTime.trim(),
                        snapshotUrl = snapshotUrl.trim(),
                        streamUrl = streamUrl.trim()
                    )
                    onConfirm(device)
                },
                enabled = canConfirm && !positionOccupied,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun DeviceTypeSelector(
    selected: DeviceType,
    onSelect: (DeviceType) -> Unit
) {
    val types = listOf(
        DeviceType.OUTLET to "Outlet",
        DeviceType.LIGHT to "Bulb",
        DeviceType.MULTI_SWITCH to "Multi-Switch",
        DeviceType.IRON to "Safety Iron",
        DeviceType.CAMERA to "Security Cam"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (type, label) ->
            val isSelected = selected == type
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(type) },
                label = {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    Text("$count-Way", style = MaterialTheme.typography.labelSmall)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = Color.Transparent
)

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
                label = "Channel ${i + 1}",
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
