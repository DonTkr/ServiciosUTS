# Sistema Integrado de Servicios Estudiantiles UTS

Aplicación Android nativa (Jetpack Compose + MVVM) que simula un portal de autoservicio para estudiantes de las Unidades Tecnológicas de Santander, con tres módulos: **Perfil académico**, **Matrícula de cursos libres** e **Historial financiero**.

## Cómo ejecutar

1. Abrir la carpeta `ServiciosUTS` en Android Studio.
2. Ejecutar la configuración `app` en un emulador o dispositivo (minSdk 24).
3. Pruebas unitarias: `./gradlew testDebugUnitTest`

## Arquitectura (MVVM estricta)

```
com.uts.serviciosestudiantiles
├── data/                      ← Modelos y fuente de datos
│   ├── Modelos.kt             Facultad, Curso (costos), PerfilEstudiante, Liquidacion
│   ├── AlmacenLocal.kt        Interfaz de persistencia (+ versión sin persistencia para pruebas)
│   ├── BaseDatosLocal.kt      Persistencia con SQLite (servicios_uts.db)
│   └── ServiciosRepository.kt Fuente única de verdad (StateFlow), guarda cada cambio en el almacén
├── viewmodel/                 ← Estado + reglas de negocio
│   ├── AjustesViewModel.kt    Modo claro/oscuro elegido por el usuario
│   ├── PerfilViewModel.kt     Validación y generación del correo institucional
│   ├── MatriculaViewModel.kt  Cálculo beca/Icetex y bloqueo de seguridad
│   └── HistorialViewModel.kt  Total acumulado en tiempo real
└── ui/                        ← Solo dibuja estado y envía eventos
    ├── AppPrincipal.kt        Scaffold + NavigationBar (3 secciones)
    ├── PerfilScreen.kt
    ├── MatriculaScreen.kt
    ├── HistorialScreen.kt
    ├── Componentes.kt         Selector desplegable, monograma, etiquetas, formato de pesos y fechas
    └── theme/Theme.kt
```

### Flujo de datos

```
 UI (Screen) ──evento──▶ ViewModel ──actualiza──▶ ServiciosRepository
     ▲                       │                           │
     └── collectAsStateWith- ┘◀──── StateFlow ───────────┘
         Lifecycle()
```

- Las tres pantallas comparten **un mismo repositorio**. Así, Matrícula sabe al instante si el perfil existe, e Historial recalcula el total en cuanto se agrega una liquidación.
- Cada ViewModel expone `private val _uiState = MutableStateFlow(...)` y `val uiState: StateFlow<...> = _uiState.asStateFlow()`. La vista nunca puede modificar el estado directamente.
- Los estados son `data class` inmutables y se actualizan con `copy()`, de modo que cada cambio emite un estado nuevo.

## Cumplimiento de requerimientos

| Requerimiento | Dónde |
|---|---|
| Paquetes `data`, `viewmodel`, `ui` | Estructura del proyecto |
| `MutableStateFlow` privado / `StateFlow` público | Los 3 ViewModels y el repositorio |
| `collectAsStateWithLifecycle()` en todas las pantallas | `PerfilScreen`, `MatriculaScreen`, `HistorialScreen`, `AppPrincipal` |
| `Card`, `Column`, `Row` | Todas las pantallas |
| `LazyColumn` | `HistorialScreen` (con `key` por número de liquidación) |
| `ExposedDropdownMenuBox` | `SelectorDesplegable` (Facultad y Curso) |
| `Checkbox` (beca) y `Switch` (Icetex) | `MatriculaScreen` |
| `OutlinedTextField` con validación | `PerfilScreen` (errores generados por el ViewModel) |

## Reglas de negocio

**Módulo 1: Perfil**
- Nombre: obligatorio, mínimo 3 letras, solo letras y espacios. Se guarda en formato título ("ana ruiz" → "Ana Ruiz").
- Código: solo dígitos (se filtran al escribir), entre 6 y 12.
- Facultad: Ingenierías, Ciencias Socioeconómicas o Ciencias Humanas.
- Correo: `primernombre.codigo@uts.edu.co`, en minúsculas y sin tildes (Iván → `ivan.1098765432@uts.edu.co`).

