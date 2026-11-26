package com.uts.serviciosestudiantiles.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uts.serviciosestudiantiles.data.Curso
import com.uts.serviciosestudiantiles.data.Liquidacion
import com.uts.serviciosestudiantiles.data.PerfilEstudiante
import com.uts.serviciosestudiantiles.data.ServiciosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Desglose del costo que se muestra en vivo antes de liquidar. */
data class ResumenCosto(
    val costoBase: Long,
    val descuentoBeca: Long,
    val recargoIcetex: Long,
    val total: Long
)

data class MatriculaUiState(
    val cursoSeleccionado: Curso? = null,
    val menuCursosAbierto: Boolean = false,
    val tieneBeca: Boolean = false,
    val usaIcetex: Boolean = false,
    val perfil: PerfilEstudiante? = null,
    /** Cursos de la facultad del estudiante; sin perfil se muestran todos para explorar. */
    val cursosDisponibles: List<Curso> = Curso.entries,
    val resumen: ResumenCosto? = null,
    val mensaje: String? = null,
    /** Liquidaciones vigentes del estudiante actual. */
    val liquidacionesEstudiante: List<Liquidacion> = emptyList()
) {
    /** Bloqueo de seguridad: el Módulo 1 debe estar completo. */
    val perfilCompleto: Boolean get() = perfil != null
    val cursosLiquidados: Set<Curso> get() = liquidacionesEstudiante.map { it.curso }.toSet()
    val cursoYaLiquidado: Boolean get() = cursoSeleccionado in cursosLiquidados
    val totalEstudiante: Long get() = liquidacionesEstudiante.sumOf { it.total }
    val puedeLiquidar: Boolean
        get() = perfilCompleto && cursoSeleccionado != null && !cursoYaLiquidado
    /** Solo se cierra un registro que tenga al menos un curso liquidado. */
    val puedeTerminar: Boolean get() = perfilCompleto && liquidacionesEstudiante.isNotEmpty()
    val motivoBloqueo: String?
        get() = when {
            !perfilCompleto -> "Completa tu perfil académico para habilitar la liquidación."
            cursoSeleccionado == null -> "Elige un curso para ver su liquidación."
            cursoYaLiquidado -> "Ya liquidaste este curso. Puedes anularlo desde el historial."
            else -> null
        }
}

class MatriculaViewModel(
    private val repositorio: ServiciosRepository = ServiciosRepository.instancia
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatriculaUiState())
    val uiState: StateFlow<MatriculaUiState> = _uiState.asStateFlow()

    init {
        // Escucha el perfil (Módulo 1) y el historial (Módulo 3) a la vez:
        // - el botón "Liquidar" se habilita en cuanto se guarde el perfil;
        // - los cursos se filtran por la facultad del estudiante;
        // - si un curso suyo se liquida o se anula, el bloqueo de duplicado se actualiza solo.
        viewModelScope.launch {
            combine(repositorio.perfil, repositorio.liquidaciones) { perfil, lista -> perfil to lista }
                .collect { (perfil, lista) ->
                    _uiState.update { actual -> conEstudiante(actual, perfil, lista) }
                }
        }
    }

    fun toggleMenuCursos(abierto: Boolean) {
        _uiState.update { it.copy(menuCursosAbierto = abierto) }
    }

    fun seleccionarCurso(curso: Curso) {
        _uiState.update { recalcular(it.copy(cursoSeleccionado = curso, menuCursosAbierto = false)) }
    }

    fun cambiarBeca(valor: Boolean) {
        _uiState.update { recalcular(it.copy(tieneBeca = valor)) }
    }

    fun cambiarIcetex(valor: Boolean) {
        _uiState.update { recalcular(it.copy(usaIcetex = valor)) }
    }

    fun liquidar() {
        val estado = _uiState.value
        // La regla se valida aquí también, no solo deshabilitando el botón en la vista.
        val perfil = estado.perfil ?: return
        val curso = estado.cursoSeleccionado ?: return
        if (estado.cursoYaLiquidado || curso.facultad != perfil.facultad) return
        val resumen = calcularCosto(curso, estado.tieneBeca, estado.usaIcetex)

        val numero = repositorio.siguienteNumeroLiquidacion()
        repositorio.agregarLiquidacion(
            Liquidacion(
                numero = numero,
                estudiante = perfil,
                curso = curso,
                tieneBeca = estado.tieneBeca,
                usaIcetex = estado.usaIcetex,
                costoBase = resumen.costoBase,
                descuentoBeca = resumen.descuentoBeca,
                recargoIcetex = resumen.recargoIcetex,
                total = resumen.total
            )
        )
        _uiState.update {
            // Se limpia el formulario para la siguiente liquidación del mismo estudiante.
            it.copy(
                cursoSeleccionado = null,
                tieneBeca = false,
                usaIcetex = false,
                resumen = null,
                mensaje = "Liquidación N.º $numero de ${curso.nombreVisible} agregada al historial."
            )
        }
    }

    /**
     * Cierra el registro del estudiante: sus liquidaciones quedan en el historial
     * y el perfil se limpia para registrar a otro estudiante.
     */
    fun terminarRegistro() {
        if (!_uiState.value.puedeTerminar) return
        repositorio.terminarRegistro()
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun conEstudiante(
        estado: MatriculaUiState,
        perfil: PerfilEstudiante?,
        lista: List<Liquidacion>
    ): MatriculaUiState {
        val disponibles = perfil?.let { Curso.deFacultad(it.facultad) } ?: Curso.entries
        // Había un estudiante y ahora hay otro (o ninguno, tras terminar su registro).
        val cambioEstudiante = estado.perfil != null && perfil?.codigo != estado.perfil.codigo
        // Al cambiar de estudiante, o si el curso elegido no es de su facultad, el formulario vuelve a empezar.
        val reiniciar = cambioEstudiante || estado.cursoSeleccionado?.let { it !in disponibles } == true
        val base = if (reiniciar) {
            estado.copy(cursoSeleccionado = null, tieneBeca = false, usaIcetex = false, resumen = null)
        } else {
            estado
        }
        return base.copy(
            perfil = perfil,
            cursosDisponibles = disponibles,
            liquidacionesEstudiante = perfil?.let { p -> lista.filter { it.estudiante.codigo == p.codigo } }
                ?: emptyList()
        )
    }

    private fun recalcular(estado: MatriculaUiState): MatriculaUiState =
        estado.copy(
            resumen = estado.cursoSeleccionado?.let {
                calcularCosto(it, estado.tieneBeca, estado.usaIcetex)
            }
        )

    companion object {
        const val PORCENTAJE_BECA = 50
        const val PORCENTAJE_ICETEX = 5

        /**
         * Regla de negocio:
         * 1. La Beca de Excelencia descuenta el 50 % del costo base.
         * 2. El Icetex suma un 5 % de costos administrativos sobre el valor ya descontado.
         */
        fun calcularCosto(curso: Curso, tieneBeca: Boolean, usaIcetex: Boolean): ResumenCosto {
            val base = curso.costoBase
            val descuento = if (tieneBeca) base * PORCENTAJE_BECA / 100 else 0L
            val subtotal = base - descuento
            val recargo = if (usaIcetex) subtotal * PORCENTAJE_ICETEX / 100 else 0L
            return ResumenCosto(
                costoBase = base,
                descuentoBeca = descuento,
                recargoIcetex = recargo,
                total = subtotal + recargo
            )
        }
    }
}
