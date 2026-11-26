package com.uts.serviciosestudiantiles

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uts.serviciosestudiantiles.data.ModoTema
import com.uts.serviciosestudiantiles.ui.AppPrincipal
import com.uts.serviciosestudiantiles.ui.theme.ServiciosUTSTheme
import com.uts.serviciosestudiantiles.viewmodel.AjustesViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ajustesViewModel: AjustesViewModel = viewModel()
            val ajustes by ajustesViewModel.uiState.collectAsStateWithLifecycle()
            val oscuro = when (ajustes.modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }

            // Los íconos de la barra de estado (hora, batería) siguen el modo de la app, no el del teléfono.
            DisposableEffect(oscuro) {
                val estilo = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
                onDispose { }
            }

            ServiciosUTSTheme(darkTheme = oscuro) {
                AppPrincipal(ajustesViewModel = ajustesViewModel)
            }
        }
    }
}
