# 09 - Technical Debt And Risks

## Riesgos críticos (alta prioridad)

### R1) Contrato de votos inconsistente
- **CONFIRMADO POR CÓDIGO**:
  - `VoteRepository` implementa fallback paginado y filtrado local por usuario.
  - usa `user_id` y `userId` en paralelo.
- **Impacto**: latencia, consumo de red, resultados ambiguos ante cambios de backend.
- **Archivos**: `data/repository/VoteRepository.kt`, `ui/menu/weekly/WeeklyMenuViewModel.kt`.

### R2) Password en almacenamiento local de sesión
- **CONFIRMADO POR CÓDIGO**: `SessionManager` guarda `KEY_USER_PASSWORD`.
- **Impacto**: riesgo de seguridad y compliance.
- **Archivo**: `data/local/preferences/SessionManager.kt`.

### R3) Widget con complejidad extrema
- **CONFIRMADO POR CÓDIGO**: `MenuWidgetProvider` y `MenuWidgetService` contienen múltiples caminos de estado/intent y lógica de negocio.
- **Impacto**: regresiones difíciles de depurar, alta fragilidad ante cambios.
- **Archivos**: `util/widget/MenuWidgetProvider.kt`, `util/widget/MenuWidgetService.kt`.

## Deuda técnica media

### R4) Regla horaria duplicada
- **CONFIRMADO POR CÓDIGO**: lógica 08:00-11:00 repetida en ViewModel, adapter y widget.
- **Impacto**: divergencias de comportamiento entre pantallas/canales.

### R5) Capa de negocio dispersa
- **CONFIRMADO POR CÓDIGO**: lógica de negocio fuerte en fragments/adapters/util.
- **Impacto**: baja mantenibilidad, testing difícil.

### R6) Solapamiento AlarmManager + WorkManager
- **CONFIRMADO POR CÓDIGO**: recordatorios diarios conviven por ambas vías.
- **Impacto**: posible duplicación, complejidad operativa y de soporte.

### R7) Modelo local inconsistente (`MenuSelection`)
- **CONFIRMADO POR CÓDIGO**: discrepancia `Long` vs `String`; uso no central.
- **Impacto**: confusión técnica y riesgo de errores futuros.

## Riesgos de producto/proceso

### R8) Cobertura de pruebas insuficiente
- **CONFIRMADO POR CÓDIGO**: solo tests de ejemplo.
- **Impacto**: alto riesgo de regresiones en auth, votos, widget, alarmas.

### R9) Documentación desactualizada
- **CONFIRMADO POR CÓDIGO**:
  - docs mencionan `versionCode=1`,
  - build usa `versionCode=6`.
- **Impacto**: decisiones incorrectas de release/operación.
- **Archivos**: `INSTRUCCIONES_PRODUCCION.md`, `PRODUCCION_CHECKLIST.md`, `app/build.gradle.kts`.

### R10) Gestión de secretos en repo
- **CONFIRMADO POR CÓDIGO**: existe `keystore.properties` versionado con credenciales de firma.
- **Impacto**: riesgo crítico de seguridad/rotación de claves.

## Código huérfano/sospechoso
- `MenuPollingWorker`: existencia confirmada, bootstrap no encontrado.
  - **Clasificación**: implementación **CONFIRMADA**, uso runtime **HIPÓTESIS DÉBIL / NO CONFIRMADA**.
- Bloque de ignore Django/Python en `.gitignore`.
  - **Clasificación**: **CONFIRMADO POR CÓDIGO**, causa exacta **INFERIDA POR ESTRUCTURA**.

## Priorización recomendada
1. Seguridad de sesión/secrets (`R2`, `R10`).
2. Contrato votos y simplificación (`R1`).
3. Estabilización widget y centralización de reglas horarias (`R3`, `R4`).
4. Plan mínimo de pruebas críticas (`R8`).
