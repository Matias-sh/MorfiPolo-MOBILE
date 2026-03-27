# 05 - Navigation And Flows

## Mapa de pantallas y rutas
- **Launcher**: `LoginActivity`.
- **Host principal**: `MainActivity` con `NavHostFragment`.
- **Graph**: `navigation_graph.xml`.
  - `dailyMenuFragment` (startDestination interno).
  - `weeklyMenuFragment`.
  - `profileFragment`.
  - `notificationSettingsFragment`.

## Navegación principal
- Bottom nav (`bottom_nav_menu.xml`) conecta `daily`, `weekly`, `profile`.
- Desde `profile` -> `notificationSettings` mediante action explícita.
- Desde `weekly` -> `daily` pasando `menuDate`.
- Login exitoso -> `MainActivity` con flags de limpieza de stack.

## Flujo principal de usuario
1. Abrir app -> `LoginActivity`.
2. `AuthManager.verifyAndRefreshAuth()`:
   - si sesión válida, salta a `MainActivity`;
   - si no, permanece en login.
3. En `MainActivity`, tab por defecto: menú diario.
4. Usuario vota/quita voto (si estado y horario lo permiten).
5. Opcional: revisa menú semanal, perfil o notificaciones.

## Flujos alternativos
- **Sesión expirada**: ViewModels/fragments detectan y redirigen a login.
- **Sin menú diario**: Daily muestra estado visual "No hay menú disponible".
- **Sin red**: fallback parcial local + mensajes de error/snackbar.
- **Voto desde otros canales** (web/widget): app emite/recibe broadcast `MENU_UPDATED` para refrescar.

## Estados relevantes de usuario
- `No autenticado`.
- `Autenticado con token válido`.
  - `TemporalError` de backend/red (sesión local aún aceptada).
  - `RefreshFailed`/`NotLoggedIn` (redirigir login).
- `Con voto` vs `sin voto` por menú.
- `Dentro de horario` (08:00-11:00, menú de hoy abierto) vs `fuera de horario`.

## Restricciones de negocio detectadas en navegación/flujo
- Horario operativo de voto fijado en código (08:00-11:00).
- La app prioriza no romper experiencia en errores temporales de refresh.
- Menús con `status=draft` no se presentan al usuario final.

## Certeza por bloques
- **CONFIRMADO POR CÓDIGO**: rutas, acciones, start destinations, redirecciones y broadcasts.
- **INFERIDO POR ESTRUCTURA**: el flujo web/app conviviente motivó la estrategia de sincronización por broadcast.
