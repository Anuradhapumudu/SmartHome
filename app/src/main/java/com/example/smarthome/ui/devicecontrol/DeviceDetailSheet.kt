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
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.*
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
import java.util.*
import java.util.concurrent.TimeUnit

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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DeviceSheetHeader(device = device)

            when (device.deviceType()) {
                DeviceType.OUTLET -> OutletControls(device = device, onToggle = onToggle)
                DeviceType.IRON -> IronControls(device = device, onToggle = onToggle)
                DeviceType.LIGHT -> LightControls(device = device, onToggle = onToggle, onUpdateDevice = onUpdateDevice)
                DeviceType.MULTI_SWITCH -> MultiSwitchControls(device = device, onSwitchToggle = onSwitchToggle)
                DeviceType.CAMERA -> CameraControls(device = device)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Device")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove device?") },
            text = {
                Text("Are you sure you want to remove \"${device.name}\"?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                    onDismiss()
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun DeviceSheetHeader(device: Device) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceTypeIcon(device.deviceType()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun OutletControls(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON.name
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isOn) "Current Active" else "Currently Inactive",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

@Composable
private fun IronControls(device: Device, onToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ON.name
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
    val pct = if (maxSec > 0) (elapsedSeconds.toFloat() / maxSec.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (isOn) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Safety Timer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${remMin}m ${String.format("%02d", remSecPart)}s left",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (remMin < 5) SoftRed else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { 1f - pct },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = if (remMin < 5) SoftRed else SoftGreen,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }
}

@Composable
private fun LightControls(device: Device, onToggle: () -> Unit, onUpdateDevice: (Device) -> Unit = {}) {
    val isOn = device.status == DeviceStatus.ON.name
    var showEditSchedule by remember { mutableStateOf(false) }
    val timeRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScheduleItem(label = "On at", time = device.turnOnTime.ifBlank { "--:--" })
                    ScheduleItem(label = "Off at", time = device.turnOffTime.ifBlank { "--:--" })
                }
                OutlinedButton(
                    onClick = { showEditSchedule = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modify Schedule")
                }
            }
        }
        
        BigToggleButton(isOn = isOn, onToggle = onToggle)
        GridPositionInfo(device = device)
    }

    if (showEditSchedule) {
        var editOnTime by remember { mutableStateOf(device.turnOnTime) }
        var editOffTime by remember { mutableStateOf(device.turnOffTime) }
        val isTimeValid = timeRegex.matches(editOnTime.trim()) && timeRegex.matches(editOffTime.trim())

        AlertDialog(
            onDismissRequest = { showEditSchedule = false },
            title = { Text("Scheduling") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editOnTime,
                        onValueChange = { editOnTime = it },
                        label = { Text("Start Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOffTime,
                        onValueChange = { editOffTime = it },
                        label = { Text("End Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDevice(device.copy(turnOnTime = editOnTime.trim(), turnOffTime = editOffTime.trim()))
                        showEditSchedule = false
                    },
                    enabled = isTimeValid,
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSchedule = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun ScheduleItem(label: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MultiSwitchControls(device: Device, onSwitchToggle: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Individually Addressable Switches",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        device.switches.forEach { sw ->
            val isOn = sw.status == DeviceStatus.ON.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sw.label.ifBlank { "Switch ${sw.switchIndex + 1}" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = isOn,
                    onCheckedChange = { onSwitchToggle(sw.switchIndex) }
                )
            }
        }
        GridPositionInfo(device = device)
    }
}

@Composable
private fun CameraControls(device: Device) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (device.cameraSnapshotUrl.isNotBlank()) {
            AsyncImage(
                model = device.cameraSnapshotUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(device.cameraStreamUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {}
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = device.cameraStreamUrl.isNotBlank()
        ) {
            Icon(Icons.AutoMirrored.Outlined.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Stream")
        }
        GridPositionInfo(device = device)
    }
}

@Composable
private fun BigToggleButton(isOn: Boolean, onToggle: () -> Unit) {
    val color = if (isOn) SoftGreen else MaterialTheme.colorScheme.primary
    Button(
        onClick = onToggle,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = if (isOn) Icons.Outlined.PowerSettingsNew else Icons.Outlined.PowerSettingsNew,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isOn) "Switch Off" else "Switch On",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        DeviceStatus.ON.name -> SoftGreen
        DeviceStatus.ERROR.name -> SoftRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TypeBadge(type: DeviceType) {
    val displayType = type.name.lowercase().replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
    }
    Text(
        text = displayType,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GridPositionInfo(device: Device) {
    Text(
        text = "Location: Sector ${device.gridX}-${device.gridY}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

fun deviceTypeIcon(type: DeviceType) = when (type) {
    DeviceType.OUTLET -> Icons.Outlined.Power
    DeviceType.MULTI_SWITCH -> Icons.Outlined.Tune
    DeviceType.IRON -> Icons.Outlined.Timer
    DeviceType.LIGHT -> Icons.Outlined.Lightbulb
    DeviceType.CAMERA -> Icons.Outlined.Videocam
}

fun statusColor(status: String): Color = when (status) {
    DeviceStatus.ON.name -> SoftGreen
    DeviceStatus.ERROR.name -> SoftRed
    else -> SoftGrey
}
