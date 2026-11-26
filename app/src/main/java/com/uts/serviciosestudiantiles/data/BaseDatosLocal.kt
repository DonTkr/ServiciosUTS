package com.uts.serviciosestudiantiles.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.concurrent.Executors

/**
 * Persistencia local con SQLite (incluido en Android, sin librerías extra).
 *
 * Tablas:
 * - liquidacion: cada liquidación con la copia del estudiante que la hizo.
 * - perfil_activo: el estudiante que está liquidando ahora (máximo una fila).
 * - ajuste: valores sueltos, como el último número de liquidación usado y el modo claro/oscuro.
 *
 * Las escrituras van a un único hilo de fondo: no bloquean la interfaz y se aplican en orden.
 */
class BaseDatosLocal(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NOMBRE, null, VERSION),
    AlmacenLocal {

    private val escritor = Executors.newSingleThreadExecutor()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE liquidacion (
                numero INTEGER PRIMARY KEY,
                codigo TEXT NOT NULL,
                nombre TEXT NOT NULL,
                facultad TEXT NOT NULL,
                correo TEXT NOT NULL,
                curso TEXT NOT NULL,
                beca INTEGER NOT NULL,
                icetex INTEGER NOT NULL,
                costo_base INTEGER NOT NULL,
                descuento_beca INTEGER NOT NULL,
                recargo_icetex INTEGER NOT NULL,
                total INTEGER NOT NULL,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE perfil_activo (
                id INTEGER PRIMARY KEY CHECK (id = 0),
                codigo TEXT NOT NULL,
                nombre TEXT NOT NULL,
                facultad TEXT NOT NULL,
                correo TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE ajuste (clave TEXT PRIMARY KEY, valor INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Primera versión del esquema: no hay migraciones todavía.
    }

    // ---------- Lectura (una sola vez, al abrir la app) ----------

    override fun cargar(): DatosGuardados {
        val db = readableDatabase
        val perfil = db.rawQuery("SELECT codigo, nombre, facultad, correo FROM perfil_activo", null).use { c ->
            if (c.moveToFirst()) perfilDe(c, 0) else null
        }
        val liquidaciones = db.rawQuery(
            "SELECT numero, codigo, nombre, facultad, correo, curso, beca, icetex, " +
                "costo_base, descuento_beca, recargo_icetex, total, fecha FROM liquidacion ORDER BY numero",
            null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val estudiante = perfilDe(c, 1) ?: continue
                    val curso = enumDe<Curso>(c.getString(5)) ?: continue
                    add(
                        Liquidacion(
                            numero = c.getInt(0),
                            estudiante = estudiante,
                            curso = curso,
                            tieneBeca = c.getInt(6) == 1,
                            usaIcetex = c.getInt(7) == 1,
                            costoBase = c.getLong(8),
                            descuentoBeca = c.getLong(9),
                            recargoIcetex = c.getLong(10),
                            total = c.getLong(11),
                            fechaMillis = c.getLong(12)
                        )
                    )
                }
            }
        }
        val ultimoNumero = leerAjuste(db, CLAVE_ULTIMO_NUMERO) ?: 0
        val modoTema = leerAjuste(db, CLAVE_MODO_TEMA)?.let { ModoTema.entries.getOrNull(it) } ?: ModoTema.SISTEMA
        return DatosGuardados(
            perfilActivo = perfil,
            liquidaciones = liquidaciones,
            // Por seguridad, nunca menor que el número más alto guardado.
            ultimoNumero = maxOf(ultimoNumero, liquidaciones.maxOfOrNull { it.numero } ?: 0),
            modoTema = modoTema
        )
    }

    private fun leerAjuste(db: SQLiteDatabase, clave: String): Int? =
        db.rawQuery("SELECT valor FROM ajuste WHERE clave = ?", arrayOf(clave))
            .use { c -> if (c.moveToFirst()) c.getInt(0) else null }

    // ---------- Escritura (en segundo plano) ----------

    override fun guardarPerfilActivo(perfil: PerfilEstudiante?) = escribir { db ->
        db.delete("perfil_activo", null, null)
        if (perfil != null) {
            db.insert("perfil_activo", null, ContentValues().apply {
                put("id", 0)
                put("codigo", perfil.codigo)
                put("nombre", perfil.nombre)
                put("facultad", perfil.facultad.name)
                put("correo", perfil.correoInstitucional)
            })
        }
    }

    override fun insertarLiquidacion(liquidacion: Liquidacion) = escribir { db ->
        val e = liquidacion.estudiante
        db.insertWithOnConflict("liquidacion", null, ContentValues().apply {
            put("numero", liquidacion.numero)
            put("codigo", e.codigo)
            put("nombre", e.nombre)
            put("facultad", e.facultad.name)
            put("correo", e.correoInstitucional)
            put("curso", liquidacion.curso.name)
            put("beca", if (liquidacion.tieneBeca) 1 else 0)
            put("icetex", if (liquidacion.usaIcetex) 1 else 0)
            put("costo_base", liquidacion.costoBase)
            put("descuento_beca", liquidacion.descuentoBeca)
            put("recargo_icetex", liquidacion.recargoIcetex)
            put("total", liquidacion.total)
            put("fecha", liquidacion.fechaMillis)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun eliminarLiquidacion(numero: Int) = escribir { db ->
        db.delete("liquidacion", "numero = ?", arrayOf(numero.toString()))
    }

    override fun guardarUltimoNumero(numero: Int) = guardarAjuste(CLAVE_ULTIMO_NUMERO, numero)

    /** Se guarda la posición en el enum: 0 = según el teléfono, 1 = claro, 2 = oscuro. */
    override fun guardarModoTema(modo: ModoTema) = guardarAjuste(CLAVE_MODO_TEMA, modo.ordinal)

    private fun guardarAjuste(clave: String, valor: Int) = escribir { db ->
        db.insertWithOnConflict("ajuste", null, ContentValues().apply {
            put("clave", clave)
            put("valor", valor)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun escribir(accion: (SQLiteDatabase) -> Unit) {
        escritor.execute { accion(writableDatabase) }
    }

    // ---------- Conversión fila -> modelo ----------

    /** Lee codigo, nombre, facultad y correo a partir de la columna [desde]. */
    private fun perfilDe(c: Cursor, desde: Int): PerfilEstudiante? {
        val facultad = enumDe<Facultad>(c.getString(desde + 2)) ?: return null
        return PerfilEstudiante(
            codigo = c.getString(desde),
            nombre = c.getString(desde + 1),
            facultad = facultad,
            correoInstitucional = c.getString(desde + 3)
        )
    }

    /** Si un curso o facultad se renombra en una versión futura, la fila se ignora en vez de cerrar la app. */
    private inline fun <reified T : Enum<T>> enumDe(nombre: String): T? =
        enumValues<T>().firstOrNull { it.name == nombre }

    private companion object {
        const val NOMBRE = "servicios_uts.db"
        const val VERSION = 1
        const val CLAVE_ULTIMO_NUMERO = "ultimo_numero"
        const val CLAVE_MODO_TEMA = "modo_tema"
    }
}
