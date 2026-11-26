package com.uts.serviciosestudiantiles.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uts.serviciosestudiantiles.ui.theme.EstiloCifras
import com.uts.serviciosestudiantiles.ui.theme.LocalColoresCarne
import com.uts.serviciosestudiantiles.viewmodel.PerfilUiState
import com.uts.serviciosestudiantiles.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CarneEstudiantil(estado)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EncabezadoSeccion(
                    titulo = "Datos del estudiante",
                    ayuda = "Con ellos se genera tu correo institucional y se habilita la matrícula.",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = estado.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text("Nombre completo") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    isError = estado.errorNombre != null,
                    supportingText = estado.errorNombre?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estado.codigo,
                    onValueChange = viewModel::onCodigoChange,
                    label = { Text("Código estudiantil") },
                    leadingIcon = { Icon(Icons.Outlined.Numbers, contentDescription = null) },
                    singleLine = true,
                    isError = estado.errorCodigo != null,
                    supportingText = {
                        Text(
                            estado.errorCodigo
                                ?: "Solo números, entre ${PerfilViewModel.MIN_CODIGO} y ${PerfilViewModel.MAX_CODIGO} dígitos."
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                SelectorDesplegable(
                    etiqueta = "Facultad",
                    opciones = viewModel.facultades,
                    seleccion = estado.facultad,
                    textoDe = { it.nombreVisible },
                    expandido = estado.menuFacultadAbierto,
                    onExpandidoChange = viewModel::toggleMenuFacultad,
                    onSeleccion = viewModel::seleccionarFacultad,
                    error = estado.errorFacultad,
                    modifier = Modifier.fillMaxWidth()
                )

                val guardado = estado.perfilGuardado != null
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.guardarPerfil()
                    },
                    enabled = !guardado || estado.hayCambiosSinGuardar,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(top = 4.dp)
                ) {
                    when {
                        !guardado -> Text("Guardar perfil")
                        estado.hayCambiosSinGuardar -> Text("Guardar cambios")
                        else -> {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Perfil guardado")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Carné digital: muestra en vivo lo que el ViewModel calcula (iniciales y correo).
 * Mientras no se guarde aparece como "Vista previa" en un tono atenuado.
 */
@Composable
private fun CarneEstudiantil(estado: PerfilUiState) {
    val colores = LocalColoresCarne.current
    val guardado = estado.perfilGuardado
    val activo = guardado != null && !estado.hayCambiosSinGuardar

    val fondo by animateColorAsState(
        if (activo) colores.fondo else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "fondoCarne"
    )
    val texto = if (activo) colores.texto else MaterialTheme.colorScheme.onSurface
    val textoSuave = if (activo) colores.textoSuave else MaterialTheme.colorScheme.onSurfaceVariant

    val nombre = estado.nombre.trim().ifEmpty { "Tu nombre" }
    val facultad = estado.facultad?.nombreVisible ?: "Facultad sin elegir"
    val codigo = estado.codigo.ifEmpty { "··········" }
    val correo = estado.correoVistaPrevia ?: "nombre.codigo@uts.edu.co"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto),
        border = if (activo) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            // Franja superior del carné: institución y estado.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (activo) {
                            Brush.horizontalGradient(listOf(colores.fondoProfundo, colores.fondo))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        }
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    "UTS",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                    color = if (activo) colores.acento else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Unidades Tecnológicas\nde Santander",
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 13.sp,
                    color = textoSuave,
                    modifier = Modifier.weight(1f)
                )
                EstadoCarne(activo = activo, sinGuardar = guardado != null && estado.hayCambiosSinGuardar)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)
            ) {
                Monograma(
                    iniciales = estado.inicialesVistaPrevia,
                    fondo = if (activo) colores.acento else MaterialTheme.colorScheme.surfaceContainerLowest,
                    texto = if (activo) colores.fondoProfundo else MaterialTheme.colorScheme.onSurfaceVariant,
                    tamano = 60.dp,
                    tamanoLetra = 22.sp
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        nombre,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(facultad, style = MaterialTheme.typography.bodyMedium, color = textoSuave)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                DatoCarne("Código estudiantil", codigo, textoSuave)
                DatoCarne("Correo institucional", correo, textoSuave)
            }
        }
    }
}

@Composable
private fun DatoCarne(
    etiqueta: String,
    valor: String,
    colorEtiqueta: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = colorEtiqueta)
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium.merge(EstiloCifras),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EstadoCarne(activo: Boolean, sinGuardar: Boolean) {
    val colores = LocalColoresCarne.current
    val (texto, color) = when {
        activo -> "Activo" to colores.acento
        sinGuardar -> "Cambios sin guardar" to MaterialTheme.colorScheme.tertiary
        else -> "Vista previa" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(texto, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
