# 06 - Data Models And State

## Modelos principales
- **API/domain models** (`domain/model`):
  - `User`, `Menu`, `MenuOption`, `Vote`.
  - `MenusResponse`, `VotesResponse`, `Meta`, `Links`.
  - Requests: `LoginRequest`, `RefreshTokenRequest`, `CreateVoteRequest`, `ChangePasswordRequest`.
- **Modelos de UI/estado**:
  - `DailyMenuUiState`, `WeeklyMenuUiState`, `ProfileUiState`, `PasswordChangeState`, `NotificationSettingsUiState`.
- **Modelos locales**:
  - Room: `UserEntity`, `MenuEntity`, `MenuSelectionEntity`.
  - Preferencias: sesión/tokens (`SessionManager`), alarmas (`AlarmPreferences`), notificaciones custom (`CustomNotification` serializado con Gson).

## DTOs, entities y mapeos
- **CONFIRMADO POR CÓDIGO**:
  - `MenuRepository` mapea `Menu` <-> `MenuEntity` manualmente.
  - `UserRepository` reconstruye `User` desde `UserEntity` con campos faltantes vacíos.
- **INFERIDO POR ESTRUCTURA**:
  - no existe capa mapper dedicada; el mapeo está embebido en repositorios.

## State holders / controllers
- `ViewModel + MutableStateFlow` en todas las pantallas principales.
- Eventos de sesión expirada modelados por `StateFlow<Boolean>` en daily/weekly.
- Caches puntuales en ViewModel (ej. mapa de votos en `WeeklyMenuViewModel`).

## Flujo de datos resumido
- UI dispara acción -> ViewModel orquesta -> Repository consulta remoto/local -> ViewModel publica estado -> Fragment renderiza.
- Datos cross-feature:
  - `MENU_UPDATED` broadcast sincroniza daily/weekly y widget.
  - Session/token state afecta toda llamada protegida.

## Inconsistencias de modelo detectadas
- **CONFIRMADO POR CÓDIGO**:
  - `MenuSelection` (dominio) usa `Long`; persistencia real de `MenuSelectionEntity` usa `String`.
  - `MenuSelectionDao` y entidad existen, pero no son fuente principal de verdad para votos actuales.
- **Clasificación**:
  - inconsistencia de tipos: **CONFIRMADO POR CÓDIGO**.
  - uso residual/no operativo: **INFERIDO POR ESTRUCTURA**.

## Gestión de estado de carga/error/éxito
- Estados sellados por pantalla.
- Mensajes de error normalizados en repositorios y reinterpretados en UI.
- Manejo de retry manual por pull-to-refresh y snackbars con acción.
