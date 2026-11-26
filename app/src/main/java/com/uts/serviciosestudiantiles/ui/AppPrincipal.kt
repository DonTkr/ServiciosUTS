package com.uts.serviciosestudiantiles.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uts.serviciosestudiantiles.data.ModoTema
import com.uts.serviciosestudiantiles.viewmodel.AjustesViewModel
import com.uts.serviciosestudiantiles.viewmodel.HistorialViewModel
import com.uts.serviciosestudiantiles.viewmodel.PerfilViewModel

/** Las tres secciones siguen el orden real del trámite: perfil, matrícula, historial. */
private enum class Seccion(
    val etiqueta: String,
    val titulo: String,
    val iconoActivo: ImageVector,
    val iconoInactivo: ImageVector
) {
    PERFIL("Perfil", "Perfil académico", Icons.Filled.Badge, Icons.Outlined.Badge),
    MATRICULA("Matrícula", "Matrícula de cursos libres", Icons.Filled.EditNote, Icons.Outlined.EditNote),
    HISTORIAL("Historial", "Historial financiero", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrincipal(
    perfilViewModel: PerfilViewModel = viewModel(),
    historialViewModel: HistorialViewModel = viewModel(),
    ajustesViewModel: AjustesViewModel = viewModel()
) {
    val ajustes by ajustesViewModel.uiState.collectAsStateWithLifecycle()
    // Estado de navegación (solo UI). Los ViewModels viven en la Activity,
    // así que los datos se conservan al cambiar de pestaña.
    var seccion by rememberSaveable { mutableStateOf(Seccion.PERFIL) }
    val perfilState by perfilViewModel.uiState.collectAsStateWithLifecycle()
    val historialState by historialViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val perfil = perfilState.perfilGuardado

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(seccion.titulo, style = MaterialTheme.typography.titleLarge)
                        Text(
                            perfil?.let { "Hola, ${it.nombre.substringBefore(' ')}" }
                                ?: "Servicios Estudiantiles UTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    SelectorTema(
                        modo = ajustes.modoTema,
                        opciones = ajustesViewModel.opcionesTema,
                        abierto = ajustes.menuTemaAbierto,
                        onAbiertoChange = ajustesViewModel::toggleMenuTema,
                        onElegir = ajustesViewModel::elegirModoTema,
                        modifier = Modifier.padding(end = if (perfil == null) 8.dp else 4.dp)
                    )
                    perfil?.let {
                        Monograma(
                            iniciales = it.iniciales,
                            fondo = MaterialTheme.colorScheme.primary,
                            texto = MaterialTheme.colorScheme.onPrimary,
                            tamano = 36.dp,
                            tamanoLetra = MaterialTheme.typography.labelLarge.fontSize,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Seccion.entries.forEach { item ->
                    val seleccionado = seccion == item
                    NavigationBarItem(
                        selected = seleccionado,
                        onClick = { seccion = item },
                        icon = {
                            BadgedBox(
                                badge = {
                                    // Pista del trámite: perfil pendiente y cantidad de liquidaciones.
                                    when {
                                        item == Seccion.PERFIL && perfil == null -> Badge()
                                        item == Seccion.HISTORIAL && historialState.liquidaciones.isNotEmpty() ->
                                            Badge { Text(historialState.liquidaciones.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    if (seleccionado) item.iconoActivo else item.iconoInactivo,
                                    contentDescription = null
                                )
                            }
                        },
                        label = { Text(item.etiqueta) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        Crossfade(targetState = seccion, animationSpec = tween(180), label = "seccion") { actual ->
            when (actual) {
                Seccion.PERFIL -> PerfilScreen(snackbarHostState, modifier, perfilViewModel)
                Seccion.MATRICULA -> MatriculaScreen(
                    snackbarHostState = snackbarHostState,
                    onIrAPerfil = { seccion = Seccion.PERFIL },
                    onIrAHistorial = { seccion = Seccion.HISTORIAL },
                    modifier = modifier
                )
                Seccion.HISTORIAL -> HistorialScreen(
                    snackbarHostState = snackbarHostState,
                    onIrAMatricula = { seccion = Seccion.MATRICULA },
                    modifier = modifier,
                    viewModel = historialViewModel
                )
            }
        }
    }
}

/** Botón de la barra superior: muestra el modo actual y abre el menú para cambiarlo. */
@Composable
private fun SelectorTema(
    modo: ModoTema,
    opciones: List<ModoTema>,
    abierto: Boolean,
    onAbiertoChange: (Boolean) -> Unit,
    onElegir: (ModoTema) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        IconButton(onClick = { onAbiertoChange(true) }) {
            Icon(modo.icono, contentDescription = "Apariencia: ${modo.nombreVisible}")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { onAbiertoChange(false) }) {
            Text(
                "Apariencia",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.nombreVisible) },
                    leadingIcon = { Icon(opcion.icono, contentDescription = null) },
                    trailingIcon = if (opcion == modo) {
                        { Icon(Icons.Outlined.Check, contentDescription = "Seleccionado") }
                    } else {
                        null
                    },
                    onClick = { onElegir(opcion) }
                )
            }
        }
    }
}

private val ModoTema.icono: ImageVector
    get() = when (this) {
        ModoTema.SISTEMA -> Icons.Outlined.BrightnessAuto
        ModoTema.CLARO -> Icons.Outlined.LightMode
        ModoTema.OSCURO -> Icons.Outlined.DarkMode
    }