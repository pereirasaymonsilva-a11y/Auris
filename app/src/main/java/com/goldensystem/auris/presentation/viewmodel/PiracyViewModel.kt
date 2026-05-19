package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PiracyViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PiracyUiState>(PiracyUiState.Valid)
    val uiState: StateFlow<PiracyUiState> = _uiState.asStateFlow()

    fun checkPackageIntegrity(scriptUrl: String) {
        viewModelScope.launch {
            updateRepository.fetchAppVersion(scriptUrl).fold(
                onSuccess = { info ->
                    val expectedPackage = info.originalPackage
                    if (expectedPackage != null && context.packageName != expectedPackage) {
                        Log.e("PiracyViewModel", "Package mismatch! Current: ${context.packageName}, Expected: $expectedPackage")
                        _uiState.value = PiracyUiState.Mismatch(
                            downloadUrl = info.downloadUrl,
                            officialPackage = expectedPackage
                        )
                    } else {
                        Log.d("PiracyViewModel", "Package name OK")
                        _uiState.value = PiracyUiState.Valid
                    }
                },
                onFailure = { error ->
                    Log.e("PiracyViewModel", "Failed to fetch package info", error)
                    // Se falhar, consideramos como válido para não bloquear o usuário? Ou mostramos erro?
                    // Vou optar por considerar válido (evita falsos positivos)
                    _uiState.value = PiracyUiState.Valid
                }
            )
        }
    }

    fun reset() {
    }
}

sealed class PiracyUiState {
    object Valid : PiracyUiState()
    data class Mismatch(val downloadUrl: String, val officialPackage: String) : PiracyUiState()
}