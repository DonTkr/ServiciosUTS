package com.uts.serviciosestudiantiles.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fuente única de verdad de la app.
 *
 * Los tres ViewModels comparten la misma instancia, así:
 * - Matrícula sabe si el perfil del Módulo 1 ya está completo (bloqueo de seguridad).
 * - Historial se entera al instante de cada liquidación nueva o anulada (total en tiempo real).
 * - Perfil se limpia cuando Matrícula termina el registro del estudiante.
 *
 * Los StateFlow son lo que ve la interfaz; cada cambio se copia además en el [almacen]
 * (SQLite en la app), así los datos siguen ahí al cerrar y volver a abrir la app.
 */
class ServiciosRepository(private val almacen: AlmacenLocal = SinPersistencia) {

    private val guardados = almacen.cargar()

    private val _perfil = MutableStateFlow(guardados.perfilActivo)
    val perfil: StateFlow<PerfilEstudiante?> = _perfil.asStateFlow()

    private val _liquidaciones = MutableStateFlow(guardados.liquidaciones)
    val liquidaciones: StateFlow<List<Liquidacion>> = _liquidaciones.asStateFlow()

    // Consecutivo independiente del tamaño de la lista: al anular una liquidación
    // el siguiente número nunca se repite (la LazyColumn usa el número como key).
    private var ultimoNumero = guardados.ultimoNumero

    private val _modoTema = MutableStateFlow(guardados.modoTema)
    val modoTema: StateFlow<ModoTema> = _modoTema.asStateFlow()

    fun cambiarModoTema(modo: ModoTema) {
        _modoTema.value = modo
        almacen.guardarModoTema(modo)
    }

    /** Estudiante que está liquidando ahora (el del Módulo 1). */
    fun guardarPerfil(perfil: PerfilEstudiante) {
        _perfil.value = perfil
        almacen.guardarPerfilActivo(perfil)
    }

    /**
     * Cierra el registro del estudiante actual. Sus liquidaciones se quedan en el historial
     * y el perfil vuelve a estar vacío para registrar a otro estudiante.
     */
    fun terminarRegistro() {
        _perfil.value = null
        almacen.guardarPerfilActivo(null)
    }

    /** Perfil con el que un código ya tiene liquidaciones en el historial, si existe. */
    fun perfilRegistrado(codigo: String): PerfilEstudiante? =
        _liquidaciones.value.lastOrNull { it.estudiante.codigo == codigo }?.estudiante

    fun siguienteNumeroLiquidacion(): Int {
        ultimoNumero++
        almacen.guardarUltimoNumero(ultimoNumero)
        return ultimoNumero
    }

    fun agregarLiquidacion(liquidacion: Liquidacion) {
        // Nueva lista inmutable en cada cambio: así StateFlow detecta el cambio y emite.
        _liquidaciones.update { actuales -> actuales + liquidacion }
        almacen.insertarLiquidacion(liquidacion)
    }

    fun eliminarLiquidacion(numero: Int) {
        _liquidaciones.update { actuales -> actuales.filterNot { it.numero == numero } }
        almacen.eliminarLiquidacion(numero)
    }

    companion object {
        /**
         * Instancia compartida por toda la app (sin librería de inyección de dependencias).
         * La crea [com.uts.serviciosestudiantiles.ServiciosApp] con la base de datos SQLite.
         */
        lateinit var instancia: ServiciosRepository
    }
}
