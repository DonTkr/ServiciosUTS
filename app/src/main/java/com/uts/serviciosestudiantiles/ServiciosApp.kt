package com.uts.serviciosestudiantiles

import android.app.Application
import com.uts.serviciosestudiantiles.data.BaseDatosLocal
import com.uts.serviciosestudiantiles.data.ServiciosRepository

/**
 * Se crea antes que cualquier pantalla: aquí se arma el repositorio compartido
 * con la base de datos local, para que los ViewModels ya encuentren los datos guardados.
 */
class ServiciosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiciosRepository.instancia = ServiciosRepository(BaseDatosLocal(this))
    }
}
