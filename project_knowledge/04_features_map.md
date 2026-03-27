# 04 - Features Map

## Criterio de estado
- `terminada`: flujo funcional completo y consistente.
- `parcial`: funciona, pero con limitaciones técnicas/deuda.
- `rota`: evidencia de no funcionamiento o cobertura inexistente.
- `incierta`: no hay evidencia suficiente en código ejecutado.

## Mapa de funcionalidades

### 1) Login y sesión
- **Propósito**: autenticar y habilitar acceso al resto de la app.
- **Estado**: `parcial`.
- **Pantallas**: `LoginActivity`.
- **Archivos**: `ui/login/LoginActivity.kt`, `ui/login/LoginViewModel.kt`, `data/repository/UserRepository.kt`, `data/remote/AuthManager.kt`, `data/remote/TokenManager.kt`.
- **Modelos/servicios**: `LoginRequest`, `LoginResponse`, `SessionManager`.
- **Riesgo al modificar**: alto (puede romper acceso general y refresh automático).
- **Clasificación**:
  - guardado de password en prefs: **CONFIRMADO POR CÓDIGO**.
  - flujo de refresh resiliente: **CONFIRMADO POR CÓDIGO**.

### 2) Menú diario y votación
- **Propósito**: ver menú por fecha y votar/quitar voto.
- **Estado**: `parcial` (usable, complejidad alta).
- **Pantallas**: `DailyMenuFragment`.
- **Archivos**: `ui/menu/daily/*`, `data/repository/MenuRepository.kt`, `data/repository/VoteRepository.kt`.
- **Modelos/servicios**: `Menu`, `MenuOption`, `Vote`.
- **Dependencias**: auth activa + API de votos/menús.
- **Riesgo**: alto por reglas de horario y sincronización con backend/widget.

### 3) Menú semanal
- **Propósito**: listar menús recientes y operar votos por ítem.
- **Estado**: `parcial`.
- **Pantallas**: `WeeklyMenuFragment`.
- **Archivos**: `ui/menu/weekly/*`, `VoteRepository.kt`, `MenuRepository.kt`.
- **Dependencias**: backend de votos (paginación/filtrado), sesión.
- **Riesgo**: alto por costo de consultas y workarounds de filtrado.

### 4) Perfil y cambio de contraseña
- **Propósito**: visualizar usuario, cambiar contraseña, cerrar sesión.
- **Estado**: `terminada`.
- **Pantallas**: `ProfileFragment`.
- **Archivos**: `ui/profile/*`, `UserRepository.kt`.
- **Dependencias**: token válido y endpoint `PATCH user/change-password`.
- **Riesgo**: medio (impacta credenciales; guarda nuevo password en prefs).

### 5) Notificaciones diarias (9:00, 9:30, 10:00)
- **Propósito**: recordar votación dentro de horario operativo.
- **Estado**: `parcial` (muy completa, pero compleja).
- **Pantallas**: configuración en `NotificationSettingsFragment`; ejecución por receiver/worker.
- **Archivos**: `util/alarm/*`, `util/notifications/NotificationHelper.kt`, `util/work/DailyReminderWorker.kt`.
- **Dependencias**: permisos OS, estado de menú, estado de voto.
- **Riesgo**: alto por coexistencia de AlarmManager + WorkManager.

### 6) Notificaciones personalizadas por día/hora
- **Propósito**: permitir agenda configurable de recordatorios.
- **Estado**: `terminada` funcional / `parcial` en mantenibilidad.
- **Pantallas**: `NotificationSettingsFragment`.
- **Archivos**: `ui/notifications/*`, `NotificationConfigRepository.kt`, `AlarmScheduler.kt`, `AlarmReceiver.kt`.
- **Persistencia**: SharedPreferences (JSON con Gson).
- **Riesgo**: medio-alto por calendario/timezone y reprogramación.

### 7) Widget interactivo de menú
- **Propósito**: mostrar y operar voto desde home screen.
- **Estado**: `parcial` (funciona con alto nivel de complejidad).
- **Archivos**: `util/widget/MenuWidgetProvider.kt`, `util/widget/MenuWidgetService.kt`, `res/layout/widget_*`.
- **Dependencias**: sesión, API de menús/votos, broadcast/update widget.
- **Riesgo**: muy alto (código extenso, múltiples paths de intent, sensibilidad a cambios).

### 8) Sync background de menú por polling
- **Propósito**: detectar menú nuevo y notificar.
- **Estado**: `incierta`.
- **Archivos**: `util/work/MenuPollingWorker.kt`.
- **Hallazgo**: no se detecta bootstrap explícito del worker en startup.
- **Clasificación**: `worker existe` **CONFIRMADO POR CÓDIGO**; `se ejecuta en runtime real` **HIPÓTESIS DÉBIL / NO CONFIRMADA**.

### 9) Testing automatizado
- **Propósito**: asegurar regresiones.
- **Estado**: `rota/insuficiente`.
- **Archivos**: `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`.
- **Clasificación**: **CONFIRMADO POR CÓDIGO**.
