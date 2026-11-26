package com.uts.serviciosestudiantiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uts.serviciosestudiantiles.data.Curso
import com.uts.serviciosestudiantiles.data.PerfilEstudiante
import com.uts.serviciosestudiantiles.ui.theme.EstiloCifras
import com.uts.serviciosestudiantiles.ui.theme.EstiloTotal
import com.uts.serviciosestudiantiles.viewmodel.MatriculaViewModel
import com.uts.serviciosestudiantiles.viewmodel.ResumenCosto
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@Composable
fun MatriculaScreen(
    snackbarHostState: SnackbarHostState,
    onIrAPerfil: () -> Unit,
    onIrAHistorial: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatriculaViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // El mensaje se consume antes de mostrarse: si el usuario navega con la acción
    // "Ver historial", no vuelve a aparecer al regresar a esta pantalla.
    LaunchedEffect(viewModel) {
        viewModel.uiState.map { it.mensaje }.filterNotNull().collect { mensaje ->
            viewModel.mensajeMostrado()
            val resultado = snackbarHostState.showSnackbar(mensaje, actionLabel = "Ver historial")
            if (resultado == SnackbarResult.ActionPerformed) onIrAHistorial()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val perfil = estado.perfil
        if (perfil == null) AvisoPerfilPendiente(onIrAPerfil) else FranjaEstudiante(perfil)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(vertical = 20.dp)) {
                EncabezadoSeccion(
                    titulo = "Curso libre",
                    ayuda = perfil?.let { "Cursos de la facultad de ${it.facultad.nombreVisible}, cada uno con su costo base." }
                        ?: "Cada facultad ofrece sus propios cursos. Al guardar tu perfil verás solo los tuyos.",
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                SelectorDesplegable(
                    etiqueta = "Curso",
                    opciones = estado.cursosDisponibles,
                    seleccion = estado.cursoSeleccionado,
                    textoDe = { it.nombreVisible },
                    textoSecundarioDe = { curso ->
                        if (curso in estado.cursosLiquidados) "Liquidado" else curso.costoBase.enPesos()
                    },
                    iconoDe = { it.icono },
                    expandido = estado.menuCursosAbierto,
                    onExpandidoChange = viewModel::toggleMenuCursos,
                    onSeleccion = viewModel::seleccionarCurso,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                )
                AnimatedVisibility(
                    visible = estado.cursoSeleccionado != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    estado.cursoSeleccionado?.let {
                        DetalleCurso(it, yaLiquidado = estado.cursoYaLiquidado)
                    }
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                EncabezadoSeccion(
                    titulo = "Beneficios y financiación",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                FilaOpcion(
                    icono = Icons.Outlined.School,
                    titulo = "Beca de Excelencia UTS",
                    detalle = "Descuento del ${MatriculaViewModel.PORCENTAJE_BECA} % sobre el costo del curso",
                    activo = estado.tieneBeca,
                    onCambio = viewModel::cambiarBeca,
                    rol = Role.Checkbox
                ) {
                    Checkbox(checked = estado.tieneBeca, onCheckedChange = null)
                }
                FilaOpcion(
                    icono = Icons.Outlined.AccountBalance,
                    titulo = "Financiar con Icetex",
                    detalle = "Recargo del ${MatriculaViewModel.PORCENTAJE_ICETEX} % por costos administrativos",
                    activo = estado.usaIcetex,
                    onCambio = viewModel::cambiarIcetex,
                    rol = Role.Switch
                ) {
                    Switch(checked = estado.usaIcetex, onCheckedChange = null)
                }
            }
        }

        AnimatedVisibility(
            visible = estado.resumen != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            estado.resumen?.let { Recibo(it, estado.cursoSeleccionado) }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Button(
                onClick = viewModel::liquidar,
                enabled = estado.puedeLiquidar,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    estado.resumen?.let { "Liquidar ${it.total.enPesos()}" } ?: "Liquidar",
                    style = MaterialTheme.typography.labelLarge.merge(EstiloCifras)
                )
            }
            estado.motivoBloqueo?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        perfil?.let {
            CierreRegistro(
                nombre = it.nombre.substringBefore(' '),
                cursos = estado.liquidacionesEstudiante.size,
                total = estado.totalEstudiante,
                habilitado = estado.puedeTerminar,
                onTerminar = {
                    viewModel.terminarRegistro()
                    // Navegación (solo UI): el perfil ya quedó vacío para el siguiente estudiante.
                    onIrAPerfil()
                }
            )
        }
    }
}

/** Cierra el registro del estudiante actual y deja el perfil libre para otro. */
@Composable
private fun CierreRegistro(
    nombre: String,
    cursos: Int,
    total: Long,
    habilitado: Boolean,
    onTerminar: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Registro de $nombre", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (cursos) {
                            0 -> "Liquida al menos un curso para terminar."
                            1 -> "1 curso liquidado"
                            else -> "$cursos cursos liquidados"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (cursos > 0) {
                    Text(total.enPesos(), style = MaterialTheme.typography.titleMedium.merge(EstiloCifras))
                }
            }
            OutlinedButton(
                onClick = onTerminar,
                enabled = habilitado,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Terminar liquidación")
            }
            Text(
                "Sus cursos quedan en el historial y el perfil se limpia para registrar a otro estudiante.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Bloqueo de seguridad visible: explica por qué no se puede liquidar y cómo resolverlo. */
@Composable
private fun AvisoPerfilPendiente(onIrAPerfil: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.padding(top = 2.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Primero completa tu perfil", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Puedes explorar cursos y costos, pero la liquidación se habilita cuando guardes tu perfil académico.",
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalButton(onClick = onIrAPerfil, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Completar perfil")
                }
            }
        }
    }
}

/** Confirma a nombre de quién se liquida. */
@Composable
private fun FranjaEstudiante(perfil: PerfilEstudiante) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Monograma(
                iniciales = perfil.iniciales,
                fondo = MaterialTheme.colorScheme.primaryContainer,
                texto = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(Modifier.weight(1f)) {
                Text(perfil.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Código ${perfil.codigo}, ${perfil.facultad.nombreVisible}",
                    style = MaterialTheme.typography.bodySmall.merge(EstiloCifras),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Perfil completo",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DetalleCurso(curso: Curso, yaLiquidado: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
    ) {
        val fondo = MaterialTheme.colorScheme.surfaceContainer
        val texto = MaterialTheme.colorScheme.onSurfaceVariant
        Etiqueta("${curso.horas} horas", fondo, texto, icono = Icons.Outlined.Schedule)
        Etiqueta(curso.modalidad, fondo, texto, icono = Icons.Outlined.Place)
        if (yaLiquidado) {
            Etiqueta(
                "Ya liquidado",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                icono = Icons.Outlined.CheckCircle
            )
        }
    }
}

/** Fila completa tocable: mejora accesibilidad y área táctil del Checkbox/Switch. */
@Composable
private fun FilaOpcion(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    rol: Role,
    control: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = activo, onValueChange = onCambio, role = rol)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        control()
    }
}

