package com.example.smarthome.data.model

import com.google.firebase.Timestamp

/**
 * Represents a single floor plan in the home.
 * Firestore path: floorPlans/{floorPlanId}
 */
data class FloorPlan(
    val id: String = "",
    val userId: String = "",         // Owner of this floor plan
    val name: String = "",           // e.g. "Ground Floor", "First Floor"
    val imageUrl: String = "",       // URL or local drawable reference
    val createdAt: Timestamp? = null
)

/**
 * All possible device types in the system.
 */
enum class DeviceType {
    OUTLET,         // Simple ON/OFF power outlet
    MULTI_SWITCH,   // Gang-box with multiple individually addressable switches
    IRON,           // Fire-hazard device with max_on_duration enforcement
    LIGHT,          // Scheduled light bulb (auto on/off by time window)
    CAMERA          // Security camera (mock snapshot / stream URL)
}

/**
 * The four possible operational states any device can be in.
 */
enum class DeviceStatus {
    ON,
    OFF,
    ERROR,
    DISCONNECTED
}

/**
 * Represents a single child switch inside a MULTI_SWITCH unit.
 */
data class SwitchState(
    val switchIndex: Int = 0,           // 0-based index within the multi-switch unit
    val label: String = "",             // e.g. "Switch 1", "Fan", "AC"
    val status: String = DeviceStatus.OFF.name  // ON | OFF
)

/**
 * Represents a device placed on a floor plan grid.
 * Firestore path: floorPlans/{floorPlanId}/devices/{deviceId}
 */
data class Device(
    val id: String = "",
    val floorPlanId: String = "",
    val name: String = "",
    val type: String = DeviceType.OUTLET.name,       // DeviceType enum name

    // Grid position on the floor plan overlay
    val gridX: Int = 0,
    val gridY: Int = 0,

    // Operational state
    val status: String = DeviceStatus.OFF.name,      // DeviceStatus enum name

    // ---- IRON-specific fields ----
    val maxOnDurationMinutes: Int = 30,              // max active duration before auto-cutoff
    val lastTurnedOnAt: Timestamp? = null,           // written when toggled ON

    // ---- LIGHT-specific fields ----
    val turnOnTime: String = "",                     // "HH:mm" 24h format, e.g. "18:00"
    val turnOffTime: String = "",                    // "HH:mm" 24h format, e.g. "06:00"

    // ---- CAMERA-specific fields ----
    val cameraSnapshotUrl: String = "",              // mock snapshot image URL
    val cameraStreamUrl: String = "",               // mock RTSP/stream URL

    // ---- MULTI_SWITCH-specific fields ----
    val switchCount: Int = 2,                        // total number of switches in this unit
    val switches: List<SwitchState> = emptyList(),  // child switch states

    // Metadata
    val createdAt: Timestamp? = null
) {
    /** Convenience accessor for the typed enum */
    fun deviceType(): DeviceType = DeviceType.valueOf(type)

    /** Convenience accessor for the typed status enum */
    fun deviceStatus(): DeviceStatus = DeviceStatus.valueOf(status)
}

/**
 * Represents a usage log entry for device activity tracking.
 * Firestore path: usageLogs/{logId}
 */
data class UsageLog(
    val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val floorPlanId: String = "",
    val floorPlanName: String = "",
    val event: String = "",              // "ON" | "OFF" | "CUTOFF" | "SCHEDULE_ON" | "SCHEDULE_OFF"
    val timestamp: Any? = null           // Can be Timestamp or String (due to simulator bug)
) {
    /** Safely extract timestamp as a Firestore Timestamp */
    fun safeTimestamp(): Timestamp? {
        return when (timestamp) {
            is Timestamp -> timestamp
            is com.google.firebase.Timestamp -> timestamp
            is String -> null // Skip invalid string timestamps
            else -> null
        }
    }
}
