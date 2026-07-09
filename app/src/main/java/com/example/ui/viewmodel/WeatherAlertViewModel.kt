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
        if (!force && lastSuccess != null && now - lastSuccessAtMillis < CACHE_TTL_MILLIS) {
            _uiState.value = WeatherAlertUiState.Success(
                snapshot = lastSuccess!!,
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
                        var remainingRequests = MAX_REQUESTS_PER_REFRESH - 1
                        val confirmedPrimary = if (body.needsConfirmation() && remainingRequests > 0) {
                            remainingRequests--
                            confirmWeather(body) ?: body
                        } else {
                            body
                        }
                        val regional = fetchRegionalWeather(confirmedPrimary, remainingRequests)
                        val snapshot = WeatherAlertSnapshot(confirmedPrimary, regional)
                        val fetchedLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        lastSuccess = snapshot
                        lastSuccessAtMillis = System.currentTimeMillis()
                        lastFetchedLabel = fetchedLabel
                        _uiState.value = WeatherAlertUiState.Success(snapshot, fetchedLabel)
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

    private suspend fun fetchRegionalWeather(primary: WeatherAlertResponse, requestBudget: Int): List<WeatherAlertResponse> {
        val probes = regionalProbeCities(primary)
        if (probes.isEmpty() || requestBudget <= 0) return emptyList()

        val result = mutableListOf<WeatherAlertResponse>()
        for (city in probes.take(minOf(MAX_REGIONAL_PROBES, requestBudget))) {
            try {
                val response = RetrofitClient.weatherApi.fetch(WEATHER_URL + "&city=" + city.encodeUrl())
                if (response.code() == 429) break
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    result.add(body)
                }
            } catch (_: Exception) {
                // Regional probes are a best-effort supplement to the IP-based primary query.
            }
        }
        return result
    }

    private suspend fun confirmWeather(weather: WeatherAlertResponse): WeatherAlertResponse? {
        val city = listOfNotNull(weather.city, weather.district, weather.province)
            .firstOrNull { it.isNotBlank() }
            ?: return null
        return try {
            val response = RetrofitClient.weatherApi.fetch(WEATHER_URL + "&city=" + city.encodeUrl())
            if (response.code() == 429) null else response.body()?.takeIf { response.isSuccessful }
        } catch (_: Exception) {
            null
        }
    }

    private fun regionalProbeCities(primary: WeatherAlertResponse): List<String> {
        val province = primary.province.orEmpty()
        val checkedNames = setOf(primary.city, primary.district, primary.province)
            .mapNotNull { it?.removeSuffix("市")?.removeSuffix("省") }
            .toSet()
        return PROVINCE_PROBE_CITIES.entries
            .firstOrNull { province.contains(it.key) }
            ?.value
            .orEmpty()
            .filterNot { checkedNames.contains(it.removeSuffix("市")) }
    }

    private fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")

    private fun WeatherAlertResponse.needsConfirmation(): Boolean {
        if (!alerts.isNullOrEmpty()) return false
        return forecast.orEmpty().any { day ->
            val text = listOfNotNull(day.weatherDay, day.weatherNight).joinToString(" ")
            val windScale = day.windScaleDay.orEmpty().filter { it.isDigit() }.toIntOrNull() ?: 0
            val precip = day.precip ?: 0.0
            precip >= 20.0 || windScale >= 6 || CONFIRMATION_RISK_KEYWORDS.any { text.contains(it) }
        }
    }

    companion object {
        private const val WEATHER_URL = "https://uapis.cn/api/v1/misc/weather?extended=true&forecast=true&hourly=true&minutely=true"
        private const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
        private const val MAX_REQUESTS_PER_REFRESH = 4
        private const val MAX_REGIONAL_PROBES = 3
        private val CONFIRMATION_RISK_KEYWORDS = listOf("暴雨", "大到暴雨", "中到大雨", "台风", "大风", "暴雪")

        private val PROVINCE_PROBE_CITIES = mapOf(
            "浙江" to listOf("宁波", "台州", "温州"),
            "福建" to listOf("福州", "厦门", "泉州"),
            "广东" to listOf("广州", "深圳", "汕头"),
            "海南" to listOf("海口", "三亚", "琼海"),
            "上海" to listOf("上海"),
            "江苏" to listOf("南京", "苏州", "南通"),
            "山东" to listOf("青岛", "烟台", "威海"),
            "台湾" to listOf("台北", "高雄", "花莲")
        )
    }
}
