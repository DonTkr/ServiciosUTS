package com.uts.serviciosestudiantiles.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uts.serviciosestudiantiles.data.Liquidacion
import com.uts.serviciosestudiantiles.ui.theme.EstiloCifras
import com.uts.serviciosestudiantiles.ui.theme.EstiloTotal
import com.uts.serviciosestudiantiles.ui.theme.LocalColoresCarne
import com.uts.serviciosestudiantiles.viewmodel.HistorialUiState
import com.uts.serviciosestudiantiles.viewmodel.HistorialViewModel
import com.uts.serviciosestudiantiles.viewmodel.RegistroEstudiante

@Composable
fun HistorialScreen(
    snackbarHostState: SnackbarHostState,
    onIrAMatricula: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    // Diálogo de confirmación controlado por el estado del ViewModel.
    estado.liquidacionPorAnular?.let { liquidacion ->
        DialogoAnular(
            liquidacion = liquidacion,
            onConfirmar = viewModel::confirmarAnulacion,
            onCancelar = viewModel::cancelarAnulacion
        )
    }

    // El botón atrás del teléfono cierra el detalle antes de salir de la app.
    BackHandler(enabled = estado.detalle != null, onBack = viewModel::cerrarDetalle)

    // El detalle entra desde la derecha y vuelve a la lista hacia la izquierda.
    AnimatedContent(
        targetState = estado.detalle,
        contentKey = { it?.estudiante?.codigo },
        transitionSpec = {
            val abriendo = targetState != null
            val desplazamiento = { ancho: Int -> if (abriendo) ancho / 4 else -ancho / 4 }
            (fadeIn(tween(220)) + slideInHorizontally(tween(220), desplazamiento))
                .togetherWith(fadeOut(tween(120)))
        },
        label = "detalleEstudiante",
        modifier = modifier.fillMaxSize()
    ) { detalle ->
        if (detalle == null) {
            ListaEstudiantes(estado, onAbrir = viewModel::abrirDetalle, onIrAMatricula = onIrAMatricula)
        } else {
            DetalleEstudiante(
                registro = detalle,
                onVolver = viewModel::cerrarDetalle,
                onAnular = viewModel::solicitarAnulacion
            )
        }
    }
}

// ---------- Lista de estudiantes ----------