**Módulo 2: Matrícula**
- Cursos por facultad (en Matrícula solo aparecen los de la facultad del estudiante):
  - Ingenierías: Inglés Técnico $320.000, Programación Web $450.000, Ciberseguridad $580.000.
  - Ciencias Socioeconómicas: Excel Financiero $300.000, Marketing Digital $380.000, Emprendimiento e Innovación $350.000.
  - Ciencias Humanas: Oratoria y Expresión Oral $260.000, Fotografía Digital $420.000, Lengua de Señas Colombiana $340.000.
- Beca de Excelencia: −50 % del costo base.
- Icetex: +5 % sobre el valor ya descontado.
- Ejemplo: Ciberseguridad con beca e Icetex → 580.000 − 290.000 = 290.000 + 14.500 = **$304.500**.
- **Bloqueo de seguridad:** "Liquidar" solo se habilita si el perfil está guardado y hay un curso elegido. `liquidar()` vuelve a validarlo en el ViewModel, por si se llama sin pasar por el botón.
- **Terminar liquidación:** cierra el registro del estudiante (requiere al menos un curso liquidado). Sus liquidaciones quedan en el historial y el perfil se limpia para registrar a otro estudiante.
- Los montos se manejan como `Long` en pesos enteros para evitar errores de redondeo de `Double`.

**Módulo 3: Historial**
- Lista de estudiantes (el más reciente primero) con su total y los cursos que liquidó.
- Al tocar un estudiante se abre su detalle: carné con código, facultad y correo, su total, ahorro por beca, recargos Icetex y cada liquidación con su desglose y fecha (desde ahí se anula).
- Tarjeta destacada con el **Total Acumulado a Pagar**, recalculado por el ViewModel cada vez que cambia la lista del repositorio. También muestra el ahorro total por becas y los recargos Icetex.

## Funciones adicionales

- **Carné estudiantil digital** que se llena en vivo: el ViewModel calcula la vista previa del correo y las iniciales mientras el estudiante escribe.
- **Cambios sin guardar**: si se edita un perfil ya guardado, el botón pasa a "Guardar cambios".
- **Detalle del curso**: horas y modalidad.
- **Curso duplicado bloqueado**: un estudiante no puede liquidar dos veces el mismo curso, aunque vuelva a registrarse con su código.
- **Código protegido**: un código que ya está en el historial no puede registrarse con otro nombre o facultad.
- **Anular liquidación** con diálogo de confirmación; el total se recalcula y el curso queda disponible de nuevo.
- El número de liquidación es un consecutivo que nunca se repite, aunque se anulen liquidaciones (ni después de cerrar la app).
- **Persistencia local con SQLite** (`data/BaseDatosLocal.kt`, sin librerías extra): el estudiante activo, todas las liquidaciones y el consecutivo se guardan en `servicios_uts.db` y se recuperan al abrir la app. El repositorio depende de la interfaz `AlmacenLocal`, por eso las pruebas usan un almacén en memoria. Las escrituras van a un hilo de fondo, en orden.
- **Modo claro / oscuro / según el teléfono:** botón en la barra superior (`AjustesViewModel`). La elección se guarda en la base de datos y también ajusta los íconos de la barra de estado.
- **Solo vertical:** la pantalla está bloqueada en orientación vertical (`android:screenOrientation="portrait"` en el manifiesto).

## Pruebas

`app/src/test/.../ServiciosViewModelsTest.kt` (21 pruebas) cubre: generación del correo, las tres facultades, formato del nombre, validaciones, filtro del código, los 4 casos de cálculo (sin descuentos, beca, Icetex, ambos), el bloqueo sin perfil, la habilitación al guardar el perfil, el total acumulado, la vista previa del correo, los cambios sin guardar, el curso duplicado, la anulación, el consecutivo de liquidaciones, los cursos por facultad, terminar la liquidación, el historial agrupado por estudiante con su detalle, el código protegido la recuperación de los datos y del modo claro/oscuro al reabrir la app (con un almacén de prueba en lugar de SQLite).
