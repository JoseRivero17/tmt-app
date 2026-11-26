# RF02 — Lo que falta para usar el sitio real

El acceso y la reserva funcionan de punta a punta con fuentes falsas (`BuildConfig.SITIO_REAL = false`).
Para pasar al sitio real faltan páginas que solo se ven con una cuenta de jugador. Todo lo pendiente está
marcado en el código con `TODO(cuenta)`:

```sh
grep -rn "TODO(cuenta)" app/src
```

## Estado (26/11)

Con la cuenta de Lucas se vieron, **sin enviar ninguna inscripción**, la barra con sesión y la página de
categorías de un torneo. Quedaron anonimizadas en `loginExitoso.html` e `inscripcionFormulario.html`.

| Parte | Estado |
|---|---|
| Login exitoso y nombre del jugador | Hecho: con sesión la barra dice "Bienvenido <nombre> \| Acceder a mi panel de control \| Salir". |
| Cerrar sesión en el sitio | Hecho: `salir.asp`. |
| Categorías, horarios y motivo de las no habilitadas | Hecho: `InscripcionParser.opciones`. |
| Mensaje de clave incorrecta | Falta `loginClaveIncorrecta.html` (hoy cualquier otro mensaje es `CredencialesInvalidas`). |
| Envío de la inscripción | Visto sin enviarlo. "Inscribirme" abre `jugadores_inscripcionesForm.asp?index=<fila>`: copia los campos ocultos de la fila y pide dos casillas (compartir datos con el organizador y deslinde de responsabilidad). El POST va a `jugadores_inscripciones_paso2.asp` con esos campos más `consentimiento=1`, `chkResp=on` y `chkDDJJ=on`. La app arma esos campos (`InscripcionParser.camposEnvio`), pero **no los envía**: con `enviarAlSitio = false` da la inscripción por reservada y la pantalla aclara que fue simulada. |
| Respuesta al envío | Falta: requiere una inscripción real. Con ella, implementar `InscripcionParser.resultadoReserva` y recién ahí pasar `enviarAlSitio` a `true`. |

## 1. Páginas a guardar

Guardarlas desde el navegador con **Ctrl+S → "Página web, solo HTML"** (no "completa": no hacen falta
imágenes ni scripts). Van en `app/src/test/resources/html/`. Conviene abrir DevTools → *Network* con
*Preserve log* activado para anotar también la URL, el método y los campos de cada POST.

| Archivo a crear | Cómo obtenerla | Para qué sirve |
|---|---|---|
| `loginExitoso.html` | Ingresar con código y clave correctos desde el encabezado. Guardar la página a la que redirige (`inicio.asp`). | Ver cómo cambia el encabezado con sesión y de dónde leer el nombre del jugador. |
| `loginClaveIncorrecta.html` | Ingresar con el código correcto y una clave incorrecta. Guardar la respuesta. | Confirmar el mensaje real de la caja amarilla de "ACCESO DE JUGADORES". |
| `inscripcionFormulario.html` | Con sesión, abrir `jugadores_inscripciones.asp?torneo_cod=<torneo con inscripción abierta>`. | Categorías, horarios, opciones deshabilitadas y su motivo; `action`, método y campos del `<form>`. Si se puede, guardar también uno con alguna categoría no habilitada por rating. |
| `inscripcionRespuesta.html` | Enviar el formulario y guardar la página que llega tras el POST. | Distinguir lugar confirmado, lista de espera y rechazo. Idealmente una variante de cada uno (`inscripcionRespuestaEspera.html`, `inscripcionRespuestaRechazo.html`). |

Anotar aparte (en el PR, no en el HTML): la URL y los campos del POST de inscripción, y si el sitio tiene una
URL para cerrar sesión. **Ojo:** enviar el formulario hace una inscripción real; elegir un torneo en el que
el jugador realmente quiera anotarse o que se pueda dar de baja.

## 2. Cómo anonimizarlas

Antes de agregarlas al repo (las muestras actuales usan "misma estructura que el sitio, con nombres inventados"):

1. Reemplazar nombre, apellido, código de jugador, DNI, mail, teléfono y rating por valores inventados
   (por ejemplo `Jugador Demo`, código `99999`). Buscar también en atributos `value=` y en URLs.
2. Borrar cualquier `value=` de campos `clave`/`password` y cualquier token de sesión, `ASPSESSIONID…`,
   o parámetro oculto que identifique la sesión.
3. Quitar `<script>` de analítica o de terceros que no hagan falta para la estructura.
4. **No tocar** la estructura: tablas, clases (`TituloSecciones`), `bgcolor`, `name` de los inputs ni el
   `action` de los formularios. El parser depende de eso.
5. Revisar el diff completo (`git diff --cached`) antes de commitear: ningún dato real debe quedar.

## 3. Qué completar en cada archivo

### Acceso (`data/remote`)

- `parser/AccesoParser.kt`
  - `BARRA_SUPERIOR`: validar con `loginExitoso.html` que la barra sigue presente con sesión.
  - `parsear`, regla de éxito: reemplazar la regla conservadora por una basada en algo que solo aparezca
    con sesión (p. ej. un "Salir" o el nombre en el encabezado) y leer el `nombre`.
  - `pantallaDeAcceso`: con `loginClaveIncorrecta.html`, reconocer el mensaje real y dejar
    `CredencialesInvalidas` solo para él (lo desconocido debería pasar a `LecturaException`).
