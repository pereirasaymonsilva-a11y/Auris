package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.model.AppVersionInfo
import com.goldensystem.auris.data.repository.UpdateRepository
import com.goldensystem.auris.utils.isNewerVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<AppVersionInfo?>(null)
    val updateInfo: StateFlow<AppVersionInfo?> = _updateInfo.asStateFlow()

    private val _showOverlay = MutableStateFlow(false)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    fun checkForUpdate(sheetUrl: String, currentVersion: String) {
        val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check", 0L)
        val remindUntil = prefs.getLong("remind_later_until", 0L)
        val now = System.currentTimeMillis()

        if (now < remindUntil) return
        if (now - lastCheck < 6 * 60 * 60 * 1000) return

        viewModelScope.launch {
            updateRepository.fetchAppVersion(sheetUrl)
                .onSuccess { info ->
                    prefs.edit().putLong("last_check", now).apply()
                    if (isNewerVersion(info.version, currentVersion)) {
                        _updateInfo.value = info
                        _showOverlay.value = true
                    }
                }
        }
    }

    fun dismissUpdate() {
        _showOverlay.value = false
    }

    fun remindLater() {
        val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)
        prefs.edit().putLong("remind_later_until", System.currentTimeMillis() + 2 * 60 * 60 * 1000).apply()
        _showOverlay.value = false
    }
}