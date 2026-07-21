package com.example.smarthome.ui.devicecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.ui.theme.*
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

/**
 * Bottom sheet showing device details and controls.
 * Handles all device types: OUTLET, MULTI_SWITCH, IRON, LIGHT, CAMERA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailSheet(
    device: Device,
    onToggle: () -> Unit,
    onSwitchToggle: (Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            DeviceSheetHeader(device = device)

            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))

            // Controls — per device type
            when (device.deviceType()) {
                DeviceType.OUTLET -> OutletControls(device = device, onToggle = onToggle)
                DeviceType.IRON -> IronControls(device = device, onToggle = onToggle)
                DeviceType.LIGHT -> LightControls(device = device, onToggle = onToggle)
                DeviceType.MULTI_SWITCH -> MultiSwitchControls(device = device, onSwitchToggle = onSwitchToggle)
                DeviceType.CAMERA -> CameraControls(device = device)
            }

            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = 0.15f))

            // Delete button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = StatusError.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Device", fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Device?") },
            text = {
                Text(
                    "\"${device.name}\" will be permanently removed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Remove", color = StatusError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            containerColor = SurfaceVariantDark
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceSheetHeader(device: Device) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon + status indicator
        Box(modifier = Modifier.size(56.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceTypeIcon(device.deviceType()),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor(device.status))
                    .border(2.dp, SurfaceDark, CircleShape)
                    .align(Alignment.TopEnd)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = device.status)
                TypeBadge(type = device.deviceType())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Outlet controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OutletControls(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON.name
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isOn) "Outlet is ON" else "Outlet is OFF",
            style = MaterialTheme.typography.titleMedium,
            color = if (isOn) StatusOn else OnSurfaceVariant
        )
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Iron controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun IronControls(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON.name

    // Compute remaining safe time
    val minutesOn = remember(device.lastTurnedOnAt) {
        device.lastTurnedOnAt?.let { ts ->
            val elapsedMs = System.currentTimeMillis() - ts.seconds * 1000
            TimeUnit.MILLISECONDS.toMinutes(elapsedMs).toInt()
        } ?: 0
    }
    val minutesRemaining = (device.maxOnDurationMinutes - minutesOn).coerceAtLeast(0)
    val dangerZone = isOn && minutesRemaining <= 5

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isOn) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dangerZone) StatusError.copy(alpha = 0.15f)
                    else AmberContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (dangerZone) Icons.Filled.Warning else Icons.Filled.Timer,
                        contentDescription = null,
                        tint = if (dangerZone) StatusError else AmberAccent
                    )
                    Column {
                        Text(
                            text = if (dangerZone) "⚠ Auto-shutoff soon!" else "Active Timer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dangerZone) StatusError else AmberAccent
                        )
                        Text(
                            text = "$minutesRemaining min remaining (max ${device.maxOnDurationMinutes} min)",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Light controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LightControls(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON.name
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Schedule info card
        Card(
            colors = CardDefaults.cardColors(containerColor = TealContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScheduleItem(label = "Turns ON", time = device.turnOnTime.ifBlank { "--:--" })
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = OnSurfaceVariant.copy(alpha = 0.3f)
                )
                ScheduleItem(label = "Turns OFF", time = device.turnOffTime.ifBlank { "--:--" })
            }
        }
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

@Composable
private fun ScheduleItem(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TealPrimary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Multi-switch controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MultiSwitchControls(device: Device, onSwitchToggle: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Individual Switches",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface
        )
        if (device.switches.isEmpty()) {
            Text("No switch states found.", color = OnSurfaceVariant)
        } else {
            device.switches.forEach { sw ->
                val isOn = sw.status == DeviceStatus.ON.name
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOn) StatusOn else StatusOff)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = sw.label.ifBlank { "Switch ${sw.switchIndex + 1}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isOn,
                            onCheckedChange = { onSwitchToggle(sw.switchIndex) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = StatusOn,
                                uncheckedThumbColor = OnSurfaceVariant,
                                uncheckedTrackColor = SurfaceVariantDark
                            )
                        )
                    }
                }
            }
        }
        GridPositionInfo(device = device)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraControls(device: Device) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Snapshot
        if (device.cameraSnapshotUrl.isNotBlank()) {
            Text(
                "Live Snapshot",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            AsyncImage(
                model = device.cameraSnapshotUrl,
                contentDescription = "Camera snapshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No snapshot configured", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Stream URL
        if (device.cameraStreamUrl.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = TealPrimary)
                    Text(
                        text = device.cameraStreamUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimaryLight,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
        GridPositionInfo(device = device)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BigToggleButton(isOn: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) StatusOn.copy(alpha = 0.2f) else SurfaceVariantDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isOn) StatusOn else OnSurfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(50.dp),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = if (isOn) Icons.Filled.PowerOff else Icons.Filled.Power,
            contentDescription = null,
            tint = if (isOn) StatusOn else OnSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isOn) "Turn OFF" else "Turn ON",
            fontWeight = FontWeight.Bold,
            color = if (isOn) StatusOn else OnSurface,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = when (status) {
        DeviceStatus.ON.name -> StatusOn.copy(alpha = 0.2f) to StatusOn
        DeviceStatus.ERROR.name -> StatusError.copy(alpha = 0.2f) to StatusError
        DeviceStatus.DISCONNECTED.name -> StatusDisconnected.copy(alpha = 0.2f) to StatusDisconnected
        else -> StatusOff.copy(alpha = 0.2f) to OnSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = bg
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TypeBadge(type: DeviceType) {
    Surface(
        shape = CircleShape,
        color = TealContainer
    ) {
        Text(
            text = type.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimaryLight,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GridPositionInfo(device: Device) {
    Text(
        text = "Grid position: (${device.gridX}, ${device.gridY})",
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceVariant
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun deviceTypeIcon(type: DeviceType) = when (type) {
    DeviceType.OUTLET -> Icons.Filled.Power
    DeviceType.MULTI_SWITCH -> Icons.Filled.ToggleOn
    DeviceType.IRON -> Icons.Filled.LocalLaundryService
    DeviceType.LIGHT -> Icons.Filled.Lightbulb
    DeviceType.CAMERA -> Icons.Filled.Videocam
}

fun statusColor(status: String): Color = when (status) {
    DeviceStatus.ON.name -> StatusOn
    DeviceStatus.ERROR.name -> StatusError
    DeviceStatus.DISCONNECTED.name -> StatusDisconnected
    else -> StatusOff
}
