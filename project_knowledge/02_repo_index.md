# 02 - Repo Index

## Mapa del repositorio
- `app/`: módulo Android principal (único módulo incluido en build).
- `gradle/`: wrapper y catálogo de dependencias.
- Archivos raíz de build/scripts/docs: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `build-release.ps1`, `build-release.sh`, `INSTRUCCIONES_PRODUCCION.md`, `PRODUCCION_CHECKLIST.md`, `ANALISIS_FUNCIONALIDADES_FALTANTES.md`.

## Estructura funcional principal
- **App y entrada**
  - `app/src/main/AndroidManifest.xml`: permisos, activities, receivers, service, widget provider.
  - `app/src/main/java/com/cocido/morfipolo/MorfipoloApplication.kt`: wiring global (DB, repos, auth, workers).
  - `app/src/main/java/com/cocido/morfipolo/ui/login/LoginActivity.kt`: launcher (`MAIN/LAUNCHER`).
  - `app/src/main/java/com/cocido/morfipolo/ui/main/MainActivity.kt`: host de navegación.

- **UI**
  - `ui/login`: login.
  - `ui/menu/daily`: menú diario + votación.
  - `ui/menu/weekly`: listado semanal + acciones por opción.
  - `ui/profile`: perfil, cambio de contraseña, logout.
  - `ui/notifications`: configuración de notificaciones personalizadas.

- **Data**
  - `data/remote`: RetrofitClient, TokenManager, AuthManager, interceptor.
  - `data/repository`: User/Menu/Vote/NotificationConfig.
  - `data/local/database`: Room DB + DAO + entidades + migraciones.
  - `data/local/preferences`: sesión en SharedPreferences.

- **Dominio**
  - `domain/model`: DTOs/modelos de API y algunos modelos locales.

- **Infra utilitaria**
  - `util/alarm`: scheduler + receivers + flags de notificación.
  - `util/work`: workers de recordatorio, refresh de sesión y polling.
  - `util/widget`: provider + service/factory de widget interactivo.
  - `util/notifications`: helper para creación/envío de notificaciones.

## Entry points reales
- **CONFIRMADO POR CÓDIGO**:
  - App entry: `LoginActivity`.
  - Navigation entry interna: `dailyMenuFragment` en `navigation_graph.xml`.
  - Servicios de background arrancados en `MorfipoloApplication.onCreate()`.
  - Receivers activos: `AlarmReceiver`, `BootReceiver`, `MenuWidgetProvider`.

## Configuración y build
- `settings.gradle.kts`: solo `:app`.
- `app/build.gradle.kts`: compile/min/target SDK, signing, build types, deps.
- `gradle/libs.versions.toml`: versiones centralizadas.
- `keystore.properties`: configuración de firma release (archivo sensible).

## Recursos y estilos
- Layouts críticos:
  - `layout/activity_login.xml`, `layout/activity_main.xml`,
  - `layout/fragment_daily_menu.xml`, `layout/fragment_weekly_menu.xml`,
  - `layout/fragment_profile.xml`, `layout/fragment_notification_settings.xml`.
- Navegación:
  - `navigation/navigation_graph.xml`,
  - `menu/bottom_nav_menu.xml`.
- Strings y temas:
  - `values/strings.xml`, `values/themes.xml`, `values/colors.xml`.

## Testing y calidad
- `app/src/test/.../ExampleUnitTest.kt` y `app/src/androidTest/.../ExampleInstrumentedTest.kt`.
- **CONFIRMADO POR CÓDIGO**: no hay suite real de pruebas de negocio.

## Zonas importantes y trazabilidad rápida
- **Auth/sesión**: `AuthManager.kt` -> `TokenManager.kt` -> `AuthInterceptor.kt` -> `SessionManager.kt`.
- **Menús**: `MenuRepository.kt` + `MenuDao.kt`.
- **Votos**: `VoteRepository.kt` + `MorfiPoloApiService.kt`.
- **Notificaciones**: `AlarmScheduler.kt` + `AlarmReceiver.kt` + `NotificationHelper.kt`.
- **Widget**: `MenuWidgetProvider.kt` + `MenuWidgetService.kt`.

## Archivos/zonas sospechosas o especiales
- `keystore.properties`: contiene datos sensibles de firma.
- `.gitignore` incluye rastros de stack Django/Python no presente en el runtime Android.
- `util/work/MenuPollingWorker.kt`: implementado pero sin bootstrap visible en app startup.
- Documentación previa con contradicciones de versión frente al build actual.
