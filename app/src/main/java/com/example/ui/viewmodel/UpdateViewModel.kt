package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.AppUpdate
import com.example.data.update.UpdateCheckResult
import com.example.data.update.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository()
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _availableUpdate = MutableStateFlow<AppUpdate?>(null)
    val availableUpdate: StateFlow<AppUpdate?> = _availableUpdate.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var checkJob: Job? = null

    init {
        checkForUpdate(manual = false)
    }

    fun checkForUpdate() {
        checkForUpdate(manual = true)
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    fun ignoreUpdate() {
        val version = _availableUpdate.value?.version ?: return
        preferences.edit().putString(KEY_IGNORED_VERSION, version).apply()
        _availableUpdate.value = null
        _messages.tryEmit("已忽略版本 $version")
    }

    private fun checkForUpdate(manual: Boolean) {
        checkJob?.cancel()
        if (manual) _messages.tryEmit("正在检查更新")
        checkJob = viewModelScope.launch {
            when (val result = repository.check(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.Available -> {
                    val ignoredVersion = preferences.getString(KEY_IGNORED_VERSION, null)
                    if (manual || ignoredVersion != result.update.version) {
                        _availableUpdate.value = result.update
                    }
                }
                UpdateCheckResult.NoUpdate -> {
                    if (manual) _messages.emit("当前已是最新版本 ${BuildConfig.VERSION_NAME}")
                }
                UpdateCheckResult.Unavailable -> {
                    if (manual) _messages.emit("检查失败，请稍后重试")
                }
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "update_preferences"
        const val KEY_IGNORED_VERSION = "ignored_version"
    }
}
