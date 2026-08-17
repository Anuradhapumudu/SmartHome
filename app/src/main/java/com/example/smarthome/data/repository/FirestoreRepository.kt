package com.example.smarthome.data.repository

import android.util.Log
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.FloorPlan
import com.example.smarthome.data.model.UsageLog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val floorPlansRef = db.collection("floorPlans")
    private val usageLogsRef = db.collection("usageLogs")

    private val currentUid get() = auth.currentUser?.uid ?: ""

    fun observeFloorPlans(): Flow<List<FloorPlan>> = callbackFlow {
        val registration: ListenerRegistration = floorPlansRef
            .whereEqualTo("userId", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeFloorPlans error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val plans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FloorPlan::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(plans)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addFloorPlan(floorPlan: FloorPlan): String {
        val docRef = floorPlansRef.add(
            floorPlan.copy(
                userId = currentUid,
                createdAt = Timestamp.now()
            )
        ).await()
        return docRef.id
    }

    suspend fun deleteFloorPlan(floorPlanId: String) {
        floorPlansRef.document(floorPlanId).delete().await()
    }

    fun observeDevices(floorPlanId: String): Flow<List<Device>> = callbackFlow {
        val devicesRef = floorPlansRef.document(floorPlanId).collection("devices")
        val registration: ListenerRegistration = devicesRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeDevices error", error)
                    return@addSnapshotListener
                }
                val devices = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Device::class.java)?.copy(id = doc.id, floorPlanId = floorPlanId)
                } ?: emptyList()
                trySend(devices)
            }
        awaitClose { registration.remove() }
    }

    fun observeDevice(floorPlanId: String, deviceId: String): Flow<Device?> = callbackFlow {
        val docRef = floorPlansRef
            .document(floorPlanId)
            .collection("devices")
            .document(deviceId)
        val registration: ListenerRegistration = docRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeDevice error", error)
                    return@addSnapshotListener
                }
                val device = snapshot?.toObject(Device::class.java)
                    ?.copy(id = snapshot.id, floorPlanId = floorPlanId)
                trySend(device)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addDevice(floorPlanId: String, device: Device): String {
        val devicesRef = floorPlansRef.document(floorPlanId).collection("devices")
        val docRef = devicesRef.add(
            device.copy(floorPlanId = floorPlanId, createdAt = Timestamp.now())
        ).await()
        return docRef.id
    }

    suspend fun updateDevice(floorPlanId: String, device: Device) {
        val docRef = floorPlansRef
            .document(floorPlanId)
            .collection("devices")
            .document(device.id)
        docRef.set(device).await()
    }

    suspend fun toggleDeviceStatus(floorPlanId: String, device: Device) {
        val newStatus = if (device.status == DeviceStatus.ON.name)
            DeviceStatus.OFF.name else DeviceStatus.ON.name

        val docRef = floorPlansRef
            .document(floorPlanId)
            .collection("devices")
            .document(device.id)

        val updates = mutableMapOf<String, Any>(
            "status" to newStatus
        )

        if (device.type == "IRON" && newStatus == DeviceStatus.ON.name) {
            updates["lastTurnedOnAt"] = Timestamp.now()
        }

        docRef.update(updates).await()

        logUsage(device, floorPlanId, newStatus)
    }

    suspend fun updateMultiSwitch(
        floorPlanId: String,
        deviceId: String,
        updatedSwitches: List<com.example.smarthome.data.model.SwitchState>
    ) {
        val docRef = floorPlansRef
            .document(floorPlanId)
            .collection("devices")
            .document(deviceId)
        docRef.update("switches", updatedSwitches).await()
    }

    suspend fun deleteDevice(floorPlanId: String, deviceId: String) {
        floorPlansRef
            .document(floorPlanId)
            .collection("devices")
            .document(deviceId)
            .delete()
            .await()
    }

    fun observeUsageLogs(limitDays: Int = 7): Flow<List<UsageLog>> = callbackFlow {
        val registration: ListenerRegistration = usageLogsRef
            .whereEqualTo("userId", currentUid)
            .limit(200)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeUsageLogs error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(UsageLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreRepo", "Error parsing log ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { registration.remove() }
    }

    fun observeDeviceUsageLogs(deviceId: String): Flow<List<UsageLog>> = callbackFlow {
        val registration: ListenerRegistration = usageLogsRef
            .whereEqualTo("deviceId", deviceId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeDeviceUsageLogs error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(UsageLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("FirestoreRepo", "Error parsing log ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { registration.remove() }
    }

    private suspend fun logUsage(device: Device, floorPlanId: String, event: String) {
        try {
            val log = UsageLog(
                userId = currentUid,
                deviceId = device.id,
                deviceName = device.name,
                floorPlanId = floorPlanId,
                event = event,
                timestamp = Timestamp.now()
            )
            usageLogsRef.add(log).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Failed to write usage log", e)
        }
    }
}
