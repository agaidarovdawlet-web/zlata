package com.example.zlata.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zlata.data.BusinessUiState
import com.example.zlata.data.EnergoDatabase
import com.example.zlata.data.EnergoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EnergoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EnergoRepository(EnergoDatabase.get(application))
    private val _uiState = MutableStateFlow(BusinessUiState())
    val uiState: StateFlow<BusinessUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        refresh(query)
    }

    fun loadMeterReadings(meterId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMeterReadings = repository.meterReadings(meterId)) }
        }
    }

    fun addReading(meterId: Long, date: String, value: Double, method: String) {
        viewModelScope.launch {
            repository.addReading(meterId, date, value, method)
            _uiState.update { it.copy(selectedMeterReadings = repository.meterReadings(meterId)) }
            refresh()
        }
    }

    fun addPayment(accountId: Long, date: String, amount: Double, method: String) {
        viewModelScope.launch {
            repository.addPayment(accountId, date, amount, method)
            refresh()
        }
    }

    private fun refresh(query: String = _uiState.value.query) {
        viewModelScope.launch {
            val state = repository.loadState(query.trim())
            _uiState.value = state.copy(
                query = query,
                selectedMeterReadings = _uiState.value.selectedMeterReadings
            )
        }
    }
}
