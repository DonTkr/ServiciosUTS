package com.uts.serviciosestudiantiles.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SignLanguage
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.uts.serviciosestudiantiles.data.Curso
import com.uts.serviciosestudiantiles.ui.theme.EstiloCifras
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Formato de presentación (no es lógica de negocio: solo cómo se ve un valor) ----------

private val localeCo: Locale = Locale.forLanguageTag("es-CO")

private val formatoPesos: NumberFormat =
    NumberFormat.getCurrencyInstance(localeCo).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

/** 320000 -> "$ 320.000". */
fun Long.enPesos(): String = formatoPesos.format(this)

/** 1731440000000 -> "12 nov, 3:40 p. m." */
// Se crea una sola vez (solo se usa desde el hilo de la interfaz).
private val formatoFecha = SimpleDateFormat("d MMM, h:mm a", localeCo)

fun Long.comoFecha(): String = formatoFecha.format(Date(this))

/** Ícono que representa cada curso en listas y selectores. */
val Curso.icono: ImageVector
    get() = when (this) {
        Curso.INGLES_TECNICO -> Icons.Outlined.Translate
        Curso.PROGRAMACION_WEB -> Icons.Outlined.Code
        Curso.CIBERSEGURIDAD -> Icons.Outlined.Security
        Curso.EXCEL_FINANCIERO -> Icons.Outlined.TableChart
        Curso.MARKETING_DIGITAL -> Icons.Outlined.Campaign
        Curso.EMPRENDIMIENTO -> Icons.Outlined.Lightbulb
        Curso.ORATORIA -> Icons.Outlined.RecordVoiceOver
        Curso.FOTOGRAFIA_DIGITAL -> Icons.Outlined.PhotoCamera
        Curso.LENGUA_DE_SENAS -> Icons.Outlined.SignLanguage
    }

// ---------- Componentes reutilizables ----------

/**
 * Selector M3 reutilizable (ExposedDropdownMenuBox). Es una vista "tonta":
 * recibe el estado y avisa los eventos; el ViewModel decide qué cambia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectorDesplegable(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    expandido: Boolean,
    onExpandidoChange: (Boolean) -> Unit,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    textoSecundarioDe: ((T) -> String)? = null,
    iconoDe: ((T) -> ImageVector)? = null
) {
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = onExpandidoChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccion?.let(textoDe).orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            placeholder = { Text("Toca para elegir") },
            leadingIcon = if (iconoDe != null && seleccion != null) {
                { Icon(iconoDe(seleccion), contentDescription = null) }
            } else {
                null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { onExpandidoChange(false) }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    leadingIcon = iconoDe?.let { icono -> { Icon(icono(opcion), contentDescription = null) } },
                    trailingIcon = textoSecundarioDe?.let { secundario ->
                        {
                            Text(
                                secundario(opcion),
                                style = MaterialTheme.typography.bodyMedium.merge(EstiloCifras),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSeleccion(opcion) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

/** Círculo con las iniciales del estudiante. */
@Composable
fun Monograma(
    iniciales: String,
    fondo: Color,
    texto: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
    tamanoLetra: TextUnit = MaterialTheme.typography.titleMedium.fontSize
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(fondo, CircleShape)
    ) {
        Text(
            iniciales.ifEmpty { "?" },
            color = texto,
            fontSize = tamanoLetra,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Cuadro con el ícono del curso, usado en el historial y en la matrícula. */
@Composable
fun IconoCurso(curso: Curso, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(curso.icono, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

/** Etiqueta pequeña de color (beca, Icetex, horas...). */
@Composable
fun Etiqueta(
    texto: String,
    fondo: Color,
    contenido: Color,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = fondo,
        contentColor = contenido,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            icono?.let { Icon(it, contentDescription = null, modifier = Modifier.size(14.dp)) }
            Text(texto, style = MaterialTheme.typography.labelMedium.merge(EstiloCifras))
        }
    }
}

/** Título de sección dentro de una tarjeta, con ayuda opcional debajo. */
@Composable
fun EncabezadoSeccion(titulo: String, modifier: Modifier = Modifier, ayuda: String? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        ayuda?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Línea punteada tipo "corte de recibo". */
@Composable
fun LineaPerforada(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}
