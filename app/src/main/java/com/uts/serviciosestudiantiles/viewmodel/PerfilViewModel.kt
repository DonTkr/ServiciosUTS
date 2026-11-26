package com.uts.serviciosestudiantiles.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uts.serviciosestudiantiles.data.Facultad
import com.uts.serviciosestudiantiles.data.PerfilEstudiante
import com.uts.serviciosestudiantiles.data.ServiciosRepository
import com.uts.serviciosestudiantiles.data.inicialesDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class PerfilUiState(
    val nombre: String = "",
    val codigo: String = "",
    val facultad: Facultad? = null,
    val menuFacultadAbierto: Boolean = false,
    val errorNombre: String? = null,
    val errorCodigo: String? = null,
    val errorFacultad: String? = null,
    val perfilGuardado: PerfilEstudiante? = null,
    val mensaje: String? = null,
    /** Vista previa en vivo del carné mientras el estudiante escribe. */
    val correoVistaPrevia: String? = null,
    val inicialesVistaPrevia: String = ""
) {
    /** Hay datos escritos que todavía no coinciden con el perfil guardado. */
    val hayCambiosSinGuardar: Boolean
        get() = perfilGuardado != null && (
            nombre.trim() != perfilGuardado.nombre ||
                codigo != perfilGuardado.codigo ||
                facultad != perfilGuardado.facultad
            )
}

