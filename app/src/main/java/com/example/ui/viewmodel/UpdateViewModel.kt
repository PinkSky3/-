package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.AppUpdate
import com.example.data.update.UpdateCheckResult
import com.example.data.update.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(
    private val repository: UpdateRepository = UpdateRepository()
) : ViewModel() {
    private val _availableUpdate = MutableStateFlow<AppUpdate?>(null)
    val availableUpdate: StateFlow<AppUpdate?> = _availableUpdate.asStateFlow()

    init {
        checkForUpdate()
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val result = repository.check(BuildConfig.VERSION_NAME)
            if (result is UpdateCheckResult.Available) {
                _availableUpdate.value = result.update
            }
        }
    }
}
