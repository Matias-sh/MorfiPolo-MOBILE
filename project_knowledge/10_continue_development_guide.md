# 10 - Continue Development Guide

## Objetivo
Continuar evolución sin romper comportamiento existente de login, votación, notificaciones y widget.

## Reglas operativas
1. Antes de cambiar código, revisar `project_knowledge` completo.
2. Identificar primero feature y superficies impactadas (UI, VM, repo, API, local, widget, alarmas).
3. Mantener arquitectura actual (MVVM pragmática + repos) salvo instrucción explícita de rediseño.
4. Evitar introducir reglas duplicadas; si se toca horario/voto, centralizar gradualmente.
5. No modificar contrato de sesión/auth sin validar `AuthManager`, `TokenManager`, `AuthInterceptor`, `SessionManager`.
6. Cambios en votos requieren validación cruzada en:
   - `DailyMenu*`,
   - `WeeklyMenu*`,
   - `VoteRepository`,
   - widget.
7. Cambios en notificaciones requieren validar:
   - permisos Android 13+,
   - alarmas 9:00/9:30/10:00,
   - reinicio dispositivo (`BootReceiver`).
8. Toda modificación relevante debe actualizar documentación en `project_knowledge`.

## Qué no tocar sin revisión previa
- `util/widget/*` (zona frágil).
- `data/remote/interceptor/AuthInterceptor.kt`.
- `TokenManager.kt` y `AuthManager.kt`.
- `AlarmReceiver.kt` y `AlarmScheduler.kt`.
- `VoteRepository.kt` (workarounds backend).

## Cómo agregar una nueva feature
1. Definir feature + flujo de usuario.
2. Ubicar capa principal (UI/VM/repo).
3. Reutilizar patrones existentes:
   - sealed ui state,
   - repos con `Result`,
   - manejo explícito de sesión/errores.
4. Integrar navegación en `navigation_graph.xml` si aplica.
5. Incluir estrategia de fallback/error y validación manual cross-feature.
6. Actualizar `04_features_map.md` y `12_change_impact_guide.md`.

## Cómo actualizar esta memoria
- Agregar evidencia concreta por archivo modificado.
- Reclasificar hallazgos (`CONFIRMADO`, `INFERIDO`, `HIPÓTESIS`) si la certeza cambia.
- Registrar contradicciones nuevas entre docs y código.
