# 01 - Project Overview

## Qué es el proyecto
- **CONFIRMADO POR CÓDIGO**: `MorfiPolo-MOBILE` es una app Android nativa (Kotlin + XML) para gestionar selección de menú/comida diaria y semanal, con votación de opciones por usuario autenticado.
- **Evidencia**: `app/src/main/java/com/cocido/morfipolo/ui/menu/daily`, `app/src/main/java/com/cocido/morfipolo/ui/menu/weekly`, `app/src/main/java/com/cocido/morfipolo/data/repository/VoteRepository.kt`, `app/src/main/res/navigation/navigation_graph.xml`.

## Propósito de negocio detectado
- **CONFIRMADO POR CÓDIGO**: permitir que una persona usuaria:
  - inicie sesión con DNI/contraseña,
  - vea menú del día y semanal,
  - elija o quite su voto en ventana horaria acotada,
  - reciba recordatorios por notificación y tenga acceso rápido por widget.
- **Evidencia**: `ui/login/LoginActivity.kt`, `ui/menu/daily/DailyMenuViewModel.kt`, `ui/menu/weekly/WeeklyMenuAdapter.kt`, `util/alarm/AlarmReceiver.kt`, `util/widget/MenuWidgetProvider.kt`.

## Alcance funcional actual
- **CONFIRMADO POR CÓDIGO**:
  - autenticación con refresh de token (`auth/login`, `auth/refresh-token`),
  - menú diario y semanal,
  - votación (`POST /votes`, `DELETE /votes/{id}`),
  - perfil y cambio de contraseña (`PATCH /user/change-password`),
  - notificaciones diarias y personalizadas,
  - widget interactivo para votar/quitar voto.
- **Evidencia**: `data/remote/api/MorfiPoloApiService.kt`, `ui/profile/ProfileViewModel.kt`, `ui/notifications/NotificationSettingsFragment.kt`.

## Stack tecnológico
- **CONFIRMADO POR CÓDIGO**:
  - Android SDK 36 / min 28, Kotlin 2.0.21, AGP 8.10.1.
  - UI: Activities/Fragments + ViewBinding + Navigation Component.
  - Estado: ViewModel + StateFlow.
  - Red: Retrofit + OkHttp + Moshi.
  - Persistencia: Room + SharedPreferences.
  - Background: WorkManager + AlarmManager + BroadcastReceiver.
- **Evidencia**: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MorfipoloApplication.kt`.

## Resumen ejecutivo técnico
- **CONFIRMADO POR CÓDIGO**: arquitectura real tipo MVVM pragmática con repositorios y un service locator manual en `MorfipoloApplication`.
- **CONFIRMADO POR CÓDIGO**: existe robustez operativa en sesión/notificaciones (refresh automático, reprogramación de alarmas en reinicio, fallback local de menús).
- **CONFIRMADO POR CÓDIGO**: hay deuda técnica en complejidad de votos/widget y en seguridad de sesión (password en SharedPreferences).
- **INFERIDO POR ESTRUCTURA**: el sistema evolucionó con parches incrementales para adaptarse a comportamientos no estables del backend.

## Estado general del proyecto
- **CONFIRMADO POR CÓDIGO**: producto funcional y desplegable (scripts de release, signing config), no greenfield.
- **CONFIRMADO POR CÓDIGO**: cobertura de tests prácticamente inexistente (solo tests ejemplo).
- **CONFIRMADO POR CÓDIGO**: documentación previa existe pero está parcialmente desactualizada respecto al build actual.
- **Estado estimado**: `estable para operación básica`, `frágil en puntos críticos` (votos, widget, coordinación de notificaciones).