@Composable
private fun ListaEstudiantes(
    estado: HistorialUiState,
    onAbrir: (String) -> Unit,
    onIrAMatricula: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Tarjeta destacada: Total Acumulado a Pagar (calculado por el ViewModel).
        TarjetaTotal(estado, Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp))

        if (estado.estudiantes.isEmpty()) {
            EstadoVacio(onIrAMatricula)
        } else {
            Text(
                "Estudiantes",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(estado.estudiantes, key = { it.estudiante.codigo }) { registro ->
                    ItemEstudiante(
                        registro = registro,
                        onClick = { onAbrir(registro.estudiante.codigo) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaTotal(estado: HistorialUiState, modifier: Modifier = Modifier) {
    val colores = LocalColoresCarne.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colores.fondo, contentColor = colores.texto),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Total acumulado a pagar", style = MaterialTheme.typography.titleSmall, color = colores.textoSuave)
            Text(estado.totalAcumulado.enPesos(), style = EstiloTotal)
            Text(
                resumenCantidades(estado.liquidaciones.size, estado.estudiantes.size),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.textoSuave
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp)
            ) {
                Indicador(
                    "Ahorro por becas",
                    estado.ahorroPorBecas.enPesos(),
                    colores.acento,
                    colores.fondoProfundo,
                    Modifier.weight(1f)
                )
                Indicador(
                    "Recargos Icetex",
                    estado.recargosIcetex.enPesos(),
                    Color(0xFFFFC870),
                    colores.fondoProfundo,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

private fun resumenCantidades(cursos: Int, estudiantes: Int): String {
    if (cursos == 0) return "Sin liquidaciones todavía"
    val textoCursos = if (cursos == 1) "1 curso liquidado" else "$cursos cursos liquidados"
    val textoEstudiantes = if (estudiantes == 1) "1 estudiante" else "$estudiantes estudiantes"
    return "$textoCursos por $textoEstudiantes"
}

@Composable
private fun Indicador(etiqueta: String, valor: String, colorPunto: Color, fondo: Color, modifier: Modifier) {
    val colores = LocalColoresCarne.current
    Column(
        modifier
            .background(fondo, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(colorPunto, CircleShape)
            )
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = colores.textoSuave)
        }
        Text(valor, style = MaterialTheme.typography.titleMedium.merge(EstiloCifras))
    }
}

@Composable
private fun EstadoVacio(onIrAMatricula: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Text("Aún no hay liquidaciones", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cada estudiante que liquide cursos aparecerá aquí; tócalo para ver todo lo que liquidó.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onIrAMatricula, shape = MaterialTheme.shapes.medium) {
                Text("Liquidar un curso")
            }
        }
    }
}

/** Una fila por estudiante: quién es, cuánto debe y qué cursos liquidó. Se toca para ver el detalle. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemEstudiante(registro: RegistroEstudiante, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val estudiante = registro.estudiante
    val cantidad = registro.liquidaciones.size
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Monograma(
                    iniciales = estudiante.iniciales,
                    fondo = MaterialTheme.colorScheme.primaryContainer,
                    texto = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        estudiante.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${estudiante.codigo}, ${estudiante.facultad.nombreVisible}",
                        style = MaterialTheme.typography.bodySmall.merge(EstiloCifras),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(registro.total.enPesos(), style = MaterialTheme.typography.titleMedium.merge(EstiloCifras))
                    Text(
                        if (cantidad == 1) "1 curso" else "$cantidad cursos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Ver detalle de ${estudiante.nombre}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                registro.liquidaciones.forEach { liquidacion ->
                    Etiqueta(
                        liquidacion.curso.nombreVisible,
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        icono = liquidacion.curso.icono
                    )
                }
            }
        }
    }
}

// ---------- Detalle de un estudiante ----------

@Composable
private fun DetalleEstudiante(
    registro: RegistroEstudiante,
    onVolver: () -> Unit,
    onAnular: (Liquidacion) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            TextButton(onClick = onVolver, contentPadding = PaddingValues(start = 4.dp, end = 12.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Todos los estudiantes", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item { CarneDetalle(registro, Modifier.padding(bottom = 6.dp)) }
        item {
            Text(
                "Cursos liquidados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        items(registro.liquidaciones, key = { it.numero }) { liquidacion ->
            ItemLiquidacion(
                liquidacion = liquidacion,
                onAnular = { onAnular(liquidacion) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/** Carné del estudiante con su total: la misma identidad visual que el Módulo 1. */
@Composable
private fun CarneDetalle(registro: RegistroEstudiante, modifier: Modifier = Modifier) {
    val colores = LocalColoresCarne.current
    val estudiante = registro.estudiante
    Card(
        colors = CardDefaults.cardColors(containerColor = colores.fondo, contentColor = colores.texto),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Monograma(
                    iniciales = estudiante.iniciales,
                    fondo = colores.fondoProfundo,
                    texto = colores.texto,
                    tamano = 52.dp,
                    tamanoLetra = MaterialTheme.typography.titleLarge.fontSize
                )
                Column(Modifier.weight(1f)) {
                    Text(estudiante.nombre, style = MaterialTheme.typography.titleLarge)
                    Text(
                        estudiante.facultad.nombreVisible,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.textoSuave
                    )
                }
            }
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Código ${estudiante.codigo}",
                    style = MaterialTheme.typography.bodyMedium.merge(EstiloCifras),
                    color = colores.textoSuave
                )
                Text(
                    estudiante.correoInstitucional,
                    style = MaterialTheme.typography.bodyMedium.merge(EstiloCifras),
                    color = colores.textoSuave
                )
            }
            LineaPerforada(colores.textoSuave.copy(alpha = 0.5f), Modifier.padding(vertical = 16.dp))
            Text("Total a pagar", style = MaterialTheme.typography.titleSmall, color = colores.textoSuave)
            Text(registro.total.enPesos(), style = EstiloTotal)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 14.dp)
            ) {
                Indicador(
                    "Ahorro por beca",
                    registro.ahorroPorBecas.enPesos(),
                    colores.acento,
                    colores.fondoProfundo,
                    Modifier.weight(1f)
                )
                Indicador(
                    "Recargos Icetex",
                    registro.recargosIcetex.enPesos(),
                    Color(0xFFFFC870),
                    colores.fondoProfundo,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ItemLiquidacion(liquidacion: Liquidacion, onAnular: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconoCurso(liquidacion.curso)
                Column(Modifier.weight(1f)) {
                    Text(liquidacion.curso.nombreVisible, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "N.º ${liquidacion.numero}, ${liquidacion.fechaMillis.comoFecha()}",
                        style = MaterialTheme.typography.bodySmall.merge(EstiloCifras),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    liquidacion.total.enPesos(),
                    style = MaterialTheme.typography.titleMedium.merge(EstiloCifras)
                )
                IconButton(onClick = onAnular) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Anular liquidación ${liquidacion.numero}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(end = 12.dp)) {
                Etiqueta(
                    "Base ${liquidacion.costoBase.enPesos()}",
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (liquidacion.tieneBeca) {
                    Etiqueta(
                        "−${liquidacion.descuentoBeca.enPesos()}",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                        icono = Icons.Outlined.School
                    )
                }
                if (liquidacion.usaIcetex) {
                    Etiqueta(
                        "+${liquidacion.recargoIcetex.enPesos()}",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        icono = Icons.Outlined.AccountBalance
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoAnular(liquidacion: Liquidacion, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
        title = { Text("¿Anular liquidación N.º ${liquidacion.numero}?") },
        text = {
            Text(
                "Se quitará ${liquidacion.curso.nombreVisible} (${liquidacion.total.enPesos()}) " +
                    "de ${liquidacion.estudiante.nombre} y el total acumulado se recalculará."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Anular") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