class PerfilViewModel(
    private val repositorio: ServiciosRepository = ServiciosRepository.instancia
) : ViewModel() {

    private val _uiState = MutableStateFlow(estadoInicial())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    val facultades: List<Facultad> = Facultad.entries

    // Los errores se muestran después del primer intento de guardar y luego se actualizan en vivo.
    private var validarEnVivo = false

    init {
        // Cuando Matrícula termina el registro, el repositorio deja el perfil en null:
        // los campos se limpian para registrar a otro estudiante.
        viewModelScope.launch {
            repositorio.perfil.collect { perfil ->
                val anterior = _uiState.value.perfilGuardado
                if (perfil == null && anterior != null) {
                    validarEnVivo = false
                    _uiState.value = PerfilUiState(
                        mensaje = "Registro de ${anterior.nombre} terminado. Ya puedes registrar a otro estudiante."
                    )
                }
            }
        }
    }

    fun onNombreChange(valor: String) {
        _uiState.update { conVistaPrevia(validarSiAplica(it.copy(nombre = valor.take(MAX_NOMBRE)))) }
    }

    fun onCodigoChange(valor: String) {
        // Regla: el código estudiantil solo admite dígitos.
        val soloDigitos = valor.filter { it.isDigit() }.take(MAX_CODIGO)
        _uiState.update { conVistaPrevia(validarSiAplica(it.copy(codigo = soloDigitos))) }
    }

    fun toggleMenuFacultad(abierto: Boolean) {
        _uiState.update { it.copy(menuFacultadAbierto = abierto) }
    }

    fun seleccionarFacultad(facultad: Facultad) {
        _uiState.update {
            validarSiAplica(it.copy(facultad = facultad, menuFacultadAbierto = false))
        }
    }

    fun guardarPerfil() {
        validarEnVivo = true
        val validado = validar(_uiState.value)
        if (validado.errorNombre != null || validado.errorCodigo != null || validado.errorFacultad != null) {
            _uiState.value = validado
            return
        }

        // Regla: el nombre se guarda en formato título ("ana  ruiz" -> "Ana Ruiz").
        val nombreLimpio = validado.nombre.trim().split(ESPACIOS).joinToString(" ") { palabra ->
            palabra.lowercase().replaceFirstChar { it.titlecase() }
        }
        val facultad = requireNotNull(validado.facultad)

        // Regla: un código del historial no puede volver a registrarse con otro nombre o facultad.
        repositorio.perfilRegistrado(validado.codigo)?.let { previo ->
            if (previo.nombre != nombreLimpio || previo.facultad != facultad) {
                _uiState.value = validado.copy(
                    errorCodigo = "Este código ya está registrado a nombre de ${previo.nombre} " +
                        "(${previo.facultad.nombreVisible})."
                )
                return
            }
        }

        val perfil = PerfilEstudiante(
            nombre = nombreLimpio,
            codigo = validado.codigo,
            facultad = facultad,
            correoInstitucional = generarCorreo(nombreLimpio, validado.codigo)
        )
        repositorio.guardarPerfil(perfil)
        _uiState.value = conVistaPrevia(validado.copy(nombre = nombreLimpio)).copy(
            perfilGuardado = perfil,
            mensaje = "Perfil guardado. Ya puedes liquidar cursos."
        )
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun estadoInicial(): PerfilUiState {
        val guardado = repositorio.perfil.value ?: return PerfilUiState()
        return conVistaPrevia(
            PerfilUiState(
                nombre = guardado.nombre,
                codigo = guardado.codigo,
                facultad = guardado.facultad,
                perfilGuardado = guardado
            )
        )
    }

    /** Regla de negocio aplicada en vivo: el correo se arma igual que al guardar. */
    private fun conVistaPrevia(estado: PerfilUiState): PerfilUiState {
        val tieneLetras = estado.nombre.any { it.isLetter() }
        return estado.copy(
            correoVistaPrevia = if (tieneLetras && estado.codigo.isNotEmpty()) {
                generarCorreo(estado.nombre, estado.codigo)
            } else {
                null
            },
            inicialesVistaPrevia = inicialesDe(estado.nombre.filter { it.isLetter() || it == ' ' })
        )
    }

    private fun validarSiAplica(estado: PerfilUiState): PerfilUiState =
        if (validarEnVivo) validar(estado) else estado

    private fun validar(estado: PerfilUiState): PerfilUiState {
        val nombre = estado.nombre.trim()
        val errorNombre = when {
            nombre.isEmpty() -> "Escribe tu nombre completo."
            nombre.length < MIN_NOMBRE -> "El nombre debe tener al menos $MIN_NOMBRE letras."
            !nombre.matches(SOLO_LETRAS) -> "Usa solo letras y espacios."
            else -> null
        }
        val errorCodigo = when {
            estado.codigo.isEmpty() -> "Escribe tu código estudiantil."
            estado.codigo.length < MIN_CODIGO -> "El código debe tener entre $MIN_CODIGO y $MAX_CODIGO dígitos."
            else -> null
        }
        val errorFacultad = if (estado.facultad == null) "Selecciona tu facultad." else null
        return estado.copy(
            errorNombre = errorNombre,
            errorCodigo = errorCodigo,
            errorFacultad = errorFacultad
        )
    }

    companion object {
        const val MIN_NOMBRE = 3
        const val MAX_NOMBRE = 60
        const val MIN_CODIGO = 6
        const val MAX_CODIGO = 12

        // Se compilan una sola vez: estas reglas se evalúan con cada tecla.
        private val ESPACIOS = Regex("\\s+")
        private val SOLO_LETRAS = Regex("^[\\p{L} ]+$")
        private val TILDES = Regex("\\p{Mn}+")

        /**
         * Regla de negocio: correo simulado nombre.codigo@uts.edu.co
         * Usa el primer nombre en minúsculas, sin tildes ni eñes ("Iván" -> "ivan", "Ñusta" -> "nusta").
         */
        fun generarCorreo(nombreCompleto: String, codigo: String): String {
            val primerNombre = nombreCompleto.trim().split(ESPACIOS).first()
            val sinTildes = Normalizer.normalize(primerNombre, Normalizer.Form.NFD)
                .replace(TILDES, "")
                .lowercase()
                .filter { it in 'a'..'z' }
            return "$sinTildes.$codigo@uts.edu.co"
        }
    }
}
