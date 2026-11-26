package com.uts.serviciosestudiantiles.data

/** Lo que se recupera del almacenamiento al abrir la app. */
data class DatosGuardados(
    val perfilActivo: PerfilEstudiante? = null,
    val liquidaciones: List<Liquidacion> = emptyList(),
    val ultimoNumero: Int = 0,
    val modoTema: ModoTema = ModoTema.SISTEMA
)

/**
 * Persistencia local que usa el repositorio. Es una interfaz para que el repositorio
 * no dependa de Android: la app usa SQLite ([BaseDatosLocal]) y las pruebas usan [SinPersistencia].
 */
interface AlmacenLocal {
    fun cargar(): DatosGuardados
    /** null borra el perfil activo (al terminar el registro de un estudiante). */
    fun guardarPerfilActivo(perfil: PerfilEstudiante?)
    fun insertarLiquidacion(liquidacion: Liquidacion)
    fun eliminarLiquidacion(numero: Int)
    fun guardarUltimoNumero(numero: Int)
    fun guardarModoTema(modo: ModoTema)
}

/** Almacén vacío: los datos solo viven en memoria (pruebas unitarias). */
object SinPersistencia : AlmacenLocal {
    override fun cargar() = DatosGuardados()
    override fun guardarPerfilActivo(perfil: PerfilEstudiante?) = Unit
    override fun insertarLiquidacion(liquidacion: Liquidacion) = Unit
    override fun eliminarLiquidacion(numero: Int) = Unit
    override fun guardarUltimoNumero(numero: Int) = Unit
    override fun guardarModoTema(modo: ModoTema) = Unit
}
