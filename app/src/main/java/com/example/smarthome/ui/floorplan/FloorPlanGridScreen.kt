package com.example.smarthome.ui.floorplan

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.ui.devicecontrol.AddDeviceDialog
import com.example.smarthome.ui.devicecontrol.DeviceDetailSheet
import com.example.smarthome.ui.devicecontrol.deviceTypeIcon
import com.example.smarthome.ui.devicecontrol.statusColor
import com.example.smarthome.ui.theme.*

private const val GRID_COLUMNS = 8
private const val GRID_ROWS = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanGridScreen(
    floorPlanId: String,
    viewModel: FloorPlanGridViewModel = viewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAddDevice by remember { mutableStateOf(false) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var pendingAddPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val selectedDevice = remember(selectedDeviceId, devices) {
        devices.firstOrNull { it.id == selectedDeviceId }
    }

    val deviceMap = remember(devices) {
        devices.associateBy { Pair(it.gridX, it.gridY) }
    }
    val occupiedPositions = remember(devices) {
        devices.map { Pair(it.gridX, it.gridY) }.toSet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DeviceLegend()

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                FloorPlanGrid(
                    deviceMap = deviceMap,
                    onCellClick = { device -> selectedDeviceId = device.id },
                    onEmptyCellClick = { x, y ->
                        pendingAddPosition = Pair(x, y)
                        showAddDevice = true
                    }
                )
            }
        }
    }

    selectedDevice?.let { device ->
        DeviceDetailSheet(
            device = device,
            onToggle = { viewModel.toggleDevice(device) },
            onSwitchToggle = { switchIndex -> viewModel.toggleSwitch(device, switchIndex) },
            onDelete = {
                viewModel.deleteDevice(device.id)
                selectedDeviceId = null
            },
            onDismiss = { selectedDeviceId = null },
            onUpdateDevice = { updatedDevice -> viewModel.updateDeviceFields(updatedDevice) }
        )
    }

    if (showAddDevice) {
        AddDeviceDialog(
            occupiedPositions = occupiedPositions,
            initialPosition = pendingAddPosition,
            onConfirm = { newDevice ->
                viewModel.addDevice(newDevice)
                showAddDevice = false
                pendingAddPosition = null
            },
            onDismiss = {
                showAddDevice = false
                pendingAddPosition = null
            }
        )
    }
}

@Composable
private fun FloorPlanGrid(
    deviceMap: Map<Pair<Int, Int>, Device>,
    onCellClick: (Device) -> Unit,
    onEmptyCellClick: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize: Dp = (maxWidth - 2.dp) / GRID_COLUMNS

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellSize * GRID_ROWS)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            for (row in 0..GRID_ROWS) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().offset(y = cellSize * row),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }
            for (col in 0..GRID_COLUMNS) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().offset(x = cellSize * col),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }

            for (row in 0 until GRID_ROWS) {
                for (col in 0 until GRID_COLUMNS) {
                    val pos = Pair(col, row)
                    val device = deviceMap[pos]

                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize)
                            .clickable {
                                if (device != null) {
                                    onCellClick(device)
                                } else {
                                    onEmptyCellClick(col, row)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (device != null) {
                            DeviceCell(device = device, cellSize = cellSize)
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                modifier = Modifier.size(cellSize * 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCell(device: Device, cellSize: Dp) {
    val isOn = device.status == DeviceStatus.ON.name
    val iconSize = (cellSize.value * 0.4f).dp

    Box(
        modifier = Modifier
            .size(cellSize * 0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isOn) statusColor(device.status).copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = if (isOn) statusColor(device.status).copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = deviceTypeIcon(device.deviceType()),
            contentDescription = device.name,
            tint = if (isOn) statusColor(device.status) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(iconSize)
        )

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor(device.status))
                .align(Alignment.TopEnd)
                .offset(x = (-4).dp, y = 4.dp)
        )
    }
}

@Composable
private fun DeviceLegend() {
    val items = listOf<Pair<Color, String>>(
        SoftGreen to "On",
        SoftGrey to "Off",
        SoftRed to "Error"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
