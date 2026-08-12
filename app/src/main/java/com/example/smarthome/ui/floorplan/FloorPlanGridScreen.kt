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
    onBack: () -> Unit = {},
    viewModel: FloorPlanGridViewModel = viewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val floorPlan by viewModel.floorPlan.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAddDevice by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    // Map for fast lookup by grid position
    val deviceMap = remember(devices) {
        devices.associateBy { Pair(it.gridX, it.gridY) }
    }
    val occupiedPositions = remember(devices) {
        devices.map { Pair(it.gridX, it.gridY) }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = floorPlan?.name ?: "Floor Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Text(
                            text = "${devices.size} device${if (devices.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDevice = true },
                containerColor = TealPrimary,
                contentColor = BackgroundDark,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Device")
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device status legend
            DeviceLegend()

            // The interactive grid
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else {
                FloorPlanGrid(
                    deviceMap = deviceMap,
                    onCellClick = { device -> selectedDevice = device }
                )
            }
        }
    }

    // Device detail bottom sheet
    selectedDevice?.let { device ->
        DeviceDetailSheet(
            device = device,
            onToggle = { viewModel.toggleDevice(device) },
            onSwitchToggle = { switchIndex -> viewModel.toggleSwitch(device, switchIndex) },
            onDelete = {
                viewModel.deleteDevice(device.id)
                selectedDevice = null
            },
            onDismiss = { selectedDevice = null },
            onUpdateDevice = { updatedDevice -> viewModel.updateDeviceFields(updatedDevice) }
        )
    }

    // Add device dialog
    if (showAddDevice) {
        AddDeviceDialog(
            occupiedPositions = occupiedPositions,
            onConfirm = { newDevice ->
                viewModel.addDevice(newDevice)
                showAddDevice = false
            },
            onDismiss = { showAddDevice = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloorPlanGrid(
    deviceMap: Map<Pair<Int, Int>, Device>,
    onCellClick: (Device) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize: Dp = (maxWidth - 2.dp) / GRID_COLUMNS

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cellSize * GRID_ROWS)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceDark, CardDark)
                    )
                )
                .border(
                    width = 1.dp,
                    color = TealPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Draw grid lines
            for (row in 0..GRID_ROWS) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = cellSize * row),
                    color = OnSurfaceVariant.copy(alpha = 0.06f),
                    thickness = 1.dp
                )
            }
            for (col in 0..GRID_COLUMNS) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = cellSize * col),
                    color = OnSurfaceVariant.copy(alpha = 0.06f),
                    thickness = 1.dp
                )
            }

            // Draw devices at their positions
            for (row in 0 until GRID_ROWS) {
                for (col in 0 until GRID_COLUMNS) {
                    val pos = Pair(col, row)
                    val device = deviceMap[pos]

                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * col, y = cellSize * row)
                            .size(cellSize)
                            .then(
                                if (device != null) {
                                    Modifier.clickable { onCellClick(device) }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (device != null) {
                            DeviceCell(device = device, cellSize = cellSize)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual grid cell with device icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceCell(device: Device, cellSize: Dp) {
    val isOn = device.status == DeviceStatus.ON.name
    val iconSize = (cellSize.value * 0.45f).dp
    val dotSize = (cellSize.value * 0.18f).dp

    // Pulse animation when ON
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${device.id}")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(cellSize * 0.82f)
            .scale(pulseScale)
            .clip(RoundedCornerShape((cellSize.value * 0.22f).dp))
            .background(
                if (isOn) statusColor(device.status).copy(alpha = 0.18f)
                else CardDark
            )
            .border(
                width = 1.dp,
                color = if (isOn) statusColor(device.status).copy(alpha = 0.6f)
                else OnSurfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape((cellSize.value * 0.22f).dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = deviceTypeIcon(device.deviceType()),
                contentDescription = device.name,
                tint = if (isOn) statusColor(device.status) else OnSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }

        // Status dot — top right corner
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(statusColor(device.status))
                .border(1.dp, SurfaceDark, CircleShape)
                .align(Alignment.TopEnd)
                .offset(x = (-2).dp, y = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legend
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceLegend() {
    val items = listOf(
        StatusOn to "ON",
        StatusOff to "OFF",
        StatusError to "ERROR",
        StatusDisconnected to "N/A"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
