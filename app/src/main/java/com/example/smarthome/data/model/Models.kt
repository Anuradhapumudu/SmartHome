package com.example.smarthome.data.model

import com.google.firebase.Timestamp

data class FloorPlan(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null
)

enum class DeviceType {
    OUTLET,
    MULTI_SWITCH,
    IRON,
    LIGHT,
    CAMERA
}

enum class DeviceStatus {
    ON,
    OFF,
    ERROR,
    DISCONNECTED
}

data class SwitchState(
    val switchIndex: Int = 0,
    val label: String = "",
    val status: String = DeviceStatus.OFF.name
)

data class Device(
    val id: String = "",
    val floorPlanId: String = "",
    val name: String = "",
    val type: String = DeviceType.OUTLET.name,

    val gridX: Int = 0,
    val gridY: Int = 0,

    val status: String = DeviceStatus.OFF.name,

    val maxOnDurationMinutes: Int = 30,
    val lastTurnedOnAt: Timestamp? = null,

    val turnOnTime: String = "",
    val turnOffTime: String = "",

    val cameraSnapshotUrl: String = "",
    val cameraStreamUrl: String = "",

    val switchCount: Int = 2,
    val switches: List<SwitchState> = emptyList(),

    val createdAt: Timestamp? = null
) {
    fun deviceType(): DeviceType = DeviceType.valueOf(type)

    fun deviceStatus(): DeviceStatus = DeviceStatus.valueOf(status)
}

data class UsageLog(
    val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val floorPlanId: String = "",
    val floorPlanName: String = "",
    val event: String = "",
    val timestamp: Any? = null
) {
    fun safeTimestamp(): Timestamp? {
        return when (timestamp) {
            is Timestamp -> timestamp
            is com.google.firebase.Timestamp -> timestamp
            is String -> null
            else -> null
        }
    }
}
