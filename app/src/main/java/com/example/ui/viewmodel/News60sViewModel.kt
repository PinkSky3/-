package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FallbackFetchResult
import com.example.data.api.RetrofitClient
import com.example.data.api.fetchFirstParsed
import com.example.data.model.News60sRootResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface News60sUiState {
    object Loading : News60sUiState
    data class Success(
        val newsList: List<String>,
        val updateTime: String? = null
    ) : News60sUiState
    data class Error(val message: String) : News60sUiState
}

class News60sViewModel : ViewModel() {

    private val apiBases = listOf(
        "https://60s.viki.moe",
        "https://60s.crystelf.top",
        "https://api.yanyua.icu",
        "https://60s.7se.cn",
        "https://60s.superjeason.qzz.io"
    )

    private val _uiState = MutableStateFlow<News60sUiState>(News60sUiState.Loading)
    val uiState: StateFlow<News60sUiState> = _uiState.asStateFlow()

    init {
        fetchNews()
    }

    fun refresh() {
        fetchNews()
    }

    private fun fetchNews() {
        _uiState.value = News60sUiState.Loading
        viewModelScope.launch {
            when (val result = RetrofitClient.publicApi.fetchFirstParsed(
                urls = apiBases.map { "$it/v2/60s" },
                parse = { _, body -> parseResponse(body) }
            )) {
                is FallbackFetchResult.Success -> {
                    _uiState.value = News60sUiState.Success(
                        newsList = result.value.newsList,
                        updateTime = result.value.updateTime
                    )
                }
                is FallbackFetchResult.Failure -> {
                    val lastError = result.errors.lastOrNull()
                    val detail = lastError?.let { "\uFF1A${it.url} ${it.reason}" }.orEmpty()
                    _uiState.value = News60sUiState.Error(
                        "60S\u65B0\u95FB\u83B7\u53D6\u5931\u8D25\uFF0C\u5DF2\u5C1D\u8BD5\u591A\u4E2A\u516C\u5171\u5B9E\u4F8B$detail"
                    )
                }
            }
        }
    }

    private data class ParsedNews60s(
        val newsList: List<String>,
        val updateTime: String?
    )

    private fun parseResponse(body: String): ParsedNews60s? {
        return try {
            val adapter = RetrofitClient.moshi.adapter(News60sRootResponse::class.java)
            val root = adapter.fromJson(body)
            if (root?.code != 200) return null
            val data = root.data ?: return null
            val news = data.news
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            if (news.isEmpty()) null
            else ParsedNews60s(
                newsList = news,
                updateTime = data.updated ?: data.api_updated ?: data.date
            )
        } catch (_: Exception) {
            null
        }
    }
}
