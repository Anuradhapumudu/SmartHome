package com.example.smarthome.ui.floorplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.model.FloorPlan
import com.example.smarthome.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloorPlanViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    private val _floorPlans = MutableStateFlow<List<FloorPlan>>(emptyList())
    val floorPlans: StateFlow<List<FloorPlan>> = _floorPlans.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFloorPlans().collect { plans ->
                _floorPlans.value = plans
                _isLoading.value = false
            }
        }
    }

    fun addFloorPlan(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addFloorPlan(FloorPlan(name = name.trim()))
        }
    }

    fun deleteFloorPlan(floorPlanId: String) {
        viewModelScope.launch {
            repository.deleteFloorPlan(floorPlanId)
        }
    }
}
