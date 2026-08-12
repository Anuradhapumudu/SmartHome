package com.example.smarthome.ui.devicecontrol

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.ui.theme.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
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
    onDismiss: () -> Unit,
    onUpdateDevice: (Device) -> Unit = {}
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
                DeviceType.LIGHT -> LightControls(device = device, onToggle = onToggle, onUpdateDevice = onUpdateDevice)
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

    // Live countdown — ticks every second using LaunchedEffect
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(device.lastTurnedOnAt, isOn) {
        if (isOn && device.lastTurnedOnAt != null) {
            while (true) {
                val nowSec = System.currentTimeMillis() / 1000L
                elapsedSeconds = nowSec - device.lastTurnedOnAt.seconds
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val maxSec = (device.maxOnDurationMinutes).toLong() * 60L
    val remSec = (maxSec - elapsedSeconds).coerceAtLeast(0L)
    val remMin = remSec / 60
    val remSecPart = remSec % 60
    val dangerZone = isOn && remMin <= 5
    val pct = if (maxSec > 0) (elapsedSeconds.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isOn) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dangerZone) StatusError.copy(alpha = 0.15f) else AmberContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (dangerZone) Icons.Filled.Warning else Icons.Filled.Timer,
                            contentDescription = null,
                            tint = if (dangerZone) StatusError else AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (dangerZone) "⚠ Auto-shutoff imminent!" else "Iron Safety Timer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (dangerZone) StatusError else AmberAccent
                            )
                            Text(
                                text = "Max duration: ${device.maxOnDurationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    // Live countdown display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (dangerZone) StatusError.copy(alpha = 0.1f) else AmberAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (remSec <= 0L) "CUTOFF PENDING" else "${remMin}m ${String.format("%02d", remSecPart)}s remaining",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (dangerZone) StatusError else AmberAccent
                        )
                    }
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(OnSurfaceVariant.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pct)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (dangerZone) StatusError else AmberAccent)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text("${device.maxOnDurationMinutes} min", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }
                }
            }
        }
        Text(
            text = if (isOn) "Light is ON" else "Light is OFF",
            style = MaterialTheme.typography.titleMedium,
            color = if (isOn) StatusOn else OnSurfaceVariant
        )
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Light controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LightControls(device: Device, onToggle: () -> Unit, onUpdateDevice: (Device) -> Unit = {}) {
    val isOn = device.status == DeviceStatus.ON.name

    // Editable schedule state
    var editOnTime by remember(device.turnOnTime) { mutableStateOf(device.turnOnTime) }
    var editOffTime by remember(device.turnOffTime) { mutableStateOf(device.turnOffTime) }
    var showEditSchedule by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                if (device.turnOnTime.isBlank() && device.turnOffTime.isBlank()) {
                    Text(
                        text = "No auto-schedule configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text(
                        text = "☁ Cloud function auto-toggles this light every minute",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                // Edit schedule button
                OutlinedButton(
                    onClick = { showEditSchedule = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Schedule", fontWeight = FontWeight.Medium)
                }
            }
        }
        Text(
            text = if (isOn) "Light is ON" else "Light is OFF",
            style = MaterialTheme.typography.titleMedium,
            color = if (isOn) StatusOn else OnSurfaceVariant
        )
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }

    // Edit Schedule Dialog
    if (showEditSchedule) {
        AlertDialog(
            onDismissRequest = { showEditSchedule = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                    Text("Edit Light Schedule")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set the daily time window. The cloud function toggles this light automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editOnTime,
                        onValueChange = { editOnTime = it },
                        label = { Text("Turn ON time (HH:mm)") },
                        placeholder = { Text("e.g. 18:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            focusedLabelColor = TealPrimary
                        )
                    )
                    OutlinedTextField(
                        value = editOffTime,
                        onValueChange = { editOffTime = it },
                        label = { Text("Turn OFF time (HH:mm)") },
                        placeholder = { Text("e.g. 06:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            focusedLabelColor = TealPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateDevice(device.copy(turnOnTime = editOnTime.trim(), turnOffTime = editOffTime.trim()))
                        showEditSchedule = false
                    },
                    enabled = !isSaving
                ) { Text("Save", color = TealPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditSchedule = false }) { Text("Cancel") }
            },
            containerColor = SurfaceVariantDark
        )
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
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Snapshot section
        Text(
            "Live Snapshot",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface
        )
        if (device.cameraSnapshotUrl.isNotBlank()) {
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
                    Text("No snapshot URL configured", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Add a cameraSnapshotUrl in Firestore to display a feed",
                        color = OnSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Stream URL card + open button
        if (device.cameraStreamUrl.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealContainer),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Stream URL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                    Text(
                        text = device.cameraStreamUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimaryLight,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(device.cameraStreamUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // URL not openable
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Stream", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.SignalWifiOff, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text("No stream URL configured", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
