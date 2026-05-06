package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import android.util.Log
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
        val remindUntil = prefs.getLong("remind_later_until", 0L)
        val now = System.currentTimeMillis()

        // Respeita o "lembrar depois" (usuário pediu para não ver agora)
        if (now < remindUntil) return

        viewModelScope.launch {
            updateRepository.fetchAppVersion(sheetUrl)
                .onSuccess { info ->
                    if (isNewerVersion(info.version, currentVersion)) {
                        _updateInfo.value = info
                        _showOverlay.value = true
                    }
                }
                .onFailure { throwable ->
                    // Loga o erro para depuração (visível no Logcat ou Termux)
                    Log.e("UpdateViewModel", "Falha ao verificar atualização", throwable)
                }
        }
    }

    fun dismissUpdate() {
        _showOverlay.value = false
    }

    fun remindLater() {
        val prefs = context.getSharedPreferences("update", Context.MODE_PRIVATE)
        // Lembrar novamente em 2 horas
        prefs.edit().putLong("remind_later_until", System.currentTimeMillis() + 2 * 60 * 60 * 1000).apply()
        _showOverlay.value = false
    }
}
