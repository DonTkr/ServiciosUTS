package com.uts.serviciosestudiantiles.viewmodel

import com.uts.serviciosestudiantiles.data.AlmacenLocal
import com.uts.serviciosestudiantiles.data.Curso
import com.uts.serviciosestudiantiles.data.DatosGuardados
import com.uts.serviciosestudiantiles.data.Facultad
import com.uts.serviciosestudiantiles.data.Liquidacion
import com.uts.serviciosestudiantiles.data.ModoTema
import com.uts.serviciosestudiantiles.data.PerfilEstudiante
import com.uts.serviciosestudiantiles.data.ServiciosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelsTest {

    private lateinit var repositorio: ServiciosRepository

    @Before
    fun setUp() {
        // viewModelScope usa Dispatchers.Main; en pruebas JVM se reemplaza.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repositorio = ServiciosRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun guardarPerfilValido(): PerfilViewModel =
        PerfilViewModel(repositorio).apply {
            onNombreChange("Iván Peña Rojas")
            onCodigoChange("1098765432")
            seleccionarFacultad(Facultad.INGENIERIAS)
            guardarPerfil()
        }

    // ---------- Módulo 1 ----------

    @Test
    fun `genera correo institucional con primer nombre sin tildes`() {
        assertEquals("ivan.1098765432@uts.edu.co", PerfilViewModel.generarCorreo("Iván Peña", "1098765432"))
        assertEquals("nusta.123456@uts.edu.co", PerfilViewModel.generarCorreo("  Ñusta  ", "123456"))
    }

    @Test
    fun `guardar perfil valido lo publica en el repositorio`() {
        val vm = guardarPerfilValido()
        val perfil = repositorio.perfil.value
        assertNotNull(perfil)
        assertEquals("ivan.1098765432@uts.edu.co", perfil!!.correoInstitucional)
        assertEquals(perfil, vm.uiState.value.perfilGuardado)
    }

    @Test
    fun `nombre se guarda en formato titulo`() {
        PerfilViewModel(repositorio).apply {
            onNombreChange("  ana   ruiz ")
            onCodigoChange("123456")
            seleccionarFacultad(Facultad.CIENCIAS_SOCIOECONOMICAS)
            guardarPerfil()
        }
        assertEquals("Ana Ruiz", repositorio.perfil.value?.nombre)
    }

    @Test
    fun `menu ofrece las tres facultades y guarda Ciencias Humanas`() {
        val vm = PerfilViewModel(repositorio)
        assertEquals(
            listOf("Ingenierías", "Ciencias Socioeconómicas", "Ciencias Humanas"),
            vm.facultades.map { it.nombreVisible }
        )
        vm.onNombreChange("Laura Gómez")
        vm.onCodigoChange("1005432109")
        vm.seleccionarFacultad(Facultad.CIENCIAS_HUMANAS)
        vm.guardarPerfil()
        assertEquals(Facultad.CIENCIAS_HUMANAS, repositorio.perfil.value?.facultad)
    }

    @Test
    fun `perfil vacio muestra errores y no se guarda`() {
        val vm = PerfilViewModel(repositorio)
        vm.guardarPerfil()
        val estado = vm.uiState.value
        assertNotNull(estado.errorNombre)
        assertNotNull(estado.errorCodigo)
        assertNotNull(estado.errorFacultad)
        assertNull(repositorio.perfil.value)
    }

    @Test
    fun `codigo solo acepta digitos`() {
        val vm = PerfilViewModel(repositorio)
        vm.onCodigoChange("12a3-45 6")
        assertEquals("123456", vm.uiState.value.codigo)
    }

    // ---------- Módulo 2 ----------

    @Test
    fun `calculo de costos con beca e icetex`() {
        val base = MatriculaViewModel.calcularCosto(Curso.PROGRAMACION_WEB, tieneBeca = false, usaIcetex = false)
        assertEquals(450_000L, base.total)

        val beca = MatriculaViewModel.calcularCosto(Curso.PROGRAMACION_WEB, tieneBeca = true, usaIcetex = false)
        assertEquals(225_000L, beca.total)

        val icetex = MatriculaViewModel.calcularCosto(Curso.PROGRAMACION_WEB, tieneBeca = false, usaIcetex = true)
        assertEquals(472_500L, icetex.total)

        val ambos = MatriculaViewModel.calcularCosto(Curso.PROGRAMACION_WEB, tieneBeca = true, usaIcetex = true)
        assertEquals(225_000L, ambos.descuentoBeca)
        assertEquals(11_250L, ambos.recargoIcetex)
        assertEquals(236_250L, ambos.total)
    }

    @Test
    fun `liquidar esta bloqueado sin perfil`() {
        val vm = MatriculaViewModel(repositorio)
        vm.seleccionarCurso(Curso.CIBERSEGURIDAD)
        assertFalse(vm.uiState.value.puedeLiquidar)

        vm.liquidar() // aunque se llame directamente, la regla lo impide
        assertTrue(repositorio.liquidaciones.value.isEmpty())
    }

    @Test
    fun `liquidar se habilita al guardar el perfil`() {
        val vm = MatriculaViewModel(repositorio)
        vm.seleccionarCurso(Curso.INGLES_TECNICO)
        assertFalse(vm.uiState.value.puedeLiquidar)

        guardarPerfilValido()
        assertTrue(vm.uiState.value.puedeLiquidar)

        vm.liquidar()
        assertEquals(1, repositorio.liquidaciones.value.size)
    }

    // ---------- Módulo 3 ----------

    @Test
    fun `total acumulado se recalcula con cada liquidacion`() {
        guardarPerfilValido()
        val matricula = MatriculaViewModel(repositorio)
        val historial = HistorialViewModel(repositorio)
        assertEquals(0L, historial.uiState.value.totalAcumulado)

        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar() // 320.000
        assertEquals(320_000L, historial.uiState.value.totalAcumulado)

        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        matricula.cambiarBeca(true)
        matricula.cambiarIcetex(true)
        matricula.liquidar() // 580.000 -> 290.000 + 14.500 = 304.500
        val estado = historial.uiState.value
        assertEquals(624_500L, estado.totalAcumulado)
        assertEquals(290_000L, estado.ahorroPorBecas)
        assertEquals(2, estado.liquidaciones.first().numero) // más reciente primero
        assertEquals(14_500L, estado.recargosIcetex)
    }

    // ---------- Funciones adicionales ----------

    @Test
    fun `vista previa del correo se calcula en vivo`() {
        val vm = PerfilViewModel(repositorio)
        assertNull(vm.uiState.value.correoVistaPrevia)
        vm.onNombreChange("Ana Ruiz")
        vm.onCodigoChange("123456")
        assertEquals("ana.123456@uts.edu.co", vm.uiState.value.correoVistaPrevia)
        assertEquals("AR", vm.uiState.value.inicialesVistaPrevia)
    }

    @Test
    fun `detecta cambios sin guardar en el perfil`() {
        val vm = guardarPerfilValido()
        assertFalse(vm.uiState.value.hayCambiosSinGuardar)
        vm.onCodigoChange("1098765433")
        assertTrue(vm.uiState.value.hayCambiosSinGuardar)
    }

    @Test
    fun `no permite liquidar el mismo curso dos veces`() {
        guardarPerfilValido()
        val vm = MatriculaViewModel(repositorio)
        vm.seleccionarCurso(Curso.PROGRAMACION_WEB)
        vm.liquidar()

        vm.seleccionarCurso(Curso.PROGRAMACION_WEB)
        assertTrue(vm.uiState.value.cursoYaLiquidado)
        assertFalse(vm.uiState.value.puedeLiquidar)
        vm.liquidar()
        assertEquals(1, repositorio.liquidaciones.value.size)
    }

    @Test
    fun `anular liquidacion recalcula el total y libera el curso`() {
        guardarPerfilValido()
        val matricula = MatriculaViewModel(repositorio)
        val historial = HistorialViewModel(repositorio)
        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar()
        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        matricula.liquidar()
        assertEquals(900_000L, historial.uiState.value.totalAcumulado)

        val ingles = historial.uiState.value.liquidaciones.first { it.curso == Curso.INGLES_TECNICO }
        historial.solicitarAnulacion(ingles)
        assertEquals(ingles, historial.uiState.value.liquidacionPorAnular)
        historial.confirmarAnulacion()

        assertNull(historial.uiState.value.liquidacionPorAnular)
        assertEquals(580_000L, historial.uiState.value.totalAcumulado)
        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        assertTrue(matricula.uiState.value.puedeLiquidar)
    }

    @Test
    fun `el numero de liquidacion no se repite despues de anular`() {
        guardarPerfilValido()
        val matricula = MatriculaViewModel(repositorio)
        val historial = HistorialViewModel(repositorio)
        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar() // N.º 1
        historial.solicitarAnulacion(historial.uiState.value.liquidaciones.first())
        historial.confirmarAnulacion()

        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar()
        assertEquals(2, repositorio.liquidaciones.value.single().numero)
    }

    // ---------- Persistencia ----------

    /** Imita a la base de datos: guarda lo que el repositorio escribe y lo devuelve al "reabrir". */
    private class AlmacenDePrueba : AlmacenLocal {
        var perfil: PerfilEstudiante? = null
        val liquidaciones = mutableListOf<Liquidacion>()
        var ultimoNumero = 0
        var modoTema = ModoTema.SISTEMA
        override fun cargar() = DatosGuardados(perfil, liquidaciones.toList(), ultimoNumero, modoTema)
        override fun guardarModoTema(modo: ModoTema) { modoTema = modo }
        override fun guardarPerfilActivo(perfil: PerfilEstudiante?) { this.perfil = perfil }
        override fun insertarLiquidacion(liquidacion: Liquidacion) { liquidaciones += liquidacion }
        override fun eliminarLiquidacion(numero: Int) { liquidaciones.removeAll { it.numero == numero } }
        override fun guardarUltimoNumero(numero: Int) { ultimoNumero = numero }
    }

    @Test
    fun `los datos se recuperan al volver a abrir la app`() {
        val almacen = AlmacenDePrueba()
        repositorio = ServiciosRepository(almacen)
        guardarPerfilValido()
        val matricula = MatriculaViewModel(repositorio)
        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar() // N.º 1
        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        matricula.liquidar() // N.º 2
        HistorialViewModel(repositorio).apply {
            solicitarAnulacion(uiState.value.liquidaciones.first { it.numero == 2 })
            confirmarAnulacion()
        }

        // "Reabrir": repositorio y ViewModels nuevos sobre el mismo almacén.
        repositorio = ServiciosRepository(almacen)
        val perfil = PerfilViewModel(repositorio).uiState.value
        assertEquals("Iván Peña Rojas", perfil.perfilGuardado?.nombre)
        assertEquals("1098765432", perfil.codigo)
        val historial = HistorialViewModel(repositorio).uiState.value
        assertEquals(320_000L, historial.totalAcumulado)
        assertEquals(1, historial.estudiantes.size)

        // El consecutivo continúa: el N.º 2 anulado no se reutiliza.
        MatriculaViewModel(repositorio).apply {
            seleccionarCurso(Curso.PROGRAMACION_WEB)
            liquidar()
        }
        assertEquals(3, repositorio.liquidaciones.value.last().numero)
    }

    @Test
    fun `el modo claro u oscuro elegido se conserva al reabrir`() {
        val almacen = AlmacenDePrueba()
        repositorio = ServiciosRepository(almacen)
        val ajustes = AjustesViewModel(repositorio)
        assertEquals(ModoTema.SISTEMA, ajustes.uiState.value.modoTema)

        ajustes.toggleMenuTema(true)
        ajustes.elegirModoTema(ModoTema.OSCURO)
        assertEquals(ModoTema.OSCURO, ajustes.uiState.value.modoTema)
        assertFalse(ajustes.uiState.value.menuTemaAbierto)

        // "Reabrir".
        repositorio = ServiciosRepository(almacen)
        assertEquals(ModoTema.OSCURO, AjustesViewModel(repositorio).uiState.value.modoTema)
    }

    // ---------- Varios estudiantes ----------

    private fun registrar(nombre: String, codigo: String, facultad: Facultad): PerfilViewModel =
        PerfilViewModel(repositorio).apply {
            onNombreChange(nombre)
            onCodigoChange(codigo)
            seleccionarFacultad(facultad)
            guardarPerfil()
        }

    @Test
    fun `cada facultad tiene sus propios cursos en matricula`() {
        val matricula = MatriculaViewModel(repositorio)
        assertEquals(9, matricula.uiState.value.cursosDisponibles.size) // sin perfil: todos

        registrar("Laura Gómez", "1005432109", Facultad.CIENCIAS_HUMANAS)
        val cursos = matricula.uiState.value.cursosDisponibles
        assertEquals(3, cursos.size)
        assertTrue(cursos.all { it.facultad == Facultad.CIENCIAS_HUMANAS })

        // Aunque se llame directamente, no se liquida un curso de otra facultad.
        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        matricula.liquidar()
        assertTrue(repositorio.liquidaciones.value.isEmpty())
    }

    @Test
    fun `terminar liquidacion limpia el perfil y conserva el historial`() {
        val perfil = guardarPerfilValido()
        val matricula = MatriculaViewModel(repositorio)
        assertFalse(matricula.uiState.value.puedeTerminar) // sin cursos todavía

        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar()
        assertTrue(matricula.uiState.value.puedeTerminar)
        matricula.terminarRegistro()

        assertNull(repositorio.perfil.value)
        val estadoPerfil = perfil.uiState.value
        assertEquals("", estadoPerfil.nombre)
        assertEquals("", estadoPerfil.codigo)
        assertNull(estadoPerfil.facultad)
        assertNull(estadoPerfil.perfilGuardado)
        assertFalse(matricula.uiState.value.puedeLiquidar)
        assertEquals(1, repositorio.liquidaciones.value.size)
    }

    @Test
    fun `historial agrupa por estudiante y abre su detalle`() {
        val matricula = MatriculaViewModel(repositorio)
        val historial = HistorialViewModel(repositorio)

        guardarPerfilValido() // Iván, Ingenierías
        matricula.seleccionarCurso(Curso.INGLES_TECNICO)
        matricula.liquidar() // 320.000
        matricula.seleccionarCurso(Curso.PROGRAMACION_WEB)
        matricula.liquidar() // 450.000
        matricula.terminarRegistro()

        registrar("Laura Gómez", "1005432109", Facultad.CIENCIAS_HUMANAS)
        matricula.seleccionarCurso(Curso.ORATORIA)
        matricula.cambiarBeca(true)
        matricula.liquidar() // 260.000 -> 130.000
        matricula.terminarRegistro()

        val estado = historial.uiState.value
        assertEquals(900_000L, estado.totalAcumulado)
        assertEquals(listOf("1005432109", "1098765432"), estado.estudiantes.map { it.estudiante.codigo })
        assertEquals(770_000L, estado.estudiantes[1].total)

        historial.abrirDetalle("1098765432")
        val detalle = historial.uiState.value.detalle
        assertNotNull(detalle)
        assertEquals("Iván Peña Rojas", detalle!!.estudiante.nombre)
        assertEquals(2, detalle.liquidaciones.size)

        // Al anular todos sus cursos, el detalle se cierra solo.
        detalle.liquidaciones.forEach {
            historial.solicitarAnulacion(it)
            historial.confirmarAnulacion()
        }
        assertNull(historial.uiState.value.detalle)
        assertEquals(1, historial.uiState.value.estudiantes.size)
    }

    @Test
    fun `un estudiante que vuelve conserva sus cursos y no puede cambiar de identidad`() {
        val matricula = MatriculaViewModel(repositorio)
        guardarPerfilValido()
        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        matricula.liquidar()
        matricula.terminarRegistro()

        // Mismo código con otro nombre: se rechaza.
        val otro = registrar("Pedro Díaz", "1098765432", Facultad.INGENIERIAS)
        assertNotNull(otro.uiState.value.errorCodigo)
        assertNull(repositorio.perfil.value)

        // El mismo estudiante vuelve: su curso sigue bloqueado como duplicado.
        guardarPerfilValido()
        matricula.seleccionarCurso(Curso.CIBERSEGURIDAD)
        assertTrue(matricula.uiState.value.cursoYaLiquidado)
    }
}
