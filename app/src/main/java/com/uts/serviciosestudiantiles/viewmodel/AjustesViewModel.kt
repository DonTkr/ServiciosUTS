package com.uts.serviciosestudiantiles.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uts.serviciosestudiantiles.data.ModoTema
import com.uts.serviciosestudiantiles.data.ServiciosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AjustesUiState(
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val menuTemaAbierto: Boolean = false
)

/** Apariencia de la app: modo claro, oscuro o según el teléfono (se guarda en la base de datos). */
class AjustesViewModel(
    private val repositorio: ServiciosRepository = ServiciosRepository.instancia
) : ViewModel() {

    private val _uiState = MutableStateFlow(AjustesUiState(modoTema = repositorio.modoTema.value))
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    val opcionesTema: List<ModoTema> = ModoTema.entries

    init {
        viewModelScope.launch {
            repositorio.modoTema.collect { modo -> _uiState.update { it.copy(modoTema = modo) } }
        }
    }

    fun toggleMenuTema(abierto: Boolean) {
        _uiState.update { it.copy(menuTemaAbierto = abierto) }
    }

    fun elegirModoTema(modo: ModoTema) {
        repositorio.cambiarModoTema(modo)
        _uiState.update { it.copy(menuTemaAbierto = false) }
    }
}
