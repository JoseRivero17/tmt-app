# TMT Go — App móvil del circuito Tenis de Mesa para Todos

TPO de Desarrollo de Aplicaciones I (UADE, 2° cuatrimestre 2026, Jueves Noche).

**El problema.** Los jugadores del circuito [Tenis de Mesa para Todos](https://tenisdemesaparatodos.com)
consultan torneos, ranking y clubes en un sitio web pensado para computadora, lento en el celular y que
no funciona sin señal, algo común en clubes y en el viaje. No hay app ni API.

**La solución.** Una app Android que lee los datos públicos del sitio, los guarda en el teléfono y
los muestra al instante, con o sin conexión:

- **Torneos:** calendario con filtros, detalle e inscripción a una categoría.
- **Jugadores:** ranking por división, búsqueda, fichas y jugadores seguidos.
- **Clubes:** listado ordenado por cercanía y "Cómo llegar".
- **Mi TMT:** tu ficha y tus inscripciones.

## Integrantes

| Integrante | Rol | Responsabilidades |
|---|---|---|
| Lucas Lemes | UI / UX / Frontend | Diseño en Figma, interfaz y sus estados, requisitos no funcionales |
| Mateo Giarrocco | Casos de uso / Frontend / Testing | Modelos de dominio, casos de uso e interfaces de repositorio |
| Delfina Gómez Puegher | Persistencia / Offline first | Base local con Room, actualización de datos y preferencias |
| José Rivero | Networking / Scraping / Testing | Fuente remota, manejo de errores de red y plan de pruebas |

## Instrucciones de ejecución

**Requisitos:** Android Studio 2026.1 o posterior (el proyecto usa AGP 9.4.1 y el JDK que trae
Android Studio) y un emulador o teléfono con Android 8.0 (API 26) o superior. Se probó en un
emulador Pixel 9 Pro con API 37.

1. Clonar el repositorio y abrirlo con Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar la configuración `app`.

La primera vez la app necesita internet para bajar el calendario, el ranking y los clubes. Después
funciona sin conexión con lo guardado.

### Modo demo (el de siempre)

Inscribirse a un torneo requiere una cuenta del circuito, y probarlo contra el sitio inscribiría a
alguien de verdad. Por eso, por defecto, el **login y la inscripción son inventados**:

- Jugador demo: **código `99999`, clave `demo`**.
- Todo lo demás (calendario, torneos, ranking, fichas y clubes) se lee del sitio real.

### Con una cuenta real del circuito

```sh
./gradlew installDebug -PsitioReal=true
```

En Android Studio: *Settings → Build, Execution, Deployment → Gradle-Android Compiler →
Command-line Options* = `-PsitioReal=true`, y después *Run*. Así:

- El login y la sesión son los del sitio.
- Mi TMT muestra la ficha real del jugador.
- Las categorías de cada torneo y su habilitación salen del sitio.
- **El envío final de la inscripción se simula:** la app arma el pedido, pero no lo manda (ver
  *Limitaciones*).

La clave nunca se guarda (RNF06): la sesión vive solo en memoria.

### Tests

```sh
./gradlew testDebugUnitTest          # 183 tests unitarios (parsers, repositorios, casos de uso, ViewModels)
./gradlew connectedDebugAndroidTest  # Room en un dispositivo o emulador
```

Los parsers se prueban con páginas del sitio guardadas en `app/src/test/resources/html/`. Las páginas
con sesión están anonimizadas.

## Funcionalidades implementadas

**Torneos (RF01, RF02)**

- **Calendario:** los próximos torneos agrupados por día, con filtros combinables por división,
  localidad y rango de fechas.
- **Detalle del torneo:** sede, dirección, divisiones con horario y cupo, y estado (inscripción
  abierta, sin inscripción online o cancelado).
- **Inscripción:**
  - eligiendo una de las categorías habilitadas y aceptando las condiciones;
  - si falta la sesión, se pide el login y al volver se retoma la misma inscripción;
  - las categorías donde ya estás inscripto aparecen bloqueadas.

**Jugadores (RF03)**

- **Ranking:** por división, de 1ra a 8va.
- **Búsqueda:** por nombre o código desde 3 caracteres, sin distinguir tildes.
- **Ficha del jugador:** división, puesto, rating y club, con la opción de seguirlo.
- **Pestaña Seguidos:** tu perfil primero.

**Clubes (RF04)**

- **Orden por cercanía:** con una sola lectura de la ubicación aproximada, y la distancia a cada club.
- **Búsqueda** por localidad o nombre.
- **Detalle del club:** dirección y contacto, con "Cómo llegar" (abre la app de mapas) y "Llamar".

**Mi TMT (RF02, RF03)**

- **Tu ficha:** sale del jugador que inició sesión.
- **Inscripciones:** las hechas desde la app.
- **Cerrar sesión.**

**En toda la app**

- **Funciona sin conexión:** todo se lee de lo guardado en el teléfono. Cada pantalla dice cuándo se
  actualizó por última vez y avisa si no hay conexión.
- **Estados claros:** carga, vacío (con la acción para salir de él), error con reintentar, sin
  conexión y permiso rechazado.
- **Accesibilidad:** tema claro y oscuro con la identidad del circuito, textos escalables (probado al
  200 %), descripciones para TalkBack y áreas táctiles de 48 dp.
- **Pantalla:** solo vertical (RNF10) y splash con el logo.

## Requisitos funcionales alcanzados

| RF | Criterio de aceptación (preentrega) | Estado |
|---|---|---|
| **RF01** Calendario | 1. Los filtros combinados muestran solo los torneos que cumplen todo; sin coincidencias, estado vacío con "Limpiar filtros". | ✅ |
| | 2. Un torneo cancelado se marca "Cancelado" y no ofrece inscripción. | ✅ |
| | 3. Sin conexión se ve el último calendario con la fecha y hora de actualización. | ✅ |
| **RF02** Inscripción | 1. Solo en torneos no cancelados y solo con las categorías habilitadas. | ✅ Con cuenta real, las habilitadas y el motivo de las demás salen del sitio. |
| | 2. "Confirmada" solo si el sitio confirma; si rechaza, el motivo; la confirmada aparece en "Mis inscripciones". | ⚠️ El rechazo y "Mis inscripciones" funcionan. El envío al sitio está **simulado**: la app muestra "Lugar reservado" con la aclaración de que no se envió (ver *Limitaciones*). |
| | 3. Sin conexión, la acción se ve deshabilitada con un mensaje y no se encola. | ✅ |
| | 4. Con la sesión vencida, pide el login y retoma la inscripción del mismo torneo y categoría. | ✅ |
| **RF03** Ranking y fichas | 1. Una búsqueda de 3 o más caracteres devuelve coincidencias por nombre o código; si no hay, estado vacío. | ✅ |
| | 2. Las fichas de los seguidos se ven sin conexión e indican su última actualización. | ✅ |
| | 3. Tras un corte de ranking, la próxima sincronización con conexión refleja el nuevo puntaje y posición. | ✅ Se actualiza una vez por día por división; la ficha propia, también. |
| **RF04** Clubes | 1. Con el permiso, los clubes se ordenan por distancia y cada uno muestra la suya. | ✅ |
| | 2. Si se rechaza el permiso, no se bloquea: búsqueda por localidad y cómo habilitarlo. | ✅ |
| | 3. "Cómo llegar" abre la app de mapas con la dirección del club. | ✅ |

## Arquitectura final

MVVM con principios de Clean Architecture, en tres capas y con inyección de dependencias manual:

```
Compose (pantalla) → ViewModel (StateFlow de UiState) → caso de uso → repositorio (interfaz)
                                                                          ↓ implementación
                                                       Room (lo que se muestra) ← Sincronizador ← sitio (Retrofit + Jsoup)
```

| Paquete (`app/src/main/java/com/uade/tmt/`) | Capa | Responsabilidad |
|---|---|---|
| `presentation/<sección>` | Presentación | Pantallas Compose, `UiState` y ViewModel de torneos, reserva, ranking, clubes, perfil y login |
| `presentation/components` | Presentación | Estados compartidos: carga, vacío, error, sin conexión, última actualización |
| `presentation/navigation` | Presentación | Rutas tipadas y grafo: cada pestaña con su lista y su detalle anidados |
| `presentation/theme` | Presentación | Tema Material 3 con la paleta del Figma, claro y oscuro |
| `domain/model` | Dominio | Modelos sin dependencias de Android |
| `domain/repository` | Dominio | Interfaces de repositorio, conectividad y ubicación |
| `domain/usecase` | Dominio | Reglas de negocio: filtrar torneos, reservar lugar, iniciar y cerrar sesión, ordenar clubes por cercanía, buscar clubes |
| `data/local` | Datos | Room: torneos y categorías, jugadores, seguidos, clubes, inscripciones y fechas de sincronización |
| `data/remote` | Datos | Retrofit pide el HTML del sitio y un parser (Jsoup) por página lo convierte en modelos |
| `data/repository` | Datos | Implementaciones: leen de Room y sincronizan con el sitio |
| `data/platform` | Datos | Ubicación con el Fused Location Provider |
| `di` | Transversal | `AppContainer`: crea las dependencias una vez y elige entre el sitio real y el modo demo |

**Regla de dependencias:** `presentation` → `domain` ← `data`. El dominio no conoce Android, Room ni
el HTML: si el sitio cambia, solo se toca su parser en `data/remote` (RNF04).

**Offline first:**
- **Una sola fuente de verdad:** la interfaz siempre lee de Room. Al abrir una pantalla, su ViewModel
  le pide al repositorio que sincronice.
- **El `Sincronizador`** consulta el sitio solo si lo guardado venció (RNF05): calendario y torneo cada
  30 min, ranking y ficha propia cada día, clubes cada 7 días. "Deslizar para actualizar" fuerza el
  pedido.
- **Si el pedido falla,** lo guardado se sigue mostrando, con un aviso del motivo: sin conexión,
  timeout, error del sitio o página ilegible.

**Navegación:** Navigation Compose con rutas tipadas. Cada pestaña es un grafo anidado (lista →
detalle), así la pestaña queda marcada en el detalle y la lista conserva su estado. La reserva vive
dentro de Torneos para sobrevivir a la ida y vuelta al login.

## Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Kotlin 2.2, coroutines y Flow | Lenguaje, asincronía y estado reactivo |
| Jetpack Compose + Material 3 (BOM 2026.02) | Interfaz |
| Lifecycle ViewModel y Runtime Compose 2.11 | MVVM, `StateFlow`, recolección según el ciclo de vida |
| Navigation Compose 2.9 + kotlinx-serialization | Navegación con rutas tipadas |
| Room 2.8 (KSP) | Persistencia local con el esquema exportado en `app/schemas/` |
| Retrofit 3 + OkHttp | Pedidos HTTP al sitio (convertidor de texto), cookies de sesión en memoria y timeouts |
| Jsoup 1.21 | Lectura del HTML del sitio |
| Play Services Location 21.3 | Ubicación aproximada para ordenar clubes |
| Core SplashScreen 1.2 | Splash con el logo desde Android 8 |
| JUnit 4 + AndroidX Test | Tests unitarios e instrumentados |
| Gradle 9.6, AGP 9.4.1, compileSdk/targetSdk 37, minSdk 26 | Build |

## Decisiones relevantes

- **Backend: scraping del sitio, sin servidor propio.** El sitio no tiene API, y un servidor
  intermedio agregaba hosting, costo y otro punto de falla. La app lee el HTML con Retrofit y Jsoup.
  Cada página tiene su parser aislado y testeado con HTML real.
- **Offline first con Room como única fuente de verdad.** Los jugadores consultan en el club o en el
  viaje, muchas veces sin señal. Mostrar lo guardado al instante y actualizar por detrás da una app
  rápida (RNF03) que funciona igual sin conexión.
- **Cuidar el sitio.** Cada recurso tiene un intervalo mínimo entre pedidos (RNF05). La precarga se
  hace una división por vez y solo con la app a la vista (RNF12). No se piden las fichas excluidas
  por su `robots.txt`.
- **Ranking por división y precarga.** La página del ranking sin filtro tarda más que el timeout. Se
  pide por división, y al abrir la app se precargan los primeros 100 de cada una para poder buscar
  sin conexión.
- **Timeout de 45 s solo para el ranking**, como excepción a los 15 s de RNF03. El sitio tarda de 16
  a 24 s en armar algunas divisiones. Se carga de fondo mientras se ve lo guardado, así que no
  bloquea ninguna pantalla.
- **El perfil propio sale del login,** igual que en el sitio: no se elige a mano, así nadie se marca
  como otro jugador. Su ficha se baja por código, porque casi nunca está entre los primeros 100.
- **La inscripción es siempre online y no se encola** (RF02.3). Encolar una inscripción podría
  reservar un lugar horas después, sin que el jugador lo sepa.
- **Las inscripciones se registran en el teléfono.** El sitio no publica "mis inscripciones", así que
  la app guarda las que hace. El registro es de la cuenta con sesión: se vacía al cerrar sesión o al
  entrar con otra cuenta.
- **Credenciales (RNF06):** la clave no se guarda en disco ni se loguea. La sesión es una cookie en
  memoria y la clave viaja solo en el cuerpo del POST.
- **Ubicación mínima (RNF07):** permiso aproximado, pedido recién en Clubes y con explicación previa.
  Se hace una sola lectura y no se guarda; la distancia se calcula con las coordenadas guardadas.
- **Inyección de dependencias manual** (`AppContainer`), sin Hilt ni Koin. Para el tamaño del
  proyecto alcanza, se entiende sin anotaciones y permite cambiar el sitio real por el modo demo en
  un solo lugar.
- **Solo vertical** (RNF10). En apaisado las listas y Mi TMT quedaban con muy poco espacio útil.

## Limitaciones conocidas

- **El envío final de la inscripción está simulado.** Con una cuenta real, la app valida todo contra
  el sitio (sesión, categoría y habilitación por rating) y arma el pedido exacto
  (`jugadores_inscripciones_paso2.asp`), pero no lo manda.
  - No se pudo ver la respuesta del sitio sin hacer una inscripción real, y sin esa respuesta una
    inscripción hecha podría verse como error.
  - La pantalla lo aclara.
  - Para activarlo: `enviarAlSitio` en `SitioInscripcionRemoteDataSource`, después de implementar
    `InscripcionParser.resultadoReserva` (ver `docs/rf02-pendiente-cuenta.md`).
- **Falta validar el mensaje del sitio para una clave incorrecta.** Hoy cualquier mensaje de la
  pantalla de acceso se toma como credenciales inválidas.
- **Solo se guardan los primeros 100 jugadores de cada división** (más el perfil propio). La búsqueda
  devuelve hasta 100 resultados.
- **La ficha del sitio da la posición en el ranking general**, no dentro de la división. Para no
  mezclarlas, el perfil propio fuera de los 100 primeros se ve sin puesto.
- **No se muestran los últimos torneos de un jugador** (parte de la descripción de RF03): requieren
  otra página del sitio y quedaron fuera de esta entrega.
- **El detalle de un club que nunca se abrió no se ve sin conexión** (dirección y teléfono). El
  listado y la distancia sí.
- **No hay sincronización en segundo plano:** los datos se actualizan al usar la app.
  `work-runtime` está declarado pero no se usa.
- **El sitio puede cambiar su HTML.** Si pasa, el parser afectado informa "no pudimos leer el sitio"
  en lugar de mostrar datos incorrectos, y se corrige en `data/remote`.

## Forma de trabajo

- **Ramas:** `main` para las entregas, `develop` para integración y `feature/<nombre>` por tarea.
  Todo entra a `develop` con un pull request que dice qué cambia, cómo se probó y lleva capturas
  (en `docs/capturas/`).
- **Commits:** chicos, en español, con prefijo `feat:`, `fix:`, `refactor:`, `test:`, `docs:` o
  `chore:`.

## Uso de inteligencia artificial

Usamos asistentes de IA (Claude) para consultar, revisar código, diagnosticar errores y redactar
parte de la implementación y de los tests. Todo lo que entró al repositorio pasó por un pull request,
con tests y pruebas en el emulador. Los commits asistidos lo indican con `Co-Authored-By`.
