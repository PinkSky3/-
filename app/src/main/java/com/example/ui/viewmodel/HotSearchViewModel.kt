package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FallbackFetchResult
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.model.HotPlatform
import com.example.data.model.HotSearchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState {
    object Loading : UiState
    data class Success(
        val items: List<HotSearchItem>,
        val updateTime: String?
    ) : UiState
    data class Error(val message: String) : UiState
}

class HotSearchViewModel : ViewModel() {

    private val _activePlatform = MutableStateFlow(HotPlatform.WEIBO)
    val activePlatform: StateFlow<HotPlatform> = _activePlatform.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var rawItems: List<HotSearchItem> = emptyList()
    private var fetchedTime: String? = null

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchHotSearch(HotPlatform.WEIBO)
    }

    fun selectPlatform(platform: HotPlatform) {
        if (_activePlatform.value == platform && _uiState.value !is UiState.Error && rawItems.isNotEmpty()) {
            return
        }
        _activePlatform.value = platform
        _searchQuery.value = ""
        fetchHotSearch(platform)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refreshActivePlatform() {
        fetchHotSearch(_activePlatform.value)
    }

    private fun fetchHotSearch(platform: HotPlatform) {
        fetchJob?.cancel()
        _uiState.value = UiState.Loading
        rawItems = emptyList()

        val endpoints = getFallbackEndpoints(platform)

        fetchJob = viewModelScope.launch {
            when (val result = RetrofitClient.publicApi.fetchFirstParsed(
                urls = endpoints,
                parse = { _, body -> extractListFromJson(body).takeIf { it.isNotEmpty() } }
            )) {
                is FallbackFetchResult.Success -> {
                    rawItems = result.value
                    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    fetchedTime = "$formattedTime | 源: ${getHostName(result.url)}"
                    applyFilter()
                }
                is FallbackFetchResult.Failure -> {
                    val message = result.errors.joinToString("\n") {
                        "节点(${getHostName(it.url)})失败: ${it.reason}"
                    }.ifBlank { "数据获取失败，所有节点均不可用。" }
                    _uiState.value = UiState.Error(message)
                }
            }
        }
    }

    private fun getFallbackEndpoints(platform: HotPlatform): List<String> {
        val list = mutableListOf<String>()
        val dailyHotMirrors = listOf(
            "https://dailyhotapi.3yu3.top",
            "https://dailyhot.api.lkwplus.com"
        )

        when (platform) {
            HotPlatform.WEIBO -> {
                list.add("https://cn.apihz.cn/api/xinwen/weibo.php?id=88888888&key=88888888")
                list.add("https://www.haotechs.cn/ljh-wx/api/weiboHot")
                list.add("https://api.xunjinlu.fun/api/rebang/weibo.php")
            }
            HotPlatform.ZHIHU -> {
                list.add("https://v.api.aa1.cn/api/zhihu-news/index.php?aa1=xiarou")
            }
            HotPlatform.BAIDU -> {
                list.add("https://api.xma.run/api/tools/bdhot/?type=game")
                list.add("https://v.api.aa1.cn/api/sougou-baidu/index.php?aa1=xiarou")
            }
            HotPlatform.NGABBS -> {
                list.add("https://cn.apihz.cn/api/xinwen/nga.php?id=88888888&key=88888888")
            }
            else -> {}
        }

        for (mirror in dailyHotMirrors) {
            list.add("$mirror/${platform.key}")
        }
        return list
    }

    private fun extractListFromJson(jsonString: String): List<HotSearchItem> {
        val items = mutableListOf<HotSearchItem>()
        try {
            val rootStr = jsonString.trim()
            if (rootStr.startsWith("[")) {
                val jsonArray = JSONArray(rootStr)
                items.addAll(parseJsonArray(jsonArray))
            } else if (rootStr.startsWith("{")) {
                val rootObj = JSONObject(rootStr)
                val array = findDeepestArray(rootObj)
                if (array != null) {
                    items.addAll(parseJsonArray(array))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: StackOverflowError) {
            System.err.println("JSON too deeply nested, skipping")
        }
        return items
    }

    private fun findDeepestArray(obj: JSONObject, depth: Int = 0): JSONArray? {
        if (depth > 20) return null
        val knownKeys = listOf("data", "list", "result", "news", "hotList", "hot", "items", "routes")
        for (key in knownKeys) {
            if (obj.has(key)) {
                val value = obj.get(key)
                if (value is JSONArray && value.length() > 0) return value
            }
        }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            if (value is JSONArray && value.length() > 0) return value
            if (value is JSONObject) {
                val inner = findDeepestArray(value, depth + 1)
                if (inner != null) return inner
            }
        }
        return null
    }

    private fun parseJsonArray(array: JSONArray): List<HotSearchItem> {
        val list = mutableListOf<HotSearchItem>()
        for (i in 0 until array.length()) {
            val element = array.get(i)
            if (element is JSONObject) {
                val title = extractStringExt(element, listOf("title", "name", "keyword", "word", "hotword", "text", "content", "topic_name"))
                if (title.isNullOrBlank()) continue
                val url = extractStringExt(element, listOf("url", "link", "mobileUrl", "href", "short_url"))
                val desc = extractStringExt(element, listOf("desc", "description", "summary", "detail", "note"))
                val hotStr = extractStringExt(element, listOf("hot", "score", "index", "hotValue", "num", "hot_score", "search_volume"))
                list.add(HotSearchItem(title = title, url = url, desc = desc, hot = hotStr))
            } else if (element is String) {
                list.add(HotSearchItem(title = element, url = null))
            }
        }
        return list
    }

    private fun extractStringExt(obj: JSONObject, candidates: List<String>): String? {
        for (candidate in candidates) {
            if (obj.has(candidate) && !obj.isNull(candidate)) {
                return obj.get(candidate).toString()
            }
        }
        return null
    }

    private fun getHostName(url: String): String {
        return try {
            URL(url).host
        } catch (e: Exception) {
            url
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim()
        val currentRaw = rawItems
        val filtered = if (query.isEmpty()) {
            currentRaw
        } else {
            currentRaw.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                    (item.desc?.contains(query, ignoreCase = true) ?: false)
            }
        }

        _uiState.value = UiState.Success(
            items = filtered,
            updateTime = fetchedTime
        )
    }
}
