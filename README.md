# TMT — App móvil del circuito Tenis de Mesa para Todos

TPO de Desarrollo de Aplicaciones I (UADE, 2° cuatrimestre 2026).

App Android para el jugador del circuito [Tenis de Mesa para Todos](https://tenisdemesaparatodos.com):
calendario de torneos, reserva de lugar, ranking y jugadores, y clubes cercanos,
con consulta sin conexión.

## Integrantes

| Integrante | Rol |
|---|---|
| José Rivero | _completar_ |
| _completar_ | _completar_ |

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