- `SitioAccesoRemoteDataSource.kt` → `cerrarSesion`: si el sitio tiene URL de salida, agregarla a
  `AccesoService` y llamarla antes de `cookies.limpiar()`.
- `data/repository/SesionRepositoryImpl.kt` → revisar si `SesionRequerida`/`SinSesion` tras el POST
  puede ser en realidad una clave incorrecta.

### Inscripción

- `data/remote/parser/InscripcionParser.kt`
  - `opciones(html)`: con `inscripcionFormulario.html`, devolver `ResultadoOpciones.Disponibles` con
    una `OpcionInscripcion` por categoría (`categoria`, `horario`, `habilitada`, `motivo`). Si la
    estructura no es la esperada, lanzar `LecturaException` (el repositorio la traduce a `ErrorSitio`).
  - `resultadoReserva(html)`: con `inscripcionRespuesta*.html`, devolver `Reservada`, `ListaEspera` o
    `Rechazada(motivo)`; una página de acceso → `SesionRequerida` (usar `pideSesion`).
  - Revisar `pideSesion` con `inscripcionFormulario.html`: debe dar `false`.
- `data/remote/InscripcionService.kt`: agregar el `@FormUrlEncoded @POST` con el `action` y los campos
  reales del formulario.
- `data/remote/SitioInscripcionRemoteDataSource.kt` → `enviarFormulario`: leer del `formulario` los
  campos ocultos y llamar al POST nuevo. Queda dentro del `try/catch` de `enviarReserva`, que traduce
  una `IOException` durante el envío a `ResultadoReserva.Incierta` y pasa la respuesta a
  `InscripcionParser.resultadoReserva`. **No reintentar el POST automáticamente.**

### Mitigación futura: verificar una reserva `Incierta`

`Incierta` significa que el POST salió pero la respuesta no llegó: el sitio pudo haber registrado la
inscripción. Hoy no se guarda ni se reintenta. Para resolverla hace falta saber cómo muestra el sitio a un
jugador que ya está inscripto. Con la cuenta:

1. Inscribirse en un torneo (ver la advertencia de la sección 1) y, ya inscripto, volver a abrir
   `jugadores_inscripciones.asp?torneo_cod=<ese torneo>`. Guardarla como
   `inscripcionYaInscripto.html` (anonimizada, sección 2).
2. Compararla con `inscripcionFormulario.html`: buscar qué marca la inscripción existente (un mensaje
   "ya estás inscripto", la categoría marcada o deshabilitada, un botón de baja, un listado de inscriptos
   con el jugador). Anotar también si aparece la lista de espera.
3. Revisar si el sitio tiene otra página con las inscripciones del jugador o los inscriptos del torneo
   (`torneos_ampliar.asp` u otra) y guardarla si existe.
4. Con eso, agregar a `InscripcionParser` una función que, dado el formulario, diga si el jugador ya
   figura en una categoría (y en qué estado). Después, ante `Incierta`, la UI puede ofrecer
   "Verificar" que vuelva a pedir el formulario (un GET, nunca el POST) y convierta el resultado en
   `Reservada` / `ListaEspera` (y entonces guardarlo) o en "no figura".

### Tests a agregar

- `AccesoParserTest`: `loginExitoso.html` → `Exito(nombre = "Jugador Demo")`;
  `loginClaveIncorrecta.html` → `CredencialesInvalidas`.
- `InscripcionParserTest`: opciones de `inscripcionFormulario.html`; un test por cada respuesta.
- `SitioInscripcionRemoteDataSourceTest`: un `InscripcionService` falso que devuelva el formulario y la
  respuesta, verificando los campos que se envían en el POST.
- Cambiar el test `con sesion, mientras falte el formulario real, es error del sitio`, que dejará de valer.

## 4. Correr los tests

Los tests unitarios nunca hacen requests: usan el HTML de `test/resources/html` y fuentes falsas (RNF11).

```sh
./gradlew :app:testDebugUnitTest
# solo RF02
./gradlew :app:testDebugUnitTest --tests "*Inscripcion*" --tests "*Reserva*" --tests "*Acceso*" --tests "*Sesion*"
```

Informe: `app/build/reports/tests/testDebugUnitTest/index.html`.

## 5. Pasar a `SITIO_REAL = true`

Para probar con una cuenta real sin cambiar ningún archivo:

```sh
./gradlew installDebug -PsitioReal=true
```

(En Android Studio: `sitioReal=true` en el `gradle.properties` del usuario, no en el del proyecto.)
Hoy funcionan el login, Mi TMT con la ficha propia y las categorías de cada torneo; reservar termina en
`ErrorSitio` hasta que se resuelva el envío. Dejarlo fijo en `true` recién cuando los `TODO(cuenta)` estén
resueltos y sus tests pasen.

`DefaultAppContainer` elige con ese flag `SitioAccesoRemoteDataSource` + `SitioInscripcionRemoteDataSource`
(comparten el `CookieJarEnMemoria`) en lugar de las falsas. Para probar el real solo en debug se puede mover
el campo a `buildTypes { debug { … } }` y dejar `false` en `defaultConfig`.

Prueba manual mínima: login correcto, login con clave incorrecta, ver opciones de un torneo, reservar,
y con el modo avión activado verificar que la reserva diga "sin conexión" y no quede registrada.
