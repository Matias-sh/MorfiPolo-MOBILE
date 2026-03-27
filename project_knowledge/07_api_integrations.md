# 07 - API Integrations

## Servicio externo principal
- **CONFIRMADO POR CÓDIGO**:
  - Base URL: `https://secytdi.formosa.gob.ar/morfi-polo/api/`
  - Archivo: `data/remote/RetrofitClient.kt`.

## Endpoints detectados
- `POST auth/login`
- `POST auth/refresh-token`
- `GET menus`
- `POST votes`
- `GET votes` (con query params `page`, `limit`, `user_id`, `userId`, `menu_id`)
- `DELETE votes/{voteId}`
- `PATCH user/change-password`
- **Fuente**: `data/remote/api/MorfiPoloApiService.kt`.

## Auth, headers y sesión
- **CONFIRMADO POR CÓDIGO**:
  - `Authorization: Bearer <token>` inyectado por `AuthInterceptor`.
  - `TokenManager` refresca token con umbral de expiración.
  - `AuthManager` distingue errores temporales vs expiración real.
  - sesión y tokens en SharedPreferences (`SessionManager`).

## Contratos request/response
- **CONFIRMADO POR CÓDIGO**:
  - login/refresh retornan `LoginResponse(accessToken, refreshToken, user)`.
  - menús y votos usan payload paginado `data + meta + links`.
  - voto crea `Vote`, borrar voto no retorna body relevante.

## Manejo de errores de integración
- Repositorios mappean códigos HTTP a mensajes amigables.
- 401 protegido suele derivar en `SessionExpiredException`.
- 5xx y errores de red se consideran temporales en auth.
- `VoteRepository` usa fallbacks y búsquedas paginadas para compensar filtrado inconsistente.

## Mocks y diferencias teoría vs práctica
- **CONFIRMADO POR CÓDIGO**:
  - no hay capa de mocks/fakes de API para runtime o tests.
- **CONFIRMADO POR CÓDIGO**:
  - `VoteRepository` implementa workaround explícito porque `GET /votes` no filtra de forma confiable por usuario.
  - se envían dos nombres de query param (`user_id` y `userId`) para compatibilidad.
- **INFERIDO POR ESTRUCTURA**:
  - el contrato backend es inestable o divergente entre ambientes/versiones.

## Persistencia e integración local-remota
- Menús: remoto como fuente primaria, Room como fallback/cache.
- Votos: remoto como fuente principal.
- Notificaciones custom y flags: local por SharedPreferences.

## Señales de riesgo de integración
- Dependencia fuerte de comportamientos no contractuales del endpoint de votos.
- Alto volumen de llamadas por paginación/filtrado local.
- Guardado de password local para soporte de refresh/flujo actual.
