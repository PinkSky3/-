package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.data.model.WeatherAlertResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherAlertSnapshot(
    val primary: WeatherAlertResponse,
    val regional: List<WeatherAlertResponse> = emptyList()
)

sealed interface WeatherAlertUiState {
    object Loading : WeatherAlertUiState
    data class Success(
        val snapshot: WeatherAlertSnapshot,
        val fetchedTime: String,
        val fromCache: Boolean = false
    ) : WeatherAlertUiState
    data class Error(val message: String, val lastSuccess: WeatherAlertSnapshot? = null) : WeatherAlertUiState
}

class WeatherAlertViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<WeatherAlertUiState>(WeatherAlertUiState.Loading)
    val uiState: StateFlow<WeatherAlertUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null
    private var lastSuccess: WeatherAlertSnapshot? = null
    private var lastSuccessAtMillis: Long = 0L
    private var lastFetchedLabel: String = ""

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val cached = lastSuccess
        if (!force && cached != null && now - lastSuccessAtMillis < CACHE_TTL_MILLIS) {
            _uiState.value = WeatherAlertUiState.Success(
                snapshot = cached,
                fetchedTime = lastFetchedLabel,
                fromCache = true
            )
            return
        }

        fetchJob?.cancel()
        _uiState.value = WeatherAlertUiState.Loading

        fetchJob = viewModelScope.launch {
            try {
                val response = RetrofitClient.weatherApi.fetch(WEATHER_URL)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        publishSuccess(WeatherAlertSnapshot(body))
                    } else {
                        _uiState.value = WeatherAlertUiState.Error("天气预警接口返回为空", lastSuccess)
                    }
                } else {
                    val message = if (response.code() == 429) {
                        "天气预警接口访问过快，请稍后刷新"
                    } else {
                        "天气预警接口错误：${response.code()}"
                    }
                    _uiState.value = WeatherAlertUiState.Error(message, lastSuccess)
                }
            } catch (e: Exception) {
                _uiState.value = WeatherAlertUiState.Error(
                    e.localizedMessage ?: e.message ?: "天气预警获取失败",
                    lastSuccess
                )
            }
        }
    }

    fun queryCity(city: String) {
        val normalizedCity = city.trim()
        if (normalizedCity.isBlank()) return

        fetchJob?.cancel()
        val current = lastSuccess
        if (current == null) _uiState.value = WeatherAlertUiState.Loading

        fetchJob = viewModelScope.launch {
            try {
                val response = RetrofitClient.weatherApi.fetch(WEATHER_URL + "&city=" + normalizedCity.encodeUrl())
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val base = lastSuccess
                        val snapshot = if (base != null) {
                            base.copy(regional = listOf(body))
                        } else {
                            WeatherAlertSnapshot(body)
                        }
                        publishSuccess(snapshot)
                    } else {
                        _uiState.value = WeatherAlertUiState.Error("城市 $normalizedCity 天气预警返回为空", lastSuccess)
                    }
                } else {
                    val message = if (response.code() == 429) {
                        "天气预警接口访问过快，请稍后再查"
                    } else {
                        "城市 $normalizedCity 查询失败：${response.code()}"
                    }
                    _uiState.value = WeatherAlertUiState.Error(message, lastSuccess)
                }
            } catch (e: Exception) {
                _uiState.value = WeatherAlertUiState.Error(
                    e.localizedMessage ?: e.message ?: "城市 $normalizedCity 查询失败",
                    lastSuccess
                )
            }
        }
    }

    private fun publishSuccess(snapshot: WeatherAlertSnapshot) {
        val fetchedLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        lastSuccess = snapshot
        lastSuccessAtMillis = System.currentTimeMillis()
        lastFetchedLabel = fetchedLabel
        _uiState.value = WeatherAlertUiState.Success(snapshot, fetchedLabel)
    }

    private fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")

    companion object {
        private const val WEATHER_URL = "https://uapis.cn/api/v1/misc/weather?extended=true&forecast=true&minutely=true"
        private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
