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

## RF04 — Clubes cercanos

| Qué | Cómo |
|---|---|
| Datos | `clubes_mapaTodos.asp` trae los ~210 clubes con sus coordenadas en un solo pedido (`ClubesParser`). La ficha `clubes_ampliar.asp` suma dirección y teléfono al abrir un club (`ClubParser`). Todo se guarda en Room y se actualiza cada 7 días (RNF05). |
| Ubicación | Permiso `ACCESS_COARSE_LOCATION` pedido recién al entrar a Clubes, con explicación previa. Una sola lectura con el Fused Location Provider; no se guarda (RNF07, RNF12). |
| Distancia | Haversine en el dominio (`OrdenarClubesPorCercania`), con las coordenadas guardadas: funciona sin conexión. |
| Permiso rechazado | La función sigue con la búsqueda por localidad o nombre (`BuscarClubes`) y un acceso a Ajustes. |
| Cómo llegar | Intent `geo:` con las coordenadas del club; si no hay app de mapas, Google Maps en el navegador. "Llamar" usa `ACTION_DIAL` (sin permisos). |

**Fuera de alcance (decisión):** clubes precargados en la app, mapa embebido, filtro "Abiertos hoy",
WhatsApp, compartir, horarios, mesas y próximos torneos del club. Sin conexión y sin una
sincronización previa se muestra el estado "Sin conexión" con reintentar.

## Ejecución

1. Clonar el repo y abrirlo con Android Studio.
2. Esperar la sincronización de Gradle.
3. Ejecutar la configuración `app` en un emulador o dispositivo con Android 8.0 o superior.
