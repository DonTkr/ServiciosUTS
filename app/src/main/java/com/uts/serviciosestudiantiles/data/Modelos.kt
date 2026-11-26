package com.uts.serviciosestudiantiles.data

/** Apariencia elegida por el usuario. SISTEMA sigue el modo claro/oscuro del teléfono. */
enum class ModoTema(val nombreVisible: String) {
    SISTEMA("Según el teléfono"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/** Facultades disponibles en el registro (Módulo 1). */
enum class Facultad(val nombreVisible: String) {
    INGENIERIAS("Ingenierías"),
    CIENCIAS_SOCIOECONOMICAS("Ciencias Socioeconómicas"),
    CIENCIAS_HUMANAS("Ciencias Humanas")
}

/**
 * Cursos libres extracurriculares (Módulo 2). Cada facultad ofrece los suyos.
 * Los costos están en pesos colombianos enteros para evitar errores de redondeo con Double.
 */
enum class Curso(
    val nombreVisible: String,
    val costoBase: Long,
    val horas: Int,
    val modalidad: String,
    val facultad: Facultad
) {
    // Ingenierías
    INGLES_TECNICO("Inglés Técnico", 320_000, 48, "Presencial", Facultad.INGENIERIAS),
    PROGRAMACION_WEB("Programación Web", 450_000, 64, "Virtual", Facultad.INGENIERIAS),
    CIBERSEGURIDAD("Ciberseguridad", 580_000, 80, "Híbrida", Facultad.INGENIERIAS),

    // Ciencias Socioeconómicas
    EXCEL_FINANCIERO("Excel Financiero", 300_000, 40, "Virtual", Facultad.CIENCIAS_SOCIOECONOMICAS),
    MARKETING_DIGITAL("Marketing Digital", 380_000, 48, "Virtual", Facultad.CIENCIAS_SOCIOECONOMICAS),
    EMPRENDIMIENTO("Emprendimiento e Innovación", 350_000, 48, "Presencial", Facultad.CIENCIAS_SOCIOECONOMICAS),

    // Ciencias Humanas
    ORATORIA("Oratoria y Expresión Oral", 260_000, 32, "Presencial", Facultad.CIENCIAS_HUMANAS),
    FOTOGRAFIA_DIGITAL("Fotografía Digital", 420_000, 56, "Presencial", Facultad.CIENCIAS_HUMANAS),
    LENGUA_DE_SENAS("Lengua de Señas Colombiana", 340_000, 48, "Híbrida", Facultad.CIENCIAS_HUMANAS);

    companion object {
        fun deFacultad(facultad: Facultad): List<Curso> = entries.filter { it.facultad == facultad }
    }
}

/** Perfil académico guardado por el estudiante. */
data class PerfilEstudiante(
    val nombre: String,
    val codigo: String,
    val facultad: Facultad,
    val correoInstitucional: String
) {
    /** Iniciales para el monograma del carné ("Ana Ruiz" -> "AR"). */
    val iniciales: String get() = inicialesDe(nombre)
}

private val ESPACIOS = Regex("\\s+")

/** "ana maría ruiz" -> "AM". Se usa en el carné guardado y en la vista previa. */
fun inicialesDe(nombre: String): String =
    nombre.trim().split(ESPACIOS).filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

/** Una liquidación de matrícula ya generada (Módulo 2) y listada en el historial (Módulo 3). */
data class Liquidacion(
    val numero: Int,
    /** Copia del perfil al momento de liquidar: el historial conserva a cada estudiante. */
    val estudiante: PerfilEstudiante,
    val curso: Curso,
    val tieneBeca: Boolean,
    val usaIcetex: Boolean,
    val costoBase: Long,
    val descuentoBeca: Long,
    val recargoIcetex: Long,
    val total: Long,
    /** Momento de la liquidación en milisegundos (epoch). */
    val fechaMillis: Long = System.currentTimeMillis()
)
