package com.uts.serviciosestudiantiles.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uts.serviciosestudiantiles.data.Liquidacion
import com.uts.serviciosestudiantiles.data.PerfilEstudiante
import com.uts.serviciosestudiantiles.data.ServiciosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Todo lo que liquidó un estudiante: una fila del historial y el contenido de su detalle. */
data class RegistroEstudiante(
    val estudiante: PerfilEstudiante,
    /** Más recientes primero. */
    val liquidaciones: List<Liquidacion>,
    val total: Long,
    val ahorroPorBecas: Long,
    val recargosIcetex: Long
)

data class HistorialUiState(
    /** Todas las liquidaciones, más recientes primero. */
    val liquidaciones: List<Liquidacion> = emptyList(),
    /** Un registro por estudiante; primero el que liquidó más recientemente. */
    val estudiantes: List<RegistroEstudiante> = emptyList(),
    val totalAcumulado: Long = 0,
    val ahorroPorBecas: Long = 0,
    val recargosIcetex: Long = 0,
    /** Código del estudiante cuyo detalle está abierto. */
    val codigoEnDetalle: String? = null,
    /** Liquidación que espera confirmación para anularse (abre el diálogo). */
    val liquidacionPorAnular: Liquidacion? = null,
    val mensaje: String? = null
) {
    val detalle: RegistroEstudiante?
        get() = codigoEnDetalle?.let { codigo -> estudiantes.firstOrNull { it.estudiante.codigo == codigo } }
}

class HistorialViewModel(
    private val repositorio: ServiciosRepository = ServiciosRepository.instancia
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        // Cálculo global en tiempo real: cada liquidación nueva o anulada recalcula los totales.
        viewModelScope.launch {
            repositorio.liquidaciones.collect { lista ->
                val recientes = lista.asReversed().toList()
                val estudiantes = agruparPorEstudiante(recientes)
                _uiState.update {
                    it.copy(
                        liquidaciones = recientes,
                        estudiantes = estudiantes,
                        totalAcumulado = lista.sumOf { l -> l.total },
                        ahorroPorBecas = lista.sumOf { l -> l.descuentoBeca },
                        recargosIcetex = lista.sumOf { l -> l.recargoIcetex },
                        // Si se anuló el último curso del estudiante abierto, se vuelve a la lista.
                        codigoEnDetalle = it.codigoEnDetalle?.takeIf { codigo ->
                            estudiantes.any { r -> r.estudiante.codigo == codigo }
                        }
                    )
                }
            }
        }
    }

    fun abrirDetalle(codigo: String) {
        _uiState.update { it.copy(codigoEnDetalle = codigo) }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(codigoEnDetalle = null) }
    }

    fun solicitarAnulacion(liquidacion: Liquidacion) {
        _uiState.update { it.copy(liquidacionPorAnular = liquidacion) }
    }

    fun cancelarAnulacion() {
        _uiState.update { it.copy(liquidacionPorAnular = null) }
    }

    fun confirmarAnulacion() {
        val liquidacion = _uiState.value.liquidacionPorAnular ?: return
        repositorio.eliminarLiquidacion(liquidacion.numero)
        _uiState.update {
            it.copy(
                liquidacionPorAnular = null,
                mensaje = "Liquidación N.º ${liquidacion.numero} anulada."
            )
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    companion object {
        /** Agrupa por código conservando el orden: el estudiante con la liquidación más reciente va primero. */
        fun agruparPorEstudiante(recientes: List<Liquidacion>): List<RegistroEstudiante> =
            recientes.groupBy { it.estudiante.codigo }.values.map { delEstudiante ->
                RegistroEstudiante(
                    estudiante = delEstudiante.first().estudiante,
                    liquidaciones = delEstudiante,
                    total = delEstudiante.sumOf { it.total },
                    ahorroPorBecas = delEstudiante.sumOf { it.descuentoBeca },
                    recargosIcetex = delEstudiante.sumOf { it.recargoIcetex }
                )
            }
    }
}
