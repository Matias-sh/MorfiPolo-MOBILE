# 12 - Change Impact Guide

## Matriz de impacto por tipo de cambio

### Navegación / pantallas
- Toca: `navigation_graph.xml`, `MainActivity`, fragments, `bottom_nav_menu.xml`.
- Impacto esperado: rutas rotas, argumentos mal mapeados, tabs inconsistentes.
- Validar: login->main, weekly->daily(menuDate), profile->notification settings.

### Auth / sesión / tokens
- Toca: `SessionManager`, `TokenManager`, `AuthManager`, `AuthInterceptor`, `UserRepository`.
- Impacto esperado: logout forzado, loops de refresh, 401 no manejados.
- Validar: inicio app con sesión previa, error temporal backend, expiración real de refresh token.

### Menús / votos / horario
- Toca: `MenuRepository`, `VoteRepository`, `DailyMenuViewModel`, `WeeklyMenuViewModel`, adapters, widget.
- Impacto esperado: voto duplicado, estado inconsistente entre vistas, degradación de performance.
- Validar: crear/quitar voto en daily, weekly y widget para mismo menú.

### Datos locales (Room/Prefs)
- Toca: entidades, DAOs, migraciones, claves de prefs.
- Impacto esperado: crashes por schema, pérdida de datos, defaults incorrectos.
- Validar: upgrade de versión, fallback offline, lectura de sesión/notificaciones.

### Notificaciones y alarmas
- Toca: `AlarmScheduler`, `AlarmReceiver`, `NotificationHelper`, `AlarmPreferences`, `DailyReminderWorker`.
- Impacto esperado: notificaciones duplicadas o ausentes, programación errónea por timezone/boot.
- Validar: alarmas 9:00/9:30/10:00, Android 13+ permisos, reboot del dispositivo.

### Widget
- Toca: `MenuWidgetProvider`, `MenuWidgetService`, layouts `widget_*`.
- Impacto esperado: botones sin acción, estado stale, crashes de RemoteViews.
- Validar: render inicial, login/logout, elegir/quitar opción, refresh al volver de background.

### Build / release
- Toca: `app/build.gradle.kts`, scripts release, keystore config, ProGuard.
- Impacto esperado: build release fallida, firma inválida, comportamiento distinto debug/release.
- Validar: `bundleRelease`, instalación release, flujo crítico auth+voto.

## Checklist de seguridad antes de editar cualquier feature
1. Identifiqué feature y dependencias cruzadas (`04_features_map.md`).
2. Revisé riesgos existentes (`09_technical_debt_and_risks.md`).
3. Definí pruebas manuales mínimas cross-feature.
4. Verifiqué si toca auth, votos, widget o alarmas (zonas críticas).
5. Si hay contexto nuevo, actualizo primero `project_knowledge`.
6. Confirmo que no introduzco secretos ni hardcodes sensibles.
7. Si cambio contrato API, documento diff teoría/práctica en `07_api_integrations.md`.