/** Desglose tipo recibo, recalculado por el ViewModel con cada cambio. */
@Composable
private fun Recibo(resumen: ResumenCosto, curso: Curso?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Resumen de liquidación",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                curso?.let {
                    Text(
                        it.nombreVisible,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            FilaRecibo("Costo del curso", resumen.costoBase.enPesos())
            if (resumen.descuentoBeca > 0) {
                FilaRecibo(
                    "Beca de Excelencia (−${MatriculaViewModel.PORCENTAJE_BECA} %)",
                    "−" + resumen.descuentoBeca.enPesos(),
                    MaterialTheme.colorScheme.primary
                )
            }
            if (resumen.recargoIcetex > 0) {
                FilaRecibo(
                    "Recargo Icetex (+${MatriculaViewModel.PORCENTAJE_ICETEX} %)",
                    "+" + resumen.recargoIcetex.enPesos(),
                    MaterialTheme.colorScheme.tertiary
                )
            }
            LineaPerforada(
                MaterialTheme.colorScheme.outline,
                Modifier.padding(vertical = 6.dp)
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "Total a pagar",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(resumen.total.enPesos(), style = EstiloTotal)
            }
        }
    }
}

@Composable
private fun FilaRecibo(concepto: String, valor: String, colorValor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(concepto, style = MaterialTheme.typography.bodyMedium)
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium.merge(EstiloCifras),
            color = colorValor
        )
    }
}
