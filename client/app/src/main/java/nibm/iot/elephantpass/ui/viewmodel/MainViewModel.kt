package nibm.iot.elephantpass.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nibm.iot.elephantpass.data.models.AlertItem
import nibm.iot.elephantpass.data.models.LiveStatus
import nibm.iot.elephantpass.data.repository.ElephantRepository

data class MainUiState(
    val liveStatus: LiveStatus = LiveStatus(),
    val alerts: List<AlertItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedFilter: String = "ALL", // ALL, CRITICAL, NIGHT, TODAY
    val selectedNavTab: Int = 0, // 0: Watch, 1: Map, 2: History, 3: Safety
    val mapTargetLat: Double = 7.9582,
    val mapTargetLng: Double = 80.7621,
    val mapFocusNonce: Int = 0
) {
    val filteredAlerts: List<AlertItem>
        get() {
            var result = alerts
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                result = result.filter { item ->
                    item.zone.lowercase().contains(q) ||
                    item.severity.lowercase().contains(q) ||
                    item.timestamp.lowercase().contains(q) ||
                    item.lighting.lowercase().contains(q)
                }
            }
            return when (selectedFilter) {
                "CRITICAL" -> result.filter { it.isCritical }
                "NIGHT" -> result.filter { it.isNight }
                "TODAY" -> result.filter { it.timestamp.isNotBlank() }
                else -> result
            }
        }
}

class MainViewModel(
    private val repository: ElephantRepository = ElephantRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        startTelemetryPolling()
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            repository.pollLiveStatus(intervalMillis = 3000L)
                .distinctUntilChanged()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { result ->
                    result.onSuccess { status ->
                        _uiState.update { state ->
                            if (state.liveStatus == status && !state.isLoading) state
                            else state.copy(
                                liveStatus = status,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }.onFailure { err ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = err.message
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            repository.pollAlerts(intervalMillis = 3000L)
                .distinctUntilChanged()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { result ->
                    result.onSuccess { alertList ->
                        _uiState.update { state ->
                            if (state.alerts == alertList && !state.isLoading) state
                            else state.copy(
                                alerts = alertList,
                                isLoading = false
                            )
                        }
                    }.onFailure { err ->
                        _uiState.update { state ->
                            state.copy(errorMessage = err.message)
                        }
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun selectNavTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedNavTab = tabIndex) }
    }

    fun focusOnCoordinates(lat: Double, lng: Double, navigateToMapTab: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                mapTargetLat = lat,
                mapTargetLng = lng,
                mapFocusNonce = state.mapFocusNonce + 1,
                selectedNavTab = if (navigateToMapTab) 1 else state.selectedNavTab
            )
        }
    }

    fun recenterCorridor() {
        focusOnCoordinates(7.9582, 80.7621, navigateToMapTab = false)
    }

    fun manualRefresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val liveRes = repository.fetchLiveStatus()
            val alertsRes = repository.fetchAlerts()

            _uiState.update { state ->
                state.copy(
                    liveStatus = liveRes.getOrDefault(state.liveStatus),
                    alerts = alertsRes.getOrDefault(state.alerts),
                    isLoading = false,
                    errorMessage = liveRes.exceptionOrNull()?.message ?: alertsRes.exceptionOrNull()?.message
                )
            }
        }
    }
}
