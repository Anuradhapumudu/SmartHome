package com.example.smarthome.ui.floorplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.Device
import com.example.smarthome.data.model.DeviceStatus
import com.example.smarthome.data.model.DeviceType
import com.example.smarthome.data.model.FloorPlan
import com.example.smarthome.data.model.SwitchState
import com.example.smarthome.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloorPlanGridViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = FirestoreRepository()
    val floorPlanId: String = checkNotNull(savedStateHandle["floorPlanId"])

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _floorPlan = MutableStateFlow<FloorPlan?>(null)
    val floorPlan: StateFlow<FloorPlan?> = _floorPlan.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe devices for this floor plan
            repository.observeDevices(floorPlanId).collect { deviceList ->
                _devices.value = deviceList
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            // Load floor plan info once
            repository.observeFloorPlans().collect { plans ->
                _floorPlan.value = plans.find { it.id == floorPlanId }
            }
        }
    }

    fun toggleDevice(device: Device) {
        viewModelScope.launch {
            repository.toggleDeviceStatus(floorPlanId, device)
        }
    }

    fun toggleSwitch(device: Device, switchIndex: Int) {
        viewModelScope.launch {
            val updatedSwitches = device.switches.map { sw ->
                if (sw.switchIndex == switchIndex) {
                    val newStatus = if (sw.status == DeviceStatus.ON.name)
                        DeviceStatus.OFF.name else DeviceStatus.ON.name
                    sw.copy(status = newStatus)
                } else sw
            }
            repository.updateMultiSwitch(floorPlanId, device.id, updatedSwitches)
        }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch {
            repository.addDevice(floorPlanId, device)
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            repository.deleteDevice(floorPlanId, deviceId)
        }
    }

    /** Builds an initial list of SwitchState for a new multi-switch device */
    fun buildDefaultSwitches(count: Int): List<SwitchState> =
        (0 until count).map { i ->
            SwitchState(
                switchIndex = i,
                label = "Switch ${i + 1}",
                status = DeviceStatus.OFF.name
            )
        }
}
