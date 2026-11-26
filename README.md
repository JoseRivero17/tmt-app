# TMT — App móvil del circuito Tenis de Mesa para Todos

TPO de Desarrollo de Aplicaciones I (UADE, 2° cuatrimestre 2026).

App Android para el jugador del circuito [Tenis de Mesa para Todos](https://tenisdemesaparatodos.com):
calendario de torneos, reserva de lugar, ranking y jugadores, y clubes cercanos,
con consulta sin conexión.

## Integrantes

| Integrante | Rol | Responsabilidades |
|---|---|---|
| Lucas Lemes | UI / UX / Frontend | Diseño en Figma, interfaz y sus estados, requisitos no funcionales |
| Mateo Giarrocco | Casos de uso / Frontend / Testing | Modelos de dominio, casos de uso e interfaces de repositorio |
| Delfina Gómez Puegher | Persistencia / Offline first | Base local con Room, actualización de datos y preferencias |
| José Rivero | Networking / Scraping / Testing | Fuente remota, manejo de errores de red y plan de pruebas |

## Stack

- Kotlin + Jetpack Compose (Material 3)
- minSdk 26 (Android 8.0) — RNF10
- Arquitectura MVVM + principios de Clean Architecture

## Arquitectura

```
UI (Compose) → ViewModel → Use Case → Repository (interfaz) → Local / Remote Data Source
```

Estructura de paquetes (`app/src/main/java/com/uade/tmt/`):

| Paquete | Capa | Responsabilidad |
|---|---|---|
| `presentation/theme` | Presentación | Tema Material 3, colores y tipografía (modo claro/oscuro) |
| `presentation/navigation` | Presentación | Grafo de navegación y barra inferior |
| `presentation/components` | Presentación | Componentes compartidos de estado: carga, vacío, error, sin conexión, última actualización |
| `presentation/<feature>` | Presentación | Pantallas Compose, `UiState` y ViewModel de cada flujo |
| `domain/model` | Dominio | Modelos de dominio (sin dependencias de Android) |
| `domain/repository` | Dominio | Interfaces de repositorio |
| `domain/usecase` | Dominio | Casos de uso y reglas de negocio |
| `data/local` | Datos | Fuente local (Room / DataStore) |
| `data/remote` | Datos | Fuente remota: extracción del HTML del sitio (RNF04) |
| `data/mapper` | Datos | Conversión entre DTOs, entidades y modelos de dominio |
| `data/repository` | Datos | Implementaciones de los repositorios (estrategia offline first) |
| `di` | Transversal | Inyección de dependencias manual — `AppContainer` |

Regla de dependencias: `presentation` → `domain` ← `data`. El dominio no conoce Android,
ni Room, ni el parser, así que un cambio en el HTML del sitio solo afecta a `data/remote`.

## Forma de trabajo

**Ramas**

- `main`: versiones estables (preentrega, entrega final).
- `develop`: integración. Toda feature se mergea acá con un pull request.
- `feature/<nombre>`: una rama por funcionalidad o tarea (ej. `feature/calendario-torneos`).

**Commits**: commits chicos, en español, con prefijo de tipo:
`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`.

**Pull requests**: de `feature/*` a `develop`, con descripción de qué cambia y cómo se probó.
`develop` se mergea a `main` en cada entrega.

## Ejecución

1. Clonar el repo y abrirlo con Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar la configuración `app` en un emulador o dispositivo con Android 8.0 o superior.

## RF03 – Ranking, búsqueda y seguidos

- Ranking por división (1ra a 8va) y búsqueda por nombre o código desde 3 caracteres, sin distinguir tildes ni mayúsculas.
- Ficha del jugador con división, puesto, rating y club. Desde la ficha se puede seguir a un jugador.
- Perfil propio: como en el sitio, es el del jugador que inició sesión (RF02); no se elige a mano. Su ficha se baja por código, así que aparece aunque no esté entre los primeros 100 de su división, y se ve en Mi TMT y primera en "Seguidos". Al cerrar sesión se quita.
- Pestaña "Seguidos": muestra los jugadores seguidos y el perfil propio con la fecha de su última actualización. Sale de lo guardado, así que funciona sin conexión.

**Limitaciones y decisiones**

- Solo se encuentran los primeros 100 jugadores de cada división (más el perfil propio), y la búsqueda muestra hasta 100 resultados.
- La ficha del sitio publica la posición en el ranking general, no dentro de la división: no se mezcla con el puesto por división del ranking, así que el perfil propio fuera de los 100 primeros se ve sin puesto.
- En modo demo (`SITIO_REAL = false`) el login es simulado, pero el código 12345 existe en el sitio: Mi TMT muestra la ficha pública de ese jugador.
- El ranking se pide siempre por división: la página sin filtro tarda más que el timeout de 15 s.
- El club se ve abreviado hasta que esté RF04.
- No se muestran los últimos torneos del jugador: requieren leer otra página del sitio y quedaron fuera de esta entrega.
- No se guardan edad ni fecha de nacimiento, y no se piden las fichas excluidas por el robots.txt.